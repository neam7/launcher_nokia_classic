package com.nokia.launcher.ui.radio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * A toy version of the old stories about phones that "scanned" calls on analog networks.
 * Everything is invented: no antenna, microphone or network is used, and the voices are
 * synthesized murmurs without words.
 */

const val SCAN_CHANNELS = 666
const val SCAN_TICK_MS = 120L
private const val FOUND_CHANCE = 0.05f

/** [talk] counts the ticks left of the conversation being "received". */
data class Scan(val channel: Int = 1, val talk: Int = 0, val held: Boolean = false) {
    val signal get() = talk > 0
    /** An invented frequency in the old 800 MHz band, 30 kHz apart. */
    val megahertz get() = 869.0 + channel * 0.03

    /** Stays while someone talks; otherwise searches on, unless the channel is held. */
    fun step(random: Random): Scan {
        if (talk > 0) return copy(talk = talk - 1)
        val next = if (held) channel else channel % SCAN_CHANNELS + 1
        val found = random.nextFloat() < if (held) FOUND_CHANCE / 3 else FOUND_CHANCE
        return copy(channel = next, talk = if (found) random.nextInt(25, 60) else 0)
    }

    /** Moves by hand, which also holds the new channel. */
    fun tune(by: Int) = Scan(Math.floorMod(channel - 1 + by, SCAN_CHANNELS) + 1, held = true)
}

/**
 * Fills [out] with static and, when there is a signal, with a murmur: a buzzing voice whose tone
 * and "vowel" change with each syllable. Two pitches take turns, like two people.
 */
fun scanAudio(out: ShortArray, position: Long, scan: Scan, random: Random) {
    val syllable = SAMPLE_RATE * 16 / 100
    for (i in out.indices) {
        val at = position + i
        var value = (random.nextFloat() * 2 - 1) * if (scan.signal) 0.05f else 0.16f
        if (scan.signal) {
            val number = at / syllable
            // A cheap scramble gives each syllable its own shape and the pauses between them.
            val mix = ((number * 7919 + scan.channel * 31) % 97).toInt()
            if (mix % 5 != 0) {
                val speaker = if ((number / 9) % 2 == 0L) 115.0 else 190.0
                val pitch = speaker * (0.9 + (mix % 7) * 0.035)
                val vowel = 400.0 + (mix % 6) * 140
                val seconds = at.toDouble() / SAMPLE_RATE
                var voice = 0.0
                for (harmonic in 1..10) {
                    val hertz = pitch * harmonic
                    // Harmonics near the vowel sound louder, as in a throat.
                    val near = 1.0 / (1.0 + ((hertz - vowel) / 220).let { it * it })
                    voice += sin(2 * PI * hertz * seconds) * (near + 0.06)
                }
                val envelope = sin(PI * (at % syllable) / syllable)
                value += (voice * envelope * 0.16).toFloat()
            }
        }
        out[i] = (value.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
    }
}
