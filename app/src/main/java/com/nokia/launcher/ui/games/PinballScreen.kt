package com.nokia.launcher.ui.games

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import kotlin.math.PI
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlin.math.abs
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import com.nokia.launcher.ui.home.Lcd
import com.nokia.launcher.ui.theme.LauncherTheme
import kotlin.math.cos
import kotlin.math.sin

private val Neon = Color(0xFF35F2A9)
private val Gold = Color(0xFFFFC933)
private val Pink = Color(0xFFFF4D8D)
private const val KEY_FLIP_MS = 220L

/** Keys cannot be held, so each press raises a flipper for a moment. */
class FlipperKeys {
    var leftUntil by mutableLongStateOf(0L)
    var rightUntil by mutableLongStateOf(0L)
    fun left() { leftUntil = System.currentTimeMillis() + KEY_FLIP_MS }
    fun right() { rightUntil = System.currentTimeMillis() + KEY_FLIP_MS }
}

@Composable
fun ColumnScope.PinballContent(
    game: PinballGame,
    best: Int,
    keys: FlipperKeys,
    onChange: () -> Unit,
) {
    // The game is mutable: the drawing reads this counter, so each frame only redraws the canvases.
    var frame by remember(game) { mutableIntStateOf(0) }
    // The scoreboard is recomposed only when one of its values changes.
    var board by remember(game) { mutableStateOf(game.board()) }
    var leftTouch by remember { mutableStateOf(false) }
    var rightTouch by remember { mutableStateOf(false) }
    val changed by rememberUpdatedState(onChange)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(game, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = withFrameNanos { it }
            var reported = board
            while (true) {
                val now = withFrameNanos { it }
                val time = System.currentTimeMillis()
                game.step(
                    ((now - last) / 1e9f).coerceAtMost(0.05f),
                    leftTouch || keys.leftUntil > time,
                    rightTouch || keys.rightUntil > time,
                )
                last = now
                frame++
                board = game.board()
                if (reported.score != board.score || reported.balls != board.balls) changed()
                reported = board
            }
        }
    }
    val description = stringResource(R.string.pinball)
    val trail = remember(game) { ArrayDeque<Offset>() }
Row(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF05060F))) {
    SidePanel(board, maxOf(best, board.score), { frame }, Modifier.width(92.dp).fillMaxHeight())
    Box(Modifier.weight(1f).fillMaxHeight()) {
    // What never moves is painted once on its own layer and reused on every frame.
    Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        onTable {
            tableBase()
            outline()
        }
    }
    Canvas(
        // The exclusion keeps the system back gesture from stealing flipper taps.
        Modifier.fillMaxSize().systemGestureExclusion().semantics { contentDescription = description }
            .pointerInput(game) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent().changes.filter { it.pressed }
                        val left = down.any { it.position.x < size.width / 2 }
                        val right = down.any { it.position.x >= size.width / 2 }
                        if ((left || right) && game.waiting) game.launch()
                        leftTouch = left
                        rightTouch = right
                    }
                }
            },
    ) {
        val time = frame
        onTable {
                tableLights(time, game)
                LANES.forEachIndexed { i, lane -> lamp(Offset(lane.x, lane.y), "UNX"[i].toString(), game.lanes and (1 shl i) != 0) }
                TARGETS.forEachIndexed { i, target -> starTarget(Offset(target.x, target.y), game.targets and (1 shl i) != 0) }
                slingshot(13f, 116f, 23f, 136f, 13f, 132f, time)
                slingshot(87f, 116f, 77f, 136f, 87f, 132f, time)
                borderLights(time)
                // Lane dividers and the bouncing face of each slingshot.
                WALLS.drop(OUTLINE.size - 1).forEach {
                    rail(Offset(it.x1, it.y1), Offset(it.x2, it.y2), if (it.points > 0) Pink else Neon)
                }
                BUMPERS.forEachIndexed { index, b -> bumper(b, index == game.lit, time) }
                flipperShape(LEFT_FLIPPER, game.leftAngle)
                flipperShape(RIGHT_FLIPPER, game.rightAngle)
                plunger(game.waiting, time)
                if (!game.over) {
                    val ball = Offset(game.x, game.y)
                    if (game.waiting) trail.clear() else trail.addLast(ball)
                    while (trail.size > 8) trail.removeFirst()
                    trail.forEachIndexed { i, at ->
                        drawCircle(Color(0xFF7FD8FF).copy(alpha = 0.03f * i), BALL_RADIUS * (0.4f + i * 0.07f), at)
                    }
                    drawCircle(Color.Black.copy(alpha = 0.45f), BALL_RADIUS, ball + Offset(1.2f, 1.6f))
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFFB9C2D4), Color(0xFF454D5E)),
                            ball - Offset(1.1f, 1.1f), BALL_RADIUS * 1.9f,
                        ),
                        BALL_RADIUS, ball,
                    )
                }
                if (game.popupTime > 0f) {
                    icon(
                        game.popup, Offset(game.popupX, game.popupY - 14f + game.popupTime * 8f), 6.5f,
                        Gold.copy(alpha = (game.popupTime * 2f).coerceAtMost(1f)),
                    )
                }
                if (game.over) {
                    drawRoundRect(Color(0xE0000000), Offset(19f, 76f), Size(62f, 28f), CornerRadius(4f))
                    icon("GAME OVER", Offset(50f, 86f), 9f, Pink)
                    icon("${game.score}", Offset(50f, 97f), 7f, Gold)
                }
        }
    }
    }
    }
}

/** Draws in table units, with the table centered in the canvas. */
private inline fun DrawScope.onTable(draw: DrawScope.() -> Unit) {
    val unit = minOf(size.width / TABLE_WIDTH, size.height / TABLE_HEIGHT)
    translate((size.width - TABLE_WIDTH * unit) / 2, (size.height - TABLE_HEIGHT * unit) / 2) {
        scale(unit, unit, Offset.Zero) { draw() }
    }
}

/** What the scoreboard shows. */
private data class PinballBoard(
    val score: Int,
    val balls: Int,
    val multiplier: Int,
    val targets: Int,
    val over: Boolean,
    val waiting: Boolean,
    val saving: Boolean,
    val popup: String,
)

private fun PinballGame.board() =
    PinballBoard(score, balls, multiplier, targets, over, waiting, saver > 0f, if (popupTime > 0f) popup else "")

private val Amber = Color(0xFFFFB32C)
private val LedOff = Color(0xFF3A2608)
private val Lcd = Color(0xFF120A02)

/** The retro scoreboard beside the table: amber figures in chrome frames. */
@Composable
private fun SidePanel(board: PinballBoard, best: Int, frame: () -> Int, modifier: Modifier) {
    val message = when {
        board.over -> stringResource(R.string.pinball_msg_over)
        board.waiting -> stringResource(R.string.pinball_msg_launch)
        board.popup.isNotEmpty() -> board.popup
        board.saving -> stringResource(R.string.pinball_msg_saver)
        else -> stringResource(R.string.pinball_msg_mission, Integer.bitCount(board.targets))
    }
    Column(
        modifier.background(Brush.horizontalGradient(listOf(Color(0xFF2B3354), Color(0xFF59648C), Color(0xFF1A203A))))
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val at = center
            val r = size.height * 0.34f
            val box = at - Offset(r * 2.1f, r * 0.45f)
            val ring = Size(r * 4.2f, r * 0.9f)
            drawArc(Amber, 180f, 180f, false, box, ring, style = Stroke(r * 0.16f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE0A0), Pink, Color(0xFF3A0A2A)), at - Offset(r * 0.4f, r * 0.4f), r * 1.6f), r, at)
            drawArc(Amber, 0f, 180f, false, box, ring, style = Stroke(r * 0.16f))
        }
        Text(
            stringResource(R.string.pinball_logo), Modifier.fillMaxWidth(), color = Color(0xFFFFE08A),
            fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
        )
        Display(stringResource(R.string.pinball_ball)) {
            // One lit ball for each one left.
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                for (i in 0 until BALLS) {
                    val at = Offset(size.width * (i + 1) / (BALLS + 1), size.height / 2)
                    val r = size.height * 0.42f
                    if (i < board.balls) {
                        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFB9C2D4), Color(0xFF39415A)), at - Offset(r * 0.35f, r * 0.35f), r * 1.7f), r, at)
                    } else {
                        drawCircle(LedOff, r, at)
                    }
                }
            }
        }
        Display(stringResource(R.string.pinball_score)) { Digits(board.score, 17) }
        Display(stringResource(R.string.pinball_record)) { Digits(best, 14) }
        Display(stringResource(R.string.pinball_bonus)) {
            Text("x${board.multiplier}", color = Amber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        // The message window takes the rest, with running lights around it.
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Lcd)
                .border(1.5.dp, Color(0xFFB9C4E8), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(5.dp)) {
                val step = 9.dp.toPx()
                val across = (size.width / step).toInt()
                for (i in 0..across) {
                    for (y in listOf(0f, size.height)) {
                        val on = (i + frame() / 8 + if (y > 0f) 1 else 0) % 3 == 0
                        drawCircle(if (on) Amber else LedOff, 1.6.dp.toPx(), Offset(i * size.width / across, y))
                    }
                }
            }
            Text(
                message, Modifier.padding(horizontal = 4.dp), color = Amber, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Display(label: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Lcd)
            .border(1.5.dp, Color(0xFFB9C4E8), RoundedCornerShape(6.dp)).padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color(0xFF9FB0E0), fontSize = 9.sp, maxLines = 1, letterSpacing = 1.sp)
        content()
    }
}

/** Seven figures with the unused ones dimly visible, like an electronic scoreboard. */
@Composable
private fun Digits(value: Int, size: Int) {
    val text = "$value"
    Box {
        listOf("8".repeat(maxOf(7, text.length)) to LedOff, text.padStart(7) to Amber).forEach { (shown, color) ->
            Text(shown, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = size.sp, maxLines = 1)
        }
    }
}

/** Light bulbs evenly spaced along the border. */
private val bulbs: List<Offset> = OUTLINE.zipWithNext().flatMap { (a, b) ->
    val from = Offset(a.first, a.second)
    val to = Offset(b.first, b.second)
    val count = ((to - from).getDistance() / 7f).toInt().coerceAtLeast(1)
    List(count) { from + (to - from) * ((it + 0.5f) / count) }
}

/** The whole border as one chrome rail. */
private fun DrawScope.outline() {
    val path = borderPath(closed = false)
    fun line(color: Color, width: Float) =
        drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    line(Neon.copy(alpha = 0.16f), 8f)
    line(Color(0xFF1B2036), 5.4f)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFFF4F7FF), Color(0xFF7D89A8), Color(0xFFE4E9F8), Color(0xFF5A6584)),
            Offset(0f, 0f), Offset(100f, 150f),
        ),
        style = Stroke(4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    line(Color(0xFF141830), 2.1f)
}

/** Arcade lights that chase each other along the rail. */
private fun DrawScope.borderLights(time: Int) {
    val colors = listOf(Gold, Pink, Color(0xFF52E5FF))
    bulbs.forEachIndexed { i, at ->
        val on = (i - time / 7).mod(3) == 0
        val color = colors[(i / 3) % colors.size]
        if (on) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.8f), Color.Transparent), at, 3.4f), 3.4f, at)
        drawCircle(if (on) Color.White else color.copy(alpha = 0.45f), 0.8f, at)
    }
}

private fun borderPath(closed: Boolean) = Path().apply {
    OUTLINE.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
    if (closed) close()
}

/** A round insert set into the table, lit or not. */
private fun DrawScope.insert(at: Offset, radius: Float, color: Color, on: Boolean) {
    if (on) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.75f), Color.Transparent), at, radius * 2.4f), radius * 2.4f, at)
    drawCircle(
        if (on) Brush.radialGradient(listOf(Color.White, color), at - Offset(radius * 0.3f, radius * 0.3f), radius * 1.2f)
        else Brush.radialGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.12f)), at, radius),
        radius, at,
    )
    drawCircle(Color(0x99FFFFFF), radius, at, style = Stroke(0.35f))
}

private val PLANET = Offset(50f, 114f)

/** The fixed paint of the table: deep space, orbits, the ringed planet and the title plate. */
private fun DrawScope.tableBase() {
    drawPath(
        borderPath(closed = true),
        Brush.verticalGradient(listOf(Color(0xFF45197F), Color(0xFF17226B), Color(0xFF0A1740), Color(0xFF040A1C)), 0f, TABLE_HEIGHT),
    )
    fun cloud(x: Float, y: Float, r: Float, color: Color) =
        drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), Offset(x, y), r), r, Offset(x, y))
    cloud(26f, 46f, 36f, Color(0x55FF3D9A))
    cloud(76f, 78f, 32f, Color(0x4428C8FF))
    cloud(30f, 128f, 30f, Color(0x3835F2A9))
    // Orbits around the planet.
    for ((w, h, alpha) in listOf(Triple(62f, 30f, 0.22f), Triple(76f, 44f, 0.13f))) {
        drawOval(Color(0xFF8FD8FF).copy(alpha = alpha), PLANET - Offset(w / 2, h / 2), Size(w, h), style = Stroke(0.45f))
    }
    val box = PLANET - Offset(23f, 5.5f)
    val ring = Size(46f, 11f)
    drawArc(Color(0xCCFFD9A0), 180f, 180f, false, box, ring, style = Stroke(1.5f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF8A3D).copy(alpha = 0.35f), Color.Transparent), PLANET, 22f), 22f, PLANET)
    val globe = Path().apply { addOval(Rect(PLANET, 13f)) }
    clipPath(globe) {
        drawCircle(Color(0xFFC4561F), 13f, PLANET)
        // Bands of clouds.
        listOf(-9f to Color(0xFFFFC27A), -4f to Color(0xFFE98A3E), 1f to Color(0xFFFFD79A), 5f to Color(0xFFA83F18), 9f to Color(0xFFE27B36))
            .forEach { (y, color) -> drawOval(color, PLANET + Offset(-17f, y - 1.6f), Size(34f, 3.2f)) }
        drawCircle(Brush.radialGradient(listOf(Color(0x55FFFFFF), Color.Transparent, Color(0xDD14041C)), PLANET - Offset(5f, 6f), 21f), 13f, PLANET)
    }
    drawArc(Color(0xFFFFE3B8), 0f, 180f, false, box, ring, style = Stroke(1.5f))
    drawArc(Color(0x88FF9A5A), 0f, 180f, false, PLANET - Offset(20f, 4.3f), Size(40f, 8.6f), style = Stroke(0.7f))
    // Title plate.
    drawRoundRect(Color(0xAA070B22), Offset(30f, 134f), Size(40f, 8f), CornerRadius(4f))
    drawRoundRect(Gold, Offset(30f, 134f), Size(40f, 8f), CornerRadius(4f), style = Stroke(0.45f))
    icon("UNIX SPACE", Offset(50f, 138f), 5.2f, Color(0xFFFFE3A3))
}

/** What blinks or moves on the table: stars, arrows, the moon and the lamps. */
private fun DrawScope.tableLights(time: Int, game: PinballGame) {
    for (i in 0 until 70) {
        val star = Offset(((i * 37) % 84 + 8).toFloat(), ((i * 53) % 138 + 10).toFloat())
        // Stars are behind the planet and the title plate.
        if ((star - PLANET).getDistance() < 15f || star.x in 30f..70f && star.y in 134f..142f) continue
        val twinkle = 0.2f + 0.65f * abs(sin((time + i * 17) / 38f))
        drawCircle(Color.White.copy(alpha = twinkle), if (i % 9 == 0) 0.6f else 0.3f, star)
        if (i % 17 == 0) {
            drawLine(Color.White.copy(alpha = twinkle * 0.6f), star - Offset(1.6f, 0f), star + Offset(1.6f, 0f), 0.2f)
            drawLine(Color.White.copy(alpha = twinkle * 0.6f), star - Offset(0f, 1.6f), star + Offset(0f, 1.6f), 0.2f)
        }
    }
    // Arrows that run upward toward the top bumper.
    for (k in 0..2) {
        val y = 94f - k * 6f
        val on = (time / 10) % 3 == k
        val arrow = Path().apply { moveTo(44f, y); lineTo(50f, y - 3.6f); lineTo(56f, y); lineTo(53.4f, y); lineTo(50f, y - 1.9f); lineTo(46.6f, y); close() }
        drawPath(arrow, if (on) Gold else Gold.copy(alpha = 0.22f))
    }
    // Side arrows pointing at the star targets.
    for (side in listOf(-1f, 1f)) {
        for (y in listOf(52f, 96f)) {
            val x = 50f + side * 33f
            val on = (time / 14 + (y / 30).toInt()) % 2 == 0
            val tip = Path().apply { moveTo(x + side * 2.6f, y); lineTo(x - side * 1.2f, y - 2.4f); lineTo(x - side * 1.2f, y + 2.4f); close() }
            drawPath(tip, if (on) Color(0xFF52E5FF) else Color(0x3352E5FF))
        }
    }
    val angle = time / 90f
    val moon = PLANET + Offset(cos(angle) * 31f, sin(angle) * 15f)
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFF8E9AB8), Color(0xFF2A3048)), moon - Offset(1f, 1f), 3.6f), 2.4f, moon)

    // Mission lamps: one for each star target already hit.
    listOf(Offset(32f, 103f), Offset(38f, 97f), Offset(62f, 97f), Offset(68f, 103f))
        .forEachIndexed { i, at -> insert(at, 1.8f, Color(0xFF35F2A9), game.targets and (1 shl i) != 0) }
    for (k in 2..5) {
        val at = Offset(38f + (k - 2) * 8f, 150f)
        insert(at, 2.9f, Amber, game.multiplier >= k)
        icon("x$k", at, 2.9f, if (game.multiplier >= k) Color(0xFF3A1E00) else Color(0xCCFFE3A3))
    }
    // Shoot-again lamp between the flippers.
    insert(Offset(50f, 173f), 2.6f, Pink, game.saver > 0f && !game.waiting && (time / 8) % 2 == 0)
}

private fun DrawScope.rail(from: Offset, to: Offset, glow: Color) {
    drawLine(Color.Black.copy(alpha = 0.5f), from + Offset(0.8f, 1.2f), to + Offset(0.8f, 1.2f), 2.8f, StrokeCap.Round)
    drawLine(glow.copy(alpha = 0.3f), from, to, 4.2f, StrokeCap.Round)
    drawLine(Color(0xFF8C97AD), from, to, 2.2f, StrokeCap.Round)
    drawLine(Color(0xFFEDF3FF), from, to, 0.7f, StrokeCap.Round)
}

private fun DrawScope.lamp(at: Offset, letter: String, on: Boolean) {
    insert(at, 4.3f, Gold, on)
    drawCircle(Gold, 4.3f, at, style = Stroke(0.6f))
    icon(letter, at, 5f, if (on) Color(0xFF4A2A00) else Color(0xFFFFE9A8))
}

private fun starPath(at: Offset, outer: Float) = Path().apply {
    for (k in 0 until 10) {
        val angle = (k * PI / 5 - PI / 2).toFloat()
        val r = if (k % 2 == 0) outer else outer * 0.45f
        val x = at.x + cos(angle) * r
        val y = at.y + sin(angle) * r
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.starTarget(at: Offset, hit: Boolean) {
    val color = if (hit) Neon else Pink
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent), at, 6.5f), 6.5f, at)
    drawCircle(Brush.linearGradient(listOf(Color.White, Color(0xFF59648C)), at - Offset(3f, 3f), at + Offset(3f, 3f)), TARGET_RADIUS + 0.5f, at)
    drawCircle(if (hit) Color(0xFF0A5C3E) else Color(0xFF6E1240), TARGET_RADIUS - 0.3f, at)
    drawPath(starPath(at, 2.5f), if (hit) Neon else Color(0xFFFFD0E4))
}

private fun DrawScope.slingshot(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, time: Int) {
    val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close() }
    drawPath(path, Brush.linearGradient(listOf(Color(0xFFFF8CC0), Color(0xFF8A1450), Color(0xFF33051F)), Offset(x1, y1), Offset(x3, y3)))
    drawPath(path, Color(0xFFFFC0D9), style = Stroke(0.6f))
    // Small lamps behind the bouncing face.
    val inner = Offset((x1 + x2 + x3) / 3, (y1 + y2 + y3) / 3)
    for (k in 1..3) {
        val on = (time / 9) % 3 == k - 1
        val face = Offset(x1 + (x2 - x1) * k / 4f, y1 + (y2 - y1) * k / 4f)
        drawCircle(if (on) Color.White else Color(0x66FFD0E4), 0.8f, face + (inner - face) * 0.4f)
    }
}

private fun DrawScope.bumper(b: Bumper, lit: Boolean, time: Int) {
    val at = Offset(b.x, b.y)
    val r = b.radius
    drawCircle(Color.Black.copy(alpha = 0.5f), r * 1.2f, at + Offset(1.2f, 2f))
    if (lit) {
        drawCircle(Brush.radialGradient(listOf(Color.White, Gold.copy(alpha = 0.5f), Color.Transparent), at, r * 2.3f), r * 2.3f, at)
        drawCircle(Color.White.copy(alpha = 0.8f), r * 1.65f, at, style = Stroke(0.7f))
    }
    drawCircle(Brush.linearGradient(listOf(Color(0xFFF4F7FF), Color(0xFF414B66)), at - Offset(r, r), at + Offset(r, r)), r * 1.2f, at)
    drawCircle(Color(0xFF1B0C3A), r * 1.0f, at)
    // A ring of lamps that turns, all of them bright on a hit.
    val lamps = if (r > 5f) 10 else 6
    repeat(lamps) {
        val angle = (it * 2 * PI / lamps).toFloat()
        val on = lit || (it + time / 8) % 3 == 0
        drawCircle(if (on) Color(0xFFFFF2B0) else Color(0xFF6B4A8A), r * 0.09f, at + Offset(cos(angle), sin(angle)) * (r * 0.86f))
    }
    drawCircle(
        Brush.radialGradient(
            if (lit) listOf(Color.White, Gold, Color(0xFFB35C00)) else listOf(Color(0xFFFFD9EA), Pink, Color(0xFF5E0A30)),
            at - Offset(r * 0.25f, r * 0.3f), r * 0.95f,
        ),
        r * 0.7f, at,
    )
    if (r > 5f) {
        drawPath(starPath(at, r * 0.62f), Color.White.copy(alpha = 0.22f))
        icon("${b.points}", at, r * 0.55f, Color.White)
    }
    drawOval(Color.White.copy(alpha = 0.45f), at - Offset(r * 0.45f, r * 0.58f), Size(r * 0.6f, r * 0.28f))
}

private fun DrawScope.flipperShape(f: Flipper, angle: Float) {
    val pivot = Offset(f.x, f.y)
    val tip = pivot + Offset(cos(angle) * FLIPPER_LENGTH, sin(angle) * FLIPPER_LENGTH)
    val side = Offset(-sin(angle), cos(angle))
    // Wide at the pivot and narrow at the tip: a red rubber with a golden top.
    fun draw(wide: Float, narrow: Float, brush: Brush) {
        val body = Path().apply {
            val a = pivot + side * wide
            val b = tip + side * narrow
            val c = tip - side * narrow
            val d = pivot - side * wide
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
        }
        drawPath(body, brush)
        drawCircle(brush, wide, pivot)
        drawCircle(brush, narrow, tip)
    }
    draw(3.7f, 2.1f, Brush.verticalGradient(listOf(Color(0xFFFF6B5A), Color(0xFF8A0F1E)), f.y - 10f, f.y + 10f))
    draw(2.3f, 0.9f, Brush.verticalGradient(listOf(Color(0xFFFFF6C8), Gold, Color(0xFFB86200)), f.y - 8f, f.y + 8f))
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFF59648C)), pivot - Offset(0.5f, 0.5f), 2.2f), 1.7f, pivot)
    drawCircle(Color(0xFF1B2036), 0.6f, pivot)
}

private fun DrawScope.plunger(waiting: Boolean, time: Int) {
    if (!waiting) return
    // A compressed spring under the ball that is ready.
    val top = 132f + abs(sin(time / 18f))
    drawRoundRect(Pink, Offset(LAUNCH_X - 2.6f, top), Size(5.2f, 1.8f), CornerRadius(1f))
    var y = top + 2f
    while (y < 137f) {
        drawLine(Color(0xFFB9C2D4), Offset(LAUNCH_X - 2f, y), Offset(LAUNCH_X + 2f, y + 0.8f), 0.6f, StrokeCap.Round)
        y += 1.2f
    }
}

@Preview(widthDp = 380, heightDp = 640)
@Composable
private fun PinballPreview() {
    LauncherTheme { Lcd(emptyList()) { PinballContent(PinballGame(), 1200, FlipperKeys()) {} } }
}
