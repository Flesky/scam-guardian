package ph.scamguardian.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** [key] is the name of the type in warnings.json and in the saved history. */
@Serializable
enum class WarningType(
    val key: String,
) {
    @SerialName("fake_link")
    FAKE_LINK("fake_link"),

    @SerialName("otp_request")
    OTP_REQUEST("otp_request"),

    @SerialName("risky_link")
    RISKY_LINK("risky_link"),

    @SerialName("money_request")
    MONEY_REQUEST("money_request"),

    @SerialName("ai_scam")
    AI_SCAM("ai_scam"),
}

data class RuleMatch(
    val type: WarningType,
    val evidence: String,
    val brand: String? = null,
)

/** The fixed rules, checked in order; the first match wins. */
class Rules(
    private val normalizer: Normalizer,
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
        val match =
            analysis.brands.firstOrNull { link.unicodeHost in it.hosts }
                ?: analysis.brands.firstOrNull { it.inText }
                ?: analysis.brands.first()
        val source = if (match.inText) "${match.brand.name} mentioned" else "${match.brand.name} name used in the link"
        return RuleMatch(WarningType.FAKE_LINK, "$source; link goes to ${link.registrableDomain}", match.brand.name)
    }

    private fun otpRequest(analysis: MessageAnalysis): RuleMatch? {
        // Sentences that deliver a code, or warn not to share one, do not count as asking for it.
        val asking = sentenceBreak.split(analysis.body).filterNot(::givesOrProtectsCode)
        val tokens = normalizer.tokens(asking.joinToString(" "))
        val codes = matcher.match(tokens, CODE_WORDS)
        val asks = matcher.match(tokens, ASK_WORDS)
        if (codes.isEmpty() || asks.isEmpty()) return null
        return RuleMatch(
            WarningType.OTP_REQUEST,
            "Mentions a code (${words(codes)}); asks you to give it (${words(asks)})",
        )
    }

    private fun givesOrProtectsCode(sentence: String): Boolean {
        val lower = sentence.lowercase()
        return codeDelivery.containsMatchIn(lower) || codeWarning.containsMatchIn(lower)
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
        // Returning somewhere or lending an object is not a promise to repay money.
        val monetary = analysis.money.isNotEmpty() || matcher.match(analysis.tokens, MONEY_CONTEXT_WORDS).isNotEmpty()
        val repayment = if (monetary) matcher.match(analysis.tokens, REPAYMENT_WORDS) else emptyList()
        if (asks.isEmpty() || (urgency.isEmpty() && repayment.isEmpty())) return null
        val pressure =
            listOfNotNull(
                "says it is urgent (${words(urgency)})".takeIf { urgency.isNotEmpty() },
                "promises to pay it back (${words(repayment)})".takeIf { repayment.isNotEmpty() },
            ).joinToString(" and ")
        return RuleMatch(WarningType.MONEY_REQUEST, "Asks for money (${words(asks)}) and $pressure")
    }

    // One word can match two keywords ("ospital", "hospital"); name it once.
    private fun words(matches: List<KeywordMatch>): String =
        matches.distinctBy { it.tokens }.joinToString(", ") { it.keyword }

    companion object {
        private const val CODE = "(?:otp|code|pin|password|passcode)"
        private val sentenceBreak = Regex("(?<=[.!?])\\s+")

        // "Your OTP is 482913", "code: 482913", "482913 is your BDO OTP".
        private val codeDelivery =
            Regex(
                "\\b$CODE\\b(?:\\s+(?:is|ay))?\\s*:?\\s*\\d{4,8}\\b|\\b\\d{4,8}\\s+is\\s+your\\b[^.]{0,30}\\b$CODE\\b",
            )

        // "Do not share this code", "Don't enter your OTP on any site", "Huwag ibigay ang code".
        private val codeWarning =
            Regex(
                "\\b(?:do not|don't|dont|never|huwag|wag)\\b[^.!?]{0,40}" +
                    "\\b(?:share|send|enter|give|forward|ibigay|ibahagi|i-share|ilagay|sabihin)\\b",
            )
        private val CODE_WORDS =
            listOf("otp", "code", "pin", "passcode", "verification code", "one time pin", "one time password")
        private val ASK_WORDS =
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
        private val MONEY_WORDS =
            listOf("padala", "padalhan", "pa-gcash", "pagcash", "pahiram", "send", "transfer", "utang")
        private val REPAYMENT_WORDS = listOf("babalik", "ibabalik", "babayaran", "bayaran")
        private val MONEY_CONTEXT_WORDS = listOf("pera", "salapi", "kwarta", "pa-gcash", "pagcash", "utang")
        private val URGENCY_WORDS =
            listOf("urgent", "agad", "ngayon na", "emergency", "ospital", "hospital", "kailangan")

        /** Rule words that also send a message to the AI check. Asking words alone are too common. */
        val SIGNAL_WORDS = CODE_WORDS + MONEY_WORDS + URGENCY_WORDS
    }
}
