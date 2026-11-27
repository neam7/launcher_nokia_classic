package com.nokia.launcher.ui.tones

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.nokia.launcher.ui.radio.SAMPLE_RATE

/** One entry of the tone list: a launcher melody or a sound that comes with the phone. */
data class ToneChoice(val key: String, val title: String, val tune: Tune? = null, val uri: Uri? = null)

/** [type] is one of RingtoneManager's TYPE constants. Blocking: call off the main thread. */
fun toneChoices(context: Context, type: Int): List<ToneChoice> {
    // Short signals for messages, melodies for calls and alarms.
    val own = TUNES.filter { it.alert == (type == RingtoneManager.TYPE_NOTIFICATION) }
        .map { ToneChoice(it.id, it.title, tune = it) }
    val system = runCatching {
        val manager = RingtoneManager(context).apply { setType(type) }
        val cursor = manager.cursor
        buildList {
            while (cursor.moveToNext()) {
                val uri = manager.getRingtoneUri(cursor.position)
                add(ToneChoice(uri.toString(), cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX), uri = uri))
            }
        }
    }.getOrDefault(emptyList())
    // Melodies already saved to the phone also show up in the system list.
    return own + system.filterNot { it.title.startsWith(FILE_PREFIX) }
}

fun currentToneTitle(context: Context, type: Int): String? = runCatching {
    RingtoneManager.getActualDefaultRingtoneUri(context, type)
        ?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context)?.removePrefix(FILE_PREFIX) }
}.getOrNull()

private const val FILE_PREFIX = "Nokia launcher "

/** Saves a launcher melody among the phone's sounds so Android can play it. */
private fun install(context: Context, tune: Tune, type: Int): Uri? {
    val folder = when (type) {
        RingtoneManager.TYPE_NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
        RingtoneManager.TYPE_ALARM -> Environment.DIRECTORY_ALARMS
        else -> Environment.DIRECTORY_RINGTONES
    }
    val name = "$FILE_PREFIX${tune.title}.wav"
    val resolver = context.contentResolver
    val sounds = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    resolver.query(
        sounds, arrayOf(MediaStore.MediaColumns._ID),
        "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
        arrayOf(name, "$folder/"), null,
    )?.use { if (it.moveToFirst()) return ContentUris.withAppendedId(sounds, it.getLong(0)) }
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-wav")
        put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
        put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneManager.TYPE_RINGTONE)
        put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneManager.TYPE_NOTIFICATION)
        put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneManager.TYPE_ALARM)
    }
    val uri = resolver.insert(sounds, values) ?: return null
    resolver.openOutputStream(uri)?.use { it.write(wav(render(tune))) } ?: return null
    resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    return uri
}

/** Makes [choice] the phone's sound. Needs the "modify system settings" access. False on failure. */
fun useTone(context: Context, choice: ToneChoice, type: Int): Boolean = runCatching {
    val uri = choice.uri ?: install(context, choice.tune!!, type) ?: return false
    RingtoneManager.setActualDefaultRingtoneUri(context, type, uri)
    true
}.getOrDefault(false)

/** Plays one tone at a time, on the media volume so it is heard while choosing. */
class TonePlayer(private val context: Context) {
    private var track: AudioTrack? = null
    private var ringtone: Ringtone? = null
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun play(choice: ToneChoice) {
        stop()
        runCatching {
            if (choice.tune != null) {
                val samples = render(choice.tune)
                track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(samples.size * 2)
                    .build()
                    .apply {
                        write(samples, 0, samples.size)
                        play()
                    }
            } else {
                ringtone = RingtoneManager.getRingtone(context, choice.uri)?.also {
                    it.audioAttributes = attributes
                    it.play()
                }
            }
        }
    }

    fun stop() {
        runCatching { track?.release() }
        runCatching { ringtone?.stop() }
        track = null
        ringtone = null
    }
}
