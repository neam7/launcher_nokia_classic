package com.nokia.launcher.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.theme.LauncherTheme
import com.nokia.launcher.ui.theme.LocalPhoneColors
import kotlin.random.Random

/** Dark enough to read on the light opened tiles of every theme. */
private val numberColors = listOf(
    Color(0xFF1257C9), Color(0xFF1E7A34), Color(0xFFD22F27), Color(0xFF6A1B9A),
    Color(0xFFB4530A), Color(0xFF00707A), Color(0xFF111111), Color(0xFF6D4C41),
)
private val Danger = Color(0xFFE5483B)

@Composable
fun ColumnScope.MinesContent(
    state: MinesState,
    onReveal: (Int) -> Unit,
    onFlag: (Int) -> Unit,
    onReset: () -> Unit = {},
) {
    val current by rememberUpdatedState(state)
    val reveal by rememberUpdatedState(onReveal)
    val flag by rememberUpdatedState(onFlag)
    val description = stringResource(R.string.mines)
    // Everything is mixed from the screen and ink colors, so it follows the phone theme.
    val theme = LocalPhoneColors.current
    val ink = theme.lcdText
    val hidden = lerp(theme.lcd, ink, 0.5f)
    val opened = lerp(theme.lcd, Color.White, 0.62f)

    Row(
        Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill("💣 ${MINE_COUNT - state.flags.size}")
        val restart = stringResource(R.string.soft_new)
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(ink).clickable(onClick = onReset)
                .semantics { contentDescription = restart },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    state.won -> "😎"
                    state.lost -> "😵"
                    else -> "🙂"
                },
                fontSize = 22.sp,
            )
        }
        Pill("⏱ %03d".format(state.seconds))
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxSize().semantics { contentDescription = description }.pointerInput(Unit) {
                fun cellAt(at: Offset): Int? {
                    val cell = minOf(size.width / MINE_COLUMNS.toFloat(), size.height / MINE_ROWS.toFloat())
                    val x = (at.x - (size.width - cell * MINE_COLUMNS) / 2) / cell
                    val y = (at.y - (size.height - cell * MINE_ROWS) / 2) / cell
                    if (x < 0 || y < 0 || x >= MINE_COLUMNS || y >= MINE_ROWS) return null
                    return y.toInt() * MINE_COLUMNS + x.toInt()
                }
                detectTapGestures(
                    onLongPress = { at -> cellAt(at)?.let(flag) },
                    onTap = { at -> cellAt(at)?.let { if (current.marking) flag(it) else reveal(it) } },
                )
            },
        ) {
            val cell = minOf(size.width / MINE_COLUMNS, size.height / MINE_ROWS)
            val board = Size(cell * MINE_COLUMNS, cell * MINE_ROWS)
            val origin = Offset((size.width - board.width) / 2, (size.height - board.height) / 2)
            val gap = cell * 0.05f
            val round = CornerRadius(cell * 0.22f)
            drawRoundRect(ink.copy(alpha = 0.18f), origin, board, round)
            for (i in 0 until MINE_COLUMNS * MINE_ROWS) {
                val at = origin + Offset(i % MINE_COLUMNS * cell + gap, i / MINE_COLUMNS * cell + gap)
                val side = Size(cell - gap * 2, cell - gap * 2)
                val middle = at + Offset(side.width / 2, side.height / 2)
                val mine = state.lost && i in state.mines && i !in state.flags
                if (i in state.open || mine) {
                    // The mine that was stepped on stays red.
                    drawRoundRect(if (mine && i == state.cursor) Danger else opened, at, side, round)
                } else {
                    drawRoundRect(lerp(hidden, Color.Black, 0.35f), at, side, round)
                    drawRoundRect(
                        Brush.verticalGradient(
                            listOf(lerp(hidden, Color.White, 0.28f), hidden), at.y, at.y + side.height,
                        ),
                        at, Size(side.width, side.height * 0.9f), round,
                    )
                }
                when {
                    mine -> mine(middle, cell)
                    i in state.flags -> {
                        flag(middle, cell, lerp(opened, Color.White, 0.5f))
                        if (state.lost && i !in state.mines) {
                            drawLine(Danger, at, at + Offset(side.width, side.height), cell * 0.09f, StrokeCap.Round)
                            drawLine(
                                Danger, at + Offset(side.width, 0f), at + Offset(0f, side.height),
                                cell * 0.09f, StrokeCap.Round,
                            )
                        }
                    }
                    i in state.open && state.count(i) > 0 ->
                        icon("${state.count(i)}", middle, cell * 0.68f, numberColors[state.count(i) - 1])
                }
                if (i == state.cursor && !state.over) {
                    drawRoundRect(Color.White, at, side, round, style = Stroke(cell * 0.11f))
                    drawRoundRect(ink, at, side, round, style = Stroke(cell * 0.05f))
                }
            }
        }
        if (state.over) {
            Text(
                stringResource(if (state.won) R.string.mines_won else R.string.mines_lost),
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    .background(ink, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 8.dp),
                color = theme.lcd, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            )
        }
    }
}

/** A counter in the same inverted style as the clock on the home screen. */
@Composable
private fun Pill(text: String) {
    val theme = LocalPhoneColors.current
    Text(
        text,
        Modifier.background(theme.lcdText, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp),
        color = theme.lcd, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp,
    )
}

private fun DrawScope.mine(at: Offset, cell: Float) {
    val r = cell * 0.2f
    for (d in listOf(Offset(r * 1.6f, 0f), Offset(0f, r * 1.6f), Offset(r * 1.1f, r * 1.1f), Offset(r * 1.1f, -r * 1.1f))) {
        drawLine(Color(0xFF202020), at - d, at + d, cell * 0.08f, StrokeCap.Round)
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFF666666), Color(0xFF0A0A0A)), at - Offset(r * 0.3f, r * 0.3f), r * 1.4f), r, at)
    drawCircle(Color.White, r * 0.24f, at - Offset(r * 0.35f, r * 0.35f))
}

private fun DrawScope.flag(at: Offset, cell: Float, pole: Color) {
    drawLine(pole, at + Offset(-cell * 0.1f, -cell * 0.27f), at + Offset(-cell * 0.1f, cell * 0.25f), cell * 0.08f, StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(at.x - cell * 0.08f, at.y - cell * 0.28f)
            lineTo(at.x + cell * 0.27f, at.y - cell * 0.12f)
            lineTo(at.x - cell * 0.08f, at.y + cell * 0.04f)
            close()
        },
        Danger,
    )
    drawLine(pole, at + Offset(-cell * 0.24f, cell * 0.26f), at + Offset(cell * 0.08f, cell * 0.26f), cell * 0.08f, StrokeCap.Round)
}

@Preview(widthDp = 380, heightDp = 480)
@Composable
private fun MinesPreview() {
    LauncherTheme { Lcd(emptyList()) { MinesContent(MinesState().reveal(0, Random(3)), {}, {}) } }
}
