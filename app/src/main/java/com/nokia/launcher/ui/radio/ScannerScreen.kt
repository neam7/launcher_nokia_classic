package com.nokia.launcher.ui.radio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

private const val BARS = 24

/** The simulated scanner. It runs and sounds only while on and in front. */
@Composable
fun ColumnScope.ScannerContent(scan: Scan, on: Boolean, onTick: () -> Unit) {
    val current by rememberUpdatedState(scan)
    val tick by rememberUpdatedState(onTick)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(on, lifecycle) {
        if (on) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch { play { out, position -> scanAudio(out, position, current, Random) } }
                while (true) {
                    delay(SCAN_TICK_MS)
                    tick()
                }
            }
        }
    }
    val ink = LocalPhoneColors.current.lcdText
    TitleBar(stringResource(R.string.scanner), stringResource(if (on) R.string.on else R.string.off))
    Text(
        "CH %03d".format(scan.channel),
        Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        color = ink, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 40.sp,
    )
    LcdText("%.2f MHz".format(scan.megahertz), Modifier.align(Alignment.CenterHorizontally))
    LcdText(
        stringResource(
            when {
                !on -> R.string.scanner_press
                scan.signal -> R.string.scanner_signal
                scan.held -> R.string.scanner_held
                else -> R.string.scanner_searching
            },
        ),
        Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
    )
    // The received level: low noise while searching, tall moving bars on a signal.
    Canvas(Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)) {
        val slot = size.width / BARS
        for (i in 0 until BARS) {
            val wave = (sin(i * 1.7f + scan.talk * 0.9f + scan.channel) + 1) / 2
            val level = when {
                !on -> 0.03f
                scan.signal -> 0.25f + 0.75f * wave
                else -> 0.05f + 0.12f * wave
            }
            drawRect(ink, Offset(i * slot + slot * 0.2f, size.height * (1 - level)), Size(slot * 0.6f, size.height * level))
        }
    }
    LcdText(
        stringResource(R.string.scanner_notice), Modifier.align(Alignment.CenterHorizontally),
        maxLines = 3, small = true,
    )
}

@Preview(widthDp = 380, heightDp = 440)
@Composable
private fun ScannerPreview() {
    LauncherTheme { Lcd(emptyList()) { ScannerContent(Scan(channel = 342, talk = 20), true) {} } }
}
