package com.nokia.launcher.ui.radio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.home.LcdText
import com.nokia.launcher.ui.home.TitleBar
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Plays what [fill] writes, starting at the given sample, until cancelled. */
internal suspend fun play(fill: (ShortArray, Long) -> Unit) = withContext(Dispatchers.Default) {
    val format = AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
    // The device may refuse a new track; the radio then stays silent.
    val track = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(format).setBufferSizeInBytes(8192).build()
            .also { it.play() }
    }.getOrNull() ?: return@withContext
    try {
        val buffer = ShortArray(1024)
        var position = 0L
        while (isActive) {
            fill(buffer, position)
            position += buffer.size
            if (track.write(buffer, 0, buffer.size) < 0) break
        }
    } finally {
        track.release()
    }
}

/** A simulated FM tuner: no antenna or network, only synthesized sound. */
@Composable
fun ColumnScope.RadioContent(frequency: Int, on: Boolean) {
    val current by rememberUpdatedState(frequency)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(on, lifecycle) {
        if (on) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                play { out, position -> synthesize(out, position, current, Random) }
            }
        }
    }
    val colors = LocalPhoneColors.current
    val station = bestStation(frequency)
    TitleBar(stringResource(R.string.radio), stringResource(if (on) R.string.on else R.string.off))
    Text(
        "%.1f FM".format(frequency / 10f),
        Modifier.align(Alignment.CenterHorizontally).weight(1f),
        color = colors.lcdText,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
    )
    LcdText(
        if (on) station?.name ?: stringResource(R.string.radio_static) else stringResource(R.string.radio_press),
        Modifier.align(Alignment.CenterHorizontally),
    )
    // The dial, with a tick per MHz and the needle on the tuned frequency.
    Canvas(Modifier.fillMaxWidth().height(46.dp)) {
        fun x(f: Int) = size.width * (f - FM_MIN) / (FM_MAX - FM_MIN)
        for (f in 880..1080 step 10) {
            val tall = if (f % 50 == 0) 0.5f else 0.28f
            drawLine(
                colors.lcdText, Offset(x(f), size.height * (1 - tall)), Offset(x(f), size.height), 2f,
            )
        }
        STATIONS.forEach { drawCircle(colors.lcdText, 3.dp.toPx(), Offset(x(it.frequency), size.height * 0.2f)) }
        drawRect(
            colors.lcdText, Offset(x(frequency) - 2.dp.toPx(), 0f), Size(4.dp.toPx(), size.height),
        )
    }
    LcdText(
        stringResource(R.string.radio_help), Modifier.align(Alignment.CenterHorizontally),
        maxLines = 2, small = true,
    )
}

@Preview(widthDp = 380, heightDp = 400)
@Composable
private fun RadioPreview() {
    LauncherTheme { Lcd(emptyList()) { RadioContent(945, false) } }
}
