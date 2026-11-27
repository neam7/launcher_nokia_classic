package com.nokia.launcher.ui.games

import kotlin.random.Random

// Taller than wide, like the game area on the screen.
const val SNAKE_COLUMNS = 18
const val SNAKE_ROWS = 22
private const val CELLS = SNAKE_COLUMNS * SNAKE_ROWS
const val BONUS_EVERY = 4
const val BONUS_TICKS = 40

/** Everyday food, one point each. */
val FOODS = listOf("🍒", "🍎", "🍌", "🍇", "🍓", "🍉", "🍍", "🥝")

data class Prize(val icon: String, val points: Int)

/** Timed prizes with Unix-world mascots: Tux, Mac, OpenBSD, Rust, Python, PostgreSQL, Docker, BSD. */
val PRIZES = listOf(
    Prize("🐧", 5), Prize("🍏", 5), Prize("🐡", 6), Prize("🦀", 6),
    Prize("🐍", 7), Prize("🐘", 7), Prize("🐳", 8), Prize("😈", 10),
)

enum class Direction(val dx: Int, val dy: Int) {
    Up(0, -1), Down(0, 1), Left(-1, 0), Right(1, 0);

    fun isOpposite(other: Direction) = dx + other.dx == 0 && dy + other.dy == 0
}

/** Cells are `y * SNAKE_COLUMNS + x`; the head is first. The edges wrap around. */
data class SnakeState(
    val body: List<Int>,
    val heading: Direction = Direction.Right,
    val next: Direction = Direction.Right,
    val food: Int,
    val score: Int = 0,
    val over: Boolean = false,
    val paused: Boolean = false,
    val eaten: Int = 0,
    val bonus: Int = -1,
    val bonusTicks: Int = 0,
    val foodKind: Int = 0,
    val bonusKind: Int = 0,
    /** A second turn pressed before the first one was applied. */
    val after: Direction? = null,
    /** Where the tail was before the last move, for the smooth animation. */
    val lastTail: Int = body.last(),
) {
    val tickMs: Long get() = (210L - eaten * 4L).coerceAtLeast(90L)

    /** A game worth continuing. */
    val inProgress: Boolean get() = !over && (score > 0 || body != new(Random(0)).body)

    // Reversal is checked against the turn before it, so quick presses cannot fold into the body.
    fun turn(direction: Direction): SnakeState = when {
        paused -> if (direction.isOpposite(heading)) copy(paused = false)
        else copy(next = direction, after = null, paused = false)
        next == heading -> if (direction.isOpposite(heading)) this else copy(next = direction)
        direction == next || direction.isOpposite(next) -> this
        else -> copy(after = direction)
    }

    fun step(random: Random = Random): SnakeState {
        if (over || paused) return this
        val x = (body.first() % SNAKE_COLUMNS + next.dx + SNAKE_COLUMNS) % SNAKE_COLUMNS
        val y = (body.first() / SNAKE_COLUMNS + next.dy + SNAKE_ROWS) % SNAKE_ROWS
        val head = y * SNAKE_COLUMNS + x
        val eatsFood = head == food
        val eatsBonus = head == bonus
        val tail = if (eatsFood) body else body.dropLast(1) // the tail cell frees up this turn
        if (head in tail) return copy(over = true)
        val moved = listOf(head) + tail
        val count = if (eatsFood) eaten + 1 else eaten
        var newBonus = if (eatsBonus || bonusTicks <= 1) -1 else bonus
        var ticks = if (newBonus == -1) 0 else bonusTicks - 1
        var kind = bonusKind
        var newFood = food
        var newFoodKind = foodKind
        if (eatsFood) {
            newFood = freeCell(moved + newBonus, random) ?: -1
            newFoodKind = random.nextInt(FOODS.size)
            if (count % BONUS_EVERY == 0 && newFood != -1) {
                newBonus = freeCell(moved + newFood, random) ?: -1
                ticks = if (newBonus == -1) 0 else BONUS_TICKS
                kind = random.nextInt(PRIZES.size)
            }
        }
        return copy(
            body = moved,
            heading = next,
            next = after ?: next,
            after = null,
            food = newFood,
            foodKind = newFoodKind,
            score = score + (if (eatsFood) 1 else 0) + (if (eatsBonus) PRIZES[bonusKind].points else 0),
            over = newFood == -1, // board full
            eaten = count,
            bonus = newBonus,
            bonusTicks = ticks,
            bonusKind = kind,
            lastTail = body.last(),
        )
    }

    /** One text format for rotation and for the saved game. */
    fun encode(): String = listOf(
        body.joinToString(","), heading.name, next.name, food, score, over, eaten, bonus, bonusTicks,
        foodKind, bonusKind,
    ).joinToString(";")

    companion object {
        /** Starts paused so the player is ready. */
        fun new(random: Random = Random): SnakeState {
            val row = SNAKE_ROWS / 2 * SNAKE_COLUMNS
            val body = listOf(row + 5, row + 4, row + 3)
            return SnakeState(body, food = freeCell(body, random)!!, paused = true)
        }

        /** Returns a paused game, or null if the text is not a valid one. */
        fun decode(text: String): SnakeState? = runCatching {
            val f = text.split(";")
            val body = f[0].split(",").map { it.toInt() }
            val food = f[3].toInt()
            val bonus = f[7].toInt()
            require(body.all { it in 0 until CELLS } && food in -1 until CELLS && bonus in -1 until CELLS)
            SnakeState(
                body, Direction.valueOf(f[1]), Direction.valueOf(f[2]), food, f[4].toInt(),
                f[5].toBooleanStrict(), paused = true, eaten = f[6].toInt(), bonus = bonus,
                bonusTicks = f[8].toInt(),
                foodKind = f[9].toInt().also { require(it in FOODS.indices) },
                bonusKind = f[10].toInt().also { require(it in PRIZES.indices) },
            )
        }.getOrNull()

        private fun freeCell(taken: List<Int>, random: Random): Int? =
            ((0 until CELLS) - taken.toSet()).randomOrNull(random)
    }
}
