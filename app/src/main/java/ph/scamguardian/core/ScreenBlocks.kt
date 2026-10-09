package ph.scamguardian.core

/** A rectangle on the screen, in pixels. */
data class ScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    /** The smallest rectangle that holds this one and [other]. */
    fun union(other: ScreenRect): ScreenRect =
        ScreenRect(
            minOf(left, other.left),
            minOf(top, other.top),
            maxOf(right, other.right),
            maxOf(bottom, other.bottom),
        )
}

/** One piece of visible text on screen, with where it is and which container holds it. */
data class ScreenNode(
    val text: String,
    val centerX: Int,
    val containerId: Int,
    val bounds: ScreenRect? = null,
)

/** Text worth checking, and where it is on screen (null when the place is not known). */
data class ScreenBlock(
    val text: String,
    val bounds: ScreenRect? = null,
)

/** Turns the text nodes of a screen into blocks worth sending to the pipeline. */
object ScreenBlocks {
    private const val MAX_BLOCK_LENGTH = 1000
    private const val MIN_WORDS = 2
    private const val AMOUNT_MAX_WORDS = 3

    // Incoming chat messages sit in the left 60% of the window.
    private const val INCOMING_NUMERATOR = 6
    private const val INCOMING_DENOMINATOR = 10

    private val uiTexts = setOf("seen", "active now", "like", "comment", "share", "reply")
    private val time = Regex("""\d{1,2}:\d{2}(\s?[ap]\.?m\.?)?""", RegexOption.IGNORE_CASE)
    private val whitespace = Regex("\\s+")

    // Messenger adds screen-reader hints to each message: ", double tap to see sent/receive date and time, …".
    private val readerHint = Regex(""",\s*double tap [^\n]*$""", RegexOption.IGNORE_CASE)
    private val link = Regex("""[^\s.]+(\.[^\s.]+)*\.\p{L}{2,}(/\S*)?""")

    /**
     * Chat apps: one block per incoming message. A message is incoming when its center is in the left 60%
     * of the app's window, which starts at [windowLeft] on the screen and is [windowWidth] wide.
     */
    fun chat(
        nodes: List<ScreenNode>,
        windowLeft: Int,
        windowWidth: Int,
    ): List<ScreenBlock> =
        nodes
            .filter { (it.centerX - windowLeft) * INCOMING_DENOMINATOR < windowWidth * INCOMING_NUMERATOR }
            .map { ScreenBlock(it.text.replace(readerHint, "").trim(), it.bounds) }
            .filterNot { isUiText(it.text) }
            .fold(emptyList(), ::addOrJoin)
            .filter { isWorthChecking(it.text) }
            .distinctBy { it.text }

    // People often send the amount on its own: "Pahiram ako please now na", then "10k". Such a short
    // amount is joined to the incoming message before it, so the two are checked as one.
    private fun addOrJoin(
        blocks: List<ScreenBlock>,
        next: ScreenBlock,
    ): List<ScreenBlock> {
        val previous = blocks.lastOrNull()
        val isLoneAmount = wordsOf(next.text).size <= AMOUNT_MAX_WORDS && MoneyDetector.hasAmount(next.text)
        return if (previous == null || !isLoneAmount) {
            blocks + next
        } else {
            blocks.dropLast(1) + ScreenBlock("${previous.text}\n${next.text}", union(previous.bounds, next.bounds))
        }
    }

    private fun wordsOf(text: String): List<String> = text.trim().split(whitespace).filter(String::isNotEmpty)

    /** Browsers: text nodes in the same container are joined, so a paragraph and its link stay together. */
    fun page(nodes: List<ScreenNode>): List<ScreenBlock> =
        nodes
            .filterNot { isUiText(it.text) }
            .groupBy { it.containerId }
            .values
            .flatMap(::join)
            .filter { isWorthChecking(it.text) }
            .distinctBy { it.text }

    /**
     * False for buttons, status labels, times and single words. A single link counts: the rules can
     * judge "gcash-win.cc" on its own, as they can a two-word "Send OTP".
     */
    fun isWorthChecking(text: String): Boolean {
        val words = wordsOf(text)
        return !isUiText(text) && (words.size >= MIN_WORDS || words.singleOrNull()?.let(::looksLikeLink) == true)
    }

    private fun looksLikeLink(word: String): Boolean = "://" in word || link.matches(word)

    private fun isUiText(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.lowercase() in uiTexts || time.matches(trimmed)
    }

    // Packs texts into blocks of about MAX_BLOCK_LENGTH characters; one long text stays whole.
    private fun join(nodes: List<ScreenNode>): List<ScreenBlock> {
        val blocks = mutableListOf<ScreenBlock>()
        nodes.forEach { node ->
            val text = node.text.trim()
            val current = blocks.lastOrNull()
            if (current == null || current.text.length + text.length + 1 > MAX_BLOCK_LENGTH) {
                blocks += ScreenBlock(text, node.bounds)
            } else {
                blocks[blocks.lastIndex] = ScreenBlock("${current.text}\n$text", union(current.bounds, node.bounds))
            }
        }
        return blocks
    }

    private fun union(
        first: ScreenRect?,
        second: ScreenRect?,
    ): ScreenRect? = if (first == null || second == null) first ?: second else first.union(second)
}
