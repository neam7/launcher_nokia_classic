package com.nokia.launcher.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val seedColors = listOf(Color(0xFF3FA7F5), Color(0xFFF2B53A), Color(0xFF6CD07A), Color(0xFFE8656F))
private val Cream = Color(0xFFFFE9C2)

/**
 * Where everything goes. On a tall screen the board is vertical: the player's pits run down
 * the left and the phone's up the right. On a wide one the player has the bottom row.
 */
private class BoardLayout(size: Size) {
    val tall = size.height > size.width * 0.95f
    private val banner = size.height * 0.1f
    val bannerCenter = Offset(size.width / 2, banner / 2)
    val board = Rect(8f, banner, size.width - 8f, size.height - 8f)
    private val store = (if (tall) board.height else board.width) / 8f

    /** Stores of the player (6) and the phone (13). */
    fun storeRect(index: Int): Rect {
        val mine = index == 6
        return if (tall) {
            val top = if (mine) board.bottom - store * 0.98f else board.top + store * 0.08f
            Rect(board.left + board.width * 0.14f, top, board.right - board.width * 0.14f, top + store * 0.9f)
        } else {
            val left = if (mine) board.right - store * 0.98f else board.left + store * 0.08f
            Rect(left, board.top + board.height * 0.14f, left + store * 0.9f, board.bottom - board.height * 0.14f)
        }
    }

    val radius: Float = if (tall) minOf(board.width * 0.2f, store * 0.47f) else minOf(store * 0.45f, board.height * 0.19f)

    fun pit(index: Int): Offset {
        val mine = index in 0..5
        val step = if (mine) index else 12 - index
        val along = store * (step + 1.5f)
        return if (tall) {
            Offset(board.left + board.width * if (mine) 0.29f else 0.71f, board.top + along)
        } else {
            Offset(board.left + along, board.top + board.height * if (mine) 0.72f else 0.28f)
        }
    }
}

class BantumiGame : MiniGame {
    private var board by mutableStateOf(Board())
    /** The pit the phone just emptied, so its move can be seen. */
    private var phonePit by mutableIntStateOf(-1)
    override val id = "bantumi"
    override val title = R.string.bantumi
    override fun restart() {
        board = Board()
        phonePit = -1
    }
    override fun encode() = board.encode()
    override fun load(text: String) { Board.decode(text)?.let { board = it } }

    override fun key(char: Char) {
        val pit = char - '1'
        if (pit in 0..5 && board.playerTurn) board = board.play(pit)
    }

    @Composable
    override fun ColumnScope.Content(record: Int, onRecord: (Int) -> Unit) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(board, lifecycle) {
            if (board.over) onRecord(board.player)
            // The phone plays after a short pause, so its move can be followed.
            if (!board.playerTurn && !board.over) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    delay(1100)
                    board.bestMove()?.let {
                        phonePit = it
                        board = board.play(it)
                    }
                }
            }
        }
        val name = stringResource(title)
        val status = when {
            !board.over && board.playerTurn -> stringResource(R.string.bantumi_your_turn)
            !board.over -> stringResource(R.string.bantumi_thinking)
            board.player > board.phone -> stringResource(R.string.game_you_win)
            board.player < board.phone -> stringResource(R.string.game_you_lose)
            else -> stringResource(R.string.game_draw)
        }
        val you = stringResource(R.string.bantumi_you)
        val phone = stringResource(R.string.bantumi_phone)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().semantics { contentDescription = name }.pointerInput(Unit) {
                    detectTapGestures { at ->
                        val layout = BoardLayout(IntSize(size.width, size.height).toSize())
                        val pit = (0..5).firstOrNull { (layout.pit(it) - at).getDistance() < layout.radius * 1.3f }
                        if (pit != null && board.playerTurn) board = board.play(pit)
                    }
                },
            ) {
                val layout = BoardLayout(size)
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2E6B3A), Color(0xFF123319))))
                val bannerSize = minOf(size.height * 0.04f, size.width * 1.7f / status.length.coerceAtLeast(1))
                icon(status, layout.bannerCenter, bannerSize, Color(0xFFFFF3D0))
                val wood = layout.board
                val round = CornerRadius(minOf(wood.width, wood.height) * 0.1f)
                drawRoundRect(Color(0x66000000), wood.topLeft + Offset(0f, 6f), wood.size, round)
                drawRoundRect(
                    Brush.linearGradient(
                        listOf(Color(0xFFD49A57), Color(0xFF9A6230), Color(0xFF6E3F18)), wood.topLeft, wood.bottomRight,
                    ),
                    wood.topLeft, wood.size, round,
                )
                drawRoundRect(Color(0x55FFE0B0), wood.topLeft, wood.size, round, style = Stroke(3f))
                store(layout.storeRect(13), "$phone  ${board.phone}", layout)
                store(layout.storeRect(6), "$you  ${board.player}", layout)
                for (i in 0..12) {
                    if (i == 6) continue
                    val at = layout.pit(i)
                    pit(i, at, layout.radius, board.canPlay(i) && board.playerTurn, i == phonePit)
                    val r = layout.radius
                    // The count sits toward the middle, and the player's key number toward the edge.
                    if (layout.tall) {
                        val inward = if (i <= 5) 1f else -1f
                        icon("${board.pits[i]}", at + Offset(inward * r * 1.4f, 0f), r * 0.68f, Color.White)
                        if (i <= 5) icon("${i + 1}", at - Offset(r * 1.36f, 0f), r * 0.55f, Cream)
                    } else {
                        val inward = if (i <= 5) -1f else 1f
                        icon("${board.pits[i]}", at + Offset(0f, inward * r * 1.35f), r * 0.62f, Color.White)
                        if (i <= 5) icon("${i + 1}", at + Offset(0f, r * 1.3f), r * 0.5f, Cream)
                    }
                }
            }
        }
    }

    private fun DrawScope.store(area: Rect, label: String, layout: BoardLayout) {
        val round = CornerRadius(minOf(area.width, area.height) * 0.5f)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF2E1706), Color(0xFF5A3312)), area.top, area.bottom),
            area.topLeft, area.size, round,
        )
        if (layout.tall) {
            icon(label, area.center, minOf(area.height * 0.42f, area.width * 0.13f), Cream)
        } else {
            val (who, count) = label.split("  ")
            icon(count, area.center, area.width * 0.55f, Cream)
            icon(who, Offset(area.center.x, area.bottom - area.width * 0.5f), area.width * 0.24f, Cream)
        }
    }

    private fun DrawScope.pit(index: Int, at: Offset, radius: Float, playable: Boolean, last: Boolean) {
        drawCircle(Color(0x66FFE0B0), radius * 1.07f, at + Offset(0f, radius * 0.05f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF6B4018), Color(0xFF2C1605)), at - Offset(0f, radius * 0.3f), radius * 1.4f), radius, at)
        if (playable) drawCircle(Color(0xFFFFE08A), radius, at, style = Stroke(radius * 0.09f))
        if (last) drawCircle(Color(0xFFFF6B6B), radius * 1.14f, at, style = Stroke(radius * 0.11f))
        val count = board.pits[index]
        // Seeds spread in a spiral so any number stays inside the pit.
        for (s in 0 until minOf(count, 18)) {
            val distance = radius * 0.68f * sqrt((s + 0.5f) / maxOf(count.coerceAtMost(18), 5))
            val angle = s * 2.4f + index
            val seed = at + Offset(cos(angle) * distance, sin(angle) * distance)
            val color = seedColors[(s + index) % 4]
            drawCircle(Color(0x88000000), radius * 0.18f, seed + Offset(0f, radius * 0.05f))
            drawCircle(Brush.radialGradient(listOf(Color.White, color, color), seed - Offset(radius * 0.06f, radius * 0.06f), radius * 0.28f), radius * 0.18f, seed)
        }
    }
}
