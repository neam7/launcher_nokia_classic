package com.nokia.launcher

import com.nokia.launcher.ui.games.BLOCK_COLUMNS
import com.nokia.launcher.ui.games.BLOCK_ROWS
import com.nokia.launcher.ui.games.BlocksState
import com.nokia.launcher.ui.games.Board
import com.nokia.launcher.ui.games.PairsState
import com.nokia.launcher.ui.games.Piece
import com.nokia.launcher.ui.games.SPACE_WIDTH
import com.nokia.launcher.ui.games.SpaceWar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NewGamesTest {
    @Test
    fun bantumiGivesAnExtraTurnAndCaptures() {
        // Pit 2 holds 4 seeds and ends exactly in the store.
        assertTrue(Board().play(2).playerTurn)
        val pits = MutableList(14) { 0 }.also { it[0] = 1; it[11] = 5; it[3] = 1; it[9] = 1 }
        val captured = Board(pits).play(0)
        assertEquals(6, captured.player)
        assertEquals(0, captured.pits[11])
    }

    @Test
    fun bantumiAlwaysFinishesKeepingAllSeeds() {
        val random = Random(3)
        repeat(20) {
            var board = Board()
            var turns = 0
            while (!board.over && turns++ < 300) {
                val pit = if (board.playerTurn) (0..5).filter(board::canPlay).random(random) else board.bestMove(2)!!
                board = board.play(pit)
                assertEquals(48, board.pits.sum())
            }
            assertTrue(board.over)
            assertEquals(board, Board.decode(board.encode()))
        }
    }

    @Test
    fun pairsKeepsMatchesAndTurnsMissesBack() {
        val start = PairsState(List(20) { it / 2 })
        assertEquals(setOf(0, 1), start.flip(0).flip(1).found)
        val miss = start.flip(0).flip(2)
        assertTrue(miss.missed)
        assertEquals(listOf(4), miss.flip(4).shown)
        assertEquals(PairsState.new(Random(1)), PairsState.decode(PairsState.new(Random(1)).encode()))
    }

    @Test
    fun blocksClearAFullRowAndRoundTrip() {
        // The bottom row misses four cells, which a flat I piece fills.
        val grid = List(BLOCK_COLUMNS * BLOCK_ROWS) { if (it / BLOCK_COLUMNS == BLOCK_ROWS - 1 && it % BLOCK_COLUMNS > 3) 1 else 0 }
        val state = BlocksState(grid, Piece(0, x = 0), queue = listOf(1, 2, 3, 4), paused = false).drop(Random(1))
        assertEquals(1, state.lines)
        assertTrue(state.grid.all { it == 0 })
        assertEquals(state.copy(paused = true), BlocksState.decode(state.encode()))
    }

    @Test
    fun blocksEndWhenTheWellFillsUp() {
        var state = BlocksState.new(Random(2)).copy(paused = false)
        repeat(200) { state = state.rotate().move(-1).drop(Random(it)) }
        assertTrue(state.over)
    }

    @Test
    fun holdSwapsOncePerPiece() {
        val start = BlocksState.new(Random(4)).copy(paused = false)
        val kept = start.swap(Random(1))
        assertEquals(start.piece.kind, kept.hold)
        assertEquals(start.queue.first(), kept.piece.kind)
        assertEquals(kept, kept.swap(Random(1)))
        assertEquals(kept.copy(paused = true), BlocksState.decode(kept.encode()))
    }

    @Test
    fun spaceWarScoresAndEventuallyEnds() {
        val war = SpaceWar(Random(5))
        var frames = 0
        while (!war.over && frames < 60 * 900) {
            // Follows the nearest enemy, like a simple player.
            war.enemies.maxByOrNull { it.y }?.let { war.moveTo(it.x, war.height - 10f) }
            war.step(1f / 60)
            assertTrue(war.shipX in 0f..SPACE_WIDTH && war.shipY <= war.height)
            frames++
        }
        assertTrue(war.score > 0)
        assertEquals(war.encode(), SpaceWar().apply { load(war.encode()) }.encode())
    }
}
