package com.neomud.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Returns a FontFamily that supports emoji rendering on all platforms.
 * On iOS, this loads a bundled NotoColorEmoji font (CBDT format, Skia-compatible).
 * On Android and Desktop, emoji render natively so this returns the default.
 */
@Composable
expect fun rememberEmojiFontFamily(): FontFamily
