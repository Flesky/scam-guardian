package ph.scamguardian.core

/** Finds money amounts: pesos, "K" amounts, "18P" style amounts and points. */
object MoneyDetector {
    private const val NUMBER = """\d[\d,]*(?:\.\d+)?"""

    private val patterns =
        listOf(
            // P4700.00, P888, P157+128, PHP 1,688, ₱10,000, P50K
            Regex("""(?<![a-z0-9])(?:₱|php|p)\s?${NUMBER}k?(?![a-z0-9])"""),
            // 3,000 pesos
            Regex("""$NUMBER\s?pesos?\b"""),
            // 865.3K, 30K
            Regex("""(?<![a-z0-9.,])\d+(?:\.\d+)?k(?![a-z0-9])"""),
            // 18P
            Regex("""(?<![a-z0-9.,])\d+p(?![a-z0-9])"""),
            // 6,552 pts, 6,225 points, points (5,980)
            Regex("""$NUMBER\s?(?:points?|pts)\b"""),
            Regex("""\b(?:points?|pts)\s?\(?$NUMBER"""),
        )

    fun find(text: String): List<String> {
        val lower = text.lowercase()
        return patterns
            .flatMap { pattern -> pattern.findAll(lower).map { it.value.trim().trimEnd(',', '.') } }
            .distinct()
    }
}
