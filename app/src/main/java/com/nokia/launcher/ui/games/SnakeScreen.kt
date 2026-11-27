package com.nokia.launcher.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Survives folding and rotation; a restored game comes back paused. */
val SnakeSaver = Saver<SnakeState, String>(save = { it.encode() }, restore = { SnakeState.decode(it) })

val SnakeBackground = Color(0xFF070B18)
private val BoardTop = Color(0xFF16233F)
private val BoardBottom = Color(0xFF0B1226)
private val Neon = Color(0xFF38F5A8)
private val HeadColor = Color(0xFF8CFF7A)
private val TailColor = Color(0xFF0FA3B1)
private val Outline = Color(0xFF05301F)
private val Apple = Color(0xFFFF3B57)
private val Gold = Color(0xFFFFC93C)

@Composable
fun ColumnScope.SnakeContent(
    state: SnakeState,
    best: Int,
    onTurn: (Direction) -> Unit,
    onToggle: () -> Unit,
    onTick: () -> Unit,
    onSave: (SnakeState) -> Unit,
) {
    val current by rememberUpdatedState(state)
    val tick by rememberUpdatedState(onTick)
    val turn by rememberUpdatedState(onTurn)
    val save by rememberUpdatedState(onSave)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        // Stops while the app is not in front, so the snake never dies unseen.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (true) {
                    delay(current.tickMs)
                    tick()
                }
            } finally {
                // Leaving the game or the app keeps it for later.
                save(current)
            }
        }
    }
    // 0..1 between the previous cell and the current one.
    val progress = remember { Animatable(1f) }
    LaunchedEffect(state.body) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(state.tickMs.toInt(), easing = LinearEasing))
    }
    // Sparks where something was just eaten.
    val burst = remember { Animatable(1f) }
    var burstCell by remember { mutableIntStateOf(0) }
    var burstColor by remember { mutableStateOf(Apple) }
    var gainedPoints by remember { mutableIntStateOf(0) }
    var lastScore by remember { mutableIntStateOf(state.score) }
    LaunchedEffect(state.score) {
        val gained = state.score - lastScore
        lastScore = state.score
        if (gained > 0) {
            burstCell = state.body.first()
            burstColor = if (gained > 1) Gold else Apple
            gainedPoints = gained
            burst.snapTo(0f)
            burst.animateTo(1f, tween(700))
        }
    }
    val loop = rememberInfiniteTransition(label = "snake")
    val pulse by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Chip(stringResource(R.string.snake_score, state.score), Neon)
        if (state.bonus >= 0) {
            val prize = PRIZES[state.bonusKind]
            Chip("${prize.icon} +${prize.points}", Gold)
        }
        Chip(stringResource(R.string.snake_best, maxOf(best, state.score)), Gold)
    }
    Box(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
        Canvas(
            // The exclusion keeps the system back gesture from stealing side swipes.
            Modifier.fillMaxSize().systemGestureExclusion().pointerInput(Unit) {
                var total = Offset.Zero
                detectDragGestures(onDragStart = { total = Offset.Zero }) { change, amount ->
                    change.consume()
                    total += amount
                    if (total.getDistance() > 18.dp.toPx()) {
                        turn(
                            if (abs(total.x) > abs(total.y)) {
                                if (total.x > 0) Direction.Right else Direction.Left
                            } else {
                                if (total.y > 0) Direction.Down else Direction.Up
                            },
                        )
                        total = Offset.Zero
                    }
                }
            },
        ) {
            val cell = minOf(size.width / SNAKE_COLUMNS, size.height / SNAKE_ROWS)
            val board = Size(cell * SNAKE_COLUMNS, cell * SNAKE_ROWS)
            val origin = Offset((size.width - board.width) / 2, (size.height - board.height) / 2)
            fun center(index: Int) =
                origin + Offset((index % SNAKE_COLUMNS + 0.5f) * cell, (index / SNAKE_COLUMNS + 0.5f) * cell)

            val corner = CornerRadius(cell * 0.6f)
            drawRoundRect(Brush.verticalGradient(listOf(BoardTop, BoardBottom)), origin, board, corner)
            for (i in 0 until SNAKE_COLUMNS * SNAKE_ROWS) {
                drawCircle(Color.White.copy(alpha = 0.07f), cell * 0.06f, center(i))
            }
            drawRoundRect(Neon.copy(alpha = 0.18f), origin, board, corner, style = Stroke(cell * 0.3f))
            drawRoundRect(Neon.copy(alpha = 0.75f), origin, board, corner, style = Stroke(1.5.dp.toPx()))

            clipRect(origin.x, origin.y, origin.x + board.width, origin.y + board.height) {
                if (state.food >= 0) {
                    glow(center(state.food), cell * 1.1f, Apple)
                    icon(FOODS[state.foodKind], center(state.food), cell * (1.05f + 0.1f * pulse))
                }
                if (state.bonus >= 0) {
                    prize(
                        PRIZES[state.bonusKind].icon, center(state.bonus), cell, pulse,
                        state.bonusTicks.toFloat() / BONUS_TICKS,
                    )
                }

                fun adjacent(a: Offset, b: Offset) = (a - b).getDistance() <= cell * 1.5f

                // The line runs through the cell centers, so corners are never cut.
                // It is split where the snake crosses an edge.
                fun slide(from: Int, to: Int): Offset {
                    val a = center(from)
                    val b = center(to)
                    return if (adjacent(a, b)) a + (b - a) * progress.value else b
                }
                val points = buildList {
                    add(slide(state.body.getOrElse(1) { state.lastTail }, state.body[0]))
                    for (i in 1 until state.body.size) add(center(state.body[i]))
                    if (state.body.size > 1) add(slide(state.lastTail, state.body.last()))
                }
                val samples = mutableListOf(points.first())
                for (i in 1 until points.size) {
                    val a = points[i - 1]
                    val b = points[i]
                    if (adjacent(a, b)) {
                        val steps = ceil((b - a).getDistance() / (cell * 0.16f)).toInt().coerceAtLeast(1)
                        for (s in 1..steps) samples.add(a + (b - a) * (s.toFloat() / steps))
                    } else {
                        samples.add(b)
                    }
                }
                val last = samples.lastIndex.coerceAtLeast(1)
                fun radius(i: Int) = cell * (0.46f - 0.22f * i / last)
                // Passes: glow, outline, body, then the shine.
                for (i in samples.indices.reversed()) {
                    drawCircle(Neon.copy(alpha = 0.035f), radius(i) * 1.9f, samples[i])
                }
                for (i in samples.indices.reversed()) drawCircle(Outline, radius(i) + cell * 0.08f, samples[i])
                for (i in samples.indices.reversed()) {
                    drawCircle(lerp(HeadColor, TailColor, i.toFloat() / last), radius(i), samples[i])
                    // Bands every few samples read as scales.
                    if (i % 9 in 0..2) drawCircle(Color.Black.copy(alpha = 0.16f), radius(i) * 0.8f, samples[i])
                }
                for (i in samples.indices.reversed()) {
                    drawCircle(
                        Color.White.copy(alpha = 0.16f), radius(i) * 0.3f,
                        samples[i] - Offset(radius(i) * 0.25f, radius(i) * 0.35f),
                    )
                }
                head(samples.first(), state, cell, pulse)

                if (burst.value < 1f) {
                    val at = center(burstCell)
                    val fade = 1f - burst.value
                    icon("+$gainedPoints", at - Offset(0f, cell * (1f + 1.5f * burst.value)), cell, burstColor.copy(alpha = fade))
                    drawCircle(
                        burstColor.copy(alpha = fade), cell * (0.4f + 1.4f * burst.value), at,
                        style = Stroke(cell * 0.14f * fade),
                    )
                    repeat(8) {
                        val angle = (it * PI / 4).toFloat()
                        drawCircle(
                            burstColor.copy(alpha = fade), cell * 0.14f * fade,
                            at + Offset(cos(angle), sin(angle)) * (cell * 2f * burst.value),
                        )
                    }
                }
            }
        }
        if (state.over || state.paused) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
                    .clickable(onClick = onToggle)
                    .background(SnakeBackground.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .border(1.5.dp, if (state.over) Apple else Neon, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SnakeText(
                    stringResource(if (state.over) R.string.snake_over else R.string.snake_paused),
                    if (state.over) Apple else Neon, 22,
                )
                SnakeText(
                    stringResource(if (state.over) R.string.snake_again else R.string.snake_resume),
                    Color.White, 13,
                )
                SnakeText(stringResource(R.string.snake_controls), Color.White.copy(alpha = 0.6f), 11)
            }
        }
    }
}

@Composable
private fun SnakeText(text: String, color: Color, size: Int, modifier: Modifier = Modifier) {
    Text(
        text, modifier, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
    )
}

@Composable
private fun Chip(text: String, color: Color) {
    SnakeText(
        text, color, 14,
        Modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

private fun DrawScope.glow(at: Offset, radius: Float, color: Color) =
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), at, radius), radius, at)

/** Emoji or short text centered on [at]. */
internal fun DrawScope.icon(text: String, at: Offset, size: Float, color: Color = Color.White) {
    val paint = android.graphics.Paint().apply {
        textSize = size
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        this.color = color.toArgb()
        // Pinball draws tiny text on a scaled canvas.
        isLinearText = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, at.x, at.y - (paint.ascent() + paint.descent()) / 2, paint)
}

/** A timed prize inside a ring that empties as its time runs out. */
private fun DrawScope.prize(emoji: String, at: Offset, cell: Float, pulse: Float, left: Float) {
    glow(at, cell * (1.5f + 0.3f * pulse), Gold)
    drawArc(
        Gold, -90f, 360f * left, false, at - Offset(cell * 0.95f, cell * 0.95f),
        Size(cell * 1.9f, cell * 1.9f), style = Stroke(cell * 0.12f, cap = StrokeCap.Round),
    )
    icon(emoji, at, cell * (1.3f + 0.15f * pulse))
}

private fun DrawScope.head(at: Offset, state: SnakeState, cell: Float, pulse: Float) {
    val forward = Offset(state.heading.dx.toFloat(), state.heading.dy.toFloat())
    val side = Offset(-forward.y, forward.x)
    if (!state.over && pulse > 0.65f) { // the tongue flicks out now and then
        val tip = at + forward * (cell * 0.95f)
        drawLine(Apple, at + forward * (cell * 0.5f), tip, cell * 0.09f, StrokeCap.Round)
        listOf(1f, -1f).forEach {
            val fork = tip + forward * (cell * 0.18f) + side * (it * cell * 0.14f)
            drawLine(Apple, tip, fork, cell * 0.07f, StrokeCap.Round)
        }
    }
    val middle = at + forward * (cell * 0.08f)
    drawCircle(Outline, cell * 0.66f, middle)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFD2FFB8), HeadColor, Color(0xFF2FBF5B)),
            at - Offset(cell * 0.2f, cell * 0.25f), cell,
        ),
        cell * 0.57f, middle,
    )
    listOf(1f, -1f).forEach {
        val eye = at + forward * (cell * 0.2f) + side * (it * cell * 0.27f)
        drawCircle(Color.White, cell * 0.2f, eye)
        if (state.over) {
            val d = cell * 0.1f
            drawLine(Outline, eye - Offset(d, d), eye + Offset(d, d), cell * 0.07f)
            drawLine(Outline, eye + Offset(-d, d), eye + Offset(d, -d), cell * 0.07f)
        } else {
            val pupil = eye + forward * (cell * 0.07f)
            drawCircle(SnakeBackground, cell * 0.11f, pupil)
            drawCircle(Color.White, cell * 0.035f, pupil - Offset(cell * 0.03f, cell * 0.03f))
        }
    }
}

@Preview(widthDp = 411, heightDp = 420)
@Composable
private fun SnakePreview() {
    LauncherTheme {
        Lcd(emptyList(), background = SnakeBackground) { SnakeContent(SnakeState.new(Random(1)), 12, {}, {}, {}, {}) }
    }
}
