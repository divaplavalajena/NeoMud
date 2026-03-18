package com.neomud.client

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.neomud.client.audio.IosAudioManager
import com.neomud.client.platform.LocalIsLandscape
import com.neomud.client.platform.LocalSetLayoutPreference
import com.neomud.client.ui.navigation.NeoMudApp
import com.neomud.client.ui.theme.NeoMudTheme
import com.neomud.client.viewmodel.AuthViewModel
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        val authViewModel = remember { AuthViewModel() }
        val audioManager = remember { IosAudioManager() }

        NeoMudTheme {
            // Derive landscape from actual screen dimensions — adapts when device rotates
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight

                CompositionLocalProvider(
                    LocalIsLandscape provides isLandscape,
                    LocalSetLayoutPreference provides { /* On iOS, rotate the device to change layout */ }
                ) {
                    NeoMudApp(
                        authViewModel = authViewModel,
                        audioManager = audioManager
                    )
                }
            }
        }
    }
}
