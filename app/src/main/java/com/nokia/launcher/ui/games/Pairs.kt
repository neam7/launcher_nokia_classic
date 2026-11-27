package com.nokia.launcher.ui.games

import kotlin.random.Random

const val PAIR_COLUMNS = 4
private const val PAIR_COUNT = 10

/** Memory: [cards] holds picture numbers; each appears twice. */
data class PairsState(
    val cards: List<Int>,
    val found: Set<Int> = emptySet(),
    val shown: List<Int> = emptyList(),
    val moves: Int = 0,
    val cursor: Int = 0,
) {
    val won: Boolean get() = found.size == cards.size

    /** Two different cards are up and must be turned back. */
    val missed: Boolean get() = shown.size == 2

    fun flip(index: Int): PairsState {
        val state = if (missed) hide() else this
        if (index in state.found || index in state.shown) return state.copy(cursor = index)
        val up = state.shown + index
        if (up.size < 2) return state.copy(shown = up, cursor = index)
        val match = state.cards[up[0]] == state.cards[up[1]]
        return state.copy(
            found = if (match) state.found + up else state.found,
            shown = if (match) emptyList() else up,
            moves = state.moves + 1,
            cursor = index,
        )
    }

    fun hide() = copy(shown = emptyList())

    fun move(dx: Int, dy: Int): PairsState {
        val rows = cards.size / PAIR_COLUMNS
        val x = (cursor % PAIR_COLUMNS + dx + PAIR_COLUMNS) % PAIR_COLUMNS
        val y = (cursor / PAIR_COLUMNS + dy + rows) % rows
        return copy(cursor = y * PAIR_COLUMNS + x)
    }

    fun encode() = listOf(
        cards.joinToString(","), found.joinToString(","), moves, cursor,
    ).joinToString(";")

    companion object {
        fun new(random: Random = Random) = PairsState(List(PAIR_COUNT * 2) { it / 2 }.shuffled(random))

        fun decode(text: String): PairsState? = runCatching {
            val f = text.split(";")
            fun numbers(s: String) = s.split(",").filter { it.isNotEmpty() }.map { it.toInt() }
            val cards = numbers(f[0])
            require(cards.size == PAIR_COUNT * 2 && cards.all { it in 0 until PAIR_COUNT })
            PairsState(cards, numbers(f[1]).toSet(), moves = f[2].toInt(), cursor = f[3].toInt().coerceIn(cards.indices))
        }.getOrNull()
    }
}
