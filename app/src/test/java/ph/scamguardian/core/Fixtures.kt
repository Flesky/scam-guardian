package ph.scamguardian.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ScamCase(
    val types: List<String>,
    val text: String,
)

@Serializable
data class PrefilterCases(
    val pass: List<String>,
    val fail: List<String>,
)

/** Test data from src/test/resources/fixtures. Tests never read the app's assets. */
object Fixtures {
    const val NO_ANCHORS = """{ "scam": [], "safe": [] }"""

    val json = Json { ignoreUnknownKeys = true }

    fun text(name: String): String =
        checkNotNull(Fixtures::class.java.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()

    fun data(
        anchors: String = NO_ANCHORS,
        brands: String = "brands_test.json",
    ): PipelineData =
        PipelineJson(
            brands = text(brands),
            keywords = text("keywords_test.json"),
            shortcuts = text("shortcuts_test.json"),
            urlRules = text("url_rules_test.json"),
            warnings = text("warnings_test.json"),
            anchors = anchors,
        ).parse()

    fun normalizer(): Normalizer = Normalizer(data().shortcuts)

    fun linkAnalyzer(): LinkAnalyzer = LinkAnalyzer(data().urlRules, data().brands)

    fun brandDetector(): BrandDetector = BrandDetector(data().brands, normalizer(), Confusables())

    fun analyzer(): MessageAnalyzer {
        val normalizer = normalizer()
        return MessageAnalyzer(data(), normalizer, KeywordMatcher(normalizer), linkAnalyzer())
    }

    /** A pipeline without anchors, so only the rules can warn. */
    fun rulesOnlyPipeline(): ScamPipeline = ScamPipeline(data(), FakeEmbedder())

    fun realScams(): List<ScamCase> = json.decodeFromString(text("real_scams.json"))

    fun safeMessages(): List<String> = json.decodeFromString(text("safe_messages.json"))
}
