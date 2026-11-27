package com.nokia.launcher.data

import android.app.Notification
import android.app.PendingIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A notification of another app, as the home screen shows it. */
data class Notice(
    val key: String,
    val app: String,
    val title: String,
    val text: String,
    val open: PendingIntent? = null,
    /** Android would remove it once opened. */
    val autoCancel: Boolean = false,
    /** Whether Android lets it be removed. */
    val clearable: Boolean = true,
)

/** What the listener service sees now. It is kept only in memory: nothing is saved or sent. */
object Notices {
    var list by mutableStateOf(emptyList<Notice>())
}

/** Leaves out permanent notices (music, background work), group headers and empty ones. */
fun showsOnHome(flags: Int, ownApp: Boolean, title: String, text: String): Boolean {
    val hidden = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or
        Notification.FLAG_GROUP_SUMMARY
    return !ownApp && flags and hidden == 0 && (title.isNotBlank() || text.isNotBlank())
}
