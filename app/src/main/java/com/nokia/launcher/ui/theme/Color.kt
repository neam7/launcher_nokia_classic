package com.nokia.launcher.ui.theme

import androidx.compose.ui.graphics.Color
import com.nokia.launcher.R

data class PhoneColors(
    val name: Int,
    val body: Color,
    val lcd: Color,
    val lcdText: Color,
    val menu: Color,
    val menuText: Color,
    val key: Color,
    val keyText: Color,
    val accent: Color,
    val onAccent: Color,
)

private val White = Color(0xFFF2F2F2)
private val DarkBody = Color(0xFF0E0E10)
private val DarkKey = Color(0xFF1B1B1E)

/** A theme with the black phone body: only the screen backlight and its ink change. */
private fun backlight(name: Int, lcd: Long, ink: Long, menu: Long) = PhoneColors(
    name = name, body = DarkBody, lcd = Color(lcd), lcdText = Color(ink),
    menu = Color(menu), menuText = White, key = DarkKey, keyText = White,
    accent = Color(lcd), onAccent = Color(ink),
)

/** Order matters: `Prefs.theme` is the index, so new themes go at the end. */
val PhoneThemes = listOf(
    // Classic: sampled from RESOURCES/imagenes_clave (01, 10, 11).
    backlight(R.string.theme_classic, 0xFF9DB925, 0xFF11150A, 0xFF222818),
    PhoneColors(
        name = R.string.theme_blue,
        body = Color(0xFF0B2A4A), lcd = Color(0xFFB8DCF5), lcdText = Color(0xFF06203A),
        menu = Color(0xFF103A63), menuText = Color(0xFFFFFFFF),
        key = Color(0xFF05233F), keyText = Color(0xFFFFFFFF),
        accent = Color(0xFF6CC4FF), onAccent = Color(0xFF06203A),
    ),
    PhoneColors(
        name = R.string.theme_light,
        body = Color(0xFFC8C8C8), lcd = Color(0xFFE8E8E8), lcdText = Color(0xFF1A1A1A),
        menu = Color(0xFFF4F4F4), menuText = Color(0xFF1A1A1A),
        key = Color(0xFFE6E6E6), keyText = Color(0xFF222222),
        accent = Color(0xFF0A6EBD), onAccent = Color(0xFFFFFFFF),
    ),
    // The unlit gray-green screen of the monochrome phones.
    backlight(R.string.theme_mono, 0xFFAEB5A5, 0xFF1C1F1A, 0xFF22241F),
    backlight(R.string.theme_amber, 0xFFF2A93B, 0xFF221300, 0xFF2A1D08),
    backlight(R.string.theme_ice, 0xFF7FD4E8, 0xFF04222B, 0xFF0C2830),
    // Night: lit characters on black.
    PhoneColors(
        name = R.string.theme_night,
        body = Color(0xFF000000), lcd = Color(0xFF0B120B), lcdText = Color(0xFF7CFF6B),
        menu = Color(0xFF050805), menuText = Color(0xFFCFFFC8),
        key = Color(0xFF111411), keyText = Color(0xFFB9F5B0),
        accent = Color(0xFF7CFF6B), onAccent = Color(0xFF061006),
    ),
    backlight(R.string.theme_white, 0xFFF4F6EE, 0xFF101010, 0xFF202020),
)

val CallKey = Color(0xFF1FAF4B)
val EndKey = Color(0xFFE52222)
