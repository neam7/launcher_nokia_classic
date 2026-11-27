package com.nokia.launcher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Repeats [tick] only while the launcher is visible. With the screen off or another app in
 * front nothing runs, so it does not use battery.
 */
@Composable
fun WhileVisible(everyMs: Long, vararg keys: Any?, tick: suspend () -> Unit) {
    val current by rememberUpdatedState(tick)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, everyMs, *keys) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                current()
                delay(everyMs)
            }
        }
    }
}
