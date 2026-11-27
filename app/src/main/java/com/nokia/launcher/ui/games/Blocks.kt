package com.nokia.launcher.ui.games

import kotlin.random.Random

const val BLOCK_COLUMNS = 10
const val BLOCK_ROWS = 20
/** How many coming pieces are shown. */
const val PREVIEW = 3

/** The seven four-square pieces, as cells inside a square box that is [size] wide. */
private val shapes = listOf(
    4 to listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1), // I
    2 to listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), // O
    3 to listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1), // T
    3 to listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1), // S
    3 to listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), // Z
    3 to listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1), // J
    3 to listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1), // L
)

data class Piece(val kind: Int, val turn: Int = 0, val x: Int = 3, val y: Int = 0) {
    val cells: List<Pair<Int, Int>>
        get() {
            val (size, base) = shapes[kind]
            var result = base
            repeat(turn % 4) { result = result.map { (cx, cy) -> size - 1 - cy to cx } }
            return result.map { (cx, cy) -> x + cx to y + cy }
        }
}

/** Pieces come in shuffled sets of seven, so none stays away for long. */
private fun bag(random: Random) = shapes.indices.shuffled(random)

/** [grid] holds 0 for empty, or a piece kind plus one. [queue] holds the coming pieces. */
data class BlocksState(
    val grid: List<Int> = List(BLOCK_COLUMNS * BLOCK_ROWS) { 0 },
    val piece: Piece,
    val queue: List<Int>,
    /** The piece set aside, or -1. */
    val hold: Int = -1,
    /** A piece can be swapped only once before it lands. */
    val swapped: Boolean = false,
    val score: Int = 0,
    val lines: Int = 0,
    val over: Boolean = false,
    val paused: Boolean = true,
) {
    val level: Int get() = lines / 10 + 1
    val tickMs: Long get() = (700L - (level - 1) * 60L).coerceAtLeast(110L)

    /** Where the piece would land. */
    val ghost: Piece
        get() {
            var landed = piece
            while (fits(landed.copy(y = landed.y + 1))) landed = landed.copy(y = landed.y + 1)
            return landed
        }

    /** Sets the falling piece aside and brings back the one kept before, or the next one. */
    fun swap(random: Random = Random): BlocksState {
        if (over || paused || swapped) return this
        val state = if (hold >= 0) copy(piece = Piece(hold)) else take(random)
        return state.copy(hold = piece.kind, swapped = true, over = !state.fits(state.piece))
    }

    private fun take(random: Random): BlocksState {
        val rest = queue.drop(1)
        return copy(piece = Piece(queue.first()), queue = if (rest.size > PREVIEW) rest else rest + bag(random))
    }

    private fun fits(candidate: Piece) = candidate.cells.all { (x, y) ->
        x in 0 until BLOCK_COLUMNS && y in 0 until BLOCK_ROWS && grid[y * BLOCK_COLUMNS + x] == 0
    }

    private fun ifFits(candidate: Piece) = if (!over && !paused && fits(candidate)) copy(piece = candidate) else this

    fun move(dx: Int) = ifFits(piece.copy(x = piece.x + dx))

    /** Turns the piece, nudging it sideways if a wall is in the way. */
    fun rotate(): BlocksState {
        if (over || paused) return this
        val turned = piece.copy(turn = piece.turn + 1)
        return listOf(0, -1, 1, -2, 2).map { turned.copy(x = turned.x + it) }.firstOrNull(::fits)
            ?.let { copy(piece = it) } ?: this
    }

    /** One row down; a piece that cannot fall is fixed in place. */
    fun step(random: Random = Random): BlocksState {
        if (over || paused) return this
        val lower = piece.copy(y = piece.y + 1)
        return if (fits(lower)) copy(piece = lower) else settle(random)
    }

    fun drop(random: Random = Random): BlocksState {
        if (over || paused) return this
        val landed = ghost
        return copy(piece = landed, score = score + (landed.y - piece.y) * 2).settle(random)
    }

    private fun settle(random: Random): BlocksState {
        val filled = grid.toMutableList()
        piece.cells.forEach { (x, y) -> filled[y * BLOCK_COLUMNS + x] = piece.kind + 1 }
        val kept = filled.chunked(BLOCK_COLUMNS).filter { row -> row.any { it == 0 } }
        val cleared = BLOCK_ROWS - kept.size
        val board = List(cleared * BLOCK_COLUMNS) { 0 } + kept.flatten()
        val spawned = copy(
            grid = board,
            swapped = false,
            score = score + listOf(0, 100, 300, 500, 800)[cleared] * level,
            lines = lines + cleared,
        ).take(random)
        return if (spawned.fits(spawned.piece)) spawned else spawned.copy(over = true)
    }

    fun encode() = listOf(
        grid.joinToString(""), piece.kind, piece.turn % 4, piece.x, piece.y, queue.joinToString(""), score, lines, over,
        hold, swapped,
    ).joinToString(";")

    companion object {
        fun new(random: Random = Random): BlocksState {
            val first = bag(random)
            return BlocksState(piece = Piece(first.first()), queue = first.drop(1))
        }

        /** A restored game comes back paused. */
        fun decode(text: String): BlocksState? = runCatching {
            val f = text.split(";")
            val grid = f[0].map { it.digitToInt() }
            val piece = Piece(f[1].toInt(), f[2].toInt(), f[3].toInt(), f[4].toInt())
            val queue = f[5].map { it.digitToInt() }
            val hold = f[9].toInt()
            require(grid.size == BLOCK_COLUMNS * BLOCK_ROWS && grid.all { it in 0..shapes.size })
            require(piece.kind in shapes.indices && queue.size >= PREVIEW && queue.all { it in shapes.indices })
            require(hold in -1 until shapes.size)
            require(piece.cells.all { (x, y) -> x in 0 until BLOCK_COLUMNS && y in 0 until BLOCK_ROWS })
            BlocksState(
                grid, piece, queue, hold, f[10].toBooleanStrict(), f[6].toInt(), f[7].toInt(),
                f[8].toBooleanStrict(), paused = true,
            )
        }.getOrNull()
    }
}
