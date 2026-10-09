package ph.scamguardian.accessibility

/**
 * Lets the app's own screen take down a warning banner that is still showing. The banner belongs to
 * another app's message; it must not sit on top of Scam Guardian.
 */
object OpenWarning {
    /** Set by the accessibility service while it runs. Safe to call from any thread. */
    @Volatile
    var dismiss: (() -> Unit)? = null
}
