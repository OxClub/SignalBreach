package com.wyrix.cybertrace

enum class GameMode {
    STEALTH_GRID,   // Top-down infiltration: dodge radar cones & lasers, collect cores, reach exit
    CORE_BREACH     // Rotating firewall rings: time your taps to fire pulses through ring gaps
}

data class RadarScanner(
    val x: Float, // normalized 0..1
    val y: Float, // normalized 0..1
    val radius: Float, // normalized to width
    val sweepAngleDeg: Float,
    val speedDegPerSec: Float,
    val startAngleDeg: Float = 0f
)

data class LaserBeam(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cyclePeriodSec: Float, // 0f = always on
    val onRatio: Float = 0.55f
)

data class DataCore(
    val id: Int,
    val x: Float,
    val y: Float
)

data class FirewallRing(
    val radiusRatio: Float, // relative to min(w,h)
    val gapAngleDeg: Float,
    val speedDegPerSec: Float,
    val initialAngleDeg: Float
)

data class GameLevel(
    val levelNum: Int,
    val codeName: String,
    val mode: GameMode,
    val timeLimitSec: Float,
    // Stealth Grid params
    val spawnX: Float = 0.5f,
    val spawnY: Float = 0.88f,
    val exitX: Float = 0.5f,
    val exitY: Float = 0.12f,
    val scanners: List<RadarScanner> = emptyList(),
    val lasers: List<LaserBeam> = emptyList(),
    val cores: List<DataCore> = emptyList(),
    // Core Breach params
    val rings: List<FirewallRing> = emptyList(),
    val pulsesRequired: Int = 3
)

object LevelRepository {
    val levels = listOf(
        // LEVEL 1: Stealth Intro
        GameLevel(
            levelNum = 1,
            codeName = "NODE-01: PERIMETER",
            mode = GameMode.STEALTH_GRID,
            timeLimitSec = 35f,
            spawnX = 0.5f, spawnY = 0.88f,
            exitX = 0.5f, exitY = 0.12f,
            scanners = listOf(
                RadarScanner(0.5f, 0.50f, 0.34f, 55f, 75f, 0f)
            ),
            cores = listOf(
                DataCore(1, 0.22f, 0.50f),
                DataCore(2, 0.78f, 0.50f)
            )
        ),
        // LEVEL 2: Core Breach Intro
        GameLevel(
            levelNum = 2,
            codeName = "CORE-02: HANDSHAKE",
            mode = GameMode.CORE_BREACH,
            timeLimitSec = 25f,
            rings = listOf(
                FirewallRing(0.16f, 65f, 95f, 0f),
                FirewallRing(0.24f, 60f, -110f, 120f),
                FirewallRing(0.32f, 55f, 85f, 240f)
            ),
            pulsesRequired = 3
        ),
        // LEVEL 3: Dual Scanners + Laser
        GameLevel(
            levelNum = 3,
            codeName = "NODE-03: LASER GATE",
            mode = GameMode.STEALTH_GRID,
            timeLimitSec = 40f,
            spawnX = 0.18f, spawnY = 0.88f,
            exitX = 0.82f, exitY = 0.12f,
            scanners = listOf(
                RadarScanner(0.32f, 0.62f, 0.28f, 50f, 95f, 45f),
                RadarScanner(0.68f, 0.36f, 0.28f, 50f, -105f, 180f)
            ),
            lasers = listOf(
                LaserBeam(0.10f, 0.49f, 0.90f, 0.49f, cyclePeriodSec = 3.2f, onRatio = 0.5f)
            ),
            cores = listOf(
                DataCore(1, 0.82f, 0.72f),
                DataCore(2, 0.18f, 0.28f),
                DataCore(3, 0.50f, 0.18f)
            )
        ),
        // LEVEL 4: Fast Core Breach
        GameLevel(
            levelNum = 4,
            codeName = "CORE-04: CIPHER LOCK",
            mode = GameMode.CORE_BREACH,
            timeLimitSec = 25f,
            rings = listOf(
                FirewallRing(0.14f, 55f, -135f, 30f),
                FirewallRing(0.21f, 52f, 145f, 180f),
                FirewallRing(0.28f, 50f, -120f, 90f),
                FirewallRing(0.35f, 48f, 110f, 270f)
            ),
            pulsesRequired = 4
        ),
        // LEVEL 5: Crossfire Grid
        GameLevel(
            levelNum = 5,
            codeName = "NODE-05: CROSSFIRE",
            mode = GameMode.STEALTH_GRID,
            timeLimitSec = 45f,
            spawnX = 0.5f, spawnY = 0.90f,
            exitX = 0.5f, exitY = 0.10f,
            scanners = listOf(
                RadarScanner(0.25f, 0.30f, 0.26f, 48f, 115f, 0f),
                RadarScanner(0.75f, 0.30f, 0.26f, 48f, -115f, 180f),
                RadarScanner(0.50f, 0.65f, 0.30f, 52f, 125f, 90f)
            ),
            lasers = listOf(
                LaserBeam(0.10f, 0.48f, 0.44f, 0.48f, cyclePeriodSec = 2.6f, onRatio = 0.55f),
                LaserBeam(0.56f, 0.48f, 0.90f, 0.48f, cyclePeriodSec = 2.6f, onRatio = 0.55f)
            ),
            cores = listOf(
                DataCore(1, 0.15f, 0.65f),
                DataCore(2, 0.85f, 0.65f),
                DataCore(3, 0.50f, 0.30f)
            )
        ),
        // LEVEL 6: Core Overload
        GameLevel(
            levelNum = 6,
            codeName = "CORE-06: OVERLOAD",
            mode = GameMode.CORE_BREACH,
            timeLimitSec = 28f,
            rings = listOf(
                FirewallRing(0.13f, 48f, 165f, 0f),
                FirewallRing(0.19f, 46f, -160f, 110f),
                FirewallRing(0.25f, 45f, 150f, 220f),
                FirewallRing(0.31f, 44f, -140f, 45f),
                FirewallRing(0.37f, 42f, 130f, 300f)
            ),
            pulsesRequired = 4
        ),
        // LEVEL 7: Vault Corridor
        GameLevel(
            levelNum = 7,
            codeName = "NODE-07: DEEP VAULT",
            mode = GameMode.STEALTH_GRID,
            timeLimitSec = 50f,
            spawnX = 0.15f, spawnY = 0.88f,
            exitX = 0.85f, exitY = 0.12f,
            scanners = listOf(
                RadarScanner(0.50f, 0.25f, 0.30f, 55f, 135f, 0f),
                RadarScanner(0.50f, 0.50f, 0.30f, 55f, -145f, 180f),
                RadarScanner(0.50f, 0.75f, 0.30f, 55f, 135f, 90f)
            ),
            lasers = listOf(
                LaserBeam(0.25f, 0.15f, 0.25f, 0.85f, cyclePeriodSec = 3.0f, onRatio = 0.45f),
                LaserBeam(0.75f, 0.15f, 0.75f, 0.85f, cyclePeriodSec = 3.0f, onRatio = 0.45f)
            ),
            cores = listOf(
                DataCore(1, 0.15f, 0.20f),
                DataCore(2, 0.85f, 0.80f),
                DataCore(3, 0.50f, 0.38f),
                DataCore(4, 0.50f, 0.62f)
            )
        ),
        // LEVEL 8: Omega Core
        GameLevel(
            levelNum = 8,
            codeName = "CORE-08: MAINFRAME",
            mode = GameMode.CORE_BREACH,
            timeLimitSec = 30f,
            rings = listOf(
                FirewallRing(0.13f, 44f, -185f, 15f),
                FirewallRing(0.19f, 44f, 180f, 140f),
                FirewallRing(0.25f, 42f, -170f, 260f),
                FirewallRing(0.31f, 42f, 165f, 75f),
                FirewallRing(0.37f, 40f, -155f, 195f)
            ),
            pulsesRequired = 5
        )
    )
}
