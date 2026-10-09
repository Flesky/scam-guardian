package ph.scamguardian.core

/** One piece of visible text on screen, with where it is and which container holds it. */
data class ScreenNode(
    val text: String,
    val centerX: Int,
    val containerId: Int,
)

/** Turns the text nodes of a screen into blocks worth sending to the pipeline. */
object ScreenBlocks {
    private const val MAX_BLOCK_LENGTH = 1000
    private const val MIN_WORDS = 3

    // Incoming chat messages sit in the left 60% of the screen.
    private const val INCOMING_NUMERATOR = 6
    private const val INCOMING_DENOMINATOR = 10

    private val uiTexts = setOf("seen", "active now", "like", "comment", "share", "reply")
    private val time = Regex("""\d{1,2}:\d{2}(\s?[ap]\.?m\.?)?""", RegexOption.IGNORE_CASE)
    private val whitespace = Regex("\\s+")

    /** Chat apps: one block per incoming message. */
    fun chat(
        nodes: List<ScreenNode>,
        screenWidth: Int,
    ): List<String> =
        nodes
            .filter { it.centerX * INCOMING_DENOMINATOR < screenWidth * INCOMING_NUMERATOR }
            .map { it.text.trim() }
            .filter(::isWorthChecking)
            .distinct()

    /** Feed and browser apps: text nodes in the same container are joined, so a post and its link stay together. */
    fun feed(nodes: List<ScreenNode>): List<String> =
        nodes
            .filterNot { isUiText(it.text) }
            .groupBy({ it.containerId }, { it.text.trim() })
            .values
            .flatMap(::join)
            .filter(::isWorthChecking)
            .distinct()

    /** False for buttons, status labels, times and anything of two words or fewer. */
    fun isWorthChecking(text: String): Boolean =
        !isUiText(text) && text.trim().split(whitespace).count(String::isNotEmpty) >= MIN_WORDS

    private fun isUiText(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.lowercase() in uiTexts || time.matches(trimmed)
    }

    // Packs texts into blocks of about MAX_BLOCK_LENGTH characters; one long text stays whole.
    private fun join(texts: List<String>): List<String> {
        val blocks = mutableListOf<StringBuilder>()
        texts.forEach { text ->
            val current = blocks.lastOrNull()
            if (current == null || current.length + text.length + 1 > MAX_BLOCK_LENGTH) {
                blocks += StringBuilder(text)
            } else {
                current.append('\n').append(text)
            }
        }
        return blocks.map(StringBuilder::toString)
    }
}
