package com.nokia.launcher.ui.games

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** A vertical shooter in table units. The width is fixed; the height follows the screen. */
const val SPACE_WIDTH = 100f
const val SHIP_RADIUS = 3.5f
const val MAX_LIVES = 5
const val MAX_WEAPON = 4
const val STAGE_KILLS = 22
private const val FIRE_EVERY = 0.22f
private const val SHOT_SPEED = 170f
private const val SAFE_SECONDS = 2f
private const val SHIELD_SECONDS = 8f

enum class Foe(val radius: Float, val health: Int, val points: Int, val speed: Float) {
    Scout(4.2f, 1, 10, 34f),
    Weaver(4.6f, 2, 20, 26f),
    Shooter(5.6f, 4, 40, 22f),
    Diver(4f, 2, 30, 30f),
    Rock(6.5f, 4, 15, 20f),
    Pebble(3.2f, 1, 5, 34f),
    Boss(15f, 60, 600, 18f),
}

class Enemy(var x: Float, var y: Float, val foe: Foe, var health: Int = foe.health, var drift: Float = 0f) {
    var age = 0f
    var fire = 1.2f
    val startX = x
    val fullHealth = health
    /** Seconds the enemy flashes after being hit. */
    var flash = 0f
}

class Shot(var x: Float, var y: Float, val vx: Float, val vy: Float)

enum class Gift { Weapon, Shield, Bomb, Life }

class Drop(var x: Float, var y: Float, val gift: Gift)

/** An explosion that grows and fades. */
class Blast(val x: Float, val y: Float, val size: Float, var age: Float = 0f)

class SpaceWar(private val random: Random = Random) {
    var height = 140f
    var shipX = SPACE_WIDTH / 2
    var shipY = 122f
    val shots = mutableListOf<Shot>()
    val enemyShots = mutableListOf<Shot>()
    val enemies = mutableListOf<Enemy>()
    val drops = mutableListOf<Drop>()
    val blasts = mutableListOf<Blast>()
    var lives = 3
    var bombs = 2
    var weapon = 1
    var score = 0
    var kills = 0
    var stage = 1
    /** Seconds of protection after being hit. */
    var safe = 0f
    var shield = 0f
    /** Seconds the stage or warning banner stays up. */
    var banner = 2f
    var bombFlash = 0f
    private var bossDue = STAGE_KILLS
    private var fire = 0f
    private var spawn = 1.2f
    val over: Boolean get() = lives <= 0
    val boss: Enemy? get() = enemies.firstOrNull { it.foe == Foe.Boss }

    fun moveTo(x: Float, y: Float) {
        shipX = x.coerceIn(5f, SPACE_WIDTH - 5f)
        shipY = y.coerceIn(height * 0.35f, height - 8f)
    }

    /** A bomb clears the screen; the boss only loses part of its health. */
    fun bomb() {
        if (over || bombs <= 0) return
        bombs--
        bombFlash = 0.4f
        enemyShots.clear()
        enemies.toList().forEach { hurt(it, if (it.foe == Foe.Boss) 18 else it.health) }
    }

    private fun hurt(enemy: Enemy, damage: Int) {
        if (enemy !in enemies) return
        enemy.health -= damage
        enemy.flash = 0.08f
        if (enemy.health > 0) return
        enemies.remove(enemy)
        score += enemy.foe.points * stage
        blasts.add(Blast(enemy.x, enemy.y, enemy.foe.radius * 1.8f))
        when (enemy.foe) {
            Foe.Boss -> {
                repeat(6) { blasts.add(Blast(enemy.x + spread(22f), enemy.y + spread(14f), 10f, -it * 0.1f)) }
                drops.add(Drop(enemy.x - 8f, enemy.y, Gift.Life))
                drops.add(Drop(enemy.x + 8f, enemy.y, Gift.Bomb))
                enemyShots.clear()
                stage++
                bossDue = kills + STAGE_KILLS
                banner = 2.5f
                return
            }
            Foe.Rock -> {
                enemies.add(Enemy(enemy.x - 3f, enemy.y, Foe.Pebble, drift = -18f))
                enemies.add(Enemy(enemy.x + 3f, enemy.y, Foe.Pebble, drift = 18f))
            }
            else -> {}
        }
        kills++
        if (kills % 9 == 0 || random.nextInt(14) == 0) {
            val roll = random.nextInt(10)
            val gift = if (roll < 5) Gift.Weapon else if (roll < 7) Gift.Shield else if (roll < 9) Gift.Bomb else Gift.Life
            drops.add(Drop(enemy.x, enemy.y, gift))
        }
        if (kills >= bossDue && boss == null) {
            enemies.add(Enemy(SPACE_WIDTH / 2, -18f, Foe.Boss, 45 + stage * 25))
            bossDue = Int.MAX_VALUE
            banner = 2f
        }
    }

    private fun spread(width: Float) = (random.nextFloat() - 0.5f) * width

    private fun hitShip() {
        if (safe > 0f) return
        blasts.add(Blast(shipX, shipY, 7f))
        if (shield > 0f) {
            shield = 0f
            safe = 0.8f
            return
        }
        lives--
        weapon = maxOf(1, weapon - 1)
        safe = SAFE_SECONDS
    }

    private fun shoot() {
        fun add(dx: Float, vx: Float = 0f) = shots.add(Shot(shipX + dx, shipY - 6f, vx, -SHOT_SPEED))
        when (weapon) {
            1 -> add(0f)
            2 -> { add(-2.2f); add(2.2f) }
            3 -> { add(0f); add(-2.5f, -26f); add(2.5f, 26f) }
            else -> { add(-1.8f); add(1.8f); add(-3.5f, -32f); add(3.5f, 32f) }
        }
    }

    /** One formation: a V of scouts, a line of weavers, or a few of the others. */
    private fun wave() {
        val x = 18f + random.nextFloat() * (SPACE_WIDTH - 36f)
        when (random.nextInt(if (stage == 1 && kills < 6) 2 else 5)) {
            0 -> for (k in -2..2) enemies.add(Enemy((x + k * 7f).coerceIn(6f, 94f), -6f - abs(k) * 7f, Foe.Scout))
            1 -> for (k in 0..3) enemies.add(Enemy(x, -6f - k * 10f, Foe.Weaver))
            2 -> for (side in listOf(-1, 1)) enemies.add(Enemy(50f + side * 28f, -8f, Foe.Shooter))
            3 -> for (k in 0..2) enemies.add(Enemy(15f + k * 35f, -6f - k * 8f, Foe.Diver))
            else -> repeat(3) { enemies.add(Enemy(8f + random.nextFloat() * 84f, -8f - it * 14f, Foe.Rock, drift = spread(14f))) }
        }
    }

    private fun aim(from: Enemy, speed: Float, offset: Float = 0f) {
        val angle = atan2(shipY - from.y, shipX - from.x) + offset
        enemyShots.add(Shot(from.x, from.y + from.foe.radius * 0.6f, cos(angle) * speed, sin(angle) * speed))
    }

    fun step(seconds: Float) {
        if (over) return
        safe -= seconds
        shield -= seconds
        banner -= seconds
        bombFlash -= seconds
        fire -= seconds
        spawn -= seconds
        blasts.forEach { it.age += seconds }
        blasts.removeAll { it.age > 0.55f }
        if (fire <= 0f) {
            shoot()
            fire = FIRE_EVERY
        }
        if (spawn <= 0f && boss == null && bossDue != Int.MAX_VALUE) {
            wave()
            spawn = (2.6f - stage * 0.18f).coerceAtLeast(1.3f)
        }
        for (list in listOf(shots, enemyShots)) {
            list.forEach { it.x += it.vx * seconds; it.y += it.vy * seconds }
            list.removeAll { it.y < -4f || it.y > height + 4f || it.x < -4f || it.x > SPACE_WIDTH + 4f }
        }
        val speedUp = 1f + (stage - 1) * 0.12f
        for (enemy in enemies.toList()) {
            enemy.age += seconds
            enemy.flash -= seconds
            enemy.fire -= seconds
            val down = enemy.foe.speed * speedUp * seconds
            when (enemy.foe) {
                Foe.Weaver -> {
                    enemy.y += down
                    enemy.x = (enemy.startX + sin(enemy.age * 2.6f) * 18f).coerceIn(5f, 95f)
                }
                Foe.Shooter -> {
                    // Comes down, stays a while shooting, then leaves.
                    if (enemy.y < height * 0.22f || enemy.age > 7f) enemy.y += down
                    if (enemy.fire <= 0f && enemy.y > 0f) {
                        aim(enemy, 48f + stage * 4f)
                        enemy.fire = (1.9f - stage * 0.15f).coerceAtLeast(0.9f)
                    }
                }
                Foe.Diver -> {
                    // Lines up with the ship, then rushes down.
                    if (enemy.age < 1.4f) {
                        enemy.y += down
                        enemy.x += (shipX - enemy.x) * minOf(1f, seconds * 1.8f)
                    } else {
                        enemy.y += down * 3f
                    }
                }
                Foe.Boss -> {
                    if (enemy.y < 24f) {
                        enemy.y += down
                    } else {
                        enemy.x = SPACE_WIDTH / 2 + sin(enemy.age * 0.8f) * 30f
                        if (enemy.fire <= 0f) {
                            // Below half health it attacks faster and wider.
                            val angry = enemy.health * 2 < enemy.fullHealth
                            val fan = if (angry) 3 else 2
                            if ((enemy.age / 4f).toInt() % 2 == 0) for (k in -fan..fan) aim(enemy, 46f, k * 0.26f)
                            else for (k in -1..1) aim(enemy, 72f, k * 0.08f)
                            enemy.fire = if (angry) 0.75f else 1.15f
                        }
                    }
                }
                else -> {
                    enemy.y += down
                    enemy.x += enemy.drift * seconds
                }
            }
            if (hypot(enemy.x - shipX, enemy.y - shipY) < enemy.foe.radius + SHIP_RADIUS * 0.7f) {
                hitShip()
                if (enemy.foe != Foe.Boss) hurt(enemy, enemy.health)
            } else if (enemy.y > height + 10f || enemy.x < -12f || enemy.x > SPACE_WIDTH + 12f) {
                enemies.remove(enemy)
            }
        }
        for (shot in shots.toList()) {
            val target = enemies.firstOrNull { it.y > -4f && hypot(it.x - shot.x, it.y - shot.y) < it.foe.radius + 1.2f }
                ?: continue
            shots.remove(shot)
            hurt(target, 1)
        }
        val hits = enemyShots.filter { hypot(it.x - shipX, it.y - shipY) < SHIP_RADIUS }
        if (hits.isNotEmpty()) {
            enemyShots.removeAll(hits)
            hitShip()
        }
        for (drop in drops.toList()) {
            drop.y += 18f * seconds
            if (hypot(drop.x - shipX, drop.y - shipY) < SHIP_RADIUS + 5f) {
                drops.remove(drop)
                collect(drop.gift)
            } else if (drop.y > height + 6f) {
                drops.remove(drop)
            }
        }
    }

    private fun collect(gift: Gift) {
        score += 50
        when (gift) {
            Gift.Weapon -> weapon = minOf(MAX_WEAPON, weapon + 1)
            Gift.Shield -> shield = SHIELD_SECONDS
            Gift.Bomb -> bombs = minOf(9, bombs + 1)
            Gift.Life -> lives = minOf(MAX_LIVES, lives + 1)
        }
    }

    /** Only the progress is kept; what is on screen is not. */
    fun encode() = "$score;$lives;$bombs;$kills;$stage;$weapon"

    fun load(text: String) {
        val f = text.split(";").mapNotNull { it.toIntOrNull() }
        if (f.size != 6) return
        score = f[0]
        lives = f[1].coerceIn(0, MAX_LIVES)
        bombs = f[2].coerceIn(0, 9)
        kills = f[3]
        stage = f[4].coerceAtLeast(1)
        weapon = f[5].coerceIn(1, MAX_WEAPON)
        bossDue = kills + STAGE_KILLS
    }
}
