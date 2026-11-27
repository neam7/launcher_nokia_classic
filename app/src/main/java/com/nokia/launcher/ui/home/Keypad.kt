package com.nokia.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.nokia.launcher.R
import com.nokia.launcher.ui.theme.CallKey
import com.nokia.launcher.ui.theme.EndKey
import com.nokia.launcher.ui.theme.LocalPhoneColors

enum class PhoneKey { SoftLeft, Center, SoftRight, Call, End }

private val digitRows = listOf(
    listOf('1' to ".,", '2' to "abc", '3' to "def"),
    listOf('4' to "ghi", '5' to "jkl", '6' to "mno"),
    listOf('7' to "pqrs", '8' to "tuv", '9' to "wxyz"),
    listOf('*' to "+", '0' to "␣", '#' to "aA"),
)

@Composable
fun Keypad(
    onKey: (PhoneKey) -> Unit,
    onChar: (Char) -> Unit,
    modifier: Modifier = Modifier,
    onLongKey: (PhoneKey) -> Unit = {},
    onLongChar: (Char) -> Unit = {},
    /** Written on the left, center and right keys when the screen shows no soft labels. */
    navLabels: List<String>? = null,
    /** Replaces "CALL" on the green key, for screens that give it another use. */
    callLabel: String? = null,
) {
    BoxWithConstraints(modifier) {
        // 2 navigation rows + 4 digit rows; text follows the key size so nothing clips.
        val textSize = min(maxHeight / 6 * 0.45f, maxWidth / 3 * 0.28f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            NavKeys(onKey, onLongKey, textSize, Modifier.weight(2f).fillMaxWidth(), navLabels, callLabel)
            digitRows.forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (char, letters) ->
                        DigitKey(char, letters, textSize, { onLongChar(char) }) { onChar(char) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NavKeys(
    onKey: (PhoneKey) -> Unit,
    onLongKey: (PhoneKey) -> Unit,
    textSize: Dp,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
    callLabel: String? = null,
) {
    val keyText = LocalPhoneColors.current.keyText
    fun side(index: Int) = labels?.get(index)?.ifEmpty { null } ?: "—"
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val half = Modifier.weight(1f).fillMaxWidth()
            Key(side(0), labels?.get(0)?.ifEmpty { null } ?: stringResource(R.string.key_soft_left), textSize, half) {
                onKey(PhoneKey.SoftLeft)
            }
            Key(
                callLabel ?: stringResource(R.string.key_call_label),
                callLabel ?: stringResource(R.string.key_call),
                textSize, half, if (callLabel == null) CallKey else keyText,
            ) {
                onKey(PhoneKey.Call)
            }
        }
        // One shape for face and ring: a second outline would show as a box over a box.
        val round = RoundedCornerShape(24.dp)
        val center = labels?.get(1).orEmpty()
        Key(
            center, center.ifEmpty { stringResource(R.string.key_center) }, textSize,
            Modifier.weight(1f).fillMaxSize().padding(vertical = 1.dp), shape = round,
            ring = keyText.copy(alpha = 0.55f),
        ) { onKey(PhoneKey.Center) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val half = Modifier.weight(1f).fillMaxWidth()
            Key(
                side(2), labels?.get(2)?.ifEmpty { null } ?: stringResource(R.string.key_soft_right), textSize, half,
                onLongClick = { onLongKey(PhoneKey.SoftRight) },
            ) { onKey(PhoneKey.SoftRight) }
            Key(stringResource(R.string.key_end_label), stringResource(R.string.key_end), textSize, half, EndKey) {
                onKey(PhoneKey.End)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.DigitKey(
    char: Char,
    letters: String,
    textSize: Dp,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalPhoneColors.current
    val size = with(LocalDensity.current) { textSize.toSp() }
    Box(
        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(12.dp)).background(colors.key)
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = size, fontWeight = FontWeight.Bold)) { append(char) }
                withStyle(SpanStyle(fontSize = size * 0.65f)) { append(" $letters") }
            },
            color = colors.keyText,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Key(
    label: String,
    description: String,
    textSize: Dp,
    modifier: Modifier,
    color: Color = LocalPhoneColors.current.keyText,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    ring: Color? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier.clip(shape).background(LocalPhoneColors.current.key)
            .then(if (ring == null) Modifier else Modifier.border(2.dp, ring, shape))
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val size = with(LocalDensity.current) { (textSize * 0.8f).toSp() }
        Text(label, color = color, fontSize = size, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
