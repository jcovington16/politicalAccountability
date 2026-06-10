package com.publicrecord.common.privacy

enum class PrivacyRiskLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

enum class PrivacyFindingType {
    EMAIL,
    PHONE,
    SSN,
    CREDIT_CARD,
    PRIVATE_ADDRESS,
    PRIVATE_SOURCE_CONTEXT
}

data class PrivacyFinding(
    val type: PrivacyFindingType,
    val risk: PrivacyRiskLevel,
    val message: String
)

data class PrivacySafetyResult(
    val safeForPublicDisplay: Boolean,
    val risk: PrivacyRiskLevel,
    val redactedText: String,
    val findings: List<PrivacyFinding>
) {
    val warnings: List<String> = findings.map { it.message }
}

object PrivacySafetyService {
    private val emailRegex = Regex("""\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("""(?<!\d)(?:\+?1[\s.-]?)?(?:\(?\d{3}\)?[\s.-]?)\d{3}[\s.-]?\d{4}(?!\d)""")
    private val ssnRegex = Regex("""(?<!\d)\d{3}-\d{2}-\d{4}(?!\d)""")
    private val creditCardRegex = Regex("""(?<!\d)(?:\d[ -]*?){13,16}(?!\d)""")
    private val privateAddressRegex = Regex(
        """(?i)\b(home address|private address|residential address|personal address)\b|(?<!\d)\d{1,6}\s+[A-Za-z0-9.'-]+(?:\s+[A-Za-z0-9.'-]+){0,5}\s+(?:street|st|avenue|ave|road|rd|drive|dr|lane|ln|court|ct|way|boulevard|blvd)\b"""
    )
    private val privateSourceRegex = Regex("""(?i)\b(leaked|private dm|private message|family member address)\b""")

    fun evaluate(text: String?): PrivacySafetyResult {
        if (text.isNullOrBlank()) {
            return PrivacySafetyResult(true, PrivacyRiskLevel.NONE, text.orEmpty(), emptyList())
        }

        val findings = mutableListOf<PrivacyFinding>()
        var redacted: String = text

        fun redact(regex: Regex, replacement: String, type: PrivacyFindingType, risk: PrivacyRiskLevel, message: String) {
            if (regex.containsMatchIn(redacted)) {
                findings += PrivacyFinding(type, risk, message)
                redacted = regex.replace(redacted, replacement)
            }
        }

        redact(ssnRegex, "[redacted ssn]", PrivacyFindingType.SSN, PrivacyRiskLevel.HIGH, "Possible SSN detected; record requires privacy review.")
        redact(creditCardRegex, "[redacted number]", PrivacyFindingType.CREDIT_CARD, PrivacyRiskLevel.HIGH, "Possible payment card number detected; record requires privacy review.")
        redact(privateAddressRegex, "[redacted private address]", PrivacyFindingType.PRIVATE_ADDRESS, PrivacyRiskLevel.HIGH, "Possible private residential address detected; record requires privacy review.")
        redact(emailRegex, "[redacted email]", PrivacyFindingType.EMAIL, PrivacyRiskLevel.MEDIUM, "Email address detected; display only if it is an official public contact.")
        redact(phoneRegex, "[redacted phone]", PrivacyFindingType.PHONE, PrivacyRiskLevel.MEDIUM, "Phone number detected; display only if it is an official public contact.")

        if (privateSourceRegex.containsMatchIn(text)) {
            findings += PrivacyFinding(
                PrivacyFindingType.PRIVATE_SOURCE_CONTEXT,
                PrivacyRiskLevel.HIGH,
                "Private, leaked, or personal-source context detected; do not publish without legal/public-record review."
            )
        }

        val risk = findings.maxOfOrNull { it.risk } ?: PrivacyRiskLevel.NONE
        return PrivacySafetyResult(
            safeForPublicDisplay = risk < PrivacyRiskLevel.HIGH,
            risk = risk,
            redactedText = redacted,
            findings = findings
        )
    }
}
