package com.nokia.launcher

import com.nokia.launcher.ui.home.InputMode
import com.nokia.launcher.ui.home.Tap
import com.nokia.launcher.ui.appdrawer.searchKey
import com.nokia.launcher.ui.home.dialString
import com.nokia.launcher.ui.home.dialTap
import com.nokia.launcher.ui.home.multiTap
import org.junit.Assert.assertEquals
import org.junit.Test

class MultiTapTest {
    @Test
    fun sameKeyWithinTimeoutCycles() {
        assertEquals("b", multiTap("a", Tap('2', 0), Tap('2', 500)))
        assertEquals("a", multiTap("2", Tap('2', 0), Tap('2', 500)))
    }

    @Test
    fun timeoutOrOtherKeyAppends() {
        assertEquals("aa", multiTap("a", Tap('2', 0), Tap('2', 800)))
        assertEquals("ad", multiTap("a", Tap('2', 0), Tap('3', 100)))
    }

    @Test
    fun emptyTextAndNonLetterKeys() {
        assertEquals("a", multiTap("", Tap('2', 0), Tap('2', 100)))
        assertEquals("a", multiTap("a", null, Tap('#', 0)))
    }

    @Test
    fun upperAndNumberModes() {
        assertEquals("B", multiTap("A", Tap('2', 0), Tap('2', 100), InputMode.Upper))
        assertEquals("22", multiTap("2", Tap('2', 0), Tap('2', 100), InputMode.Number))
        assertEquals(".", multiTap("", null, Tap('1', 0)))
    }

    @Test
    fun starCyclesLikeAClassicNokiaWhileDialing() {
        assertEquals("5+", dialTap("5*", Tap('*', 0), Tap('*', 100)))
        assertEquals("5p", dialTap("5+", Tap('*', 0), Tap('*', 100)))
        assertEquals("5*", dialTap("5w", Tap('*', 0), Tap('*', 100)))
        assertEquals("5**", dialTap("5*", Tap('*', 0), Tap('*', 900)))
        assertEquals("1,2;3", dialString("1p2w3"))
    }

    @Test
    fun starWritesSymbolsInText() {
        assertEquals("+", multiTap("*", Tap('*', 0), Tap('*', 100)))
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        assertEquals("camara", searchKey("Cámara"))
    }
}
