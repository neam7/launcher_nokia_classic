package com.nokia.launcher.ui.home

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.VibratorManager

/** Keypad sound styles, in the order they are saved. */
const val KEY_SOUND_PHONE = 0
const val KEY_SOUND_BEEP = 1
const val KEY_SOUND_CLICK = 2

class KeyFeedback(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    // The constructor throws when the audio service has no free tracks; keys then stay silent.
    private val tones = runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 60) }.getOrNull()

    fun play(char: Char?, tone: Boolean, vibrate: Boolean, style: Int = KEY_SOUND_PHONE) {
        if (tone && style == KEY_SOUND_CLICK) {
            audio?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 1f)
        } else if (tone) {
            val id = if (style == KEY_SOUND_BEEP) ToneGenerator.TONE_PROP_BEEP else when (char) {
                in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (char!! - '0')
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            tones?.startTone(id, 80)
        }
        if (vibrate) vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        tones?.release()
    }
}
