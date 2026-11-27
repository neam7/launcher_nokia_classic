package com.nokia.launcher

import com.nokia.launcher.ui.radio.SCAN_CHANNELS
import com.nokia.launcher.ui.radio.Scan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ScanTest {
    @Test
    fun searchesStaysOnConversationsAndMovesOn() {
        val random = Random(7)
        var scan = Scan(channel = SCAN_CHANNELS)
        var conversations = 0
        repeat(2_000) {
            val next = scan.step(random)
            // The channel only changes while nobody is talking.
            if (scan.signal) assertEquals(scan.channel, next.channel)
            else assertEquals(scan.channel % SCAN_CHANNELS + 1, next.channel)
            if (!scan.signal && next.signal) conversations++
            scan = next
        }
        assertTrue(conversations > 3)
    }

    @Test
    fun tuningByHandHoldsTheChannelAndWraps() {
        var scan = Scan(channel = 1).tune(-1)
        assertEquals(SCAN_CHANNELS, scan.channel)
        repeat(200) { scan = scan.step(Random(it)) }
        assertEquals(SCAN_CHANNELS, scan.channel)
    }
}
