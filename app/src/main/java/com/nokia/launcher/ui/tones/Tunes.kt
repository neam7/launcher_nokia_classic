package com.nokia.launcher.ui.tones

import com.nokia.launcher.ui.radio.SAMPLE_RATE
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** A note: semitones from A4 (null is a rest) and its length in sixteenths. */
data class Note(val pitch: Int?, val length: Int)

/** A launcher melody, synthesized here from public-domain scores or simple signals. */
data class Tune(val id: String, val title: String, val bpm: Int, val notes: List<Note>, val alert: Boolean = false)

/** Pairs of pitch and length; [R] marks a rest. */
private const val R = -100
private fun notes(vararg pairs: Int) = pairs.toList().chunked(2).map { (p, l) -> Note(p.takeIf { it != R }, l) }

val TUNES = listOf(
    // Francisco Tárrega, Gran Vals (1902)
    Tune("gran_vals", "Gran Vals (Tárrega)", 180, notes(
        7, 2, 5, 2, -3, 4, -1, 4, 4, 2, 2, 2, -7, 4, -5, 4, 2, 2, 0, 2, -8, 4, -5, 4, 0, 8, R, 6,
    )),
    Tune("badinerie", "Badinerie (Bach)", 112, notes(
        14, 2, 17, 1, 14, 1, 9, 2, 14, 1, 9, 1, 5, 2, 9, 1, 5, 1, 2, 4, R, 4,
    )),
    Tune("joy", "Himno a la alegría (Beethoven)", 140, notes(
        7, 4, 7, 4, 8, 4, 10, 4, 10, 4, 8, 4, 7, 4, 5, 4, 3, 4, 3, 4, 5, 4, 7, 4, 7, 6, 5, 2, 5, 8, R, 4,
    )),
    Tune("elise", "Para Elisa (Beethoven)", 72, notes(
        7, 1, 6, 1, 7, 1, 6, 1, 7, 1, 2, 1, 5, 1, 3, 1, 0, 3, -9, 1, -5, 1, 0, 1, 2, 3, R, 3,
    )),
    Tune("mozart40", "Sinfonía 40 (Mozart)", 120, notes(
        6, 1, 5, 1, 5, 2, 6, 1, 5, 1, 5, 2, 6, 1, 5, 1, 5, 2, 13, 2, R, 2,
        13, 1, 12, 1, 10, 2, 10, 1, 8, 1, 6, 2, 6, 1, 5, 1, 3, 2, 3, 2, R, 4,
    )),
    // Morse code for "SMS".
    Tune("morse", "Morse SMS", 150, notes(
        19, 1, R, 1, 19, 1, R, 1, 19, 1, R, 3, 19, 3, R, 1, 19, 3, R, 3, 19, 1, R, 1, 19, 1, R, 1, 19, 1,
    ), alert = true),
    Tune("double_beep", "Bip doble", 150, notes(19, 1, R, 1, 19, 1), alert = true),
    Tune("chime", "Campanita", 130, notes(15, 1, 19, 1, 22, 3), alert = true),
)

/** A thin buzzer-like voice, as on a monophonic phone. */
fun render(tune: Tune): ShortArray {
    val sixteenth = SAMPLE_RATE * 15 / tune.bpm
    val out = ShortArray(tune.notes.sumOf { it.length } * sixteenth)
    var start = 0
    for (note in tune.notes) {
        val length = note.length * sixteenth
        if (note.pitch != null) {
            val step = 2 * PI * 440.0 * 2.0.pow(note.pitch / 12.0) / SAMPLE_RATE
            for (i in 0 until length) {
                val wave = sin(step * i) + 0.3 * sin(3 * step * i) + 0.15 * sin(5 * step * i)
                // A short fade at both ends avoids clicks and separates repeated notes.
                val envelope = minOf(1.0, i / 90.0, (length - i) / 220.0)
                out[start + i] = (wave * envelope * 0.55 * Short.MAX_VALUE).toInt().toShort()
            }
        }
        start += length
    }
    return out
}

/** Mono 16-bit WAV, a format Android can use as a ringtone. */
fun wav(samples: ShortArray): ByteArray {
    val data = samples.size * 2
    return ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        put("data".toByteArray()).putInt(data)
        samples.forEach { putShort(it) }
    }.array()
}
