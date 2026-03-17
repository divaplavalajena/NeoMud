package com.neomud.client.audio

import com.neomud.client.platform.PlatformLogger
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.*
import kotlin.concurrent.withLock
import kotlin.math.log10

/**
 * BGM player using JLayer for MP3 decoding and javax.sound.sampled for PCM output.
 * Streams decoded audio on a daemon thread with looping support.
 */
class JLayerBgmPlayer {
    private val tag = "JLayerBgmPlayer"
    private val lock = ReentrantLock()

    @Volatile
    private var playing = false
    private var playbackThread: Thread? = null
    private var currentLine: SourceDataLine? = null
    private var volume: Float = 1f

    fun play(uri: String, volume: Float) {
        lock.withLock {
            stopInternal()
            this.volume = volume
            playing = true

            val thread = Thread({
                try {
                    val mp3Bytes = downloadMp3(uri)
                    if (mp3Bytes == null || !playing) return@Thread

                    // Loop forever until stopped
                    while (playing) {
                        val input = ByteArrayInputStream(mp3Bytes)
                        val bitstream = Bitstream(input)
                        val decoder = Decoder()

                        var lineInitialized = false
                        var line: SourceDataLine? = null

                        try {
                            while (playing) {
                                val header = bitstream.readFrame() ?: break
                                val output = decoder.decodeFrame(header, bitstream) as SampleBuffer
                                bitstream.closeFrame()

                                if (!lineInitialized) {
                                    val format = AudioFormat(
                                        output.sampleFrequency.toFloat(),
                                        16,
                                        output.channelCount,
                                        true,
                                        false
                                    )
                                    line = AudioSystem.getSourceDataLine(format)
                                    line.open(format, 8192)
                                    applyVolume(line)
                                    line.start()
                                    currentLine = line
                                    lineInitialized = true
                                }

                                val pcm = output.buffer
                                val len = output.bufferLength * 2 // 16-bit samples = 2 bytes each
                                val bytes = ByteArray(len)
                                for (i in 0 until output.bufferLength) {
                                    val sample = pcm[i]
                                    bytes[i * 2] = (sample.toInt() and 0xFF).toByte()
                                    bytes[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
                                }
                                line?.write(bytes, 0, len)
                            }
                        } finally {
                            try { bitstream.close() } catch (_: Exception) {}
                            // Don't close line between loops — only on stop
                            if (!playing) {
                                line?.drain()
                                line?.close()
                                currentLine = null
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    // Expected on stop
                } catch (e: Exception) {
                    if (playing) {
                        PlatformLogger.w(tag, "BGM playback error: ${e.message}")
                    }
                }
            }, "bgm-player")
            thread.isDaemon = true
            thread.start()
            playbackThread = thread
        }
    }

    fun stop() {
        lock.withLock {
            stopInternal()
        }
    }

    fun setVolume(linear: Float) {
        this.volume = linear
        currentLine?.let { applyVolume(it) }
    }

    private fun stopInternal() {
        playing = false
        playbackThread?.let { thread ->
            try {
                currentLine?.stop()
                currentLine?.close()
            } catch (_: Exception) {}
            currentLine = null
            thread.interrupt()
            try { thread.join(500) } catch (_: Exception) {}
        }
        playbackThread = null
    }

    private fun applyVolume(line: SourceDataLine) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val control = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val db = if (volume <= 0f) {
                    control.minimum
                } else {
                    (20.0 * log10(volume.toDouble())).toFloat()
                        .coerceIn(control.minimum, control.maximum)
                }
                control.value = db
            }
        } catch (_: Exception) {}
    }

    private fun downloadMp3(uri: String): ByteArray? {
        return try {
            URI(uri).toURL().openStream().use { it.readBytes() }
        } catch (e: Exception) {
            PlatformLogger.w(tag, "Failed to download BGM '$uri': ${e.message}")
            null
        }
    }
}
