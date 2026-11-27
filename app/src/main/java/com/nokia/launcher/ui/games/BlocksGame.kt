package com.nokia.launcher.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import kotlinx.coroutines.delay
import kotlin.math.abs

private val blockColors = listOf(
    Color(0xFF22D3EE), Color(0xFFFACC15), Color(0xFFB567F9), Color(0xFF4ADE80),
    Color(0xFFFB5D6B), Color(0xFF4F8BFF), Color(0xFFFF9838),
)

/** The frame changes color with the level. */
private val levelColors = listOf(
    Color(0xFF35F2FF), Color(0xFF7CFF8A), Color(0xFFFFD43A), Color(0xFFFF8A3D), Color(0xFFFF5D8F), Color(0xFFB98CFF),
)
private val PanelText = Color(0xFF94A7DC)

class BlocksGame : MiniGame {
    private var state by mutableStateOf(BlocksState.new())
    override val id = "blocks"
    override val title = R.string.blocks
    override val actionLabel get() = if (state.paused || state.over) R.string.soft_play else R.string.soft_pause
    override fun action() {
        state = if (state.over) BlocksState.new().copy(paused = false) else state.copy(paused = !state.paused)
    }
    override fun restart() { state = BlocksState.new() }
    override fun encode() = state.encode()
    override fun load(text: String) { BlocksState.decode(text)?.let { state = it } }

    override fun key(char: Char) {
        state = when (char.lowercaseChar()) {
            '4', 'a' -> state.move(-1)
            '6', 'd' -> state.move(1)
            '2', '5', 'w' -> if (state.paused) state.copy(paused = false) else state.rotate()
            '8', 's' -> state.step()
            '0', ' ' -> state.drop()
            '#', 'c' -> state.swap()
            else -> state
        }
    }

    @Composable
    override fun ColumnScope.Content(record: Int, onRecord: (Int) -> Unit) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(state.paused, state.over, lifecycle) {
            if (!state.paused && !state.over) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (true) {
                        delay(state.tickMs)
                        state = state.step()
                    }
                }
            }
        }
        LaunchedEffect(state.score) { onRecord(state.score) }
        // A short banner when lines disappear.
        var seenLines by remember { mutableIntStateOf(state.lines) }
        var cheer by remember { mutableIntStateOf(0) }
        LaunchedEffect(state.lines) {
            val cleared = state.lines - seenLines
            seenLines = state.lines
            if (cleared > 0) {
                cheer = cleared
                delay(900)
                cheer = 0
            }
        }
        val name = stringResource(title)
        val scoreLabel = stringResource(R.string.pinball_score)
        val linesLabel = stringResource(R.string.blocks_lines)
        val levelLabel = stringResource(R.string.blocks_level)
        val nextLabel = stringResource(R.string.blocks_next)
        val holdLabel = stringResource(R.string.blocks_hold)
        val recordLabel = stringResource(R.string.pinball_record)
        val banner = stringResource(if (state.over) R.string.pinball_msg_over else R.string.blocks_tap)
        val help = stringResource(R.string.blocks_help)
        val cheers = listOf(
            stringResource(R.string.blocks_one), stringResource(R.string.blocks_two),
            stringResource(R.string.blocks_three), "¡TETRIS!",
        )
        // Where the hold box was drawn, so that touching it swaps the piece.
        val holdBox = remember { arrayOf(Rect.Zero) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().systemGestureExclusion().semantics { contentDescription = name }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var anchor = down.position
                            var moved = false
                            val stepSize = 22.dp.toPx()
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                val delta = change.position - anchor
                                // Dragging sideways slides the piece; a pull down drops it.
                                if (abs(delta.x) > stepSize && abs(delta.x) > abs(delta.y)) {
                                    state = state.move(if (delta.x > 0) 1 else -1)
                                    anchor = change.position
                                    moved = true
                                } else if (delta.y > stepSize * 2.5f && !moved) {
                                    state = state.drop()
                                    moved = true
                                }
                                change.consume()
                                if (!change.pressed) break
                            }
                            if (!moved) {
                                if (holdBox[0].contains(down.position) && !state.paused) state = state.swap()
                                else if (state.over) action()
                                else key('5')
                            }
                        }
                    },
            ) {
                val glow = levelColors[(state.level - 1) % levelColors.size]
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1F52), Color(0xFF0A0B1E))))
                // The well is as tall as the screen allows; the panel takes what is left.
                val cell = minOf((size.height - 8f) / BLOCK_ROWS, size.width * 0.66f / BLOCK_COLUMNS)
                val well = Size(cell * BLOCK_COLUMNS, cell * BLOCK_ROWS)
                val panel = minOf(size.width - well.width - 24f, well.width * 0.85f)
                val origin = Offset((size.width - well.width - panel - 12f) / 2, (size.height - well.height) / 2)
                drawRect(Color(0xFF03040C), origin, well)
                for (x in 1 until BLOCK_COLUMNS) {
                    drawLine(Color(0x14FFFFFF), origin + Offset(x * cell, 0f), origin + Offset(x * cell, well.height))
                }
                for (y in 1 until BLOCK_ROWS) {
                    drawLine(Color(0x14FFFFFF), origin + Offset(0f, y * cell), origin + Offset(well.width, y * cell))
                }
                state.grid.forEachIndexed { i, value ->
                    if (value > 0) {
                        block(origin + Offset(i % BLOCK_COLUMNS * cell, i / BLOCK_COLUMNS * cell), cell, blockColors[value - 1])
                    }
                }
                if (!state.over) {
                    val color = blockColors[state.piece.kind]
                    state.ghost.cells.forEach { (x, y) ->
                        drawRoundRect(
                            color.copy(alpha = 0.55f), origin + Offset(x * cell + 2f, y * cell + 2f),
                            Size(cell - 4f, cell - 4f), CornerRadius(cell * 0.2f), style = Stroke(cell * 0.09f),
                        )
                    }
                    state.piece.cells.forEach { (x, y) -> block(origin + Offset(x * cell, y * cell), cell, color) }
                }
                drawRect(glow.copy(alpha = 0.25f), origin - Offset(3f, 3f), Size(well.width + 6f, well.height + 6f), style = Stroke(6f))
                drawRect(glow, origin - Offset(1f, 1f), Size(well.width + 2f, well.height + 2f), style = Stroke(2f))

                // Panel: hold, the next pieces and the numbers.
                val left = origin.x + well.width + 12f
                val unit = well.height / 100f
                val text = minOf(unit * 3.2f, panel * 0.17f)
                var top = origin.y
                fun card(height: Float): Offset {
                    val at = Offset(left, top)
                    drawRoundRect(Color(0xFF141840), at, Size(panel, height), CornerRadius(10f))
                    drawRoundRect(Color(0x55FFFFFF), at, Size(panel, height), CornerRadius(10f), style = Stroke(1.5f))
                    top += height + unit * 1.6f
                    return at
                }
                fun preview(kind: Int, center: Offset, small: Float, faded: Boolean = false) {
                    val shape = Piece(kind, x = 0, y = 0).cells
                    val width = shape.maxOf { it.first } - shape.minOf { it.first } + 1
                    val height = shape.maxOf { it.second } - shape.minOf { it.second } + 1
                    val start = center - Offset(
                        (shape.minOf { it.first } + width / 2f) * small, (shape.minOf { it.second } + height / 2f) * small,
                    )
                    val color = if (faded) lerp(blockColors[kind], Color.Gray, 0.7f) else blockColors[kind]
                    shape.forEach { (x, y) -> block(start + Offset(x * small, y * small), small, color) }
                }
                val small = minOf(panel / 5.2f, unit * 4.2f)
                val holdHeight = unit * 17f
                val holdAt = card(holdHeight)
                holdBox[0] = Rect(holdAt, Size(panel, holdHeight))
                icon(holdLabel, holdAt + Offset(panel / 2, unit * 3f), text, PanelText)
                if (state.hold >= 0) {
                    preview(state.hold, holdAt + Offset(panel / 2, unit * 10.5f), small, state.swapped)
                }
                val nextAt = card(unit * 34f)
                icon(nextLabel, nextAt + Offset(panel / 2, unit * 3f), text, PanelText)
                state.queue.take(PREVIEW).forEachIndexed { i, kind ->
                    preview(kind, nextAt + Offset(panel / 2, unit * (10.5f + i * 9.5f)), if (i == 0) small else small * 0.78f)
                }
                fun stat(label: String, value: String, color: Color = Color(0xFFFFD43A)) {
                    val at = card(unit * 9.6f)
                    icon(label, at + Offset(panel / 2, unit * 2.8f), text * 0.85f, PanelText)
                    icon(value, at + Offset(panel / 2, unit * 6.8f), text * 1.35f, color)
                }
                stat(scoreLabel, "${state.score}")
                stat(linesLabel, "${state.lines}", Color.White)
                stat(levelLabel, "${state.level}", glow)
                stat(recordLabel, "${maxOf(record, state.score)}", Color.White)

                val middle = origin.x + well.width / 2
                fun fit(label: String, max: Float) = minOf(max, well.width * 1.55f / label.length.coerceAtLeast(1))
                if (state.paused || state.over) {
                    val at = origin + Offset(0f, well.height * 0.34f)
                    drawRect(Color(0xE6000000), at, Size(well.width, cell * 5.4f))
                    "TETRIS".forEachIndexed { i, letter ->
                        icon("$letter", Offset(middle + (i - 2.5f) * cell * 1.25f, at.y + cell * 1.3f), cell * 1.7f, blockColors[i])
                    }
                    icon(banner, Offset(middle, at.y + cell * 3.2f), fit(banner, cell * 0.95f))
                    icon(help, Offset(middle, at.y + cell * 4.5f), fit(help, cell * 0.62f), PanelText)
                } else if (cheer > 0) {
                    val word = cheers[(cheer - 1).coerceIn(0, 3)]
                    icon(word, Offset(middle, origin.y + well.height * 0.3f), fit(word, cell * 1.8f), glow)
                }
            }
        }
    }

    private fun DrawScope.block(at: Offset, cell: Float, color: Color) {
        val corner = CornerRadius(cell * 0.2f)
        val inner = Size(cell - 1.4f, cell - 1.4f)
        val start = at + Offset(0.7f, 0.7f)
        drawRoundRect(lerp(color, Color.Black, 0.45f), start, inner, corner)
        drawRoundRect(
            Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color), start.y, start.y + inner.height),
            start + Offset(cell * 0.09f, cell * 0.07f), Size(inner.width - cell * 0.18f, inner.height - cell * 0.2f), corner,
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.35f), start + Offset(cell * 0.2f, cell * 0.14f),
            Size(cell * 0.5f, cell * 0.16f), corner,
        )
    }
}
