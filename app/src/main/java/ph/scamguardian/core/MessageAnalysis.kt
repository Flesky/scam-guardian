package ph.scamguardian.core

/** Everything the rules and the prefilter need to know about one message. */
data class MessageAnalysis(
    /** The cleaned text with the links taken out. */
    val body: String,
    val tokens: List<String>,
    val links: List<Link>,
    val brands: List<BrandMatch>,
    val money: List<String>,
    val general: List<KeywordMatch>,
    /** Words the rules look for, such as "passcode" or "pa-gcash". */
    val ruleWords: List<KeywordMatch>,
)

/** Runs the detectors over a message prepared with [Sanitizer.prepare]. */
class MessageAnalyzer(
    data: PipelineData,
    private val normalizer: Normalizer,
    private val matcher: KeywordMatcher,
    private val linkAnalyzer: LinkAnalyzer,
    confusables: Confusables,
) {
    private val keywords = data.keywords
    private val brandDetector = BrandDetector(data.brands, normalizer, confusables)

    fun analyze(prepared: String): MessageAnalysis {
        // Links are read before the text is cleaned; words are read from the text around the links.
        val body = Sanitizer.clean(linkAnalyzer.replaceLinks(prepared, " "))
        // A host that a catalog brand owns is never an imitation of another brand.
        val unownedHosts = linkAnalyzer.analyze(prepared).filter { it.owners.isEmpty() }.map { it.unicodeHost }
        val brands = brandDetector.detect(body, unownedHosts)
        val tokens = normalizer.tokens(body)
        return MessageAnalysis(
            body = body,
            tokens = tokens,
            links = linkAnalyzer.analyze(prepared, brands.filter { it.inText }.map { it.brand }),
            brands = brands,
            money = MoneyDetector.find(body),
            general = matcher.match(tokens, keywords.general),
            ruleWords = matcher.match(tokens, Rules.SIGNAL_WORDS),
        )
    }
}

/** Cheap gate before the AI check: enough words, and something worth checking. The rules run without it. */
class Prefilter(
    private val minTokens: Int = DEFAULT_MIN_TOKENS,
) {
    fun passes(analysis: MessageAnalysis): Boolean =
        analysis.tokens.size >= minTokens &&
            (
                analysis.links.isNotEmpty() ||
                    analysis.money.isNotEmpty() ||
                    analysis.general.isNotEmpty() ||
                    analysis.ruleWords.isNotEmpty()
            )

    companion object {
        const val DEFAULT_MIN_TOKENS = 2
    }
}
