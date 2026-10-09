package ph.scamguardian.core

/** Everything the prefilter and the rules need to know about one sanitized message. */
data class MessageAnalysis(
    val sanitized: String,
    /** The sanitized text with the links taken out. */
    val body: String,
    val tokens: List<String>,
    val links: List<Link>,
    val brands: List<BrandMatch>,
    val money: List<String>,
    val general: List<KeywordMatch>,
)

/** Runs the detectors over a sanitized message. */
class MessageAnalyzer(
    data: PipelineData,
    private val normalizer: Normalizer,
    private val matcher: KeywordMatcher,
    private val linkAnalyzer: LinkAnalyzer,
) {
    private val keywords = data.keywords
    private val brandDetector = BrandDetector(data.brands, normalizer, Confusables())

    fun analyze(sanitized: String): MessageAnalysis {
        // Words are read from the text around the links; the links are judged on their own.
        val body = linkAnalyzer.replaceLinks(sanitized, " ")
        // A host that a catalog brand owns is never an imitation of another brand.
        val unownedHosts = linkAnalyzer.analyze(sanitized).filter { it.owners.isEmpty() }.map { it.host }
        val brands = brandDetector.detect(body, unownedHosts)
        val tokens = normalizer.tokens(body)
        return MessageAnalysis(
            sanitized = sanitized,
            body = body,
            tokens = tokens,
            links = linkAnalyzer.analyze(sanitized, brands.filter { it.inText }.map { it.brand }),
            brands = brands,
            money = MoneyDetector.find(body),
            general = matcher.match(tokens, keywords.general),
        )
    }
}

/** Cheap gate before the rules and the AI check: enough words, and something worth checking. */
class Prefilter(
    private val minTokens: Int = DEFAULT_MIN_TOKENS,
) {
    fun passes(analysis: MessageAnalysis): Boolean =
        analysis.tokens.size >= minTokens &&
            (
                analysis.links.isNotEmpty() ||
                    analysis.money.isNotEmpty() ||
                    analysis.general.isNotEmpty()
            )

    companion object {
        const val DEFAULT_MIN_TOKENS = 2
    }
}
