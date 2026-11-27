package com.nokia.launcher

import com.nokia.launcher.ui.games.Card
import com.nokia.launcher.ui.games.MINE_COUNT
import com.nokia.launcher.ui.games.MinesState
import com.nokia.launcher.ui.games.PinballGame
import com.nokia.launcher.ui.games.SpiderState
import com.nokia.launcher.ui.games.TABLE_HEIGHT
import com.nokia.launcher.ui.games.TABLE_WIDTH
import com.nokia.launcher.ui.games.neighbours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GamesTest {
    @Test
    fun firstMineCellIsAlwaysSafeAndTheBoardRoundTrips() {
        repeat(200) { seed ->
            val start = MinesState().reveal(seed % 99, Random(seed))
            assertFalse(start.lost)
            assertEquals(MINE_COUNT, start.mines.size)
            assertTrue((neighbours(seed % 99) + (seed % 99)).none { it in start.mines })
            assertEquals(start, MinesState.decode(start.encode()))
        }
    }

    @Test
    fun spiderDealsAllCardsAndRoundTrips() {
        val game = SpiderState.new(2, Random(1))
        assertEquals(104, game.columns.sumOf { it.size } + game.stock.size)
        val dealt = game.deal()
        assertEquals(40, dealt.stock.size)
        assertEquals(game, dealt.undo().copy(history = emptyList()))
        assertEquals(dealt.copy(history = emptyList()), SpiderState.decode(dealt.encode()))
    }

    @Test
    fun spiderRemovesAFinishedRunAndTurnsTheCardUnder() {
        val almost = listOf(Card(0, 5)) + (12 downTo 1).map { Card(0, it, true) }
        val columns = listOf(almost, listOf(Card(0, 0, true))) + List(8) { listOf(Card(1, 3, true)) }
        val game = SpiderState(columns, emptyList(), 2).move(1, 0, 0)
        assertEquals(1, game.completed)
        assertEquals(listOf(Card(0, 5, true)), game.columns[0])
    }

    @Test
    fun spiderOnlyMovesSameSuitRunsOntoTheNextRank() {
        val columns = listOf(
            listOf(Card(0, 6, true), Card(1, 5, true)), listOf(Card(0, 7, true)),
        ) + List(8) { emptyList<Card>() }
        val game = SpiderState(columns, emptyList(), 2)
        assertFalse(game.movable(0, 0))
        assertFalse(game.canMove(0, 1, 1))
        assertTrue(game.canMove(0, 1, 2))
    }

    @Test
    fun pinballLaunchEntersTheTableAndNeverEscapes() {
        val game = PinballGame()
        game.launch()
        var reachedTable = false
        var frames = 0
        while (!game.over && frames < 60 * 600) {
            if (game.waiting) game.launch()
            val flip = frames % 40 < 12
            game.step(1f / 60, flip, flip)
            if (game.x in 10f..80f && game.y < 90f) reachedTable = true
            assertTrue(game.x in -5f..TABLE_WIDTH + 5f && game.y in -5f..TABLE_HEIGHT + 10f)
            frames++
        }
        assertTrue(reachedTable)
        assertTrue(game.score > 0)
    }
}
