package com.wyrix.cybertrace

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CyberBg = Color(0xFF070B0E)
val CyberPanel = Color(0xFF0E151B)
val CyberBorder = Color(0xFF1E323D)
val CyberNeonGreen = Color(0xFF00FF9C)
val CyberCyan = Color(0xFF00D4FF)
val CyberAmber = Color(0xFFFFB800)
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
    var selectedTab by remember { mutableIntStateOf(1) } // 0 = MAIL, 1 = TERMINAL, 2 = INTEL

    val activeCase = CaseRepository.cases[currentCaseIndex]
    val isCurrentSolved = solvedCases.contains(activeCase.codeName)

    val terminalLines = remember(currentCaseIndex) {
        mutableStateListOf(
            "WYRIX FORENSICS OS v4.2 [SECURE BOOT]",
            "LOADED ${activeCase.codeName}: ${activeCase.title}",
            "Type 'help' or tap quick commands below to start.",
            "------------------------------------------------"
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
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberPanel)
                .border(1.dp, CyberBorder)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SIGNAL BREACH // ${activeCase.codeName}",
                    color = CyberNeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = activeCase.title,
                    color = CyberText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            if (isCurrentSolved && currentCaseIndex < CaseRepository.cases.lastIndex) {
                Button(
                    onClick = {
                        val next = currentCaseIndex + 1
                        currentCaseIndex = next
                        prefs.edit().putInt("unlocked_case_index", next).apply()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NEXT CASE >>",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                Text(
                    text = if (isCurrentSolved) "[SOLVED]" else "[ACTIVE]",
                    color = if (isCurrentSolved) CyberNeonGreen else CyberAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // NAVIGATION TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBg)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("MAIL (${activeCase.emails.size})", "TERMINAL", "CASE INTEL").forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) CyberPanel else CyberBg, RoundedCornerShape(4.dp))
                        .border(1.dp, if (selected) CyberCyan else CyberBorder, RoundedCornerShape(4.dp))
                        .clickable { selectedTab = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) CyberCyan else CyberMuted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // MAIN CONTENT AREA
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
                                Text(mail.sender, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Text(mail.timestamp, color = CyberMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
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
                            line.startsWith("ACCESS DENIED") || line.startsWith("ERROR") -> Color(0xFFFF5252)
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

                // QUICK COMMAND CHIPS FOR MOBILE UX
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickChips = listOf("help", "ls", "cat access.log", "cat token.b64", "decode ", "whois ", "submit ")
                    quickChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .background(CyberPanel, RoundedCornerShape(4.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .clickable {
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
                // CASE INTEL & OBJECTIVE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(CyberPanel, RoundedCornerShape(6.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("INCIDENT BRIEFING", color = CyberAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(activeCase.briefing, color = CyberText, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)

                    HorizontalDivider(color = CyberBorder)

                    Text("PRIMARY OBJECTIVE", color = CyberNeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(activeCase.objective, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)

                    HorizontalDivider(color = CyberBorder)

                    Text("SAVED CASES PROGRESS", color = CyberMuted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    CaseRepository.cases.forEach { c ->
                        val done = solvedCases.contains(c.codeName)
                        Text(
                            text = "${if (done) "[✓]" else "[ ]"} ${c.codeName}: ${c.title}",
                            color = if (done) CyberNeonGreen else CyberText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
