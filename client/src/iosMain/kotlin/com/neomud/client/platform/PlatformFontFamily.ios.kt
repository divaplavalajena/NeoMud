package com.neomud.client.platform

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle as SkFontStyle

actual val EmojiSafeFontFamily: FontFamily = createEmojiFontFamily()

private fun createEmojiFontFamily(): FontFamily {
    return try {
        val skTypeface = FontMgr.default
            .matchFamilyStyle("Apple Color Emoji", SkFontStyle.NORMAL)
            ?: return FontFamily.Default

        FontFamily(Typeface(skTypeface))
    } catch (e: Exception) {
        FontFamily.Default
    }
}
