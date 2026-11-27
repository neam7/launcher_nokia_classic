package com.nokia.launcher

import android.app.Notification
import com.nokia.launcher.data.showsOnHome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticesTest {
    @Test
    fun onlyRealNoticesFromOtherAppsReachTheHomeScreen() {
        assertTrue(showsOnHome(0, false, "Ana", "Hola"))
        assertTrue(showsOnHome(Notification.FLAG_AUTO_CANCEL, false, "", "Hola"))
        assertFalse(showsOnHome(0, true, "Ana", "Hola"))
        assertFalse(showsOnHome(0, false, " ", ""))
        assertFalse(showsOnHome(Notification.FLAG_ONGOING_EVENT, false, "Música", "Sonando"))
        assertFalse(showsOnHome(Notification.FLAG_FOREGROUND_SERVICE, false, "VPN", "Activa"))
        assertFalse(showsOnHome(Notification.FLAG_GROUP_SUMMARY, false, "Chats", "2 mensajes"))
    }
}
