package com.neomud.client.audio

import com.neomud.client.platform.PlatformAudioManager
import com.neomud.client.platform.PlatformLogger
import kotlinx.coroutines.*

class DesktopAudioManager : PlatformAudioManager {
    private val tag = "DesktopAudioManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bgmPlayer = JLayerBgmPlayer()
    private val sfxPlayer = JLayerSfxPlayer()

    private var currentBgmTrack: String? = null
    private val sfxLoading = mutableSetOf<String>()

    override var masterVolume: Float = 1f
        private set
    override var sfxVolume: Float = 1f
        private set
    override var bgmVolume: Float = 0.5f
        private set

    init {
        val prefs = java.util.prefs.Preferences.userNodeForPackage(DesktopAudioManager::class.java)
        masterVolume = prefs.getFloat("volume_master", 1f)
        sfxVolume = prefs.getFloat("volume_sfx", 1f)
        bgmVolume = prefs.getFloat("volume_bgm", 0.5f)
    }

    override fun playSfx(serverBaseUrl: String, soundId: String, category: String) {
        if (soundId.isBlank() || masterVolume == 0f || sfxVolume == 0f) return

        val cacheKey = "$category/$soundId"
        if (sfxPlayer.isCached(cacheKey)) {
            val vol = masterVolume * sfxVolume
            sfxPlayer.play(cacheKey, vol)
            return
        }

        if (cacheKey in sfxLoading) return
        sfxLoading.add(cacheKey)

        scope.launch {
            try {
                val url = "$serverBaseUrl/assets/audio/$category/$soundId.mp3"
                if (sfxPlayer.loadAndCache(cacheKey, url)) {
                    val vol = masterVolume * sfxVolume
                    sfxPlayer.play(cacheKey, vol)
                }
            } catch (e: Exception) {
                PlatformLogger.w(tag, "Failed to load SFX '$category/$soundId': ${e.message}")
            } finally {
                sfxLoading.remove(cacheKey)
            }
        }
    }

    override fun playBgm(serverBaseUrl: String, trackId: String) {
        playBgmFromUri("$serverBaseUrl/assets/audio/bgm/$trackId.mp3", trackId)
    }

    override fun playBgmFromUri(uri: String, trackId: String) {
        if (trackId == currentBgmTrack) return
        if (trackId.isBlank()) {
            stopBgm()
            return
        }

        stopBgm()
        currentBgmTrack = trackId

        val vol = masterVolume * bgmVolume
        bgmPlayer.play(uri, vol)
    }

    override fun stopBgm() {
        bgmPlayer.stop()
        currentBgmTrack = null
    }

    override fun setVolumes(master: Float, sfx: Float, bgm: Float) {
        masterVolume = master.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        bgmVolume = bgm.coerceIn(0f, 1f)

        // Update BGM volume immediately
        bgmPlayer.setVolume(masterVolume * bgmVolume)

        // Persist
        val prefs = java.util.prefs.Preferences.userNodeForPackage(DesktopAudioManager::class.java)
        prefs.putFloat("volume_master", masterVolume)
        prefs.putFloat("volume_sfx", sfxVolume)
        prefs.putFloat("volume_bgm", bgmVolume)
        prefs.flush()
    }

    override fun release() {
        scope.cancel()
        stopBgm()
        sfxPlayer.clearCache()
    }
}
