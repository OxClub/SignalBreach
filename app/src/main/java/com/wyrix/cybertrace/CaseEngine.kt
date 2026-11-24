package com.wyrix.cybertrace

import android.util.Base64

data class CaseEmail(
    val id: String,
    val sender: String,
    val subject: String,
    val timestamp: String,
    val body: String
)

data class ForensicCase(
    val caseNumber: Int,
    val codeName: String,
    val title: String,
    val briefing: String,
    val objective: String,
    val emails: List<CaseEmail>,
    val files: Map<String, String>,
    val ipDatabase: Map<String, String>,
    val targetAnswer: String,
    val successMessage: String
)

object CaseRepository {
    val cases = listOf(
        ForensicCase(
            caseNumber = 1,
            codeName = "CASE-01",
            title = "THE SILENT BREACH",
            briefing = "A regional fintech gateway reported unauthorized database exports at 02:14 UTC. No firewall alerts were triggered.",
            objective = "Inspect server logs, identify the attacker's origin IP used for SQL injection, and run: submit <IP_ADDRESS>",
            emails = listOf(
                CaseEmail(
                    id = "M-101",
                    sender = "director@aegis-fin.sec",
                    subject = "URGENT: Nightly Ledger Anomaly",
                    timestamp = "03:05 UTC",
                    body = """
                        Investigator,
                        
                        Our primary vault API responded with 200 OK to an abnormal payload last night. We dumped the gateway access logs and a suspicious auth token into your workstation.
                        
                        Find the exact external IP address that executed the UNION SELECT exploit and submit it immediately.
                        
                        - Director Vance
                    """.trimIndent()
                ),
                CaseEmail(
                    id = "M-102",
                    sender = "sysadmin@aegis-fin.sec",
                    subject = "Hint: Encoded Note",
                    timestamp = "03:12 UTC",
                    body = """
                        Check 'access.log' and 'token.b64' in your workspace using 'ls' and 'cat'.
                        Use 'decode <base64_string>' if you find obfuscated strings, and 'whois <ip>' to verify ownership.
                    """.trimIndent()
                )
            ),
            files = mapOf(
                "access.log" to """
                    10.0.0.14 - - [24/Nov/2026:02:10:11] "GET /api/v1/status HTTP/1.1" 200 128
                    192.168.1.45 - - [24/Nov/2026:02:11:04] "POST /api/v1/login HTTP/1.1" 401 64
                    185.220.101.42 - - [24/Nov/2026:02:13:58] "GET /api/v1/user?id=1%20UNION%20SELECT%20key,secret%20FROM%20vault-- HTTP/1.1" 200 4096
                    10.0.0.14 - - [24/Nov/2026:02:15:00] "GET /api/v1/health HTTP/1.1" 200 64
                """.trimIndent(),
                "token.b64" to "T1JJR0lOX05PREU6IDE4NS4yMjAuMTAxLjQyIHwgUFJPWFlfQllQQVNTX0FDVElWRQ==",
                "readme.txt" to "WORKSTATION READY. Type 'help' to view available forensics commands."
            ),
            ipDatabase = mapOf(
                "10.0.0.14" to "INTERNAL HEALTH MONITOR (SAFE - LOCAL SUBNET)",
                "192.168.1.45" to "INTERNAL ADMIN TERMINAL (FAILED LOGIN - BENIGN)",
                "185.220.101.42" to "HOST: relay-exit-09.net | ASN: AS9009 | FLAG: SUSPICIOUS EXFILTRATION NODE"
            ),
            targetAnswer = "185.220.101.42",
            successMessage = "EXCELLENT WORK. 185.220.101.42 isolated and blacklisted. Vault breach contained."
        ),
        ForensicCase(
            caseNumber = 2,
            codeName = "CASE-02",
            title = "SHADOW PAYLOAD",
            briefing = "An internal whistleblower leaked a hidden command-and-control codename inside a corrupted config dump.",
            objective = "Analyze 'netdump.txt' and 'cipher.txt' to extract the rogue operation codename, then run: submit <CODENAME>",
            emails = listOf(
                CaseEmail(
                    id = "M-201",
                    sender = "unknown@encrypted.relay",
                    subject = "They are watching the port",
                    timestamp = "04:40 UTC",
                    body = """
                        If you are reading this, the breach wasn't external. Someone inside deployed a beacon under an operation codename.
                        
                        I hid the Base64 key inside 'cipher.txt'. Decode it and submit the codename before they wipe the drive.
                    """.trimIndent()
                )
            ),
            files = mapOf(
                "netdump.txt" to """
                    [TCP] 10.4.2.9:443 -> 172.16.0.8:51922 SYN_ACK
                    [WARN] Outbound beacon detected on PID 4412 (/usr/bin/sys-syncd)
                    [PAYLOAD_REF] See cipher.txt for encrypted header
                """.trimIndent(),
                "cipher.txt" to "T1BFUkFUSU9OX0VDT0lQU0U=",
                "notes.log" to "Note: Operation codenames always start with OPERATION_"
            ),
            ipDatabase = mapOf(
                "10.4.2.9" to "INTERNAL CORE ROUTER",
                "172.16.0.8" to "ISOLATED SANDBOX NODE"
            ),
            targetAnswer = "OPERATION_ECOIPSE",
            successMessage = "CASE-02 SOLVED. Rogue beacon 'OPERATION_ECOIPSE' neutralized. Clearance Level Upgraded!"
        )
    )
}

class TerminalProcessor {
    fun execute(
        rawCommand: String,
        currentCase: ForensicCase,
        onCaseSolved: () -> Unit
    ): String {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return ""

        val parts = trimmed.split("\\s+".toRegex(), limit = 2)
        val cmd = parts[0].lowercase()
        val arg = if (parts.size > 1) parts[1].trim() else ""

        return when (cmd) {
            "help" -> """
                AVAILABLE FORENSICS COMMANDS:
                  ls                  - List files in active case workspace
                  cat <filename>      - Read contents of a file
                  grep <word> <file>  - Search keyword inside a file
                  decode <base64>     - Decode Base64 encoded string
                  whois <ip>          - Query threat intelligence for IP
                  objective           - Show current case mission goal
                  submit <answer>     - Submit final evidence to close case
                  clear               - Clear terminal output
            """.trimIndent()

            "ls" -> currentCase.files.keys.joinToString("    ")

            "cat" -> {
                if (arg.isEmpty()) "Usage: cat <filename>"
                else currentCase.files[arg] ?: "cat: $arg: No such file in workspace"
            }

            "grep" -> {
                val grepParts = arg.split("\\s+".toRegex(), limit = 2)
                if (grepParts.size < 2) {
                    "Usage: grep <keyword> <filename>"
                } else {
                    val keyword = grepParts[0]
                    val fileName = grepParts[1]
                    val content = currentCase.files[fileName]
                    if (content == null) {
                        "grep: $fileName: File not found"
                    } else {
                        val matches = content.lines().filter { it.contains(keyword, ignoreCase = true) }
                        if (matches.isEmpty()) "(no matches found)" else matches.joinToString("\n")
                    }
                }
            }

            "decode" -> {
                if (arg.isEmpty()) {
                    "Usage: decode <base64_string>"
                } else {
                    try {
                        val decodedBytes = Base64.decode(arg, Base64.DEFAULT)
                        "DECODED OUTPUT:\n${String(decodedBytes, Charsets.UTF_8)}"
                    } catch (e: Exception) {
                        "ERROR: Invalid Base64 input string."
                    }
                }
            }

            "whois" -> {
                if (arg.isEmpty()) "Usage: whois <ip_address>"
                else currentCase.ipDatabase[arg] ?: "WHOIS: No public threat record for $arg"
            }

            "objective" -> "[${currentCase.codeName}] ${currentCase.objective}"

            "submit" -> {
                if (arg.isEmpty()) {
                    "Usage: submit <evidence>"
                } else if (arg.equals(currentCase.targetAnswer, ignoreCase = true)) {
                    onCaseSolved()
                    "*** VERIFICATION SUCCESSFUL ***\n${currentCase.successMessage}"
                } else {
                    "ACCESS DENIED: '$arg' does not match forensic verification hash. Re-check logs."
                }
            }

            else -> "bash: $cmd: command not found. Type 'help' for tools."
        }
    }
}
