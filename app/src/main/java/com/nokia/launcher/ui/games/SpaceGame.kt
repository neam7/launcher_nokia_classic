package com.nokia.launcher.ui.games

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nokia.launcher.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val KEY_STEP = 9f

/** Each stage has its own sky and planet. */
private val skies = listOf(
    listOf(Color(0xFF17104A), Color(0xFF060A24), Color(0xFF01020A)),
    listOf(Color(0xFF3A0F3C), Color(0xFF14082A), Color(0xFF05010C)),
    listOf(Color(0xFF05363F), Color(0xFF04192B), Color(0xFF010610)),
    listOf(Color(0xFF42140C), Color(0xFF1C0916), Color(0xFF070208)),
)
private val planets = listOf(Color(0xFF4F7BFF), Color(0xFFD65BB5), Color(0xFF3FC7A8), Color(0xFFE0743B))
private val Cyan = Color(0xFF35F2FF)
private val Gold = Color(0xFFFFD43A)
private val Red = Color(0xFFFF4D6D)

class SpaceGame : MiniGame {
    private var war by mutableStateOf(SpaceWar())
    private var paused by mutableStateOf(true)
    /** How much the ship leans while moving sideways, from -1 to 1. */
    private var bank = 0f
    private var lastShipX = 0f
    /** The simulation is mutable, so this counter makes the screen redraw. */
    private var frame by mutableIntStateOf(0)
    // Copies of what is shown outside the canvas, so that it changes as soon as the game does.
    private var stage by mutableIntStateOf(1)
    private var over by mutableStateOf(false)

    private fun sync() {
        stage = war.stage
        over = war.over
    }

    override val id = "space"
    override val title = R.string.space
    override val actionLabel get() = if (paused || over) R.string.soft_play else R.string.soft_bomb
    override fun action() {
        if (war.over) restart() else if (paused) paused = false else war.bomb()
        frame++
    }
    override fun restart() {
        war = SpaceWar()
        paused = true
        sync()
    }
    override fun encode() = war.encode()
    override fun load(text: String) {
        war = SpaceWar().also { it.load(text) }
        sync()
    }

    override fun key(char: Char) {
        when (char.lowercaseChar()) {
            '2', 'w' -> war.moveTo(war.shipX, war.shipY - KEY_STEP)
            '8', 's' -> war.moveTo(war.shipX, war.shipY + KEY_STEP)
            '4', 'a' -> war.moveTo(war.shipX - KEY_STEP, war.shipY)
            '6', 'd' -> war.moveTo(war.shipX + KEY_STEP, war.shipY)
            '5', ' ' -> return action()
        }
        paused = false
        frame++
    }

    @Composable
    override fun ColumnScope.Content(record: Int, onRecord: (Int) -> Unit) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(war, paused, lifecycle) {
            if (!paused) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    var last = withFrameNanos { it }
                    var reported = -1
                    while (!war.over) {
                        val now = withFrameNanos { it }
                        war.step(((now - last) / 1e9f).coerceAtMost(0.05f))
                        last = now
                        frame++
                        sync()
                        if (war.score != reported) {
                            reported = war.score
                            onRecord(reported)
                        }
                    }
                }
            }
        }
        val name = stringResource(title)
        val start = stringResource(R.string.blocks_tap)
        val overText = stringResource(R.string.pinball_msg_over)
        val help = stringResource(R.string.space_help)
        val gifts = stringResource(R.string.space_gifts)
        val stageText = stringResource(R.string.space_stage, stage)
        val warning = stringResource(R.string.space_boss)
        val recordText = stringResource(R.string.pinball_record) + " ${if (over) maxOf(record, war.score) else record}"
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().systemGestureExclusion().semantics { contentDescription = name }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                                // The ship moves as the finger moves, so the finger never covers it.
                                val unit = size.width / SPACE_WIDTH
                                if (change.previousPressed) {
                                    val delta = (change.position - change.previousPosition) / unit * 1.25f
                                    war.moveTo(war.shipX + delta.x, war.shipY + delta.y)
                                } else if (war.over) {
                                    restart()
                                }
                                if (!war.over) paused = false
                                frame++
                                change.consume()
                            }
                        }
                    },
            ) {
                val time = frame
                if (!paused) {
                    bank += ((war.shipX - lastShipX) * 0.7f - bank) * 0.2f
                    lastShipX = war.shipX
                } else {
                    lastShipX = war.shipX
                }
                // The field fills the width; very wide screens get margins instead.
                val unit = minOf(size.width / SPACE_WIDTH, size.height / 100f)
                war.height = size.height / unit
                if (war.shipY > war.height - 8f || paused && war.score == 0) {
                    war.moveTo(war.shipX, war.height - 18f)
                }
                drawRect(Color.Black)
                translate((size.width - SPACE_WIDTH * unit) / 2, 0f) {
                    scale(unit, unit, Offset.Zero) {
                        clipRect(0f, 0f, SPACE_WIDTH, war.height) {
                            sky(time)
                            actors(time)
                            hud()
                            if (war.banner > 0f && !paused && !war.over) {
                                val alert = war.boss != null
                                icon(
                                    if (alert) warning else stageText, Offset(50f, war.height * 0.4f), 8f,
                                    (if (alert) Red else Cyan).copy(alpha = war.banner.coerceAtMost(1f)),
                                )
                            }
                            if (war.bombFlash > 0f) {
                                drawRect(Color.White.copy(alpha = war.bombFlash * 1.8f), size = Size(SPACE_WIDTH, war.height))
                            }
                            if (paused || war.over) {
                                val middle = war.height * 0.45f
                                drawRoundRect(Color(0xE0020618), Offset(7f, middle - 20f), Size(86f, 40f), CornerRadius(5f))
                                drawRoundRect(
                                    Cyan, Offset(7f, middle - 20f), Size(86f, 40f), CornerRadius(5f), style = Stroke(0.5f),
                                )
                                icon(if (war.over) overText else name.uppercase(), Offset(50f, middle - 11f), 8f, if (war.over) Red else Gold)
                                icon(start, Offset(50f, middle - 1f), 5f)
                                icon(if (war.over) recordText else help, Offset(50f, middle + 7f), 3.3f, Color(0xFF9FB6FF))
                                if (!war.over) icon(gifts, Offset(50f, middle + 13.5f), 3.3f, Color(0xFF9FB6FF))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DrawScope.sky(time: Int) {
        val h = war.height
        val which = (war.stage - 1) % skies.size
        drawRect(Brush.verticalGradient(skies[which], 0f, h), size = Size(SPACE_WIDTH, h))
        // Two soft clouds drifting down.
        for (k in 0..2) {
            val at = Offset(listOf(18f, 82f, 45f)[k], (time * 0.03f + k * h * 0.45f) % (h + 90f) - 45f)
            val color = planets[(which + k + 1) % planets.size]
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent), at, 42f), 42f, at)
        }
        // A ringed planet: the back of the ring, the globe with its night side, then the front.
        val planet = Offset(79f, (time * 0.05f) % (h + 80f) - 40f)
        val ring = planets[which].copy(alpha = 0.45f)
        val ringBox = planet - Offset(24f, 5f)
        drawArc(ring, 180f, 180f, false, ringBox, Size(48f, 10f), style = Stroke(1.6f))
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, planets[which], Color(0xFF090A1E)), planet - Offset(7f, 7f), 25f,
            ),
            14f, planet, alpha = 0.75f,
        )
        drawCircle(planets[which].copy(alpha = 0.35f), 14.6f, planet, style = Stroke(0.7f))
        drawArc(ring, 0f, 180f, false, ringBox, Size(48f, 10f), style = Stroke(1.6f))
        // Three layers of stars; the near ones move faster and leave a short trail.
        for (i in 0 until 90) {
            val layer = i % 3
            val speed = 0.1f + layer * 0.28f
            val x = ((i * 37) % 100).toFloat()
            val y = (((i * 61) % 997) / 997f * h + time * speed) % h
            val color = Color.White.copy(alpha = 0.25f + layer * 0.25f)
            if (layer == 2) drawLine(color, Offset(x, y - 1.6f), Offset(x, y), 0.4f, StrokeCap.Round)
            else drawCircle(color, 0.22f + layer * 0.14f, Offset(x, y))
        }
    }

    private fun DrawScope.actors(time: Int) {
        war.shots.forEach {
            val head = Offset(it.x, it.y)
            val tail = Offset(it.x - it.vx * 0.04f, it.y + 7f)
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Cyan), tail, head), tail, head, 2.4f, StrokeCap.Round)
            drawLine(Color.White, Offset(it.x - it.vx * 0.018f, it.y + 3.2f), head, 0.8f, StrokeCap.Round)
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.7f), Color.Transparent), head, 2.4f), 2.4f, head)
        }
        war.enemies.forEach { enemy(it, time) }
        war.enemyShots.forEach {
            val at = Offset(it.x, it.y)
            drawCircle(Brush.radialGradient(listOf(Red, Color.Transparent), at, 3.6f), 3.6f, at)
            drawCircle(Color(0xFFFFF0C0), 1.1f, at)
        }
        war.drops.forEach { drop ->
            val at = Offset(drop.x, drop.y)
            val (color, mark) = when (drop.gift) {
                Gift.Weapon -> Gold to "P"
                Gift.Shield -> Cyan to "S"
                Gift.Bomb -> Color(0xFFFF9838) to "B"
                Gift.Life -> Red to "♥"
            }
            val pulse = 1f + 0.12f * sin(time / 6f)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.7f), Color.Transparent), at, 8f), 8f * pulse, at)
            drawCircle(Color(0xFF101530), 3.8f * pulse, at)
            drawCircle(color, 3.8f * pulse, at, style = Stroke(0.8f))
            icon(mark, at, 5f, color)
        }
        // The ship blinks while it is protected.
        if (!war.over && (war.safe <= 0f || time % 8 < 4)) player(Offset(war.shipX, war.shipY), time)
        war.blasts.forEach { blast ->
            if (blast.age >= 0f) {
                val life = (blast.age / 0.55f).coerceIn(0f, 1f)
                val at = Offset(blast.x, blast.y)
                val radius = blast.size * (0.4f + life)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White, Gold, Color(0xFFFF5A1F), Color(0x66701A10), Color.Transparent), at, radius,
                    ),
                    radius, at, alpha = 1f - life,
                )
                drawCircle(Color(0xFFFFE2A8), blast.size * (0.5f + 1.7f * life), at, alpha = (1f - life) * 0.6f, style = Stroke(0.6f))
                repeat(12) {
                    val angle = (it * 2 * PI / 12 + blast.size).toFloat()
                    val dir = Offset(cos(angle), sin(angle))
                    val far = radius * (0.8f + (it % 3) * 0.3f)
                    drawLine(
                        if (it % 2 == 0) Gold else Color(0xFFFF8A3D), at + dir * (far * 0.8f), at + dir * (far * 1.15f),
                        0.8f * (1f - life) + 0.2f, StrokeCap.Round, alpha = 1f - life,
                    )
                }
            }
        }
    }

    /** A closed shape around [at]; the points are offsets, mirrored left and right by the caller. */
    private fun shape(at: Offset, vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(at.x + x, at.y + y) else lineTo(at.x + x, at.y + y) }
        close()
    }

    private fun DrawScope.hull(path: Path, light: Color, dark: Color, at: Offset, size: Float) {
        drawPath(path, Brush.verticalGradient(listOf(light, dark), at.y - size, at.y + size))
        drawPath(path, Color.Black.copy(alpha = 0.55f), style = Stroke(0.45f))
    }

    private fun DrawScope.glow(at: Offset, radius: Float, color: Color) =
        drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), at, radius), radius, at)

    /** An engine flame: a flickering drop with a hot white core. */
    private fun DrawScope.plume(at: Offset, width: Float, length: Float) {
        fun drop(w: Float, l: Float) = Path().apply {
            moveTo(at.x - w, at.y)
            cubicTo(at.x - w, at.y + l * 0.45f, at.x - w * 0.15f, at.y + l * 0.7f, at.x, at.y + l)
            cubicTo(at.x + w * 0.15f, at.y + l * 0.7f, at.x + w, at.y + l * 0.45f, at.x + w, at.y)
            close()
        }
        glow(at + Offset(0f, length * 0.3f), width * 3f, Color(0x6635B8FF))
        drawPath(drop(width, length), Brush.verticalGradient(listOf(Color(0xFF9BE8FF), Color(0xFF2D7DFF), Color.Transparent), at.y, at.y + length))
        drawPath(drop(width * 0.45f, length * 0.55f), Brush.verticalGradient(listOf(Color.White, Color(0x00FFFFFF)), at.y, at.y + length * 0.55f))
    }

    private fun DrawScope.player(at: Offset, time: Int) {
        // The wing on the side of the turn looks narrower, as if the ship banked.
        val tilt = bank.coerceIn(-1f, 1f)
        val flicker = sin(time * 0.9f) * 0.8f + sin(time * 2.3f) * 0.4f
        plume(at + Offset(0f, 8f), 1.5f, 8f + flicker * 1.6f)
        for (side in listOf(-1f, 1f)) plume(at + Offset(side * 3.1f, 7f), 0.8f, 4.6f - flicker)
        for (side in listOf(-1f, 1f)) {
            val span = 1f - 0.3f * maxOf(0f, side * tilt)
            fun x(value: Float) = side * value * span
            val wing = shape(at, x(1.6f) to -2.5f, x(4.2f) to 0.2f, x(10.4f) to 4.6f, x(10.6f) to 7.4f, x(6.4f) to 5.9f, x(1.8f) to 6.6f)
            drawPath(
                wing,
                Brush.linearGradient(
                    listOf(Color(0xFF7FB6FF), Color(0xFF2858C8), Color(0xFF0E1F5E)),
                    at + Offset(0f, -2f), at + Offset(x(10.5f), 8f),
                ),
            )
            drawPath(wing, Color(0xFF071033), style = Stroke(0.4f))
            // Lit leading edge, red stripe and a panel line.
            drawLine(Color(0xFFD8F4FF), at + Offset(x(1.8f), -2.3f), at + Offset(x(10.4f), 4.6f), 0.45f, StrokeCap.Round)
            hull(shape(at, x(5.2f) to 2.2f, x(7f) to 3.5f, x(5.9f) to 5.5f, x(4.3f) to 5.7f), Color(0xFFFF7A6B), Color(0xFFB3152F), at, 6f)
            drawLine(Color(0x66000000), at + Offset(x(2f), 3.4f), at + Offset(x(9.4f), 6.2f), 0.3f)
            // Wing cannon.
            val gun = at + Offset(x(10.2f), 0.8f)
            drawRoundRect(Brush.horizontalGradient(listOf(Color.White, Color(0xFF5A6A94)), gun.x - 0.7f, gun.x + 0.7f), gun - Offset(0.7f, 0f), Size(1.4f, 7.4f), CornerRadius(0.7f))
            drawCircle(Cyan, 0.55f, gun)
            drawCircle(if (time % 24 < 12) Red else Color(0xFF7CFF8A), 0.6f, gun + Offset(0f, 7f))
            // Rear fin.
            hull(shape(at, x(1.5f) to 3f, x(4.6f) to 9.4f, x(3.9f) to 9.8f, x(1.2f) to 7.6f), Color(0xFFC8D6F5), Color(0xFF34457E), at, 8f)
        }
        val body = Path().apply {
            moveTo(at.x, at.y - 12f)
            cubicTo(at.x + 1.6f, at.y - 9f, at.x + 3.1f, at.y - 3f, at.x + 2.9f, at.y + 2f)
            lineTo(at.x + 2.2f, at.y + 8.2f)
            lineTo(at.x - 2.2f, at.y + 8.2f)
            lineTo(at.x - 2.9f, at.y + 2f)
            cubicTo(at.x - 3.1f, at.y - 3f, at.x - 1.6f, at.y - 9f, at.x, at.y - 12f)
            close()
        }
        // Shaded from one side so the fuselage looks round.
        drawPath(
            body,
            Brush.horizontalGradient(
                listOf(Color(0xFF8FA0C8), Color.White, Color(0xFFC3CEE8), Color(0xFF56648F)), at.x - 3f, at.x + 3f,
            ),
        )
        drawPath(body, Color(0xFF0A1440), style = Stroke(0.4f))
        drawLine(Color(0xFFE0263F), at + Offset(0f, -11.4f), at + Offset(0f, -8f), 0.8f, StrokeCap.Round)
        drawLine(Color(0x55000000), at + Offset(-2.5f, 3.6f), at + Offset(2.5f, 3.6f), 0.35f)
        drawLine(Color(0x55000000), at + Offset(0f, 3.8f), at + Offset(0f, 7.8f), 0.35f)
        // Glass canopy with a reflection.
        val glass = Size(2.8f, 6.6f)
        val corner = at + Offset(-1.4f + tilt * 0.4f, -6.2f)
        drawOval(Brush.verticalGradient(listOf(Color(0xFFE8FFFF), Cyan, Color(0xFF05496B)), corner.y, corner.y + glass.height), corner, glass)
        drawOval(Color(0xFF0A1440), corner, glass, style = Stroke(0.35f))
        drawLine(Color.White, corner + Offset(0.8f, 1.2f), corner + Offset(0.7f, 3.4f), 0.45f, StrokeCap.Round)
        // Engine nozzles.
        for ((x, w) in listOf(0f to 1.7f, -3.1f to 0.9f, 3.1f to 0.9f)) {
            drawRoundRect(Color(0xFF1C2444), at + Offset(x - w, if (x == 0f) 7.4f else 6.4f), Size(w * 2, 1.5f), CornerRadius(0.5f))
        }
        if (war.shield > 0f && (war.shield > 2f || time % 10 < 6)) {
            val pulse = 14f + sin(time / 5f) * 0.6f
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Cyan.copy(alpha = 0.4f)), at, pulse), pulse, at)
            drawCircle(Cyan.copy(alpha = 0.85f), pulse, at, style = Stroke(0.55f))
            drawArc(Color.White, (time * 4f) % 360f, 55f, false, at - Offset(pulse, pulse), Size(pulse * 2, pulse * 2), style = Stroke(0.9f, cap = StrokeCap.Round))
        }
    }

    private fun DrawScope.enemy(e: Enemy, time: Int) {
        val at = Offset(e.x, e.y)
        val r = e.foe.radius
        val halo = when (e.foe) {
            Foe.Scout -> Red
            Foe.Weaver -> Color(0xFF5CFF8A)
            Foe.Shooter -> Color(0xFFB070FF)
            Foe.Diver -> Color(0xFFFF9A2B)
            Foe.Boss -> Color(0xFF6F8CFF)
            else -> Color.Transparent
        }
        glow(at, r * 1.9f, halo.copy(alpha = if (halo == Color.Transparent) 0f else 0.22f))
        when (e.foe) {
            Foe.Scout -> {
                // A red fighter diving nose first, with swept wings and two engines.
                for (side in listOf(-1f, 1f)) {
                    glow(at + Offset(side * r * 0.35f, -r * 0.95f), r * (0.35f + 0.08f * sin(time / 2f)), Color(0xFFFFB347))
                    hull(shape(at, side * r * 0.25f to -r * 0.2f, side * r * 1.25f to -r * 0.85f, side * r * 1.15f to -r * 0.1f, side * r * 0.3f to r * 0.55f), Color(0xFFFF6B7A), Color(0xFF7A0A24), at, r)
                    drawLine(Color(0xFFFFD0D6), at + Offset(side * r * 1.2f, -r * 0.8f), at + Offset(side * r * 1.15f, r * 0.15f), r * 0.11f, StrokeCap.Round)
                }
                hull(shape(at, 0f to r * 1.2f, r * 0.42f to r * 0.1f, r * 0.34f to -r * 0.9f, -r * 0.34f to -r * 0.9f, -r * 0.42f to r * 0.1f), Color(0xFFFFB3BC), Color(0xFFC0152F), at, r)
                drawOval(Brush.verticalGradient(listOf(Color.White, Gold, Color(0xFF8A4B00)), at.y - r * 0.3f, at.y + r * 0.5f), at + Offset(-r * 0.2f, -r * 0.3f), Size(r * 0.4f, r * 0.8f))
            }
            Foe.Weaver -> {
                // A space insect: clear beating wings, striped body and antennae.
                val beat = 1f + 0.22f * sin(time / 2.2f)
                for (side in listOf(-1f, 1f)) {
                    val upper = shape(at, 0f to -r * 0.25f, side * r * 1.4f * beat to -r * 0.95f, side * r * 1.25f * beat to -r * 0.15f, side * r * 0.2f to r * 0.15f)
                    val lower = shape(at, 0f to 0f, side * r * 1.05f * beat to r * 0.15f, side * r * 0.8f * beat to r * 0.75f, side * r * 0.15f to r * 0.4f)
                    for (wing in listOf(upper, lower)) {
                        drawPath(wing, Color(0x8878FFC0))
                        drawPath(wing, Color(0xFFD8FFE8), style = Stroke(0.3f))
                    }
                    drawLine(Color(0xFF9CFFB8), at + Offset(side * r * 0.15f, r * 0.75f), at + Offset(side * r * 0.5f, r * 1.25f), 0.45f, StrokeCap.Round)
                }
                drawOval(Brush.verticalGradient(listOf(Color(0xFFB8FFCE), Color(0xFF0E7A45)), at.y - r, at.y + r), at - Offset(r * 0.42f, r * 0.9f), Size(r * 0.84f, r * 1.6f))
                for (k in 0..2) drawLine(Color(0xAA043320), at + Offset(-r * 0.36f, -r * 0.55f + k * r * 0.38f), at + Offset(r * 0.36f, -r * 0.55f + k * r * 0.38f), r * 0.13f)
                for (side in listOf(-1f, 1f)) {
                    drawCircle(Red, r * 0.17f, at + Offset(side * r * 0.24f, r * 0.62f))
                    drawCircle(Color.White, r * 0.05f, at + Offset(side * r * 0.2f, r * 0.57f))
                }
            }
            Foe.Shooter -> {
                drawOval(Brush.verticalGradient(listOf(Color(0xFFD9B8FF), Color(0xFF4B2491)), at.y - r * 0.5f, at.y + r * 0.5f), at - Offset(r * 1.15f, r * 0.42f), Size(r * 2.3f, r * 0.95f))
                drawOval(Brush.verticalGradient(listOf(Color.White, Cyan), at.y - r, at.y), at - Offset(r * 0.5f, r * 0.9f), Size(r, r))
                for (k in -2..2) drawCircle(if ((time / 8 + k) % 2 == 0) Gold else Red, r * 0.11f, at + Offset(k * r * 0.45f, r * 0.12f))
                val core = at + Offset(0f, r * 0.55f)
                drawCircle(Brush.radialGradient(listOf(Red, Color.Transparent), core, r * 0.7f), r * 0.7f * (1f - e.fire.coerceIn(0f, 1f) * 0.6f), core)
            }
            Foe.Diver -> {
                drawLine(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFFB02E)), at.y - r * 2.4f, at.y - r), at + Offset(0f, -r * 2.4f), at + Offset(0f, -r), r * 0.5f, StrokeCap.Round)
                hull(shape(at, 0f to r * 1.25f, r * 0.45f to -r * 0.2f, r * 1.05f to -r, 0f to -r * 0.55f, -r * 1.05f to -r, -r * 0.45f to -r * 0.2f), Color(0xFFFFD27A), Color(0xFFC2520C), at, r)
                drawCircle(Color(0xFF3A1200), r * 0.2f, at)
            }
            Foe.Rock, Foe.Pebble -> rotate((e.age * 60f + e.startX * 7f) % 360f, at) {
                val points = List(9) {
                    val angle = (it * 2 * PI / 9).toFloat()
                    val reach = r * (0.82f + 0.28f * ((it * 37 + e.startX.toInt()) % 5) / 4f)
                    cos(angle) * reach to sin(angle) * reach
                }
                hull(shape(at, *points.toTypedArray()), Color(0xFFB9A18A), Color(0xFF4A3A2E), at, r)
                drawCircle(Color(0x66000000), r * 0.22f, at + Offset(r * 0.25f, -r * 0.2f))
                drawCircle(Color(0x55000000), r * 0.14f, at + Offset(-r * 0.35f, r * 0.25f))
            }
            Foe.Boss -> {
                for (side in listOf(-1f, 1f)) {
                    hull(shape(at, side * 4f to -9f, side * 21f to -5f, side * 18f to 6f, side * 12f to 3f, side * 10f to 11f, side * 6f to 4f), Color(0xFF8E9BC4), Color(0xFF232A4A), at, 11f)
                    val turret = at + Offset(side * 15f, 1f)
                    drawCircle(Color(0xFF101424), 2.6f, turret)
                    drawCircle(Red, 1.2f + 0.4f * sin(time / 5f), turret)
                }
                hull(shape(at, 0f to 14f, 6f to 5f, 8f to -7f, 0f to -12f, -8f to -7f, -6f to 5f), Color(0xFFC9D3F0), Color(0xFF3D4670), at, 14f)
                val angry = e.health * 2 < e.fullHealth
                val eye = at + Offset(0f, 2f)
                drawCircle(Brush.radialGradient(listOf(Color.White, if (angry) Red else Gold, Color.Transparent), eye, 7f), 7f, eye)
            }
        }
        // A white flash shows a hit.
        if (e.flash > 0f) drawCircle(Color.White.copy(alpha = 0.55f), r * 0.85f, at)
    }

    private fun DrawScope.hud() {
        drawRect(Brush.verticalGradient(listOf(Color(0xB0000000), Color.Transparent), 0f, 12f), size = Size(SPACE_WIDTH, 12f))
        icon("♥".repeat(war.lives.coerceAtLeast(0)), Offset(15f, 5f), 5f, Red)
        icon("${war.score}", Offset(50f, 5f), 6f, Gold)
        icon("💣${war.bombs}  P${war.weapon}", Offset(84f, 5f), 4.2f)
        war.boss?.let {
            drawRoundRect(Color(0xFF4A0F18), Offset(20f, 10f), Size(60f, 2.2f), CornerRadius(1f))
            drawRoundRect(Red, Offset(20f, 10f), Size(60f * it.health / it.fullHealth, 2.2f), CornerRadius(1f))
        }
    }
}
