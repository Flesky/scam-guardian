package ph.scamguardian.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ph.scamguardian.BuildConfig
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.core.ScamPipeline
import ph.scamguardian.core.ScamWarning
import ph.scamguardian.core.ScanTiming
import ph.scamguardian.core.ScreenBlock

/**
 * Reads incoming messages in chat apps and the page text in browsers, sends them to the scam
 * pipeline, and shows a warning banner with a history entry when the pipeline warns.
 *
 * It works only while the main button is ON. Text is read once the screen has been still for 1.5 s, or
 * for 3 s after scrolling, and each
 * block of text is checked once. An app gets at most one banner every 30 seconds; a warning found in
 * between is checked again later. Nothing here may crash the service: every entry point is [guarded].
 */
class ScamAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val preferences by lazy { GuardPreferences(this) }
    private val engine by lazy { (application as ScamGuardianApp).engine }
    private val history by lazy { (application as ScamGuardianApp).history }
    private val notifier by lazy { WarningNotifier(this, scope) }
    private val log = ScanLog()

    // Blocks already sent, so the same text is not sent to the model thread on every screen change. Main thread only.
    private val sent = SentBlocks()
    private val timing = ScanTiming()

    // The history clear count when the service last looked. Main thread only.
    private var historyClears = 0

    private val processWindow = Runnable { guarded("read the screen") { processActiveWindow() } }
    private val logTotals =
        object : Runnable {
            override fun run() {
                log.logTotals()
                handler.postDelayed(this, STATS_INTERVAL_MS)
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        OpenWarning.dismiss = { handler.post { guarded("remove the banner") { notifier.dismiss() } } }
        // When the engine finishes loading, or fails, the screen is read again with what works now.
        scope.launch {
            engine.state.collect {
                handler.post {
                    guarded("read the screen again") {
                        sent.clear()
                        scanIn(0)
                    }
                }
            }
        }
        if (BuildConfig.DEBUG) handler.postDelayed(logTotals, STATS_INTERVAL_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        guarded("handle an event") {
            if (preferences.enabled) {
                val isScroll = event?.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                // After a scroll or a change of window the outline is no longer on the message.
                if (isScroll || event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    notifier.hideHighlight()
                }
                handler.removeCallbacks(processWindow)
                handler.postDelayed(processWindow, timing.delayAfterEvent(SystemClock.uptimeMillis(), isScroll))
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        OpenWarning.dismiss = null
        guarded("remove the banner") { notifier.dismiss() }
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun processActiveWindow() {
        val screen = readScreen() ?: return
        engine.startIfNeeded()
        // With no pipeline yet nothing can be checked; the screen is read again when the engine has loaded.
        val pipeline = engine.loadedPipeline ?: return
        if (notifier.mayShow(screen.packageName)) {
            send(screen, pipeline)
        } else {
            // This app had a banner less than 30 seconds ago: read the screen again when it may show one.
            scanIn(notifier.waitMs(screen.packageName))
        }
    }

    /** The blocks of text on screen now, or null when protection is off or the app in front is not watched. */
    private fun readScreen(): Screen? = rootInActiveWindow?.takeIf { preferences.enabled }?.let(ScreenReader::read)

    private fun send(
        screen: Screen,
        pipeline: ScamPipeline,
    ) {
        // Clearing the history lets old messages warn again, so they are sent again.
        val clears = history.clears
        if (clears != historyClears) {
            historyClears = clears
            sent.clear()
        }
        screen.blocks
            .filter { sent.add(screen.packageName, it.text) }
            .forEach { check(screen.packageName, it, pipeline) }
    }

    private fun scanIn(delayMs: Long) {
        handler.removeCallbacks(processWindow)
        handler.postDelayed(processWindow, delayMs)
    }

    // The pipeline reuses its analysis of a text it has seen. Whether a warning was shown is remembered
    // here, in the sent blocks, and across restarts in the history.
    private fun check(
        packageName: String,
        block: ScreenBlock,
        pipeline: ScamPipeline,
    ) {
        scope.launch(engine.modelDispatcher) {
            guarded("check a block") {
                // Protection may have been switched OFF while this block waited for the model thread.
                // A message that already has a history entry does not warn again.
                val wanted = preferences.enabled && !history.hasText(block.text)
                val report = if (wanted) pipeline.inspectCached(block.text) else null
                if (!preferences.enabled) {
                    // Not checked: forget it so it is read again when protection is back ON.
                    handler.post { sent.forget(packageName, block.text) }
                } else if (report != null) {
                    log.record(report)
                    if (BuildConfig.DEBUG) log.logBlock(packageName, block.text, report)
                    report.warning?.let { warning ->
                        handler.post { guarded("show a warning") { raise(packageName, block, warning) } }
                    }
                }
            }
        }
    }

    // Main thread. The check took a moment, so the screen is read again: the warning is shown only if
    // the message is still there, and the outline goes where the message is now. Otherwise the block is
    // forgotten and checked again the next time it is read.
    private fun raise(
        packageName: String,
        block: ScreenBlock,
        warning: ScamWarning,
    ) {
        val current =
            readScreen()
                ?.takeIf { it.packageName == packageName }
                ?.blocks
                ?.firstOrNull { it.text == block.text }
        if (current != null && notifier.show(packageName, current, warning)) return
        sent.forget(packageName, block.text)
        // Held back by the 30-second wait: read the screen again when the wait is over.
        if (current != null) scanIn(notifier.waitMs(packageName))
    }

    companion object {
        private const val STATS_INTERVAL_MS = 5 * 60 * 1_000L

        /** True when the user has switched this service on in the system accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            return manager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.name == ScamAccessibilityService::class.java.name }
        }
    }
}
