package com.nokia.launcher.ui.games

/**
 * Bantumi (a mancala game). Pits 0..5 are the player's, 6 is the player's store,
 * 7..12 are the phone's and 13 its store. Seeds move counterclockwise.
 */
data class Board(val pits: List<Int> = List(14) { if (it == 6 || it == 13) 0 else 4 }, val playerTurn: Boolean = true) {
    val over: Boolean get() = (0..5).all { pits[it] == 0 } && (7..12).all { pits[it] == 0 }
    val player: Int get() = pits[6]
    val phone: Int get() = pits[13]

    fun canPlay(pit: Int) = !over && pits[pit] > 0 && (if (playerTurn) pit in 0..5 else pit in 7..12)

    fun play(pit: Int): Board {
        if (!canPlay(pit)) return this
        val next = pits.toMutableList()
        val store = if (playerTurn) 6 else 13
        val skip = if (playerTurn) 13 else 6
        var seeds = next[pit]
        next[pit] = 0
        var at = pit
        while (seeds > 0) {
            at = (at + 1) % 14
            if (at == skip) continue
            next[at]++
            seeds--
        }
        val own = if (playerTurn) at in 0..5 else at in 7..12
        // Ending in an empty pit of your own captures it with the pit across.
        if (own && next[at] == 1 && next[12 - at] > 0) {
            next[store] += next[12 - at] + 1
            next[12 - at] = 0
            next[at] = 0
        }
        // When a side runs out, each player keeps the seeds left on their side.
        if ((0..5).all { next[it] == 0 } || (7..12).all { next[it] == 0 }) {
            for (i in 0..5) { next[6] += next[i]; next[i] = 0 }
            for (i in 7..12) { next[13] += next[i]; next[i] = 0 }
        }
        // The last seed in your own store gives another turn.
        return Board(next, if (at == store) playerTurn else !playerTurn)
    }

    private fun search(depth: Int): Int {
        if (depth == 0 || over) return phone - player
        val results = (0..13).filter(::canPlay).map { play(it).search(depth - 1) }
        return if (playerTurn) results.min() else results.max()
    }

    /** The phone's choice, looking a few moves ahead. */
    fun bestMove(depth: Int = 5): Int? = (7..12).filter(::canPlay).maxByOrNull { play(it).search(depth) }

    fun encode() = pits.joinToString(",") + ";" + playerTurn

    companion object {
        fun decode(text: String): Board? = runCatching {
            val (pits, turn) = text.split(";")
            val values = pits.split(",").map { it.toInt() }
            require(values.size == 14 && values.all { it >= 0 })
            Board(values, turn.toBooleanStrict())
        }.getOrNull()
    }
}
