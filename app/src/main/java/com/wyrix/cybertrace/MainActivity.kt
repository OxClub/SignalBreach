package com.wyrix.cybertrace

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CyberBg = Color(0xFF060A0D)
val CyberPanel = Color(0xFF0D141A)
val CyberBorder = Color(0xFF1C303B)
val CyberNeonGreen = Color(0xFF00FF9C)
val CyberCyan = Color(0xFF00D4FF)
val CyberAmber = Color(0xFFFFB800)
val CyberRed = Color(0xFFFF4D4D)
val CyberText = Color(0xFFD6E4EC)
val CyberMuted = Color(0xFF6B8594)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignalBreachApp()
        }
    }
}

@Composable
fun SignalBreachApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("signal_breach_save", Context.MODE_PRIVATE) }

    var currentCaseIndex by remember {
        mutableIntStateOf(prefs.getInt("unlocked_case_index", 0).coerceIn(0, CaseRepository.cases.lastIndex))
    }
    var solvedCases by remember {
        mutableStateOf(prefs.getStringSet("solved_cases", emptySet()) ?: emptySet())
    }
    var selectedTab by remember { mutableIntStateOf(1) } // 0=MAIL, 1=TERMINAL, 2=NET-MAP, 3=INTEL
    var muted by remember { mutableStateOf(false) }

    val activeCase = CaseRepository.cases[currentCaseIndex]
    val isCurrentSolved = solvedCases.contains(activeCase.codeName)

    val terminalLines = remember(currentCaseIndex) {
        mutableStateListOf(
            "WYRIX FORENSICS OS v4.5 [ENCRYPTED KERNEL]",
            "MOUNTED ${activeCase.codeName}: ${activeCase.title}",
            "Run 'ls' or tap quick commands below to inspect workspace.",
            "----------------------------------------------------"
        )
    }
    var commandInput by remember { mutableStateOf("") }
    val processor = remember { TerminalProcessor() }
    val listState = rememberLazyListState()

    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(terminalLines.lastIndex)
        }
    }

    fun runCommand(cmdText: String) {
        val clean = cmdText.trim()
        if (clean.isEmpty()) return
        SoundEngine.playKeyClick()
        terminalLines.add("root@wyrix:~# $clean")
        if (clean.equals("clear", ignoreCase = true)) {
            terminalLines.clear()
        } else {
            val output = processor.execute(clean, activeCase) {
                val updated = solvedCases + activeCase.codeName
                solvedCases = updated
                prefs.edit().putStringSet("solved_cases", updated).apply()
            }
            if (output.isNotEmpty()) {
                terminalLines.add(output)
            }
        }
        commandInput = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP STATUS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberPanel)
                .border(1.dp, CyberBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SIGNAL BREACH // ${activeCase.codeName}",
                    color = CyberNeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = activeCase.title,
                    color = CyberText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SFX Toggle
                Text(
                    text = if (muted) "SFX:OFF" else "SFX:ON",
                    color = if (muted) CyberMuted else CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            muted = !muted
                            SoundEngine.isMuted = muted
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )

                if (isCurrentSolved && currentCaseIndex < CaseRepository.cases.lastIndex) {
                    Button(
                        onClick = {
                            SoundEngine.playKeyClick()
                            val next = currentCaseIndex + 1
                            currentCaseIndex = next
                            prefs.edit().putInt("unlocked_case_index", next).apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "NEXT >>",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Text(
                        text = if (isCurrentSolved) "[SOLVED]" else "[ACTIVE]",
                        color = if (isCurrentSolved) CyberNeonGreen else CyberAmber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 4 NAVIGATION TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBg)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("MAIL(${activeCase.emails.size})", "TERMINAL", "NET-MAP", "INTEL")
            tabs.forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) CyberPanel else CyberBg, RoundedCornerShape(4.dp))
                        .border(1.dp, if (selected) CyberCyan else CyberBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            SoundEngine.playKeyClick()
                            if (idx == 2) SoundEngine.playSonarPing()
                            selectedTab = idx
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) CyberCyan else CyberMuted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // TAB CONTENT
        when (selectedTab) {
            0 -> {
                // MAIL CLIENT
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeCase.emails) { mail ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberPanel, RoundedCornerShape(6.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(mail.sender, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                Text(mail.timestamp, color = CyberMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mail.subject,
                                color = CyberAmber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CyberBorder)
                            Text(
                                text = mail.body,
                                color = CyberText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            1 -> {
                // TERMINAL WORKSPACE
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(Color(0xFF040709), RoundedCornerShape(6.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(terminalLines) { line ->
                        val textColor = when {
                            line.startsWith("root@wyrix") -> CyberCyan
                            line.contains("VERIFICATION SUCCESSFUL") -> CyberNeonGreen
                            line.startsWith("ACCESS DENIED") || line.startsWith("ERROR") -> CyberRed
                            else -> CyberText
                        }
                        Text(
                            text = line,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // DYNAMIC QUICK-COMMAND CHIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fileChips = activeCase.files.keys.map { "cat $it" }
                    val quickChips = listOf("help", "ls") + fileChips + listOf("decode ", "whois ", "submit ")
                    quickChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .background(CyberPanel, RoundedCornerShape(4.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .clickable {
                                    SoundEngine.playKeyClick()
                                    if (chip.endsWith(" ")) {
                                        commandInput = chip
                                    } else {
                                        runCommand(chip)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip.trim(),
                                color = CyberNeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // COMMAND INPUT BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(CyberPanel, RoundedCornerShape(6.dp))
                        .border(1.dp, CyberNeonGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ",
                        color = CyberNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    BasicTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = CyberText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(CyberNeonGreen),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { runCommand(commandInput) })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXEC",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(CyberNeonGreen, RoundedCornerShape(4.dp))
                            .clickable { runCommand(commandInput) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            2 -> {
                // ANIMATED NETWORK TRACE RADAR (NET-MAP)
                NetworkRadarView(
                    nodes = activeCase.networkNodes,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            3 -> {
                // CASE INTEL & CASE SELECTOR
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(CyberPanel, RoundedCornerShape(6.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("INCIDENT BRIEFING", color = CyberAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeCase.briefing, color = CyberText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                    item {
                        HorizontalDivider(color = CyberBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PRIMARY OBJECTIVE", color = CyberNeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeCase.objective, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                    item {
                        HorizontalDivider(color = CyberBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("CASE ARCHIVE (TAP TO SWITCH)", color = CyberMuted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    items(CaseRepository.cases.indices.toList()) { idx ->
                        val c = CaseRepository.cases[idx]
                        val done = solvedCases.contains(c.codeName)
                        val unlocked = idx == 0 || solvedCases.contains(CaseRepository.cases[idx - 1].codeName) || done
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx == currentCaseIndex) Color(0xFF14222B) else Color.Transparent, RoundedCornerShape(4.dp))
                                .border(1.dp, if (idx == currentCaseIndex) CyberCyan else CyberBorder, RoundedCornerShape(4.dp))
                                .clickable(enabled = unlocked) {
                                    SoundEngine.playKeyClick()
                                    currentCaseIndex = idx
                                    selectedTab = 1
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (done) "[✓]" else if (unlocked) "[>]" else "[🔒]"} ${c.codeName}: ${c.title}",
                                color = if (done) CyberNeonGreen else if (unlocked) CyberText else CyberMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkRadarView(nodes: List<NetNode>, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .background(Color(0xFF040709), RoundedCornerShape(6.dp))
            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "LIVE PACKET TOPOLOGY // THREAT RADAR",
            color = CyberCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            text = "RED NODES INDICATE ANOMALOUS ROUTING SIGNATURES",
            color = CyberMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle Grid Lines
            val step = 60f
            var x = 0f
            while (x < w) {
                drawLine(Color(0xFF0E1A22), Offset(x, 0f), Offset(x, h), 1f)
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(Color(0xFF0E1A22), Offset(0f, y), Offset(w, y), 1f)
                y += step
            }

            // Connect Nodes with Dashed Lines
            for (i in 0 until nodes.size - 1) {
                val p1 = Offset(nodes[i].xRatio * w, nodes[i].yRatio * h)
                val p2 = Offset(nodes[i + 1].xRatio * w, nodes[i + 1].yRatio * h)
                drawLine(
                    color = if (nodes[i + 1].isThreat) CyberRed.copy(alpha = 0.6f) else CyberCyan.copy(alpha = 0.5f),
                    start = p1,
                    end = p2,
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // Draw Nodes
            nodes.forEach { node ->
                val center = Offset(node.xRatio * w, node.yRatio * h)
                val nodeColor = if (node.isThreat) CyberRed else CyberNeonGreen

                if (node.isThreat) {
                    drawCircle(
                        color = CyberRed.copy(alpha = (1f - pulseRadius / 42f).coerceIn(0f, 0.7f)),
                        radius = pulseRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }

                drawCircle(color = CyberBg, radius = 14f, center = center)
                drawCircle(color = nodeColor, radius = 10f, center = center)

                val labelLayout = textMeasurer.measure(
                    text = "${node.label}\n${node.ip}",
                    style = TextStyle(
                        color = nodeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(
                        (center.x - labelLayout.size.width / 2f).coerceIn(8f, w - labelLayout.size.width - 8f),
                        (center.y + 18f).coerceAtMost(h - labelLayout.size.height - 8f)
                    )
                )
            }
        }
    }
}
