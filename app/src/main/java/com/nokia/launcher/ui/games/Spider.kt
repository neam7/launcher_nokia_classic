package com.nokia.launcher.ui.games

import kotlin.random.Random

const val SPIDER_COLUMNS = 10
private const val RANKS = 13

/** `rank` 0 is the ace and 12 the king. */
data class Card(val suit: Int, val rank: Int, val up: Boolean = false)

data class SpiderState(
    val columns: List<List<Card>>,
    val stock: List<Card>,
    val suits: Int,
    val completed: Int = 0,
    val score: Int = 500,
    val moves: Int = 0,
    /** The picked run: column and index of its first card. */
    val picked: Pair<Int, Int>? = null,
    /** Earlier positions, newest last, for undo. Not saved. */
    val history: List<SpiderState> = emptyList(),
) {
    val won: Boolean get() = completed == 8

    /** True when the cards from [index] down can be moved together: face up, one suit, descending. */
    fun movable(column: Int, index: Int): Boolean {
        val run = columns[column].drop(index)
        return run.isNotEmpty() && run.all { it.up } &&
            run.zipWithNext().all { (a, b) -> a.suit == b.suit && a.rank == b.rank + 1 }
    }

    fun canMove(from: Int, index: Int, to: Int): Boolean {
        if (from == to || !movable(from, index)) return false
        val target = columns[to].lastOrNull() ?: return true
        return target.rank == columns[from][index].rank + 1
    }

    fun move(from: Int, index: Int, to: Int): SpiderState {
        if (!canMove(from, index, to)) return this
        val next = columns.toMutableList()
        next[to] = next[to] + next[from].drop(index)
        next[from] = next[from].take(index)
        return remember().copy(columns = next, score = score - 1, moves = moves + 1, picked = null).tidy()
    }

    /** One more card on every column; all of them must have at least one. */
    fun deal(): SpiderState {
        if (stock.isEmpty() || columns.any { it.isEmpty() }) return this
        val next = columns.mapIndexed { i, column ->
            if (i < stock.size) column + stock[i].copy(up = true) else column
        }
        return remember().copy(columns = next, stock = stock.drop(SPIDER_COLUMNS), moves = moves + 1, picked = null).tidy()
    }

    fun undo(): SpiderState = history.lastOrNull()?.copy(history = history.dropLast(1), picked = null) ?: this

    /**
     * First tap picks a run, the second drops it. Tapping the picked run again sends it to the
     * best column: same suit first, an empty one last.
     */
    fun tap(column: Int, index: Int): SpiderState {
        val from = picked
        if (from != null) {
            val (source, start) = from
            if (source == column) {
                val card = columns[source][start]
                val best = (0 until SPIDER_COLUMNS).filter { canMove(source, start, it) }.minByOrNull {
                    val top = columns[it].lastOrNull()
                    if (top == null) 2 else if (top.suit == card.suit) 0 else 1
                }
                return if (best == null) copy(picked = null) else move(source, start, best)
            }
            if (canMove(source, start, column)) return move(source, start, column)
        }
        val at = if (movable(column, index)) index else columns[column].lastIndex
        return copy(picked = if (at >= 0) column to at else null)
    }

    /** Picks the longest movable run of a column, for the number keys. */
    fun tapColumn(column: Int): SpiderState {
        val start = columns[column].indices.firstOrNull { movable(column, it) } ?: -1
        return tap(column, start)
    }

    private fun remember() = copy(history = (history + copy(history = emptyList(), picked = null)).takeLast(50))

    /** Removes finished king-to-ace runs and turns over uncovered cards. */
    private fun tidy(): SpiderState {
        var done = 0
        val next = columns.map { column ->
            var cards = column
            val start = cards.size - RANKS
            if (start >= 0 && cards[start].rank == RANKS - 1 && copy(columns = listOf(cards)).movable(0, start)) {
                cards = cards.take(start)
                done++
            }
            val last = cards.lastOrNull()
            if (last != null && !last.up) cards.dropLast(1) + last.copy(up = true) else cards
        }
        return copy(columns = next, completed = completed + done, score = score + done * 100)
    }

    fun encode(): String {
        fun cards(list: List<Card>) = list.joinToString(",") { "${it.suit}.${it.rank}.${if (it.up) 1 else 0}" }
        return listOf(columns.joinToString("|") { cards(it) }, cards(stock), suits, completed, score, moves)
            .joinToString(";")
    }

    companion object {
        /** Two decks (104 cards) made of 1, 2 or 4 suits. */
        fun new(suits: Int, random: Random = Random): SpiderState {
            val deck = List(104) { Card((it / RANKS) % suits, it % RANKS) }.shuffled(random)
            var used = 0
            val columns = List(SPIDER_COLUMNS) { i ->
                val size = if (i < 4) 6 else 5
                val column = deck.subList(used, used + size).toList()
                used += size
                column.dropLast(1) + column.last().copy(up = true)
            }
            return SpiderState(columns, deck.drop(used), suits)
        }

        fun decode(text: String): SpiderState? = runCatching {
            val f = text.split(";")
            fun cards(s: String) = s.split(",").filter { it.isNotEmpty() }.map { card ->
                val (suit, rank, up) = card.split(".").map { it.toInt() }
                require(suit in 0..3 && rank in 0 until RANKS)
                Card(suit, rank, up == 1)
            }
            val columns = f[0].split("|").map(::cards)
            require(columns.size == SPIDER_COLUMNS)
            SpiderState(columns, cards(f[1]), f[2].toInt(), f[3].toInt(), f[4].toInt(), f.getOrNull(5)?.toInt() ?: 0)
        }.getOrNull()
    }
}
