package com.nokia.launcher.ui.connections

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nokia.launcher.R
import com.nokia.launcher.data.granted
import com.nokia.launcher.ui.home.PhoneStatus
import com.nokia.launcher.ui.home.rememberPhoneStatus
import com.nokia.launcher.ui.settings.Row

/** A Nokia-style level bar, such as `▮▮▮▯▯`. */
fun levelBar(value: Int, max: Int, cells: Int = 8): String {
    val filled = if (max <= 0) 0 else (value.coerceIn(0, max) * cells + max / 2) / max
    return "▮".repeat(filled) + "▯".repeat(cells - filled)
}

/** One step up, a quarter of the range at a time; past the top it starts again at [min]. */
fun nextLevel(value: Int, min: Int, max: Int): Int =
    if (value >= max) min else minOf(max, value + ((max - min) / 4).coerceAtLeast(1))

private const val BRIGHTNESS_MAX = 255

/**
 * The launcher's own connections panel. Android only lets an app change some of these; the
 * others open the smallest system panel that can.
 */
@Composable
fun connectionRows(
    start: (Intent) -> Unit,
    ask: (String) -> Unit,
): List<Row> {
    val context = LocalContext.current
    val status: PhoneStatus = rememberPhoneStatus()
    var changes by remember { mutableIntStateOf(0) }
    var torch by remember { mutableStateOf(false) }
    val camera = remember { context.getSystemService(CameraManager::class.java) }
    val flash = remember {
        runCatching {
            camera.cameraIdList.firstOrNull {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    DisposableEffect(camera, flash) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == flash) torch = enabled
            }
        }
        camera.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { camera.unregisterTorchCallback(callback) }
    }

    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val resolver = context.contentResolver
    changes.let {} // re-reads the values below after a change made here

    // Silent mode needs "Do not disturb" access; without it Android throws.
    fun sound(change: () -> Unit) {
        try {
            change()
            changes++
        } catch (e: SecurityException) {
            start(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    fun writeSetting(change: () -> Unit) {
        if (Settings.System.canWrite(context)) {
            change()
            changes++
        } else {
            start(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }
    }

    @Composable
    fun onOff(on: Boolean) = stringResource(if (on) R.string.on else R.string.off)

    @Composable
    fun volume(label: Int, stream: Int): Row {
        val value = audio.getStreamVolume(stream)
        val max = audio.getStreamMaxVolume(stream)
        return Row(stringResource(label), levelBar(value, max)) {
            sound { audio.setStreamVolume(stream, nextLevel(value, 0, max), 0) }
        }
    }

    val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
    val rotation = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
    val ringer = audio.ringerMode

    return listOfNotNull(
        Row(
            stringResource(R.string.wifi),
            if (status.wifiConnected) stringResource(R.string.connected) else onOff(status.wifiOn),
        ) { start(Intent(Settings.Panel.ACTION_WIFI)) },
        Row(stringResource(R.string.bluetooth), onOff(status.bluetooth)) {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            when {
                adapter == null || status.bluetooth -> start(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                !context.granted(Manifest.permission.BLUETOOTH_CONNECT) -> ask(Manifest.permission.BLUETOOTH_CONNECT)
                // The system asks once and turns Bluetooth on over the launcher.
                else -> start(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        },
        Row(stringResource(R.string.mobile_network)) {
            start(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        },
        Row(stringResource(R.string.airplane_mode), onOff(status.airplane)) {
            start(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        },
        flash?.let { id ->
            Row(stringResource(R.string.flashlight), onOff(torch)) {
                // The camera may be busy in another app.
                runCatching { camera.setTorchMode(id, !torch) }
            }
        },
        Row(
            stringResource(R.string.ringer),
            stringResource(
                when (ringer) {
                    AudioManager.RINGER_MODE_SILENT -> R.string.silent
                    AudioManager.RINGER_MODE_VIBRATE -> R.string.vibrate
                    else -> R.string.ringer_normal
                },
            ),
        ) {
            sound {
                audio.ringerMode = when (ringer) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
            }
        },
        volume(R.string.volume_ring, AudioManager.STREAM_RING),
        volume(R.string.volume_media, AudioManager.STREAM_MUSIC),
        volume(R.string.volume_alarm, AudioManager.STREAM_ALARM),
        Row(stringResource(R.string.brightness), levelBar(brightness, BRIGHTNESS_MAX)) {
            writeSetting {
                Settings.System.putInt(
                    resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
                Settings.System.putInt(
                    resolver, Settings.System.SCREEN_BRIGHTNESS, nextLevel(brightness, 10, BRIGHTNESS_MAX),
                )
            }
        },
        Row(stringResource(R.string.auto_rotate), onOff(rotation)) {
            writeSetting {
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, if (rotation) 0 else 1)
            }
        },
        Row(stringResource(R.string.location), onOff(status.location)) {
            start(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        Row(
            stringResource(R.string.dual_sim),
            stringResource(
                if (context.granted(Manifest.permission.READ_PHONE_STATE)) R.string.dual_sim_allowed
                else R.string.dual_sim_allow,
            ),
        ) { ask(Manifest.permission.READ_PHONE_STATE) },
    )
}
