package com.nokia.launcher.ui.games

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/**
 * What the launcher needs from a self-contained game. [encode] and [load] keep the game
 * across rotation, folding and leaving the screen.
 */
interface MiniGame {
    val id: String
    val title: Int
    /** Label of the center soft key, or null when `New` is all the game needs. */
    val actionLabel: Int? get() = null
    fun action() {}
    fun restart()
    fun key(char: Char)
    fun encode(): String
    /** Ignores text it cannot read. */
    fun load(text: String)

    /** [onRecord] receives a score; the launcher keeps it only if it beats the record. */
    @Composable
    fun ColumnScope.Content(record: Int, onRecord: (Int) -> Unit)
}
