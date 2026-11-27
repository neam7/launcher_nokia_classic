package com.nokia.launcher.ui.home

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val BLINK_MS = 500L

/**
 * Text being written, with the blinking bar cursor of the later classic Nokias.
 * [pending] underlines the last letter while the same key can still change it.
 */
@Composable
fun CursorText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    pending: Boolean = false,
    fontSize: TextUnit = 16.sp,
    maxLines: Int = Int.MAX_VALUE,
) {
    var visible by remember { mutableStateOf(true) }
    // The cursor stays solid while writing and blinks when idle.
    var ticks by remember(text) { mutableIntStateOf(0) }
    WhileVisible(BLINK_MS, text) { visible = ticks++ % 2 == 0 }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        buildAnnotatedString {
            append(text)
            if (pending && text.isNotEmpty()) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), text.length - 1, text.length)
            }
        },
        modifier.drawWithContent {
            drawContent()
            val result = layout
            if (visible && result != null) {
                // The layout can be one frame behind the text, so stay inside it.
                val at = result.getCursorRect(minOf(text.length, result.layoutInput.text.length))
                drawRect(color, Offset(at.left, at.top), Size(2.dp.toPx(), at.height))
            }
        },
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        onTextLayout = { layout = it },
    )
}
