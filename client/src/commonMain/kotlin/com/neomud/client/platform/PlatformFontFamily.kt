package com.neomud.client.platform

import androidx.compose.ui.text.font.FontFamily

/**
 * Returns a FontFamily that supports emoji rendering on all platforms.
 * On iOS, this includes Apple Color Emoji in the fallback chain since
 * Compose Multiplatform's Skia renderer doesn't include it by default.
 * On Android and Desktop, emoji render natively so this returns the default.
 */
expect val EmojiSafeFontFamily: FontFamily
