package com.nokia.launcher.ui.settings

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nokia.launcher.ui.home.CursorText
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import androidx.compose.foundation.layout.Row as Line

data class Row(
    val label: String,
    val detail: String? = null,
    /** A color sample shown at the end of the row, used by the theme list. */
    val swatch: Color? = null,
    val onClick: () -> Unit,
)

/** A numbered Nokia list: tap a row or press its number. */
@Composable
fun ColumnScope.ListContent(title: String, rows: List<Row>) {
    TitleBar(title, rows.size.toString())
    ListRows(rows)
}

/** Plain lines as on the classic phones; the row under the finger is inverted. */
@Composable
fun ColumnScope.ListRows(rows: List<Row>) {
    val colors = LocalPhoneColors.current
    LazyColumn(Modifier.weight(1f)) {
        itemsIndexed(rows) { index, row ->
            val touch = remember { MutableInteractionSource() }
            val pressed by touch.collectIsPressedAsState()
            val ink = if (pressed) colors.lcd else colors.lcdText
            Column(
                Modifier.fillMaxWidth().background(if (pressed) colors.lcdText else Color.Transparent)
                    .clickable(touch, indication = null, onClick = row.onClick),
            ) {
                Line(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LcdText("${index + 1}", Modifier.width(26.dp), ink)
                    Column(Modifier.weight(1f)) {
                        LcdText(row.label, color = ink, maxLines = 2)
                        row.detail?.let { LcdText(it, color = ink, maxLines = 2, small = true) }
                    }
                    row.swatch?.let { Box(Modifier.size(22.dp).background(it).border(2.dp, ink)) }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.lcdText.copy(alpha = 0.25f)))
            }
        }
    }
}

/** Text written with the phone keyboard. */
@Composable
fun ColumnScope.EditorContent(
    title: String,
    text: String,
    hint: String,
    mode: String,
    counter: String,
    pending: Boolean = false,
) {
    // Writing mode on the left and what is left on the right, as on Series 40.
    Line(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LcdText(if (mode.isEmpty()) "✎" else "✎ $mode", small = true)
        LcdText(counter, small = true)
    }
    TitleBar(title)
    val scroll = rememberScrollState()
    // Keeps the cursor in sight while writing.
    LaunchedEffect(text, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    CursorText(
        text,
        LocalPhoneColors.current.lcdText,
        Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(vertical = 8.dp),
        pending = pending,
    )
    LcdText(hint, maxLines = 2, small = true)
}

@Preview(widthDp = 411, heightDp = 420)
@Composable
private fun ListPreview() {
    LauncherTheme {
        Lcd(emptyList()) { ListContent("Language", listOf(Row("English") {}, Row("Español", "es") {})) }
    }
}

@Preview(widthDp = 411, heightDp = 420)
@Composable
private fun EditorPreview() {
    LauncherTheme { Lcd(emptyList()) { EditorContent("Message", "Hola", "# mode", "abc", "156/1", pending = true) } }
}
