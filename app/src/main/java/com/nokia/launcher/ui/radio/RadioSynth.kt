package com.nokia.launcher.ui.radio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Frequencies are in tenths of MHz: 875..1080. */
const val FM_MIN = 875
const val FM_MAX = 1080
const val SAMPLE_RATE = 22_050

/** An invented station playing a public-domain melody; notes are semitones from A4, null is a rest. */
data class Station(val frequency: Int, val name: String, val melody: List<Int?>)

val STATIONS = listOf(
    // Beethoven, Ode to Joy
    Station(891, "Radio Alegría", listOf(7, 7, 8, 10, 10, 8, 7, 5, 3, 3, 5, 7, 7, 5, 5, null)),
    // Beethoven, Für Elise
    Station(945, "Clásica FM", listOf(7, 6, 7, 6, 7, 2, 5, 3, 0, null, -9, -5, 0, 2, null, null)),
    // Traditional, Twinkle Twinkle
    Station(1013, "Estrella 101", listOf(-9, -9, -2, -2, 0, 0, -2, null, -4, -4, -5, -5, -7, -7, -9, null)),
    // Traditional, Frère Jacques
    Station(1057, "La Campana", listOf(-4, -2, 0, -4, -4, -2, 0, -4, 0, 1, 3, null, 0, 1, 3, null)),
)

/** 1 when tuned exactly, fading to 0 three steps away. */
fun reception(frequency: Int, station: Station): Float =
    (1f - abs(frequency - station.frequency) / 3f).coerceIn(0f, 1f)

fun bestStation(frequency: Int): Station? =
    STATIONS.filter { reception(frequency, it) > 0f }.maxByOrNull { reception(frequency, it) }

/** The next station up or down the dial, wrapping around. */
fun seek(frequency: Int, up: Boolean): Int {
    val all = STATIONS.map { it.frequency }
    return if (up) all.firstOrNull { it > frequency } ?: all.first()
    else all.lastOrNull { it < frequency } ?: all.last()
}

/** Fills [out] with the station mixed with static, starting at sample [position]. */
fun synthesize(out: ShortArray, position: Long, frequency: Int, random: Random) {
    val station = bestStation(frequency)
    val clear = station?.let { reception(frequency, it) } ?: 0f
    val noteLength = SAMPLE_RATE * 3 / 10
    for (i in out.indices) {
        val at = position + i
        var value = (random.nextFloat() * 2 - 1) * (1f - clear) * 0.18f
        if (station != null) {
            val note = station.melody[((at / noteLength) % station.melody.size).toInt()]
            if (note != null) {
                val hertz = 440.0 * 2.0.pow(note / 12.0)
                val inNote = (at % noteLength).toDouble() / noteLength
                val wave = sin(2 * PI * hertz * at / SAMPLE_RATE) +
                    0.25 * sin(4 * PI * hertz * at / SAMPLE_RATE)
                // A short fade in and out avoids clicks between notes.
                val envelope = minOf(1.0, inNote * 20, (1 - inNote) * 8)
                value += (wave * envelope * 0.22 * clear).toFloat()
            }
        }
        out[i] = (value.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
    }
}
