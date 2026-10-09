package ph.scamguardian.core

/** Finds money amounts: pesos, "K" amounts, "18P" style amounts and points. */
object MoneyDetector {
    private const val NUMBER = """\d[\d,]*(?:\.\d+)?"""

    // A number is only read from its first digit, so a long run of digits is scanned once.
    private const val START = """(?<![\d.,])"""

    private val patterns =
        listOf(
            // P4700.00, P888, P157+128, PHP 1,688, ₱10,000, P50K
            Regex("""(?<![a-z0-9])(?:₱|php|p)\s?${NUMBER}k?(?![a-z0-9])"""),
            // 3,000 pesos
            Regex("""$START$NUMBER\s?pesos?\b"""),
            // 865.3K, 30K, 5kyaw (slang for five thousand)
            Regex("""(?<![a-z0-9.,])\d+(?:\.\d+)?(?:k|\s?kyaw)(?![a-z0-9])"""),
            // 18P
            Regex("""(?<![a-z0-9.,])\d+p(?![a-z0-9])"""),
            // 6,552 pts, 6,225 points, points (5,980)
            Regex("""$START$NUMBER\s?(?:points?|pts)\b"""),
            Regex("""\b(?:points?|pts)\s?\(?$NUMBER"""),
        )

    // A plain number that reads as an amount: "500", "5,000", "10000". Not part of a time, a date or a
    // longer number, and not starting with 0 like a phone number.
    private val bareAmount = Regex("""(?<![\w.,:/-])(?:[1-9]\d{0,2}(?:,\d{3})+|[1-9]\d{2,5})(?![\w:/-]|[.,]\d)""")

    /** True when [text] has a money amount, also one written as a plain number such as "5,000". */
    fun hasAmount(text: String): Boolean = find(text).isNotEmpty() || bareAmount.containsMatchIn(text.lowercase())

    fun find(text: String): List<String> {
        val lower = text.lowercase()
        return patterns
            .flatMap { pattern -> pattern.findAll(lower).map { it.value.trim().trimEnd(',', '.') } }
            .distinct()
    }
}
