package ph.scamguardian.core

enum class WarningType(
    val key: String,
) {
    FAKE_LINK("fake_link"),
    OTP_REQUEST("otp_request"),
    RISKY_LINK("risky_link"),
    MONEY_REQUEST("money_request"),
    AI_SCAM("ai_scam"),
}

data class RuleMatch(
    val type: WarningType,
    val evidence: String,
    val brand: String? = null,
)

/** The fixed rules, checked in order; the first match wins. */
class Rules(
    private val matcher: KeywordMatcher,
) {
    fun evaluate(analysis: MessageAnalysis): RuleMatch? =
        fakeLink(analysis)
            ?: otpRequest(analysis)
            ?: riskyLink(analysis)
            ?: moneyRequest(analysis)

    private fun fakeLink(analysis: MessageAnalysis): RuleMatch? {
        val link = analysis.links.firstOrNull { !it.official }
        if (link == null || analysis.brands.isEmpty()) return null
        val match = analysis.brands.firstOrNull { link.host in it.hosts } ?: analysis.brands.first()
        val source = if (match.inText) "${match.brand.name} mentioned" else "${match.brand.name} name used in the link"
        return RuleMatch(WarningType.FAKE_LINK, "$source; link goes to ${link.registrableDomain}", match.brand.name)
    }

    private fun otpRequest(analysis: MessageAnalysis): RuleMatch? {
        val codes = matcher.match(analysis.tokens, CODE_WORDS)
        val asks = matcher.match(analysis.tokens, ASK_WORDS)
        if (codes.isEmpty() || asks.isEmpty() || givesCode(analysis)) return null
        return RuleMatch(WarningType.OTP_REQUEST, "Asks you to give a code (${words(codes)}; ${words(asks)})")
    }

    // A message that delivers a code, or warns not to share one, is not asking for it.
    private fun givesCode(analysis: MessageAnalysis): Boolean {
        val text = analysis.sanitized.lowercase()
        val tokens = analysis.tokens
        val codeNextToNumber =
            tokens.indices.any { index ->
                tokens[index] in CODE_TOKENS &&
                    tokens
                        .subList(
                            maxOf(0, index - CODE_WINDOW),
                            minOf(tokens.size, index + CODE_WINDOW + 1),
                        ).any(codeNumber::matches)
            }
        return codeNextToNumber || GIVES_CODE_PHRASES.any { it in text }
    }

    private fun riskyLink(analysis: MessageAnalysis): RuleMatch? {
        val link = analysis.links.firstOrNull { it.risky && !it.official }
        if (link == null || analysis.general.isEmpty()) return null
        val reasons =
            listOfNotNull(
                "link shortener".takeIf { link.shortener },
                "risky ending .${link.tld}".takeIf { link.riskyTld },
                "numbers in the address".takeIf { link.numericHost },
            ).joinToString(", ")
        return RuleMatch(
            WarningType.RISKY_LINK,
            "Link goes to ${link.registrableDomain} ($reasons); words: ${words(analysis.general)}",
        )
    }

    private fun moneyRequest(analysis: MessageAnalysis): RuleMatch? {
        val asks = matcher.match(analysis.tokens, MONEY_WORDS)
        val urgency = matcher.match(analysis.tokens, URGENCY_WORDS)
        if (analysis.links.isNotEmpty() || asks.isEmpty() || urgency.isEmpty()) return null
        return RuleMatch(
            WarningType.MONEY_REQUEST,
            "Asks for money (${words(asks)}) and says it is urgent (${words(urgency)}); no link",
        )
    }

    // One word can match two keywords ("ospital", "hospital"); name it once.
    private fun words(matches: List<KeywordMatch>): String =
        matches.distinctBy { it.tokens }.joinToString(", ") { it.keyword }

    private companion object {
        const val CODE_WINDOW = 3
        val codeNumber = Regex("\\d{4,8}")
        val CODE_TOKENS = setOf("otp", "code", "pin")
        val CODE_WORDS =
            listOf("otp", "code", "pin", "passcode", "verification code", "one time pin", "one time password")
        val ASK_WORDS =
            listOf(
                "send",
                "isend",
                "pasend",
                "reply",
                "give",
                "ibigay",
                "pakibigay",
                "share",
                "forward",
                "provide",
                "tell",
                "sabihin",
            )
        val GIVES_CODE_PHRASES =
            listOf(
                "your otp is",
                "your one-time pin is",
                "your code is",
                "do not share",
                "don't share",
                "dont share",
                "never share",
                "huwag ibigay",
                "huwag ibahagi",
                "huwag i-share",
            )
        val MONEY_WORDS = listOf("padala", "pa-gcash", "pagcash", "pahiram", "send", "transfer", "utang")
        val URGENCY_WORDS = listOf("urgent", "agad", "ngayon na", "emergency", "ospital", "hospital", "kailangan")
    }
}
