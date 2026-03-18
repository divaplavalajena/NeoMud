package com.neomud.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import neomud.client.generated.resources.NotoColorEmoji
import neomud.client.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
actual fun rememberEmojiFontFamily(): FontFamily {
    val font = Font(Res.font.NotoColorEmoji)
    return remember(font) { FontFamily(font) }
}
