package ph.scamguardian.core

/**
 * Decides how long to wait before reading the screen. The screen is read once it has been still for a
 * while; after scrolling the wait is longer, so text that only flew past is not read and warned about.
 */
class ScanTiming(
    private val pauseAfterChangeMs: Long = PAUSE_AFTER_CHANGE_MS,
    private val pauseAfterScrollMs: Long = PAUSE_AFTER_SCROLL_MS,
) {
    private var lastScrollMs: Long? = null

    /** Call for every screen event. Returns how long to wait from [nowMs] before reading the screen. */
    fun delayAfterEvent(
        nowMs: Long,
        isScroll: Boolean,
    ): Long {
        if (isScroll) lastScrollMs = nowMs
        val sinceScroll = lastScrollMs?.let { nowMs - it }
        val scrollWait = if (sinceScroll == null) 0 else pauseAfterScrollMs - sinceScroll
        return maxOf(pauseAfterChangeMs, scrollWait)
    }

    companion object {
        const val PAUSE_AFTER_CHANGE_MS = 1_500L
        const val PAUSE_AFTER_SCROLL_MS = 3_000L
    }
}
