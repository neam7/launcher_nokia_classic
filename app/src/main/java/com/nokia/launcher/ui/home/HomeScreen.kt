package com.nokia.launcher.ui.home

import com.nokia.launcher.data.Notice
import androidx.compose.ui.unit.min
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokia.launcher.R
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import com.nokia.launcher.ui.theme.PhoneThemes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

/**
 * Screen and keypad go side by side in wide windows, and also in large almost square ones, such as
 * an unfolded phone: stacked, they would leave wide empty margins. Sizes are in dp.
 */
internal fun sideBySide(width: Float, height: Float): Boolean =
    width > height || width >= 600f && width > height * 0.75f

/** The phone shell. Its layout depends only on the window size. */
@Composable
fun PhoneFrame(
    keyboard: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    name: String = "",
    showName: Boolean = true,
    keyboardWeight: Float = 1.1f,
    screen: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val wide = sideBySide(maxWidth.value, maxHeight.value)
        if (wide) {
            // Two halves, but in long windows the keypad stops growing, to keep the shape of its
            // keys, and the screen takes the rest. Nothing is left empty at the sides.
            val keyboardWidth = min(maxWidth / 2 - 20.dp, maxHeight * 0.8f)
            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (showName) Wordmark(name)
                    screen(Modifier.weight(1f).fillMaxWidth())
                }
                keyboard(Modifier.width(keyboardWidth).fillMaxHeight())
            }
        } else {
            Column(
                Modifier.widthIn(max = maxHeight * 0.65f).fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showName) Wordmark(name)
                screen(Modifier.weight(1f).fillMaxWidth())
                keyboard(Modifier.weight(keyboardWeight).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ColumnScope.Wordmark(name: String) {
    Text(
        name.ifEmpty { stringResource(R.string.brand) },
        Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp),
        color = LocalPhoneColors.current.keyText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
    )
}

/** The header of the classic menus: centered ink and a line under it. */
@Composable
fun TitleBar(title: String, detail: String = "") {
    val ink = LocalPhoneColors.current.lcdText
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)) {
            LcdText(
                title, Modifier.align(Alignment.Center).padding(horizontal = if (detail.isEmpty()) 0.dp else 56.dp),
                ink,
            )
            LcdText(detail, Modifier.align(Alignment.CenterEnd), ink, small = true)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(ink))
    }
}

/** The LCD: content plus the three soft-key labels. */
@Composable
fun Lcd(
    softLabels: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
    background: Color = LocalPhoneColors.current.lcd,
    textColor: Color = LocalPhoneColors.current.lcdText,
    padding: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(background).padding(padding)) {
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        Row(Modifier.fillMaxWidth()) {
            softLabels.forEachIndexed { index, (label, onClick) ->
                Text(
                    label,
                    Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp),
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    textAlign = listOf(TextAlign.Start, TextAlign.Center, TextAlign.End)[index],
                )
            }
        }
    }
}

@Composable
fun ColumnScope.HomeContent(
    dialed: String,
    notices: List<Notice> = emptyList(),
    onNotice: (Notice) -> Unit = {},
    onDismissNotice: (Notice) -> Unit = {},
    onClearNotices: () -> Unit = {},
    onStatusClick: () -> Unit = {},
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    WhileVisible(1_000) { now = LocalDateTime.now() }
    val colors = LocalPhoneColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE, dd MMM", locale) }
    StatusBar(rememberPhoneStatus(), onStatusClick)
    // While dialing, the number takes the whole middle.
    if (dialed.isEmpty() && notices.isNotEmpty()) {
        LcdText(dateFormat.format(now), Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
        NoticeList(notices, onNotice, onDismissNotice, Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            LcdText(stringResource(R.string.notices_count, notices.size), small = true)
            if (notices.any { it.clearable }) {
                LcdText(
                    stringResource(R.string.notices_clear),
                    Modifier.clickable(onClick = onClearNotices).padding(horizontal = 4.dp),
                )
            }
        }
    } else {
        Spacer(Modifier.weight(1f))
        LcdText(
            dialed.ifEmpty { dateFormat.format(now) },
            Modifier.align(Alignment.CenterHorizontally),
            maxLines = 2,
        )
        Spacer(Modifier.weight(1f))
    }
    Box(Modifier.fillMaxWidth().height(2.dp).background(colors.lcdText))
    Text(
        timeFormat.format(now),
        Modifier.align(Alignment.End)
            .background(colors.lcdText, CutCornerShape(bottomStartPercent = 100))
            .padding(start = 40.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        color = colors.lcd,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    )
}

@Composable
fun LcdText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPhoneColors.current.lcdText,
    maxLines: Int = 1,
    small: Boolean = false,
) {
    Text(
        text,
        modifier,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (small) FontWeight.Normal else FontWeight.Bold,
        fontSize = if (small) 13.sp else 16.sp,
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.StartEllipsis else TextOverflow.Clip,
    )
}

@Preview(name = "Fold cover", widthDp = 411, heightDp = 960)
@Preview(name = "Fold inner portrait", widthDp = 750, heightDp = 832)
@Preview(name = "Fold inner landscape", widthDp = 832, heightDp = 750)
@Preview(name = "Phone landscape", widthDp = 891, heightDp = 411)
@Composable
private fun HomePreview() {
    LauncherTheme {
        PhoneFrame({ Keypad({}, {}, it) }, Modifier.background(PhoneThemes[0].body)) { modifier ->
            Lcd(listOf("Go to" to {}, "Menu" to {}, "Setting" to {}), modifier) { HomeContent("") }
        }
    }
}
