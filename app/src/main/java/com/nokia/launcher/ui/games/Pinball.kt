package com.nokia.launcher.ui.games

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Table units. The table is symmetric around the middle. */
const val TABLE_WIDTH = 100f
const val TABLE_HEIGHT = 180f
const val BALL_RADIUS = 3f
const val BALLS = 3
private const val GRAVITY = 140f
private const val MAX_SPEED = 260f
private const val FLIPPER_SPEED = 14f // radians per second
const val FLIPPER_LENGTH = 17f
private const val FLIPPER_RADIUS = 1.8f
/** Where a ball waits to be launched, against the right side. */
const val LAUNCH_X = 93.5f
private const val LAUNCH_Y = 128f

data class Wall(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val bounce: Float = 0.45f, val points: Int = 0)

data class Bumper(val x: Float, val y: Float, val radius: Float, val points: Int)

/** The border as one line: up from the left flipper, round the top and down to the right one. */
val OUTLINE: List<Pair<Float, Float>> = listOf(
    30f to 158f, 3f to 136f, 3f to 28f, 14f to 9f, 34f to 2f, 66f to 2f, 86f to 9f, 97f to 28f, 97f to 136f, 70f to 158f,
)

val WALLS = OUTLINE.zipWithNext { a, b -> Wall(a.first, a.second, b.first, b.second) } + listOf(
    // Dividers between the top lanes.
    Wall(44f, 14f, 44f, 24f), Wall(56f, 14f, 56f, 24f),
    // Slingshots.
    Wall(13f, 116f, 23f, 136f, bounce = 1.05f, points = 10),
    Wall(87f, 116f, 77f, 136f, bounce = 1.05f, points = 10),
)

val BUMPERS = listOf(
    Bumper(31f, 40f, 7f, 100), Bumper(69f, 40f, 7f, 100), Bumper(50f, 60f, 7f, 150),
    Bumper(15f, 74f, 3.5f, 50), Bumper(85f, 74f, 3.5f, 50),
)

data class Spot(val x: Float, val y: Float)

/** Rollover lanes at the top; lighting all three raises the multiplier. */
val LANES = listOf(Spot(38f, 19f), Spot(50f, 19f), Spot(62f, 19f))

/** Star targets on both sides; hitting the four pays a jackpot. */
val TARGETS = listOf(Spot(9f, 52f), Spot(9f, 96f), Spot(91f, 52f), Spot(91f, 96f))
const val TARGET_RADIUS = 3f
const val JACKPOT = 2000
private const val SAVER_SECONDS = 8f
private const val MAX_MULTIPLIER = 5

/** A flipper: pivot, resting and raised angles (0 points right, positive points down). */
data class Flipper(val x: Float, val y: Float, val rest: Float, val raised: Float)

private fun degrees(value: Float) = value * PI.toFloat() / 180f

val LEFT_FLIPPER = Flipper(30f, 159f, degrees(30f), degrees(-30f))
val RIGHT_FLIPPER = Flipper(70f, 159f, degrees(150f), degrees(210f))

/** A mutable simulation; the screen redraws after each [step]. */
class PinballGame {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var score = 0
    var balls = BALLS
    /** True while the ball waits on the plunger. */
    var waiting = true
    /** True while the launched ball climbs to the top; it passes the side targets. */
    private var launching = false
    var leftAngle = LEFT_FLIPPER.rest
    var rightAngle = RIGHT_FLIPPER.rest
    /** The bumper that was just hit, to light it up. */
    var lit = -1
    var litTime = 0f
    var multiplier = 1
    var lanes = 0
    var targets = 0
    /** Seconds left in which a drained ball comes back; once per ball. */
    var saver = 0f
    private var saverUsed = false
    /** The last points earned, shown floating where they happened. */
    var popup = ""
    var popupX = 0f
    var popupY = 0f
    var popupTime = 0f
    val over: Boolean get() = balls == 0

    init {
        reset()
    }

    private fun reset() {
        x = LAUNCH_X
        y = LAUNCH_Y
        vx = 0f
        vy = 0f
        waiting = true
    }

    fun launch(power: Float = 1f) {
        if (!waiting || over) return
        waiting = false
        launching = true
        saver = if (saverUsed) 0f else SAVER_SECONDS
        vy = -MAX_SPEED * (0.82f + 0.18f * power.coerceIn(0f, 1f))
    }

    fun step(seconds: Float, left: Boolean, right: Boolean) {
        // Small substeps keep the fast ball from crossing walls.
        val parts = (seconds / 0.002f).toInt().coerceIn(1, 40)
        val dt = seconds / parts
        repeat(parts) { substep(dt, left, right) }
    }

    private fun substep(dt: Float, left: Boolean, right: Boolean) {
        litTime -= dt
        popupTime -= dt
        if (litTime <= 0f) lit = -1
        val oldLeft = leftAngle
        val oldRight = rightAngle
        leftAngle = turn(leftAngle, if (left) LEFT_FLIPPER.raised else LEFT_FLIPPER.rest, dt)
        rightAngle = turn(rightAngle, if (right) RIGHT_FLIPPER.raised else RIGHT_FLIPPER.rest, dt)
        if (waiting || over) return
        saver -= dt
        vy += GRAVITY * dt
        val speed = hypot(vx, vy)
        if (speed > MAX_SPEED) {
            vx *= MAX_SPEED / speed
            vy *= MAX_SPEED / speed
        }
        x += vx * dt
        y += vy * dt
        for (wall in WALLS) {
            if (segment(wall.x1, wall.y1, wall.x2, wall.y2, BALL_RADIUS, wall.bounce, 0f, 0f, 0f)) {
                score += wall.points
            }
        }
        BUMPERS.forEachIndexed { index, bumper ->
            val dx = x - bumper.x
            val dy = y - bumper.y
            val distance = hypot(dx, dy)
            val reach = bumper.radius + BALL_RADIUS
            if (distance < reach && distance > 0f) {
                val nx = dx / distance
                val ny = dy / distance
                x = bumper.x + nx * reach
                y = bumper.y + ny * reach
                val kick = maxOf(95f, hypot(vx, vy) * 0.9f)
                vx = nx * kick
                vy = ny * kick
                gain(bumper.points, bumper.x, bumper.y)
                lit = index
                litTime = 0.15f
            }
        }
        LANES.forEachIndexed { index, lane ->
            if (lanes and (1 shl index) == 0 && hypot(x - lane.x, y - lane.y) < 4f) {
                lanes = lanes or (1 shl index)
                gain(250, lane.x, lane.y)
                if (lanes == 7) {
                    lanes = 0
                    multiplier = minOf(MAX_MULTIPLIER, multiplier + 1)
                    popup = "x$multiplier"
                }
            }
        }
        if (launching && (y < 26f || vy >= 0f)) launching = false
        TARGETS.forEachIndexed { index, target ->
            if (launching) return@forEachIndexed
            val distance = hypot(x - target.x, y - target.y)
            val reach = TARGET_RADIUS + BALL_RADIUS
            if (distance < reach && distance > 0f) {
                val nx = (x - target.x) / distance
                val ny = (y - target.y) / distance
                x = target.x + nx * reach
                y = target.y + ny * reach
                val into = vx * nx + vy * ny
                if (into < 0f) {
                    vx -= 1.8f * into * nx
                    vy -= 1.8f * into * ny
                }
                if (targets and (1 shl index) == 0) {
                    targets = targets or (1 shl index)
                    gain(300, target.x, target.y)
                    if (targets == 15) {
                        targets = 0
                        gain(JACKPOT, 50f, 90f)
                    }
                }
            }
        }
        flipper(LEFT_FLIPPER, leftAngle, (leftAngle - oldLeft) / dt)
        flipper(RIGHT_FLIPPER, rightAngle, (rightAngle - oldRight) / dt)
        if (y > TABLE_HEIGHT + BALL_RADIUS || x !in 0f..TABLE_WIDTH || y < 0f) {
            if (saver > 0f) {
                saverUsed = true
            } else {
                balls--
                saverUsed = false
                multiplier = 1
                lanes = 0
            }
            reset()
        }
    }

    private fun gain(points: Int, atX: Float, atY: Float) {
        score += points * multiplier
        popup = "+${points * multiplier}"
        popupX = atX
        popupY = atY
        popupTime = 0.8f
    }

    private fun turn(angle: Float, target: Float, dt: Float): Float {
        val most = FLIPPER_SPEED * dt
        return angle + (target - angle).coerceIn(-most, most)
    }

    private fun flipper(flipper: Flipper, angle: Float, spin: Float) {
        segment(
            flipper.x, flipper.y,
            flipper.x + cos(angle) * FLIPPER_LENGTH, flipper.y + sin(angle) * FLIPPER_LENGTH,
            BALL_RADIUS + FLIPPER_RADIUS, 0.35f, flipper.x, flipper.y, spin,
        )
    }

    /** Pushes the ball out of a segment that may be rotating around a pivot. Returns true on a hit. */
    private fun segment(
        x1: Float, y1: Float, x2: Float, y2: Float, reach: Float, bounce: Float,
        pivotX: Float, pivotY: Float, spin: Float,
    ): Boolean {
        val sx = x2 - x1
        val sy = y2 - y1
        val t = (((x - x1) * sx + (y - y1) * sy) / (sx * sx + sy * sy)).coerceIn(0f, 1f)
        val px = x1 + sx * t
        val py = y1 + sy * t
        val distance = hypot(x - px, y - py)
        if (distance >= reach || distance == 0f) return false
        val nx = (x - px) / distance
        val ny = (y - py) / distance
        x = px + nx * reach
        y = py + ny * reach
        // Speed of the surface at the contact point, from the flipper's rotation.
        val wallX = -spin * (py - pivotY)
        val wallY = spin * (px - pivotX)
        val into = (vx - wallX) * nx + (vy - wallY) * ny
        if (into >= 0f) return false
        vx -= (1 + bounce) * into * nx
        vy -= (1 + bounce) * into * ny
        // Only a real impact scores, not a ball rolling along.
        return into < -20f
    }

    fun encode() = "$score;$balls"

    companion object {
        /** Restores score and balls; the ball goes back to the plunger. */
        fun decode(text: String): PinballGame = PinballGame().apply {
            val f = text.split(";")
            score = f.getOrNull(0)?.toIntOrNull() ?: 0
            balls = f.getOrNull(1)?.toIntOrNull()?.coerceIn(0, BALLS) ?: BALLS
        }
    }
}
