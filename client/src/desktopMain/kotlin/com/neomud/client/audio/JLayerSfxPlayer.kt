package com.neomud.client.audio

import com.neomud.client.platform.PlatformLogger
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import java.io.ByteArrayInputStream
import java.net.URI
import javax.sound.sampled.*
import kotlin.math.log10

/**
 * SFX player using JLayer for MP3 decoding and javax.sound.sampled for playback.
 * Decodes MP3s to PCM on load and caches them for instant fire-and-forget playback.
 */
class JLayerSfxPlayer {
    private val tag = "JLayerSfxPlayer"

    /**
     * Cached decoded SFX: PCM byte data + the format it was decoded to.
     */
    private data class CachedSfx(val pcmData: ByteArray, val format: AudioFormat)

    private val cache = mutableMapOf<String, CachedSfx>()

    fun getCachedKeys(): Set<String> = cache.keys

    fun isCached(key: String): Boolean = key in cache

    /**
     * Decode an MP3 from a URL and cache the PCM result.
     * Returns true if successfully cached.
     */
    fun loadAndCache(key: String, url: String): Boolean {
        return try {
            val mp3Bytes = URI(url).toURL().openStream().use { it.readBytes() }
            val cached = decodeMp3(mp3Bytes) ?: return false
            cache[key] = cached
            true
        } catch (e: Exception) {
            PlatformLogger.w(tag, "Failed to load SFX '$key': ${e.message}")
            false
        }
    }

    /**
     * Play a cached SFX at the given volume (0.0 - 1.0). Fire-and-forget.
     */
    fun play(key: String, volume: Float) {
        val cached = cache[key] ?: return

        try {
            val clip = AudioSystem.getClip()
            clip.open(cached.format, cached.pcmData, 0, cached.pcmData.size)

            // Set volume
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val control = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val db = if (volume <= 0f) {
                    control.minimum
                } else {
                    (20.0 * log10(volume.toDouble())).toFloat()
                        .coerceIn(control.minimum, control.maximum)
                }
                control.value = db
            }

            // Auto-close when done
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    clip.close()
                }
            }

            clip.start()
        } catch (e: Exception) {
            PlatformLogger.w(tag, "Failed to play SFX '$key': ${e.message}")
        }
    }

    fun clearCache() {
        cache.clear()
    }

    /**
     * Decode an MP3 byte array to PCM using JLayer.
     */
    private fun decodeMp3(mp3Bytes: ByteArray): CachedSfx? {
        val input = ByteArrayInputStream(mp3Bytes)
        val bitstream = Bitstream(input)
        val decoder = Decoder()

        val allPcm = mutableListOf<ByteArray>()
        var sampleRate = 0f
        var channels = 0

        try {
            while (true) {
                val header = bitstream.readFrame() ?: break
                val output = decoder.decodeFrame(header, bitstream) as SampleBuffer
                bitstream.closeFrame()

                if (sampleRate == 0f) {
                    sampleRate = output.sampleFrequency.toFloat()
                    channels = output.channelCount
                }

                val pcm = output.buffer
                val len = output.bufferLength * 2
                val bytes = ByteArray(len)
                for (i in 0 until output.bufferLength) {
                    val sample = pcm[i]
                    bytes[i * 2] = (sample.toInt() and 0xFF).toByte()
                    bytes[i * 2 + 1] = (sample.toInt() shr 8 and 0xFF).toByte()
                }
                allPcm.add(bytes)
            }
        } finally {
            try { bitstream.close() } catch (_: Exception) {}
        }

        if (allPcm.isEmpty() || sampleRate == 0f) return null

        val totalSize = allPcm.sumOf { it.size }
        val combined = ByteArray(totalSize)
        var offset = 0
        for (chunk in allPcm) {
            chunk.copyInto(combined, offset)
            offset += chunk.size
        }

        val format = AudioFormat(sampleRate, 16, channels, true, false)
        return CachedSfx(combined, format)
    }
}
