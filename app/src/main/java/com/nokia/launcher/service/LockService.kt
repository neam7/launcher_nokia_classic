package com.nokia.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Only turns the screen off for the keypad lock. It subscribes to no events and cannot read
 * windows. Unlike a device admin, locking this way keeps fingerprint unlock working.
 */
class LockService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private var instance: LockService? = null

        fun isEnabled(context: Context): Boolean {
            val wanted = ComponentName(context, LockService::class.java)
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == wanted }
        }

        /** False when the service is not running yet. */
        fun screenOff(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
    }
}
