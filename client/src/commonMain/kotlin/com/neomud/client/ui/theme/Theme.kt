package com.neomud.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.neomud.client.platform.EmojiSafeFontFamily

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFA5D6A7),
    background = Color(0xFF12100E),   // StoneTheme.panelBg
    surface = Color(0xFF1A1510),      // StoneTheme.frameDark
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

// Typography that uses EmojiSafeFontFamily — on iOS this includes
// Apple Color Emoji so emoji characters render properly via Skia.
// On Android/Desktop this is FontFamily.Default (no-op).
private val EmojiTypography = Typography(
    displayLarge = TextStyle(fontFamily = EmojiSafeFontFamily),
    displayMedium = TextStyle(fontFamily = EmojiSafeFontFamily),
    displaySmall = TextStyle(fontFamily = EmojiSafeFontFamily),
    headlineLarge = TextStyle(fontFamily = EmojiSafeFontFamily),
    headlineMedium = TextStyle(fontFamily = EmojiSafeFontFamily),
    headlineSmall = TextStyle(fontFamily = EmojiSafeFontFamily),
    titleLarge = TextStyle(fontFamily = EmojiSafeFontFamily),
    titleMedium = TextStyle(fontFamily = EmojiSafeFontFamily),
    titleSmall = TextStyle(fontFamily = EmojiSafeFontFamily),
    bodyLarge = TextStyle(fontFamily = EmojiSafeFontFamily),
    bodyMedium = TextStyle(fontFamily = EmojiSafeFontFamily),
    bodySmall = TextStyle(fontFamily = EmojiSafeFontFamily),
    labelLarge = TextStyle(fontFamily = EmojiSafeFontFamily),
    labelMedium = TextStyle(fontFamily = EmojiSafeFontFamily),
    labelSmall = TextStyle(fontFamily = EmojiSafeFontFamily),
)

@Composable
fun NeoMudTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = EmojiTypography,
        content = content
    )
}
