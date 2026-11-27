package com.nokia.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPhoneColors = staticCompositionLocalOf { PhoneThemes[0] }

@Composable
fun LauncherTheme(theme: Int = 0, content: @Composable () -> Unit) {
    val colors = PhoneThemes.getOrElse(theme) { PhoneThemes[0] }
    val scheme = darkColorScheme(
        primary = colors.lcd,
        onPrimary = colors.lcdText,
        background = colors.body,
        onBackground = colors.keyText,
        surface = colors.body,
        onSurface = colors.keyText,
    )
    CompositionLocalProvider(LocalPhoneColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
