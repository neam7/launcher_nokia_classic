package com.nokia.launcher

import com.nokia.launcher.ui.lock.UNLOCK_WINDOW_MS
import com.nokia.launcher.ui.lock.unlocks
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeypadLockTest {
    @Test
    fun onlyStarRightAfterMenuUnlocks() {
        assertTrue(unlocks(1_000, '*', 1_500))
        assertFalse(unlocks(0, '*', 1_500))
        assertFalse(unlocks(1_000, '#', 1_500))
        assertFalse(unlocks(1_000, '*', 1_001 + UNLOCK_WINDOW_MS))
    }
}
