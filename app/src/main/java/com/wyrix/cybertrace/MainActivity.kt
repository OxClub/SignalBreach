package com.wyrix.cybertrace

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

val BgDark = Color(0xFF05080C)
val PanelDark = Color(0xFF0C131B)
val NeonGreen = Color(0xFF00FF9C)
val NeonCyan = Color(0xFF00E5FF)
val NeonRed = Color(0xFFFF2A55)
val NeonAmber = Color(0xFFFFB800)
val TextLight = Color(0xFFE2F1F8)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val color: Color,
    val size: Float
)

data class ActivePulse(
    var y: Float, // normalized distance from center (0.46f -> 0f)
    val speed: Float = 0.85f
)

enum class ScreenState {
    MENU, LEVEL_SELECT, PLAYING
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalBreachGameRoot()
        }
    }
}

@Composable
fun SignalBreachGameRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("signal_breach_arcade", Context.MODE_PRIVATE) }

    var unlockedLevel by remember {
        mutableIntStateOf(prefs.getInt("unlocked_level", 1).coerceIn(1, LevelRepository.levels.size))
    }
    var currentScreen by remember { mutableStateOf(ScreenState.MENU) }
    var activeLevelIdx by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (currentScreen) {
            ScreenState.MENU -> {
                TitleMenuScreen(
                    unlockedLevel = unlockedLevel,
                    muted = muted,
                    onToggleMute = {
                        muted = !muted
                        SoundEngine.isMuted = muted
                    },
                    onPlayLatest = {
                        SoundEngine.playClick()
                        activeLevelIdx = (unlockedLevel - 1).coerceIn(0, LevelRepository.levels.lastIndex)
                        currentScreen = ScreenState.PLAYING
                    },
                    onOpenLevels = {
                        SoundEngine.playClick()
                        currentScreen = ScreenState.LEVEL_SELECT
                    }
                )
            }

            ScreenState.LEVEL_SELECT -> {
                LevelSelectScreen(
                    unlockedLevel = unlockedLevel,
                    onSelectLevel = { idx ->
                        SoundEngine.playClick()
                        activeLevelIdx = idx
                        currentScreen = ScreenState.PLAYING
                    },
                    onBack = {
                        SoundEngine.playClick()
                        currentScreen = ScreenState.MENU
                    }
                )
            }

            ScreenState.PLAYING -> {
                val level = LevelRepository.levels[activeLevelIdx]
                key(activeLevelIdx) {
                    GamePlayScreen(
                        level = level,
                        onExitToMenu = {
                            SoundEngine.playClick()
                            currentScreen = ScreenState.MENU
                        },
                        onLevelComplete = {
                            val nextUnlock = maxOf(unlockedLevel, (activeLevelIdx + 2).coerceAtMost(LevelRepository.levels.size))
                            unlockedLevel = nextUnlock
                            prefs.edit().putInt("unlocked_level", nextUnlock).apply()
                            if (activeLevelIdx < LevelRepository.levels.lastIndex) {
                                activeLevelIdx += 1
                            } else {
                                currentScreen = ScreenState.LEVEL_SELECT
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TitleMenuScreen(
    unlockedLevel: Int,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onPlayLatest: () -> Unit,
    onOpenLevels: () -> Unit
) {
    var gridOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val dt = (now - lastTime) / 1_000_000_000f
            lastTime = now
            gridOffset = (gridOffset + dt * 36f) % 60f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated Cyber Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 60f
            var x = gridOffset
            while (x < size.width) {
                drawLine(Color(0xFF0D1D28), Offset(x, 0f), Offset(x, size.height), 1.5f)
                x += step
            }
            var y = gridOffset
            while (y < size.height) {
                drawLine(Color(0xFF0D1D28), Offset(0f, y), Offset(size.width, y), 1.5f)
                y += step
            }
            // Decorative Glowing Rings
            val center = Offset(size.width / 2f, size.height * 0.36f)
            drawCircle(NeonCyan.copy(alpha = 0.08f), radius = size.width * 0.42f, center = center)
            drawCircle(NeonCyan.copy(alpha = 0.25f), radius = size.width * 0.34f, center = center, style = Stroke(2f))
            drawCircle(NeonGreen.copy(alpha = 0.25f), radius = size.width * 0.24f, center = center, style = Stroke(2f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WYRIX INTERACTIVE",
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = if (muted) "AUDIO: OFF" else "AUDIO: ON",
                    color = if (muted) Color.Gray else NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { onToggleMute() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CYBER INFILTRATION PROTOCOL",
                    color = NeonAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SIGNAL\nBREACH",
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 46.sp,
                    lineHeight = 48.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SECTOR PROGRESS: $unlockedLevel / ${LevelRepository.levels.size}",
                    color = TextLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = onPlayLatest,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "▶  PLAY MISSION (LVL $unlockedLevel)",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onOpenLevels,
                    colors = ButtonDefaults.buttonColors(containerColor = PanelDark),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "SELECT SECTOR (${LevelRepository.levels.size} LEVELS)",
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LevelSelectScreen(
    unlockedLevel: Int,
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECT TARGET NODE",
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "[ BACK ]",
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(LevelRepository.levels.size) { idx ->
                val lvl = LevelRepository.levels[idx]
                val isUnlocked = lvl.levelNum <= unlockedLevel
                Column(
                    modifier = Modifier
                        .background(PanelDark, RoundedCornerShape(8.dp))
                        .border(
                            1.5.dp,
                            if (isUnlocked) NeonCyan else Color(0xFF1F2D38),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = isUnlocked) { onSelectLevel(idx) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LVL 0${lvl.levelNum}",
                            color = if (isUnlocked) NeonGreen else Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isUnlocked) "OPEN" else "LOCKED",
                            color = if (isUnlocked) NeonAmber else Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = lvl.codeName,
                        color = if (isUnlocked) TextLight else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (lvl.mode == GameMode.STEALTH_GRID) "MODE: STEALTH GRID" else "MODE: CORE BREACH",
                        color = if (isUnlocked) NeonCyan else Color.DarkGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GamePlayScreen(
    level: GameLevel,
    onExitToMenu: () -> Unit,
    onLevelComplete: () -> Unit
) {
    var elapsedTime by remember { mutableFloatStateOf(0f) }
    var timeRemaining by remember { mutableFloatStateOf(level.timeLimitSec) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var failReason by remember { mutableStateOf("TRACE DETECTED!") }
    var shakeTimer by remember { mutableFloatStateOf(0f) }
    var restartTrigger by remember { mutableIntStateOf(0) }

    // Stealth Grid State
    var playerX by remember { mutableFloatStateOf(level.spawnX) }
    var playerY by remember { mutableFloatStateOf(level.spawnY) }
    var joyCenter by remember { mutableStateOf<Offset?>(null) }
    var joyKnob by remember { mutableStateOf<Offset?>(null) }
    var collectedCoreIds by remember { mutableStateOf(setOf<Int>()) }

    // Core Breach State
    var pulsesLanded by remember { mutableIntStateOf(0) }
    val activePulses = remember { mutableStateListOf<ActivePulse>() }

    // Shared Particles
    val particles = remember { mutableStateListOf<Particle>() }

    fun spawnBurst(nx: Float, ny: Float, color: Color, count: Int = 22) {
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 0.08f + Random.nextFloat() * 0.25f
            particles.add(
                Particle(
                    x = nx,
                    y = ny,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    alpha = 1f,
                    color = color,
                    size = 4f + Random.nextFloat() * 5f
                )
            )
        }
    }

    fun triggerFail(reason: String) {
        if (isGameOver || isVictory) return
        SoundEngine.playExplosion()
        shakeTimer = 0.45f
        failReason = reason
        isGameOver = true
        if (level.mode == GameMode.STEALTH_GRID) {
            spawnBurst(playerX, playerY, NeonRed, 32)
        } else {
            spawnBurst(0.5f, 0.5f, NeonRed, 32)
        }
    }

    fun resetLevel() {
        elapsedTime = 0f
        timeRemaining = level.timeLimitSec
        isGameOver = false
        isVictory = false
        shakeTimer = 0f
        playerX = level.spawnX
        playerY = level.spawnY
        joyCenter = null
        joyKnob = null
        collectedCoreIds = emptySet()
        pulsesLanded = 0
        activePulses.clear()
        particles.clear()
        restartTrigger++
    }

    // 60 FPS Game Loop
    LaunchedEffect(restartTrigger) {
        var lastNanos = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val dt = ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
            lastNanos = now

            if (shakeTimer > 0f) {
                shakeTimer = (shakeTimer - dt).coerceAtLeast(0f)
            }

            // Update Particles
            val pIter = particles.iterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.alpha -= dt * 1.8f
                if (p.alpha <= 0f) pIter.remove()
            }

            if (!isGameOver && !isVictory) {
                elapsedTime += dt
                timeRemaining -= dt
                if (timeRemaining <= 0f) {
                    timeRemaining = 0f
                    triggerFail("TRACE TIMER EXPIRED!")
                }

                if (level.mode == GameMode.STEALTH_GRID) {
                    // Move Player from Virtual Joystick
                    val jc = joyCenter
                    val jk = joyKnob
                    if (jc != null && jk != null) {
                        val dx = jk.x - jc.x
                        val dy = jk.y - jc.y
                        val dist = hypot(dx, dy)
                        if (dist > 6f) {
                            val maxDrag = 90f
                            val strength = (dist / maxDrag).coerceIn(0f, 1f)
                            val speed = 0.34f * strength
                            playerX = (playerX + (dx / dist) * speed * dt).coerceIn(0.05f, 0.95f)
                            playerY = (playerY + (dy / dist) * speed * dt).coerceIn(0.06f, 0.94f)
                        }
                    }

                    // Collect Data Cores
                    level.cores.forEach { core ->
                        if (!collectedCoreIds.contains(core.id)) {
                            if (hypot(playerX - core.x, playerY - core.y) < 0.048f) {
                                collectedCoreIds = collectedCoreIds + core.id
                                SoundEngine.playPickup()
                                spawnBurst(core.x, core.y, NeonCyan, 18)
                            }
                        }
                    }

                    // Check Exit Portal
                    if (collectedCoreIds.size == level.cores.size) {
                        if (hypot(playerX - level.exitX, playerY - level.exitY) < 0.055f) {
                            SoundEngine.playLevelWin()
                            spawnBurst(level.exitX, level.exitY, NeonGreen, 35)
                            isVictory = true
                        }
                    }

                    // Check Radar Scanners Collision
                    for (scanner in level.scanners) {
                        val dist = hypot(playerX - scanner.x, playerY - scanner.y)
                        if (dist < scanner.radius) {
                            val currentDir = (scanner.startAngleDeg + elapsedTime * scanner.speedDegPerSec) % 360f
                            val normDir = if (currentDir < 0f) currentDir + 360f else currentDir
                            var angleToPlayer = Math.toDegrees(
                                atan2((playerY - scanner.y).toDouble(), (playerX - scanner.x).toDouble())
                            ).toFloat()
                            if (angleToPlayer < 0f) angleToPlayer += 360f

                            var diff = abs(angleToPlayer - normDir)
                            if (diff > 180f) diff = 360f - diff
                            if (diff <= scanner.sweepAngleDeg / 2f) {
                                triggerFail("SPOTTED BY SECURITY RADAR!")
                                break
                            }
                        }
                    }

                    // Check Laser Beams Collision
                    for (laser in level.lasers) {
                        val isOn = if (laser.cyclePeriodSec <= 0f) true else {
                            (elapsedTime % laser.cyclePeriodSec) < (laser.cyclePeriodSec * laser.onRatio)
                        }
                        if (isOn) {
                            val d = pointToSegmentDist(playerX, playerY, laser.x1, laser.y1, laser.x2, laser.y2)
                            if (d < 0.024f) {
                                triggerFail("INTERCEPTED BY LASER GRID!")
                                break
                            }
                        }
                    }
                } else {
                    // CORE BREACH MODE: Update flying pulses
                    val pulseIter = activePulses.iterator()
                    while (pulseIter.hasNext()) {
                        val pulse = pulseIter.next()
                        val prevY = pulse.y
                        pulse.y -= pulse.speed * dt

                        var hitRing = false
                        for (ring in level.rings) {
                            // Check if pulse crossed ring radius
                            if (prevY >= ring.radiusRatio && pulse.y <= ring.radiusRatio) {
                                val angle = ((ring.initialAngleDeg + elapsedTime * ring.speedDegPerSec) % 360f + 360f) % 360f
                                // Emitter is at bottom (90 degrees in Canvas coordinates)
                                var diff = abs(90f - angle)
                                if (diff > 180f) diff = 360f - diff
                                if (diff > ring.gapAngleDeg / 2f) {
                                    hitRing = true
                                    break
                                }
                            }
                        }

                        if (hitRing) {
                            pulseIter.remove()
                            triggerFail("FIREWALL SHIELD BLOCKED PULSE!")
                            break
                        } else if (pulse.y <= 0.06f) {
                            pulseIter.remove()
                            pulsesLanded++
                            SoundEngine.playPickup()
                            spawnBurst(0.5f, 0.5f, NeonCyan, 20)
                            if (pulsesLanded >= level.pulsesRequired) {
                                SoundEngine.playLevelWin()
                                spawnBurst(0.5f, 0.5f, NeonGreen, 40)
                                isVictory = true
                            }
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelDark)
                .border(1.dp, NeonCyan.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = level.codeName,
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (level.mode == GameMode.STEALTH_GRID)
                        "CORES: ${collectedCoreIds.size}/${level.cores.size}"
                    else
                        "INJECTED: $pulsesLanded/${level.pulsesRequired}",
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Countdown Timer
            Text(
                text = String.format("TRACE: %.1fs", timeRemaining),
                color = if (timeRemaining < 8f) NeonRed else NeonAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )

            Text(
                text = "EXIT",
                color = TextLight,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .clickable { onExitToMenu() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // GAME CANVAS AREA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(level.mode, isGameOver, isVictory) {
                    if (level.mode == GameMode.STEALTH_GRID) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                joyCenter = offset
                                joyKnob = offset
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                joyKnob = change.position
                            },
                            onDragEnd = {
                                joyCenter = null
                                joyKnob = null
                            },
                            onDragCancel = {
                                joyCenter = null
                                joyKnob = null
                            }
                        )
                    } else {
                        detectTapGestures(
                            onTap = {
                                if (!isGameOver && !isVictory && activePulses.size < 2) {
                                    SoundEngine.playShoot()
                                    activePulses.add(ActivePulse(y = 0.44f))
                                }
                            }
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val shakeX = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 24f else 0f
                val shakeY = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 24f else 0f

                // Background Tactical Grid
                val step = 55f
                var gx = shakeX
                while (gx < w) {
                    drawLine(Color(0xFF0B1620), Offset(gx, 0f), Offset(gx, h), 1f)
                    gx += step
                }
                var gy = shakeY
                while (gy < h) {
                    drawLine(Color(0xFF0B1620), Offset(0f, gy), Offset(w, gy), 1f)
                    gy += step
                }

                if (level.mode == GameMode.STEALTH_GRID) {
                    // 1. Draw Exit Extraction Portal
                    val exitPos = Offset(level.exitX * w + shakeX, level.exitY * h + shakeY)
                    val allCollected = collectedCoreIds.size == level.cores.size
                    val exitColor = if (allCollected) NeonGreen else Color(0xFF28424F)
                    drawCircle(exitColor.copy(alpha = 0.22f), radius = 46f, center = exitPos)
                    drawCircle(exitColor, radius = 32f, center = exitPos, style = Stroke(4f))

                    // 2. Draw Rotating Radar Cones
                    for (scanner in level.scanners) {
                        val sCenter = Offset(scanner.x * w + shakeX, scanner.y * h + shakeY)
                        val rPx = scanner.radius * w
                        val currentDir = scanner.startAngleDeg + elapsedTime * scanner.speedDegPerSec
                        drawArc(
                            color = NeonRed.copy(alpha = 0.26f),
                            startAngle = currentDir - scanner.sweepAngleDeg / 2f,
                            sweepAngle = scanner.sweepAngleDeg,
                            useCenter = true,
                            topLeft = Offset(sCenter.x - rPx, sCenter.y - rPx),
                            size = Size(rPx * 2f, rPx * 2f)
                        )
                        drawCircle(NeonRed, radius = 10f, center = sCenter)
                    }

                    // 3. Draw Pulsing Laser Barriers
                    for (laser in level.lasers) {
                        val isOn = if (laser.cyclePeriodSec <= 0f) true else {
                            (elapsedTime % laser.cyclePeriodSec) < (laser.cyclePeriodSec * laser.onRatio)
                        }
                        val p1 = Offset(laser.x1 * w + shakeX, laser.y1 * h + shakeY)
                        val p2 = Offset(laser.x2 * w + shakeX, laser.y2 * h + shakeY)
                        drawCircle(NeonAmber, radius = 7f, center = p1)
                        drawCircle(NeonAmber, radius = 7f, center = p2)
                        if (isOn) {
                            drawLine(NeonRed.copy(alpha = 0.35f), p1, p2, strokeWidth = 16f, cap = StrokeCap.Round)
                            drawLine(NeonRed, p1, p2, strokeWidth = 5f, cap = StrokeCap.Round)
                        } else {
                            drawLine(Color(0xFF243440), p1, p2, strokeWidth = 2f)
                        }
                    }

                    // 4. Draw Collectible Cyan Data Cores
                    level.cores.forEach { core ->
                        if (!collectedCoreIds.contains(core.id)) {
                            val cPos = Offset(core.x * w + shakeX, core.y * h + shakeY)
                            rotate(degrees = elapsedTime * 110f, pivot = cPos) {
                                drawRect(
                                    color = NeonCyan,
                                    topLeft = Offset(cPos.x - 14f, cPos.y - 14f),
                                    size = Size(28f, 28f)
                                )
                            }
                            drawCircle(NeonCyan.copy(alpha = 0.25f), radius = 26f, center = cPos)
                        }
                    }

                    // 5. Draw Player Cyber-Orb
                    if (!isGameOver) {
                        val pPos = Offset(playerX * w + shakeX, playerY * h + shakeY)
                        drawCircle(NeonGreen.copy(alpha = 0.28f), radius = 28f, center = pPos)
                        drawCircle(NeonGreen, radius = 15f, center = pPos)
                        drawCircle(Color.White, radius = 6f, center = pPos)
                    }

                    // 6. Draw Virtual Touch Joystick
                    val jc = joyCenter
                    val jk = joyKnob
                    if (jc != null && jk != null) {
                        drawCircle(NeonCyan.copy(alpha = 0.2f), radius = 75f, center = jc, style = Stroke(3f))
                        drawCircle(NeonCyan.copy(alpha = 0.55f), radius = 28f, center = jk)
                    }
                } else {
                    // CORE BREACH MODE RENDERING
                    val center = Offset(w / 2f + shakeX, h * 0.44f + shakeY)
                    val baseDim = min(w, h)

                    // Center Vault Core
                    drawCircle(
                        brush = Brush.radialGradient(listOf(NeonCyan, Color.Transparent), center, 75f),
                        radius = 75f,
                        center = center
                    )
                    drawCircle(NeonCyan, radius = 34f, center = center)

                    // Rotating Firewall Rings with Gaps
                    for (ring in level.rings) {
                        val rPx = ring.radiusRatio * baseDim
                        val gapCenter = ring.initialAngleDeg + elapsedTime * ring.speedDegPerSec
                        val arcStart = gapCenter + ring.gapAngleDeg / 2f
                        val arcSweep = 360f - ring.gapAngleDeg
                        drawArc(
                            color = NeonRed,
                            startAngle = arcStart,
                            sweepAngle = arcSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - rPx, center.y - rPx),
                            size = Size(rPx * 2f, rPx * 2f),
                            style = Stroke(width = 12f, cap = StrokeCap.Round)
                        )
                    }

                    // Bottom Pulse Cannon
                    val cannonPos = Offset(center.x, center.y + 0.44f * baseDim)
                    val triangle = Path().apply {
                        moveTo(cannonPos.x, cannonPos.y - 24f)
                        lineTo(cannonPos.x - 20f, cannonPos.y + 16f)
                        lineTo(cannonPos.x + 20f, cannonPos.y + 16f)
                        close()
                    }
                    drawPath(triangle, NeonGreen)

                    // Flying Data Pulses
                    for (pulse in activePulses) {
                        val py = center.y + pulse.y * baseDim
                        drawCircle(NeonGreen.copy(alpha = 0.35f), radius = 18f, center = Offset(center.x, py))
                        drawCircle(NeonGreen, radius = 9f, center = Offset(center.x, py))
                    }
                }

                // Render Particles
                for (p in particles) {
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                        radius = p.size,
                        center = Offset(p.x * w, p.y * h)
                    )
                }
            }

            // Bottom Control Hint Banner
            if (!isGameOver && !isVictory) {
                Text(
                    text = if (level.mode == GameMode.STEALTH_GRID)
                        "DRAG ANYWHERE TO STEER • COLLECT ALL CYAN CORES • REACH GREEN PORTAL"
                    else
                        "TAP SCREEN TO FIRE DATA PULSE THROUGH ROTATING FIREWALL GAPS",
                    color = TextLight.copy(alpha = 0.75f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }

            // GAME OVER / VICTORY OVERLAY MODAL
            if (isGameOver || isVictory) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .background(PanelDark, RoundedCornerShape(10.dp))
                            .border(2.dp, if (isVictory) NeonGreen else NeonRed, RoundedCornerShape(10.dp))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isVictory) "SYSTEM BREACHED!" else "CONNECTION TERMINATED",
                            color = if (isVictory) NeonGreen else NeonRed,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (isVictory)
                                String.format("EXTRACTION COMPLETE • %.1fs REMAINING", timeRemaining)
                            else
                                failReason,
                            color = TextLight,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                SoundEngine.playClick()
                                if (isVictory) onLevelComplete() else resetLevel()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVictory) NeonGreen else NeonAmber
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (isVictory) "NEXT SECTOR >>" else "RETRY BREACH",
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pointToSegmentDist(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val l2 = (x2 - x1).pow(2) + (y2 - y1).pow(2)
    if (l2 == 0f) return hypot(px - x1, py - y1)
    val t = (((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2).coerceIn(0f, 1f)
    val projX = x1 + t * (x2 - x1)
    val projY = y1 + t * (y2 - y1)
    return hypot(px - projX, py - projY)
}
