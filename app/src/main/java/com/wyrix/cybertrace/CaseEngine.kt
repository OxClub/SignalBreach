package com.wyrix.cybertrace

import android.util.Base64

data class CaseEmail(
    val id: String,
    val sender: String,
    val subject: String,
    val timestamp: String,
    val body: String
)

data class NetNode(
    val label: String,
    val ip: String,
    val xRatio: Float,
    val yRatio: Float,
    val isThreat: Boolean
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
    val networkNodes: List<NetNode>,
    val targetAnswer: String,
    val successMessage: String
)

object CaseRepository {
    val cases = listOf(
        ForensicCase(
            caseNumber = 1,
            codeName = "CASE-01",
            title = "THE SILENT BREACH",
            briefing = "A fintech vault gateway suffered silent data extraction at 02:14 UTC. Firewall intrusion alerts remained completely dark.",
            objective = "Inspect 'access.log' & 'token.b64' to identify the SQLi attacker IP, verify with 'whois', then run: submit <IP>",
            emails = listOf(
                CaseEmail(
                    id = "M-101",
                    sender = "director@aegis-fin.sec",
                    subject = "URGENT: Nightly Ledger Anomaly",
                    timestamp = "03:05 UTC",
                    body = """
                        Investigator,
                        
                        Our primary vault API returned HTTP 200 to a malformed query at 02:13 UTC. No alarms fired.
                        
                        Inspect the dumped logs in your terminal, trace the external IP executing the UNION SELECT payload, and submit it immediately.
                        
                        - Director Vance
                    """.trimIndent()
                ),
                CaseEmail(
                    id = "M-102",
                    sender = "ops@aegis-fin.sec",
                    subject = "Workspace Dump Ready",
                    timestamp = "03:11 UTC",
                    body = """
                        Files mounted: 'access.log' and 'token.b64'.
                        Tip: Use 'grep UNION access.log' or 'cat token.b64' followed by 'decode <string>'.
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
                "token.b64" to "T1JJR0lOX05PREU6IDE4NS4yMjAuMTAxLjQyIHwgUFJPWFlfQllQQVNTX0FDVElWRQ=="
            ),
            ipDatabase = mapOf(
                "10.0.0.14" to "INTERNAL HEALTH MONITOR (SAFE - LOCAL SUBNET)",
                "192.168.1.45" to "INTERNAL ADMIN TERMINAL (BENIGN)",
                "185.220.101.42" to "HOST: relay-exit-09.net | ASN: AS9009 | STATUS: CONFIRMED EXFILTRATION NODE"
            ),
            networkNodes = listOf(
                NetNode("VAULT-API", "10.0.0.1", 0.50f, 0.22f, false),
                NetNode("AUTH-GW", "10.0.0.14", 0.22f, 0.52f, false),
                NetNode("ADMIN-PC", "192.168.1.45", 0.78f, 0.52f, false),
                NetNode("EXIT-09", "185.220.101.42", 0.50f, 0.82f, true)
            ),
            targetAnswer = "185.220.101.42",
            successMessage = "NODE 185.220.101.42 BLACKLISTED. Vault breach contained."
        ),
        ForensicCase(
            caseNumber = 2,
            codeName = "CASE-02",
            title = "SHADOW PAYLOAD",
            briefing = "An internal whistleblower leaked a rogue C2 beacon codename hidden inside a corrupted daemon dump.",
            objective = "Inspect 'netdump.txt' and decode 'cipher.txt' to recover the operation codename, then run: submit <CODENAME>",
            emails = listOf(
                CaseEmail(
                    id = "M-201",
                    sender = "anon@encrypted.relay",
                    subject = "The call came from inside",
                    timestamp = "04:40 UTC",
                    body = """
                        They think it was an outside hacker. It wasn't. Someone inside planted a beacon under an operation codename.
                        
                        I extracted the Base64 header into 'cipher.txt'. Decode it before they purge the logs.
                    """.trimIndent()
                )
            ),
            files = mapOf(
                "netdump.txt" to """
                    [TCP] 10.4.2.9:443 -> 172.16.0.8:51922 SYN_ACK
                    [WARN] Outbound beacon detected on PID 4412 (/usr/bin/sys-syncd)
                    [PAYLOAD_REF] Header stored in cipher.txt
                """.trimIndent(),
                "cipher.txt" to "T1BFUkFUSU9OX0VDT0lQU0U="
            ),
            ipDatabase = mapOf(
                "10.4.2.9" to "CORE INTERNAL ROUTER",
                "172.16.0.8" to "ROGUE SANDBOX BEACON ENDPOINT"
            ),
            networkNodes = listOf(
                NetNode("CORE-RTR", "10.4.2.9", 0.25f, 0.30f, false),
                NetNode("SYS-SYNCD", "PID:4412", 0.55f, 0.55f, true),
                NetNode("C2-DROP", "172.16.0.8", 0.78f, 0.80f, true)
            ),
            targetAnswer = "OPERATION_ECOIPSE",
            successMessage = "ROGUE BEACON 'OPERATION_ECOIPSE' TERMINATED. Insider trail unlocked."
        ),
        ForensicCase(
            caseNumber = 3,
            codeName = "CASE-03",
            title = "PHANTOM SSL",
            briefing = "Encrypted traffic between two diplomatic relays was intercepted using a cloned TLS certificate fingerprint.",
            objective = "Analyze 'tls_audit.log' and 'cert_note.b64' to find the forged serial key, then run: submit <SERIAL>",
            emails = listOf(
                CaseEmail(
                    id = "M-301",
                    sender = "cert-authority@netwatch.org",
                    subject = "MITM Certificate Alert",
                    timestamp = "07:19 UTC",
                    body = """
                        A rogue proxy intercepted traffic using a spoofed root certificate.
                        Find the exact 8-character HEX serial number of the forged certificate in your workspace files.
                    """.trimIndent()
                )
            ),
            files = mapOf(
                "tls_audit.log" to """
                    [OK] CERT_SERIAL=4A91B002 ISSUER=GlobalTrust_Root_G2
                    [OK] CERT_SERIAL=7F12C889 ISSUER=GlobalTrust_Root_G2
                    [ALERT] CERT_SERIAL=DE94F71C ISSUER=Unknown_SelfSigned_CA (MITM_DETECTED)
                """.trimIndent(),
                "cert_note.b64" to "Rk9SR0VEX1NFUklBTDogREU5NEY3MUM="
            ),
            ipDatabase = mapOf(
                "91.108.56.11" to "MITM INTERCEPTOR PROXY (UNVERIFIED AS)"
            ),
            networkNodes = listOf(
                NetNode("RELAY-A", "10.20.1.1", 0.20f, 0.25f, false),
                NetNode("MITM-NODE", "91.108.56.11", 0.50f, 0.50f, true),
                NetNode("RELAY-B", "10.20.1.2", 0.80f, 0.75f, false)
            ),
            targetAnswer = "DE94F71C",
            successMessage = "FORGED CERTIFICATE 'DE94F71C' REVOKED. Encrypted channel restored."
        ),
        ForensicCase(
            caseNumber = 4,
            codeName = "CASE-04",
            title = "ZERO-HOUR RANSOM",
            briefing = "Metro General Hospital's surgical scheduling cluster was locked by crypto-ransomware. A recovery key fragment was left in memory.",
            objective = "Inspect 'mem_strings.txt' and 'ransom_note.txt' to extract the decryption master key, then run: submit <KEY>",
            emails = listOf(
                CaseEmail(
                    id = "M-401",
                    sender = "ciso@metro-health.org",
                    subject = "CRITICAL: ICU Systems Locked",
                    timestamp = "11:02 UTC",
                    body = """
                        We refuse to pay the ransom. Our incident team dumped the malware's volatile memory strings before isolation.
                        Extract the decryption key and submit it immediately to unlock the cluster.
                    """.trimIndent()
                )
            ),
            files = mapOf(
                "ransom_note.txt" to "ALL VOLUMES ENCRYPTED. KEY FORMAT: AES-XXXX-XXXX-99",
                "mem_strings.txt" to """
                    0x0041A0: Loading crypto_module.so
                    0x0041F4: Connecting to 45.33.22.11:8443
                    0x0042B8: DEBUG_KEY_DUMP=AES-7741-KRON-99
                    0x004300: Wiping local shadow copies...
                """.trimIndent()
            ),
            ipDatabase = mapOf(
                "45.33.22.11" to "RANSOMWARE PAYMENT & KEY ESCROW SERVER"
            ),
            networkNodes = listOf(
                NetNode("ICU-CLUSTER", "10.99.0.5", 0.25f, 0.70f, false),
                NetNode("INFECTED-GW", "10.99.0.1", 0.50f, 0.45f, true),
                NetNode("EXTORT-C2", "45.33.22.11", 0.80f, 0.25f, true)
            ),
            targetAnswer = "AES-7741-KRON-99",
            successMessage = "MASTER KEY ACCEPTED. All 48 hospital servers decrypted with zero data loss!"
        ),
        ForensicCase(
            caseNumber = 5,
            codeName = "CASE-05",
            title = "PROTOCOL OMEGA",
            briefing = "All four previous breaches link back to a single architect alias operating from an offshore dark-fiber hub.",
            objective = "Cross-reference 'syndicate.log' and decode 'architect.b64' to unmask the mastermind alias, then run: submit <ALIAS>",
            emails = listOf(
                CaseEmail(
                    id = "M-501",
                    sender = "director@aegis-fin.sec",
                    subject = "FINAL DIRECTIVE: Unmask The Architect",
                    timestamp = "23:59 UTC",
                    body = """
                        Investigator,
                        
                        Every IP, every forged cert, and every beacon leads to one mastermind.
                        Crack their final encrypted signature and close the file for good.
                    """.trimIndent()
                )
            ),
            files = mapOf(
                "syndicate.log" to """
                    LINK_01: 185.220.101.42 -> FUNDED_BY_ACCT_8821
                    LINK_02: OPERATION_ECOIPSE -> SIGNED_BY_ARCHITECT
                    LINK_03: DE94F71C -> ROUTED_THROUGH_OMEGA_HUB
                """.trimIndent(),
                "architect.b64" to "QVJDSElURUNUX1ZPUlRFWA=="
            ),
            ipDatabase = mapOf(
                "198.51.100.77" to "OMEGA OFFSHORE FIBER TERMINUS (HIGH PRIORITY TARGET)"
            ),
            networkNodes = listOf(
                NetNode("EXIT-09", "185.220.101.42", 0.20f, 0.25f, true),
                NetNode("MITM-NODE", "91.108.56.11", 0.80f, 0.25f, true),
                NetNode("EXTORT-C2", "45.33.22.11", 0.20f, 0.75f, true),
                NetNode("OMEGA-HUB", "198.51.100.77", 0.50f, 0.50f, true)
            ),
            targetAnswer = "ARCHITECT_VORTEX",
            successMessage = "SEASON 1 COMPLETE! 'ARCHITECT_VORTEX' exposed and arrested. You are an Elite Cyber Investigator."
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
                FORENSICS TOOLKIT v2.0:
                  ls                  - List files in active case workspace
                  cat <filename>      - Read file contents
                  grep <word> <file>  - Search keyword in file
                  decode <base64>     - Decode Base64 string
                  whois <ip>          - Query threat intelligence DB
                  objective           - Display current mission goal
                  submit <evidence>   - Verify & close current case
                  clear               - Clear terminal screen
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
                        "DECODED PAYLOAD:\n${String(decodedBytes, Charsets.UTF_8)}"
                    } catch (e: Exception) {
                        "ERROR: Invalid Base64 string."
                    }
                }
            }

            "whois" -> {
                SoundEngine.playSonarPing()
                if (arg.isEmpty()) "Usage: whois <ip_address>"
                else currentCase.ipDatabase[arg] ?: "WHOIS: No threat intelligence record for $arg"
            }

            "objective" -> "[${currentCase.codeName}] ${currentCase.objective}"

            "submit" -> {
                if (arg.isEmpty()) {
                    "Usage: submit <evidence>"
                } else if (arg.equals(currentCase.targetAnswer, ignoreCase = true)) {
                    SoundEngine.playSuccessChime()
                    onCaseSolved()
                    "*** VERIFICATION SUCCESSFUL ***\n${currentCase.successMessage}"
                } else {
                    SoundEngine.playErrorBuzz()
                    "ACCESS DENIED: '$arg' failed forensic hash check. Re-analyze evidence."
                }
            }

            else -> {
                SoundEngine.playErrorBuzz()
                "bash: $cmd: command not found. Type 'help' for tools."
            }
        }
    }
}
