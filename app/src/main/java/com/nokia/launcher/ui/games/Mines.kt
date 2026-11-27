package com.nokia.launcher.ui.games

import kotlin.math.abs
import kotlin.random.Random

// Taller than wide, like the game area on the screen.
const val MINE_COLUMNS = 9
const val MINE_ROWS = 11
const val MINE_COUNT = 13
private const val MINE_CELLS = MINE_COLUMNS * MINE_ROWS

fun neighbours(cell: Int): List<Int> = (0 until MINE_CELLS).filter {
    it != cell && abs(it % MINE_COLUMNS - cell % MINE_COLUMNS) <= 1 &&
        abs(it / MINE_COLUMNS - cell / MINE_COLUMNS) <= 1
}

/** Mines are placed on the first opening, so that one is always safe. */
data class MinesState(
    val mines: Set<Int> = emptySet(),
    val open: Set<Int> = emptySet(),
    val flags: Set<Int> = emptySet(),
    val cursor: Int = MINE_CELLS / 2,
    val lost: Boolean = false,
    /** When on, a tap marks instead of opening. */
    val marking: Boolean = false,
    /** Time played, shown on the clock. */
    val seconds: Int = 0,
) {
    val running: Boolean get() = mines.isNotEmpty() && !over
    val won: Boolean get() = !lost && open.size == MINE_CELLS - MINE_COUNT
    val over: Boolean get() = lost || won

    fun count(cell: Int) = neighbours(cell).count { it in mines }

    fun move(dx: Int, dy: Int): MinesState {
        val x = (cursor % MINE_COLUMNS + dx + MINE_COLUMNS) % MINE_COLUMNS
        val y = (cursor / MINE_COLUMNS + dy + MINE_ROWS) % MINE_ROWS
        return copy(cursor = y * MINE_COLUMNS + x)
    }

    fun flag(cell: Int): MinesState =
        if (over || cell in open) this
        else copy(flags = if (cell in flags) flags - cell else flags + cell, cursor = cell)

    fun reveal(cell: Int, random: Random = Random): MinesState {
        if (over || cell in flags) return this
        val placed = mines.ifEmpty {
            val safe = neighbours(cell).toSet() + cell
            ((0 until MINE_CELLS) - safe).shuffled(random).take(MINE_COUNT).toSet()
        }
        if (cell in placed) return copy(mines = placed, lost = true, cursor = cell)
        val board = copy(mines = placed)
        val seen = open.toMutableSet()
        val queue = ArrayDeque(listOf(cell))
        // Empty cells open their surroundings.
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (next in flags || !seen.add(next)) continue
            if (board.count(next) == 0) queue.addAll(neighbours(next))
        }
        return board.copy(open = seen, cursor = cell)
    }

    // The leading tag drops games saved with another board size.
    fun encode(): String = listOf(
        "$MINE_COLUMNS×$MINE_ROWS",
        mines.joinToString(","), open.joinToString(","), flags.joinToString(","), cursor, lost, marking, seconds,
    ).joinToString(";")

    companion object {
        fun decode(text: String): MinesState? = runCatching {
            val f = text.split(";")
            require(f[0] == "$MINE_COLUMNS×$MINE_ROWS")
            fun cells(s: String) = s.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
            MinesState(
                cells(f[1]), cells(f[2]), cells(f[3]), f[4].toInt(), f[5].toBoolean(), f[6].toBoolean(), f[7].toInt(),
            )
        }.getOrNull()
    }
}
