package com.nokia.launcher.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nokia.launcher.R
import kotlinx.coroutines.delay
import kotlin.math.abs

private val pictures = listOf("🐧", "🍏", "🐡", "🦀", "🐍", "🐘", "🐳", "😈", "🦊", "🐙")
/** The top strip with the counters, as a share of the height. */
private const val HEADER = 0.1f

class PairsGame : MiniGame {
    private var state by mutableStateOf(PairsState.new())
    override val id = "pairs"
    override val title = R.string.pairs
    override fun restart() { state = PairsState.new() }
    override fun encode() = state.encode()
    override fun load(text: String) { PairsState.decode(text)?.let { state = it } }

    override fun key(char: Char) {
        state = when (char.lowercaseChar()) {
            '2', 'w' -> state.move(0, -1)
            '8', 's' -> state.move(0, 1)
            '4', 'a' -> state.move(-1, 0)
            '6', 'd' -> state.move(1, 0)
            '5', ' ' -> state.flip(state.cursor)
            else -> state
        }
    }

    @Composable
    override fun ColumnScope.Content(record: Int, onRecord: (Int) -> Unit) {
        LaunchedEffect(state.shown) {
            if (state.missed) {
                delay(900)
                state = state.hide()
            }
        }
        // The record is the score: fewer moves give more points.
        LaunchedEffect(state.won) {
            if (state.won) onRecord(maxOf(1, 1000 - (state.moves - state.cards.size / 2) * 30))
        }
        // 0 shows the back and 1 the picture; halfway the card is seen edge on.
        val turns = state.cards.indices.map { i ->
            animateFloatAsState(
                if (i in state.found || i in state.shown) 1f else 0f, tween(260), label = "card$i",
            )
        }
        val name = stringResource(title)
        val moves = stringResource(R.string.pairs_moves, state.moves)
        val found = stringResource(R.string.pairs_found, state.found.size / 2, state.cards.size / 2)
        val best = stringResource(R.string.pinball_record) + " $record"
        val won = stringResource(R.string.pairs_won, state.moves)
        val rows = state.cards.size / PAIR_COLUMNS
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().semantics { contentDescription = name }.pointerInput(Unit) {
                    detectTapGestures { at ->
                        val top = size.height * HEADER
                        val x = (at.x / (size.width / PAIR_COLUMNS.toFloat())).toInt()
                        val y = ((at.y - top) / ((size.height - top) / rows)).toInt()
                        if (state.won) restart()
                        else if (x in 0 until PAIR_COLUMNS && at.y >= top && y in 0 until rows) {
                            state = state.flip(y * PAIR_COLUMNS + x)
                        }
                    }
                },
            ) {
                drawRect(Brush.radialGradient(listOf(Color(0xFF17935A), Color(0xFF06432A)), center, size.height * 0.75f))
                val header = size.height * HEADER
                val letters = minOf(header * 0.34f, size.width * 0.04f)
                listOf(moves, found, best).forEachIndexed { i, label ->
                    val box = Size(size.width / 3 - 12f, header * 0.68f)
                    val at = Offset(size.width / 3 * i + 6f, header * 0.16f)
                    drawRoundRect(Color(0x66000000), at, box, CornerRadius(box.height / 2))
                    icon(label, at + Offset(box.width / 2, box.height / 2), letters, if (i == 1) Color(0xFFFFE08A) else Color.White)
                }
                val cell = Size(size.width / PAIR_COLUMNS, (size.height - header) / rows)
                val gap = minOf(cell.width, cell.height) * 0.055f
                val round = CornerRadius(gap * 2f)
                state.cards.forEachIndexed { i, picture ->
                    val at = Offset(i % PAIR_COLUMNS * cell.width + gap, header + i / PAIR_COLUMNS * cell.height + gap)
                    val face = Size(cell.width - gap * 2, cell.height - gap * 2)
                    val middle = at + Offset(face.width / 2, face.height / 2)
                    val turn = turns[i].value
                    scale(abs(turn * 2f - 1f).coerceAtLeast(0.04f), 1f, middle) {
                        drawRoundRect(Color(0x55000000), at + Offset(gap * 0.3f, gap * 0.7f), face, round)
                        if (turn > 0.5f) {
                            drawRoundRect(
                                Brush.verticalGradient(
                                    if (i in state.found) listOf(Color(0xFFF1FFE9), Color(0xFFB6E8A4))
                                    else listOf(Color.White, Color(0xFFE6E9F4)),
                                    at.y, at.y + face.height,
                                ),
                                at, face, round,
                            )
                            icon(pictures[picture], middle, minOf(face.width, face.height) * 0.62f)
                        } else {
                            drawRoundRect(
                                Brush.linearGradient(listOf(Color(0xFF4B80E8), Color(0xFF12297F)), at, at + Offset(face.width, face.height)),
                                at, face, round,
                            )
                            drawRoundRect(
                                Color(0xAAFFFFFF), at + Offset(gap, gap), Size(face.width - gap * 2, face.height - gap * 2),
                                round, style = Stroke(gap * 0.22f),
                            )
                            drawCircle(Color(0x33FFFFFF), minOf(face.width, face.height) * 0.27f, middle)
                            icon("?", middle, minOf(face.width, face.height) * 0.38f, Color(0xFFFFE08A))
                        }
                        if (i == state.cursor && !state.won) {
                            drawRoundRect(Color(0xFFFFD43A), at, face, round, style = Stroke(gap * 0.6f))
                        }
                    }
                }
                if (state.won) {
                    val box = Size(size.width * 0.88f, size.height * 0.1f)
                    val at = Offset((size.width - box.width) / 2, size.height * 0.46f)
                    drawRoundRect(Color(0xE6000000), at, box, CornerRadius(box.height / 3))
                    icon(won, at + Offset(box.width / 2, box.height / 2), minOf(box.height * 0.34f, box.width * 1.7f / won.length), Color(0xFFFFE08A))
                }
            }
        }
    }
}
