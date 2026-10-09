package ph.scamguardian.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Rect
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
import ph.scamguardian.core.ScreenBlocks

/**
 * Reads on-screen text in the monitored apps, sends it to the scam pipeline, and shows a warning banner
 * with a history entry when the pipeline warns.
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
    private val notifier by lazy { WarningNotifier(this, scope) }
    private val log = ScanLog()

    // Blocks already sent, so the same text is not sent to the model thread on every screen change. Main thread only.
    private val sent = SentBlocks()
    private val timing = ScanTiming()

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
        guarded("remove the banner") { notifier.dismiss() }
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun processActiveWindow() {
        val root = rootInActiveWindow?.takeIf { preferences.enabled } ?: return
        val packageName = root.packageName?.toString().orEmpty()
        val kind = MonitoredApps.kindOf(packageName) ?: return
        val nodes = ScreenReader.textNodes(root)
        val blocks =
            when (kind) {
                AppKind.CHAT -> {
                    // Measured against the app's own window, which is not the whole screen in split-screen.
                    val window = Rect().also(root::getBoundsInScreen)
                    ScreenBlocks.chat(nodes, windowLeft = window.left, windowWidth = window.width())
                }

                AppKind.FEED -> {
                    ScreenBlocks.feed(nodes)
                }
            }
        send(packageName, blocks)
    }

    private fun send(
        packageName: String,
        blocks: List<ScreenBlock>,
    ) {
        val pipeline = engine.loadedPipeline
        if (pipeline == null) {
            // Until the model is loaded, blocks are dropped, not queued. They are read again on a later change.
            engine.start()
        } else if (notifier.mayShow(packageName)) {
            blocks.filter { sent.add(packageName, it.text) }.forEach { check(packageName, it, pipeline) }
        }
        // Otherwise this app had a banner less than 30 seconds ago. Its blocks are left for a later
        // change of the screen, so a warning found now is not lost.
    }

    // The pipeline cache decides if a text is new: the same text never gives a second banner, in any app.
    private fun check(
        packageName: String,
        block: ScreenBlock,
        pipeline: ScamPipeline,
    ) {
        scope.launch(engine.modelDispatcher) {
            guarded("check a block") {
                // Protection may have been switched OFF while this block waited for the model thread.
                val report = if (preferences.enabled) pipeline.inspectNew(block.text) else null
                if (!preferences.enabled) {
                    // Not checked, or checked too late to act on: forget it so it is read again when ON.
                    handler.post { sent.forget(packageName, block.text) }
                    if (report != null) pipeline.forget(block.text)
                } else if (report != null) {
                    log.record(report)
                    if (BuildConfig.DEBUG) log.logBlock(packageName, block.text, report)
                    report.warning?.let { warning ->
                        handler.post { guarded("show a warning") { raise(packageName, block, warning, pipeline) } }
                    }
                }
            }
        }
    }

    // Main thread. A block whose banner cannot be shown now is forgotten, so it is checked again the next
    // time it is read.
    private fun raise(
        packageName: String,
        block: ScreenBlock,
        warning: ScamWarning,
        pipeline: ScamPipeline,
    ) {
        if (preferences.enabled && notifier.show(packageName, block, warning)) return
        sent.forget(packageName, block.text)
        scope.launch(engine.modelDispatcher) { guarded("forget a block") { pipeline.forget(block.text) } }
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
