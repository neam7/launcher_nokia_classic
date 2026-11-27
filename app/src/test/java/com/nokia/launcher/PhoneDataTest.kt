package com.nokia.launcher

import com.nokia.launcher.data.Sms
import com.nokia.launcher.data.conversations
import com.nokia.launcher.data.groupContacts
import com.nokia.launcher.ui.connections.levelBar
import com.nokia.launcher.ui.connections.nextLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneDataTest {
    @Test
    fun oneConversationPerThreadWithItsNewestMessage() {
        val list = listOf(Sms(1, "a", "new", 3, false), Sms(2, "b", "x", 2, true), Sms(1, "a", "old", 1, true))
        assertEquals(listOf("new", "x"), conversations(list).map { it.body })
    }

    @Test
    fun contactNumbersAreGroupedWithoutRepeats() {
        val rows = listOf(Triple(1L, "Ana", "555 0100"), Triple(1L, "Ana", "5550100"), Triple(1L, "Ana", "5550111"))
        assertEquals(listOf("555 0100", "5550111"), groupContacts(rows).single().numbers)
    }

    @Test
    fun levelsStepUpAndWrap() {
        assertEquals(0, nextLevel(7, 0, 7))
        assertEquals(7, nextLevel(6, 0, 7))
        assertEquals("▮▮▮▮▯▯▯▯", levelBar(50, 100))
    }
}
