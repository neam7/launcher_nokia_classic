package com.nokia.launcher.ui.games

import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.theme.LauncherTheme
import kotlin.random.Random

/** The green cloth of the card table. */
private val Felt = Color(0xFF007F00)

private val suitMarks = listOf("♠", "♥", "♣", "♦")
private val suitColors = listOf(Color(0xFF14161C), Color(0xFFD0102F), Color(0xFF14161C), Color(0xFFD0102F))
private val rankNames = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
private val GoldTrim = Color(0xFFF6C445)
private const val CARD_RATIO = 1.5f

/** Vertical offset of every card; long columns are squeezed to fit [height]. */
private fun offsets(cards: List<Card>, width: Float, height: Float): List<Float> {
    val steps = cards.dropLast(1).map { if (it.up) width * 0.68f else width * 0.24f }
    val room = (height - width * CARD_RATIO).coerceAtLeast(0f)
    val scale = minOf(1f, room / steps.sum().coerceAtLeast(1f))
    return steps.runningFold(0f) { y, step -> y + step * scale }
}

/** The bottom strip holds the finished suits and the stock. */
private fun tableHeight(width: Float, height: Float) = height - width / SPIDER_COLUMNS * CARD_RATIO - width / 40

private data class Drag(val column: Int, val index: Int, val start: Offset, val now: Offset)

@Composable
fun ColumnScope.SpiderContent(
    state: SpiderState,
    onTap: (Int, Int) -> Unit,
    onMove: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onDeal: () -> Unit = {},
    onUndo: () -> Unit = {},
) {
    val current by rememberUpdatedState(state)
    val tap by rememberUpdatedState(onTap)
    val move by rememberUpdatedState(onMove)
    val deal by rememberUpdatedState(onDeal)
    val undo by rememberUpdatedState(onUndo)
    var drag by remember { mutableStateOf<Drag?>(null) }
    val scoreText = stringResource(R.string.spider_score, state.score)
    val movesText = stringResource(R.string.spider_moves, state.moves)
    val wonText = stringResource(R.string.spider_won)
    val description = stringResource(R.string.spider)
Box(Modifier.weight(1f).fillMaxWidth()) {
    Canvas(
        Modifier.fillMaxSize().systemGestureExclusion().semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val width = size.width / SPIDER_COLUMNS.toFloat()
                    val table = tableHeight(size.width.toFloat(), size.height.toFloat())
                    val column = (down.position.x / width).toInt().coerceIn(0, SPIDER_COLUMNS - 1)
                    val cards = current.columns[column]
                    val index = if (cards.isEmpty()) -1
                    else offsets(cards, width, table).indexOfLast { it <= down.position.y }.coerceAtLeast(0)
                    val canDrag = down.position.y < table && index >= 0 && current.movable(column, index)
                    var last = down.position
                    var dragging = false
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        last = change.position
                        if (!dragging && (last - down.position).getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                        }
                        if (dragging && canDrag) drag = Drag(column, index, down.position, last)
                        change.consume()
                        if (!change.pressed) break
                    }
                    drag = null
                    when {
                        dragging && canDrag -> {
                            // The middle of the dragged card decides the column.
                            val ys = offsets(cards, width, table)
                            val middle = column * width + width / 2 + last.x - down.position.x
                            val target = (middle / width).toInt().coerceIn(0, SPIDER_COLUMNS - 1)
                            if (ys.isNotEmpty() && target != column) move(column, index, target)
                        }
                        down.position.y >= table -> when {
                            down.position.x > size.width * 0.75f -> deal()
                            down.position.x > size.width * 0.59f -> undo()
                        }
                        !dragging -> tap(column, index)
                    }
                }
            },
    ) {
        drawRect(Felt)
        val width = size.width / SPIDER_COLUMNS
        val table = tableHeight(size.width, size.height)
        val held = drag
        // The run being moved: dragged, or picked with a tap or a number key.
        val source = held?.let { it.column to it.index } ?: state.picked
        state.columns.forEachIndexed { c, cards ->
            val x = c * width
            val ys = offsets(cards, width, table)
            val target = source != null && state.canMove(source.first, source.second, c)
            if (cards.isEmpty()) slot(Offset(x, 0f), width, if (target) GoldTrim else Color.White.copy(alpha = 0.25f))
            cards.forEachIndexed { i, card ->
                val moving = held != null && held.column == c && i >= held.index
                if (!moving) {
                    val picked = held == null && state.picked?.let { it.first == c && i >= it.second } == true
                    playingCard(card, Offset(x, ys[i]), width, picked)
                }
            }
            if (target && cards.isNotEmpty()) slot(Offset(x, ys.last()), width, GoldTrim)
            if (c < 9 && cards.isEmpty()) icon("${c + 1}", Offset(x + width / 2, width * 0.7f), width * 0.4f, Color.White.copy(alpha = 0.4f))
        }
        // Finished suits on the left, stock on the right.
        val bottom = size.height - width * CARD_RATIO - width * 0.08f
        repeat(8) { i ->
            val at = Offset(i * width * 0.28f + width * 0.1f, bottom)
            if (i < state.completed) playingCard(Card(i % state.suits, 12, true), at, width, false)
            else if (i == state.completed) slot(at, width, Color.White.copy(alpha = 0.2f))
        }
        val deals = state.stock.size / SPIDER_COLUMNS
        repeat(deals) { i ->
            playingCard(Card(0, 0), Offset(size.width - width * 1.15f - (deals - 1 - i) * width * 0.3f, bottom), width, false)
        }
        // The score box and the undo button sit between the two piles.
        val box = Size(size.width * 0.26f, width * CARD_RATIO * 0.82f)
        val boxAt = Offset(size.width * 0.32f, bottom + (width * CARD_RATIO - box.height) / 2)
        val undoAt = Offset(size.width * 0.665f, boxAt.y + box.height / 2)
        drawCircle(Color(0xFF006200), width * 0.55f, undoAt)
        drawCircle(Color.White.copy(alpha = 0.7f), width * 0.55f, undoAt, style = Stroke(1.5f))
        icon("↶", undoAt, width * 0.8f, if (state.history.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White)
        drawRect(Color(0xFF006200), boxAt, box)
        drawRect(Color.White.copy(alpha = 0.7f), boxAt, box, style = Stroke(1.5f))
        val letters = minOf(box.height * 0.28f, box.width * 0.13f)
        icon(scoreText, boxAt + Offset(box.width / 2, box.height * 0.32f), letters)
        icon(movesText, boxAt + Offset(box.width / 2, box.height * 0.7f), letters)
        if (held != null) {
            val ys = offsets(state.columns[held.column], width, table)
            val shift = held.now - held.start
            state.columns[held.column].drop(held.index).forEachIndexed { i, card ->
                playingCard(card, Offset(held.column * width, ys[held.index + i]) + shift, width, true)
            }
        }
        if (state.won) {
            drawRect(Color(0xCC000000), Offset(0f, size.height * 0.42f), Size(size.width, size.height * 0.12f))
            icon(wonText, Offset(size.width / 2, size.height * 0.48f), minOf(size.height * 0.04f, size.width * 1.6f / wonText.length), GoldTrim)
        }
    }
    }
}

private fun DrawScope.slot(at: Offset, width: Float, color: Color) = drawRoundRect(
    color, at + Offset(2f, 1f), Size(width - 4f, width * CARD_RATIO - 2f), CornerRadius(width * 0.14f),
    style = Stroke(width * 0.06f),
)

private fun DrawScope.playingCard(card: Card, at: Offset, width: Float, lifted: Boolean) {
    val origin = at + Offset(1.5f, 0f)
    val face = Size(width - 3f, width * CARD_RATIO)
    val round = CornerRadius(width * 0.09f)
    if (!card.up) {
        drawRoundRect(Color.White, origin, face, round)
        val inset = width * 0.07f
        val inner = Size(face.width - inset * 2, face.height - inset * 2)
        val corner = origin + Offset(inset, inset)
        drawRect(Brush.linearGradient(listOf(Color(0xFF3D6FD8), Color(0xFF0B2A86)), corner, corner + Offset(inner.width, inner.height)), corner, inner)
        // A woven pattern with rings, like the old card backs.
        clipRect(corner.x, corner.y, corner.x + inner.width, corner.y + inner.height) {
            var k = -face.height
            while (k < face.width + face.height) {
                val line = Color(0x66BFD4FF)
                drawLine(line, origin + Offset(k, 0f), origin + Offset(k + face.height, face.height), width * 0.025f)
                drawLine(line, origin + Offset(k, face.height), origin + Offset(k + face.height, 0f), width * 0.025f)
                k += width * 0.2f
            }
            val middle = origin + Offset(face.width / 2, face.height / 2)
            for (r in 1..3) drawCircle(Color(0xAAFFE08A), width * 0.11f * r, middle, style = Stroke(width * 0.025f))
        }
        drawRoundRect(Color.Black, origin, face, round, style = Stroke(1.5f))
        return
    }
    drawRoundRect(if (lifted) Color(0xFFD6E4FF) else Color.White, origin, face, round)
    drawRoundRect(
        if (lifted) Color(0xFF0A246A) else Color.Black, origin, face, round,
        style = Stroke(if (lifted) width * 0.07f else 1.5f),
    )
    val color = suitColors[card.suit]
    val mark = suitMarks[card.suit]
    // Rank and suit share the top strip, the part left visible when covered.
    icon(rankNames[card.rank], origin + Offset(width * 0.27f, width * 0.28f), width * 0.46f, color)
    icon(mark, origin + Offset(width * 0.73f, width * 0.28f), width * 0.4f, color)
    val body = origin + Offset(width * 0.1f, width * 0.56f)
    val area = Size(face.width - width * 0.2f, face.height - width * 0.64f)
    when {
        card.rank >= 10 -> courtCard(card.rank, mark, color, body, area)
        card.rank == 0 -> icon(mark, body + Offset(area.width / 2, area.height / 2), width * 0.72f, color)
        else -> pips[card.rank - 1].forEach { (x, y) ->
            icon(mark, body + Offset(area.width * x, area.height * y), width * if (card.rank > 5) 0.27f else 0.32f, color)
        }
    }
}

/** Where the suit marks go on cards 2 to 10, as fractions of the card's body. */
private val pips: List<List<Pair<Float, Float>>> = run {
    val l = 0.22f
    val c = 0.5f
    val r = 0.78f
    val two = listOf(c to 0.14f, c to 0.86f)
    val three = two + (c to 0.5f)
    val four = listOf(l to 0.14f, r to 0.14f, l to 0.86f, r to 0.86f)
    val five = four + (c to 0.5f)
    val six = four + listOf(l to 0.5f, r to 0.5f)
    val seven = six + (c to 0.32f)
    val eight = seven + (c to 0.68f)
    val nineBase = listOf(0.1f, 0.37f, 0.63f, 0.9f).flatMap { listOf(l to it, r to it) }
    listOf(two, three, four, five, six, seven, eight, nineBase + (c to 0.5f), nineBase + listOf(c to 0.235f, c to 0.765f))
}

/** Jack, queen and king: a framed figure with its own hat, face and robe. */
private fun DrawScope.courtCard(rank: Int, mark: String, color: Color, at: Offset, area: Size) {
    val w = area.width
    val h = area.height
    fun point(x: Float, y: Float) = at + Offset(w * x, h * y)
    fun shape(fill: Color, vararg points: Pair<Float, Float>) {
        val path = Path().apply {
            points.forEachIndexed { i, (x, y) ->
                val p = point(x, y)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(path, fill)
        drawPath(path, Color.Black, style = Stroke(1f))
    }
    val gold = Color(0xFFF2C21B)
    val robe = if (color == suitColors[0]) Color(0xFF1F4E9E) else Color(0xFFC0182C)
    drawRect(Color(0xFFFFF3C4), at, area)
    drawRect(color, at, area, style = Stroke(1.5f))
    shape(robe, 0.1f to 1f, 0.28f to 0.62f, 0.72f to 0.62f, 0.9f to 1f)
    shape(gold, 0.42f to 0.62f, 0.58f to 0.62f, 0.53f to 1f, 0.47f to 1f)
    // Hair, face and eyes.
    drawOval(if (rank == 11) Color(0xFF8A4B14) else Color(0xFFD9962B), point(0.26f, 0.26f), Size(w * 0.48f, h * 0.38f))
    drawOval(Color(0xFFFFD9B3), point(0.34f, 0.3f), Size(w * 0.32f, h * 0.32f))
    drawCircle(Color.Black, w * 0.03f, point(0.43f, 0.43f))
    drawCircle(Color.Black, w * 0.03f, point(0.57f, 0.43f))
    when (rank) {
        10 -> shape(robe, 0.26f to 0.33f, 0.38f to 0.14f, 0.72f to 0.2f, 0.72f to 0.33f) // jack's cap
        11 -> shape(gold, 0.3f to 0.31f, 0.33f to 0.12f, 0.42f to 0.23f, 0.5f to 0.08f, 0.58f to 0.23f, 0.67f to 0.12f, 0.7f to 0.31f)
        else -> {
            shape(gold, 0.26f to 0.31f, 0.24f to 0.07f, 0.38f to 0.19f, 0.5f to 0.04f, 0.62f to 0.19f, 0.76f to 0.07f, 0.74f to 0.31f)
            shape(Color(0xFFEDEDED), 0.36f to 0.53f, 0.64f to 0.53f, 0.5f to 0.74f) // beard
        }
    }
    icon(mark, point(0.2f, 0.82f), w * 0.24f, Color.White)
}

@Preview(widthDp = 380, heightDp = 520)
@Composable
private fun SpiderPreview() {
    LauncherTheme { Lcd(emptyList()) { SpiderContent(SpiderState.new(2, Random(1)), { _, _ -> }) } }
}
