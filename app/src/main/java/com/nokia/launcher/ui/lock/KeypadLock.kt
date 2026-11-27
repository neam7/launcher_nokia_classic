package com.nokia.launcher.ui.lock

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.WhileVisible
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Time allowed between the Menu key and `*`. */
const val UNLOCK_WINDOW_MS = 3_000L

/**
 * The classic two-key unlock: Menu, then `*`. It only guards against accidental presses;
 * security is the job of Android's own lock screen, which is asked for right after.
 */
fun unlocks(menuPressedAt: Long, key: Char, now: Long): Boolean =
    key == '*' && menuPressedAt > 0 && now - menuPressedAt in 0..UNLOCK_WINDOW_MS

@Composable
fun ColumnScope.LockedContent(armed: Boolean) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    WhileVisible(1_000) { now = LocalDateTime.now() }
    val ink = LocalPhoneColors.current.lcdText
    Spacer(Modifier.weight(1f))
    Text(
        DateTimeFormatter.ofPattern("HH:mm").format(now),
        Modifier.align(Alignment.CenterHorizontally),
        color = ink, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 52.sp,
    )
    Text("🔒", Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp), fontSize = 28.sp)
    LcdText(stringResource(R.string.locked_title), Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
    Spacer(Modifier.weight(1f))
    Text(
        stringResource(if (armed) R.string.locked_press_star else R.string.locked_hint),
        Modifier.fillMaxWidth(), color = ink, fontFamily = FontFamily.Monospace,
        fontSize = 14.sp, textAlign = TextAlign.Center,
    )
}

@Preview(widthDp = 380, heightDp = 440)
@Composable
private fun LockedPreview() {
    LauncherTheme { Lcd(emptyList()) { LockedContent(false) } }
}
