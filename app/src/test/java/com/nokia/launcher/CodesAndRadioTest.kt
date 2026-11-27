package com.nokia.launcher

import com.nokia.launcher.ui.codes.SecretCode
import com.nokia.launcher.ui.codes.findCode
import com.nokia.launcher.ui.home.Tap
import com.nokia.launcher.ui.home.dialTap
import com.nokia.launcher.ui.radio.STATIONS
import com.nokia.launcher.ui.radio.reception
import com.nokia.launcher.ui.radio.seek
import com.nokia.launcher.ui.radio.synthesize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CodesAndRadioTest {
    @Test
    fun onlyCodesWithRealDataLeaveTheLauncher() {
        val real = SecretCode.entries.filter { it.systemScreen != null }
        assertEquals(
            listOf(SecretCode.Imei, SecretCode.Version, SecretCode.LifeTimer, SecretCode.Bluetooth, SecretCode.Wlan, SecretCode.TestMenu),
            real,
        )
    }

    @Test
    fun everyCodeCanBeTypedOnTheKeypad() {
        for (code in SecretCode.entries) {
            var text = ""
            var last: Tap? = null
            // One key per second, so `*` never turns into `+`.
            code.code.forEachIndexed { i, key ->
                val tap = Tap(key, i * 1000L)
                text = dialTap(text, last, tap)
                last = tap
                // No shorter code may fire while typing a longer one.
                if (i < code.code.lastIndex) assertNull(code.code, findCode(text))
            }
            assertEquals(code, findCode(text))
        }
    }

    @Test
    fun seekWrapsAroundTheDial() {
        assertEquals(STATIONS.first().frequency, seek(STATIONS.last().frequency, up = true))
        assertEquals(STATIONS.last().frequency, seek(STATIONS.first().frequency, up = false))
    }

    @Test
    fun receptionFadesAndSoundStaysInRange() {
        val station = STATIONS[0]
        assertEquals(1f, reception(station.frequency, station))
        assertEquals(0f, reception(station.frequency + 3, station))
        val out = ShortArray(4096)
        synthesize(out, 0, station.frequency, Random(1))
        assertTrue(out.any { it.toInt() != 0 })
    }
}
