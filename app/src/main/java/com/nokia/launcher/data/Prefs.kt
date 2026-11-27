package com.nokia.launcher.data

import androidx.compose.runtime.mutableIntStateOf
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

const val NAME_MAX = 12

/** Launcher preferences: observable by Compose, saved in SharedPreferences. */
class Prefs(context: Context) {
    private val store = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    private var nameState by mutableStateOf(store.getString("name", "") ?: "")
    private var themeState by mutableIntStateOf(store.getInt("theme", 0))
    private var toneState by mutableStateOf(store.getBoolean("tone", true))
    private var soundState by mutableIntStateOf(store.getInt("key_sound", 0))
    private var gameSoundState by mutableStateOf(store.getBoolean("game_sound", true))
    private var rotateState by mutableStateOf(store.getBoolean("rotate", false))
    private var vibrateState by mutableStateOf(store.getBoolean("vibrate", true))
    private var qwertyState by mutableStateOf(store.getBoolean("qwerty", false))
    private var radioState by mutableIntStateOf(store.getInt("radio", 945).coerceIn(875, 1080))
    private var favoritesState by mutableStateOf(
        store.getString("favorites", "").orEmpty().split('|').filter { it.isNotEmpty() },
    )
    private var pinballBestState by mutableIntStateOf(store.getInt("pinball_best", 0))
    private var bestState by mutableIntStateOf(store.getInt("snake_best", 0))

    /** Empty means the default wordmark. */
    var phoneName: String
        get() = nameState
        set(value) { nameState = value.trim().take(NAME_MAX); store.edit { putString("name", nameState) } }
    var theme: Int
        get() = themeState
        set(value) { themeState = value; store.edit { putInt("theme", value) } }
    var keyTone: Boolean
        get() = toneState
        set(value) { toneState = value; store.edit { putBoolean("tone", value) } }
    var keySound: Int
        get() = soundState
        set(value) { soundState = value; store.edit { putInt("key_sound", value) } }
    var keyVibrate: Boolean
        get() = vibrateState
        set(value) { vibrateState = value; store.edit { putBoolean("vibrate", value) } }
    var qwerty: Boolean
        get() = qwertyState
        set(value) { qwertyState = value; store.edit { putBoolean("qwerty", value) } }
    var radioFrequency: Int
        get() = radioState
        set(value) { radioState = value; store.edit { putInt("radio", value) } }
    /** The unfinished Snake game, encoded. */
    var snakeGame: String
        get() = store.getString("snake_game", "") ?: ""
        set(value) = store.edit { putString("snake_game", value) }
    /** Apps shown under "Go to", as flattened component names. */
    var favorites: List<String>
        get() = favoritesState
        set(value) { favoritesState = value; store.edit { putString("favorites", value.joinToString("|")) } }
    var minesGame: String
        get() = store.getString("mines_game", "") ?: ""
        set(value) = store.edit { putString("mines_game", value) }
    var spiderGame: String
        get() = store.getString("spider_game", "") ?: ""
        set(value) = store.edit { putString("spider_game", value) }
    var pinballGame: String
        get() = store.getString("pinball_game", "") ?: ""
        set(value) = store.edit { putString("pinball_game", value) }
    /** Key sounds while playing, switched from the game itself. */
    var gameSound: Boolean
        get() = gameSoundState
        set(value) { gameSoundState = value; store.edit { putBoolean("game_sound", value) } }
    /** Off by default: the launcher stays upright, like the classic phones. */
    var rotate: Boolean
        get() = rotateState
        set(value) { rotateState = value; store.edit { putBoolean("rotate", value) } }
    /** The first-run question about notifications was answered; it is not asked again. */
    var askedNotices: Boolean
        get() = store.getBoolean("asked_notices", false)
        set(value) = store.edit { putBoolean("asked_notices", value) }
    var pinballBest: Int
        get() = pinballBestState
        set(value) { pinballBestState = value; store.edit { putInt("pinball_best", value) } }
    var snakeBest: Int
        get() = bestState
        set(value) { bestState = value; store.edit { putInt("snake_best", value) } }

    // Records are observable so the game list and panels update at once.
    private val records = mutableStateMapOf<String, Int>()
    fun record(game: String): Int = records[game] ?: store.getInt("record_$game", 0)
    fun setRecord(game: String, score: Int) {
        if (score <= record(game)) return
        records[game] = score
        store.edit { putInt("record_$game", score) }
    }
    fun savedGame(game: String): String = store.getString("saved_$game", "") ?: ""
    fun saveGame(game: String, text: String) = store.edit { putString("saved_$game", text) }

    /** Restores the settings; favorites, records and saved games are kept. */
    fun reset() {
        phoneName = ""
        theme = 0
        keyTone = true
        keySound = 0
        gameSound = true
        keyVibrate = true
        qwerty = false
        rotate = false
    }
}
