package com.nokia.launcher

import com.nokia.launcher.ui.games.BONUS_EVERY
import com.nokia.launcher.ui.games.PRIZES
import com.nokia.launcher.ui.games.Direction
import com.nokia.launcher.ui.games.SNAKE_COLUMNS
import com.nokia.launcher.ui.games.SnakeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeTest {
    private val start = SnakeState(body = listOf(45, 44, 43), food = 0)
    private val width = SNAKE_COLUMNS

    @Test
    fun movesAndGrowsWhenEating() {
        assertEquals(listOf(46, 45, 44), start.step().body)
        val fed = start.copy(food = 46).step()
        assertEquals(listOf(46, 45, 44, 43), fed.body)
        assertEquals(1, fed.score)
        assertFalse(fed.food in fed.body)
    }

    @Test
    fun cannotReverseEvenWithTwoQuickPresses() {
        assertEquals(Direction.Right, start.turn(Direction.Left).next)
        assertEquals(Direction.Up, start.turn(Direction.Up).turn(Direction.Left).next)
    }

    @Test
    fun edgesWrapAndOwnTailCellIsFree() {
        val edge = SnakeState(listOf(width - 1), food = 50)
        assertEquals(listOf(0), edge.step().body)
        // 2x2 loop: the head moves into the cell the tail is leaving.
        val loop = SnakeState(listOf(41, 41 + width, 42 + width, 42), Direction.Up, Direction.Right, food = 0)
        assertFalse(loop.step().over)
    }

    @Test
    fun bitingTheBodyEndsTheGame() {
        val loop = SnakeState(listOf(41, 41 + width, 42 + width, 42, 43), Direction.Up, Direction.Right, food = 0)
        assertTrue(loop.step().over)
    }

    @Test
    fun everyFewFruitsBringATimedPrize() {
        val due = start.copy(food = 46, eaten = BONUS_EVERY - 1).step()
        assertNotEquals(-1, due.bonus)
        assertEquals(-1, start.copy(bonus = 100, bonusTicks = 1).step().bonus)
        assertEquals(PRIZES[0].points, start.copy(bonus = 46, bonusTicks = 9).step().score)
    }

    @Test
    fun twoQuickTurnsAreBothApplied() {
        val first = start.turn(Direction.Up).turn(Direction.Left).step()
        assertEquals(Direction.Up, first.heading)
        assertEquals(Direction.Left, first.step().heading)
    }

    @Test
    fun anySwipeResumesAPausedGame() {
        assertFalse(start.copy(paused = true).turn(Direction.Left).paused)
    }

    @Test
    fun savedGameRoundTripsAndRejectsGarbage() {
        val game = start.copy(score = 7, bonus = 100, bonusTicks = 5, bonusKind = 2, foodKind = 3)
        assertEquals(game.copy(paused = true), SnakeState.decode(game.encode()))
        assertNull(SnakeState.decode("nonsense"))
        assertNull(SnakeState.decode(game.encode().replace("45,44,43", "9999")))
    }
}
