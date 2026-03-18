package com.neomud.client.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.neomud.client.platform.EmojiSafeFontFamily

/**
 * Text composable that uses EmojiSafeFontFamily for rendering.
 * On iOS this includes Apple Color Emoji so emoji characters render properly.
 * On Android/Desktop this is identical to regular Text (FontFamily.Default).
 *
 * Use this instead of Text() for any string containing emoji characters.
 */
@Composable
fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = EmojiSafeFontFamily,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines
    )
}
