package com.nokia.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.nokia.launcher.R
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors

private val letterRows = listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm")
private val symbolRows = listOf("1234567890", "@#$%&*-+()", "!\"':;/?")

/** Holding a vowel writes it with its accent. */
private val accented = mapOf('a' to 'á', 'e' to 'é', 'i' to 'í', 'o' to 'ó', 'u' to 'ú')

private enum class Shift { Off, Once, Locked }

@Composable
fun QwertyKeyboard(
    onKey: (PhoneKey) -> Unit,
    onText: (Char) -> Unit,
    onDelete: () -> Unit,
    onClearAll: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
    onLongKey: (PhoneKey) -> Unit = {},
    navLabels: List<String>? = null,
    callLabel: String? = null,
) {
    var symbols by rememberSaveable { mutableStateOf(false) }
    var shift by rememberSaveable { mutableStateOf(Shift.Off) }
    fun type(char: Char) {
        onText(if (shift == Shift.Off) char else char.uppercaseChar())
        if (shift == Shift.Once) shift = Shift.Off
    }
    BoxWithConstraints(modifier) {
        val textSize = min(maxHeight / 6 * 0.45f, maxWidth / 10 * 0.6f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NavKeys(onKey, onLongKey, textSize * 1.2f, Modifier.weight(2f).fillMaxWidth(), navLabels, callLabel)
            val rows = if (symbols) symbolRows else letterRows
            rows.forEachIndexed { index, row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val key = Modifier.weight(1f).fillMaxHeight()
                    val wide = Modifier.weight(1.5f).fillMaxHeight()
                    if (index == 2) {
                        val label = when {
                            symbols -> "_"
                            shift == Shift.Locked -> "⇪"
                            else -> "⇧"
                        }
                        // A frame in the accent color shows that shift is on.
                        val active = if (shift == Shift.Off) wide else wide.background(
                            LocalPhoneColors.current.accent, RoundedCornerShape(12.dp),
                        ).padding(2.dp)
                        Key(label, stringResource(R.string.key_shift), textSize, active) {
                            if (symbols) onText('_')
                            else shift = Shift.entries[(shift.ordinal + 1) % Shift.entries.size]
                        }
                    }
                    row.forEach { char ->
                        val shown = if (shift == Shift.Off) char else char.uppercaseChar()
                        Key(
                            shown.toString(), shown.toString(), textSize, key,
                            onLongClick = accented[char]?.let { accent -> { type(accent) } },
                        ) { type(char) }
                    }
                    if (index == 2) {
                        Key(
                            "⌫", stringResource(R.string.key_delete), textSize, wide,
                            onLongClick = onClearAll, onClick = onDelete,
                        )
                    }
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                fun cell(weight: Float) = Modifier.weight(weight).fillMaxHeight()
                val layer = if (symbols) "abc" else "123"
                Key(layer, layer, textSize, cell(2f)) { symbols = !symbols }
                Key(",", ",", textSize, cell(1f)) { onText(',') }
                Key("␣", stringResource(R.string.key_space), textSize, cell(4f)) { onText(' ') }
                Key(".", ".", textSize, cell(1f)) { onText('.') }
                Key("↵", stringResource(R.string.key_enter), textSize, cell(2f), onClick = onEnter)
            }
        }
    }
}

@Preview(widthDp = 411, heightDp = 460)
@Composable
private fun QwertyPreview() {
    LauncherTheme { QwertyKeyboard({}, {}, {}, {}, {}) }
}
