package ph.scamguardian.core

/** Allows one warning banner per app every [intervalMs]. Not thread-safe: use it from one thread. */
class BannerLimiter(
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    private val lastShown = HashMap<String, Long>()

    /** True when [app] may show a banner at [nowMs]. With [unlimited] (demo mode) it always may. */
    fun isOpen(
        app: String,
        nowMs: Long,
        unlimited: Boolean = false,
    ): Boolean = unlimited || lastShown[app]?.let { nowMs - it >= intervalMs } ?: true

    /** How long until [app] may show a banner again; 0 when it may show one now. */
    fun remainingMs(
        app: String,
        nowMs: Long,
        unlimited: Boolean = false,
    ): Long {
        val last = lastShown[app]
        return if (unlimited || last == null) 0 else (intervalMs - (nowMs - last)).coerceAtLeast(0)
    }

    /** True when [app] may show a banner at [nowMs]; the next one is then allowed [intervalMs] later. */
    fun tryShow(
        app: String,
        nowMs: Long,
        unlimited: Boolean = false,
    ): Boolean = isOpen(app, nowMs, unlimited).also { open -> if (open) lastShown[app] = nowMs }

    companion object {
        const val DEFAULT_INTERVAL_MS = 30_000L
    }
}
