package ph.scamguardian.accessibility

/**
 * Remembers which blocks of text were already sent to the pipeline, so the same text is not sent again on
 * every screen change. It keeps the whole text, not a hash: two different messages must never be taken
 * for each other. Not thread-safe; use it from one thread.
 */
class SentBlocks(
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val keys = LinkedHashSet<String>()

    /** Remembers the block. Returns false if it was already remembered. */
    fun add(
        packageName: String,
        block: String,
    ): Boolean {
        val isNew = keys.add(key(packageName, block))
        while (keys.size > capacity) keys.remove(keys.first())
        return isNew
    }

    /** Forgets the block, so it is sent again the next time it is read. */
    fun forget(
        packageName: String,
        block: String,
    ) {
        keys.remove(key(packageName, block))
    }

    /** Forgets every block. */
    fun clear() = keys.clear()

    private fun key(
        packageName: String,
        block: String,
    ): String = "$packageName\n$block"

    private companion object {
        const val DEFAULT_CAPACITY = 500
    }
}
