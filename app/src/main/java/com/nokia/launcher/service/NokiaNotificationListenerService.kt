package com.nokia.launcher.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nokia.launcher.data.Notice
import com.nokia.launcher.data.Notices
import com.nokia.launcher.data.showsOnHome

private const val MAX_NOTICES = 20

/**
 * Optional: runs only after the user grants notification access. Android calls it when something
 * changes, so it never polls.
 */
class NokiaNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        instance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        instance = null
        Notices.list = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        // Throws if Android has just disconnected the listener.
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        Notices.list = active.sortedByDescending { it.postTime }.mapNotNull(::notice).take(MAX_NOTICES)
    }

    private fun notice(sbn: StatusBarNotification): Notice? {
        val notification = sbn.notification
        fun extra(name: String) = notification.extras.getCharSequence(name)?.toString().orEmpty().replace('\n', ' ')
        val title = extra(Notification.EXTRA_TITLE)
        val text = extra(Notification.EXTRA_TEXT)
        if (!showsOnHome(notification.flags, sbn.packageName == packageName, title, text)) return null
        // Apps without a launcher icon may be invisible to this app; their package name is shown.
        val app = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName.substringAfterLast('.'))
        return Notice(
            sbn.key, app, title, text, notification.contentIntent,
            notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
            sbn.isClearable,
        )
    }

    companion object {
        private var instance: NokiaNotificationListenerService? = null

        /** Removes them from the phone too; the list updates when Android confirms. */
        fun dismiss(vararg keys: String) {
            runCatching { instance?.cancelNotifications(keys.toList().toTypedArray()) }
        }

        /** Whether access is granted. Also reconnects the service if Android dropped it. */
        fun hasAccess(context: Context): Boolean {
            val component = ComponentName(context, NokiaNotificationListenerService::class.java)
            val granted = context.getSystemService(NotificationManager::class.java)
                ?.isNotificationListenerAccessGranted(component) == true
            if (granted && instance == null) requestRebind(component)
            return granted
        }
    }
}
