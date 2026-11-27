package com.nokia.launcher

import com.nokia.launcher.ui.home.sideBySide
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {
    @Test
    fun unfoldedAndLandscapeUseTwoHalvesButTallWindowsStack() {
        assertTrue(sideBySide(750f, 832f)) // Fold inner, portrait
        assertTrue(sideBySide(832f, 750f)) // Fold inner, landscape
        assertTrue(sideBySide(891f, 411f)) // phone, landscape
        assertFalse(sideBySide(411f, 960f)) // Fold cover
        assertFalse(sideBySide(800f, 1232f)) // tablet, portrait
        assertFalse(sideBySide(400f, 480f)) // small almost square window: halves would be too narrow
    }
}
