package com.nokia.launcher.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nokia.launcher.R
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors

/** One SIM slot: `level` 0..4, or null when there is no signal to show. */
data class SimSignal(val present: Boolean, val level: Int?)

data class PhoneStatus(
    val sims: List<SimSignal> = emptyList(),
    val airplane: Boolean = false,
    val wifiOn: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetooth: Boolean = false,
    val location: Boolean = false,
    val ringer: Int = AudioManager.RINGER_MODE_NORMAL,
    val doNotDisturb: Boolean = false,
    val alarm: Boolean = false,
    val headset: Boolean = false,
    val battery: Int = 0,
    val charging: Boolean = false,
)

fun hasPhoneStatePermission(context: Context) =
    context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun readSims(context: Context): List<SimSignal> {
    val telephony = context.getSystemService(TelephonyManager::class.java) ?: return emptyList()
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return emptyList()
    fun level(manager: TelephonyManager) = runCatching { manager.signalStrength?.level }.getOrNull()
    // Per-SIM readings need the optional phone permission; without it only the main SIM is shown.
    if (hasPhoneStatePermission(context)) {
        val active = runCatching {
            context.getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList
        }.getOrNull().orEmpty().sortedBy { it.simSlotIndex }
        if (active.isNotEmpty()) {
            return active.map { SimSignal(true, level(telephony.createForSubscriptionId(it.subscriptionId))) }
        }
    }
    val present = telephony.simState != TelephonyManager.SIM_STATE_ABSENT
    return listOf(SimSignal(present, if (present) level(telephony) else null))
}

fun readStatus(context: Context): PhoneStatus {
    val resolver = context.contentResolver
    fun flag(name: String) = Settings.Global.getInt(resolver, name, 0) != 0
    val airplane = flag(Settings.Global.AIRPLANE_MODE_ON)
    val audio = context.getSystemService(AudioManager::class.java)
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val network = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
    val battery = context.getSystemService(BatteryManager::class.java)
    val headsets = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
    )
    return PhoneStatus(
        sims = readSims(context),
        airplane = airplane,
        wifiOn = flag("wifi_on"),
        wifiConnected = network?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
        bluetooth = flag("bluetooth_on"),
        location = context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true,
        ringer = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
        doNotDisturb = (context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
            ?: 0) > NotificationManager.INTERRUPTION_FILTER_ALL,
        alarm = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock != null,
        headset = audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.any { it.type in headsets } == true,
        battery = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0,
        charging = battery?.isCharging == true,
    )
}

@Composable
fun rememberPhoneStatus(): PhoneStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PhoneStatus()) }
    WhileVisible(2_000) { status = readStatus(context) }
    return status
}

// Pixel-art icons, drawn like the ones on a monochrome LCD.
private val WIFI = listOf(".#####.", "#.....#", "..###..", ".#...#.", "...#...", "..###..", "...#...")
private val BLUETOOTH = listOf("..##...", "#.#.#..", ".###...", "..#....", ".###...", "#.#.#..", "..##...")
private val AIRPLANE = listOf("...#...", "...#...", ".#####.", "#######", "...#...", "...#...", "..###..")
private val SILENT = listOf("#..#...", ".###...", "####...", "##.#...", "####.#.", "..##..#", "...#...")
private val VIBRATE = listOf("..###..", "#.#.#.#", "#.#.#.#", "#.#.#.#", "#.#.#.#", "#.###.#", ".......")
private val ALARM = listOf("##...##", "#.###.#", ".#.#.#.", ".#.##..", ".#...#.", "..###..", ".#...#.")
private val HEADSET = listOf("..###..", ".#...#.", "#.....#", "#.....#", "##...##", "##...##", "##...##")
private val LOCATION = listOf("..###..", ".#...#.", ".#.#.#.", ".#...#.", "..#.#..", "...#...", ".......")
private val MOON = listOf("..###..", ".##....", "##.....", "##.....", "##.....", ".##....", "..###..")
private val CHARGE = listOf("....##.", "...##..", "..##...", ".#####.", "...##..", "..##...", ".##....")

@Composable
private fun PixelIcon(rows: List<String>, color: Color, description: String) {
    Canvas(Modifier.size(15.dp).semantics { contentDescription = description }) {
        val pixel = size.width / rows[0].length
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                if (c == '#') drawRect(color, Offset(x * pixel, y * pixel), Size(pixel + 0.5f, pixel + 0.5f))
            }
        }
    }
}

/** Antenna plus four rising bars. A crossed antenna means no SIM or airplane mode. */
@Composable
private fun SignalBars(sim: SimSignal?, label: String, color: Color, compact: Boolean) {
    val level = sim?.level ?: 0
    val text = stringResource(R.string.status_signal, label, level)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Canvas(
            Modifier.size(34.dp, if (compact) 11.dp else 20.dp).semantics { contentDescription = text },
        ) {
            val unit = size.width / 17f
            val stroke = unit * 1.1f
            // Antenna
            drawLine(color, Offset(unit * 2.5f, 0f), Offset(unit * 2.5f, size.height), stroke)
            drawLine(color, Offset(0f, 0f), Offset(unit * 2.5f, size.height * 0.45f), stroke)
            drawLine(color, Offset(unit * 5f, 0f), Offset(unit * 2.5f, size.height * 0.45f), stroke)
            for (bar in 0 until 4) {
                val height = size.height * (bar + 1) / 4f
                // Empty bars stay as a faint outline of the full scale.
                drawRect(
                    color.copy(alpha = if (bar < level) 1f else 0.2f),
                    Offset(unit * (7f + bar * 2.6f), size.height - height), Size(unit * 1.9f, height),
                )
            }
            if (sim == null || !sim.present) {
                drawLine(color, Offset(unit * 8f, 0f), Offset(unit * 15f, size.height), stroke)
                drawLine(color, Offset(unit * 15f, 0f), Offset(unit * 8f, size.height), stroke)
            }
        }
        if (compact) LcdText(label, color = color, small = true)
    }
}

@Composable
private fun Battery(level: Int, color: Color) {
    val text = stringResource(R.string.status_battery, level)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        LcdText("$level", color = color, small = true)
        Canvas(Modifier.size(26.dp, 13.dp).semantics { contentDescription = text }) {
            val tip = size.width * 0.1f
            val body = Size(size.width - tip, size.height)
            val line = size.height * 0.14f
            drawRect(color, Offset.Zero, Size(body.width, line))
            drawRect(color, Offset(0f, body.height - line), Size(body.width, line))
            drawRect(color, Offset.Zero, Size(line, body.height))
            drawRect(color, Offset(body.width - line, 0f), Size(line, body.height))
            drawRect(color, Offset(body.width, size.height * 0.3f), Size(tip, size.height * 0.4f))
            val segments = (level + 12) / 25
            val width = (body.width - line * 3) / 4f
            for (i in 0 until segments.coerceIn(0, 4)) {
                drawRect(
                    color, Offset(line * 2 + i * width, line * 2),
                    Size(width - line, body.height - line * 4),
                )
            }
        }
    }
}

/** The top of the home screen; tapping it opens the connections list. */
@Composable
fun StatusBar(status: PhoneStatus, onClick: () -> Unit) {
    val color = LocalPhoneColors.current.lcdText
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (status.airplane) {
            PixelIcon(AIRPLANE, color, stringResource(R.string.airplane_mode))
        } else if (status.sims.size > 1) {
            // Two SIMs: one above the other.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                status.sims.take(2).forEachIndexed { i, sim -> SignalBars(sim, "${i + 1}", color, true) }
            }
        } else {
            SignalBars(status.sims.firstOrNull(), "1", color, false)
        }
        Row(
            Modifier.height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status.location) PixelIcon(LOCATION, color, stringResource(R.string.location))
            if (status.alarm) PixelIcon(ALARM, color, stringResource(R.string.alarm))
            if (status.doNotDisturb) PixelIcon(MOON, color, stringResource(R.string.do_not_disturb))
            when (status.ringer) {
                AudioManager.RINGER_MODE_SILENT -> PixelIcon(SILENT, color, stringResource(R.string.silent))
                AudioManager.RINGER_MODE_VIBRATE -> PixelIcon(VIBRATE, color, stringResource(R.string.vibrate))
            }
            if (status.headset) PixelIcon(HEADSET, color, stringResource(R.string.headset))
            if (status.bluetooth) PixelIcon(BLUETOOTH, color, stringResource(R.string.bluetooth))
            if (status.wifiOn) {
                // Faint while Wi-Fi is on but not connected.
                PixelIcon(
                    WIFI, if (status.wifiConnected) color else color.copy(alpha = 0.4f),
                    stringResource(R.string.wifi),
                )
            }
            if (status.charging) PixelIcon(CHARGE, color, stringResource(R.string.charging))
            Battery(status.battery, color)
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun StatusBarPreview() {
    LauncherTheme {
        Lcd(emptyList(), Modifier.height(80.dp)) {
            StatusBar(
                PhoneStatus(
                    sims = listOf(SimSignal(true, 4), SimSignal(true, 2)), wifiOn = true,
                    wifiConnected = true, bluetooth = true, alarm = true, battery = 62,
                ),
            ) {}
        }
    }
}
