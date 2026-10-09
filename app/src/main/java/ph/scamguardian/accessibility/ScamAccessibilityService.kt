package ph.scamguardian.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ph.scamguardian.BuildConfig
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.core.PipelineReport
import ph.scamguardian.core.ScamPipeline
import ph.scamguardian.core.ScreenBlocks
import ph.scamguardian.core.WarningType
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reads on-screen text in the monitored apps and sends it to the scam pipeline.
 *
 * It works only while the main button is ON. Text is read 1.5 s after the screen last changed, and each
 * block of text is checked once. Nothing here may crash the service: every entry point catches and logs.
 */
class ScamAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val preferences by lazy { GuardPreferences(this) }
    private val engine by lazy { (application as ScamGuardianApp).engine }

    // Blocks already sent, so the same text is not checked on every screen change. Main thread only.
    private val sent = LinkedHashSet<Int>()

    private val blocksSeen = AtomicInteger()
    private val ruleWarnings = AtomicInteger()
    private val aiRuns = AtomicInteger()
    private val aiWarnings = AtomicInteger()

    private val processWindow = Runnable { guarded("read the screen") { processActiveWindow() } }
    private val logStats =
        object : Runnable {
            override fun run() {
                Log.d(
                    TAG,
                    "last 5 min: blocks seen=${blocksSeen.getAndSet(0)}, rule warnings=${ruleWarnings.getAndSet(0)}, " +
                        "AI runs=${aiRuns.getAndSet(0)}, AI warnings=${aiWarnings.getAndSet(0)}",
                )
                handler.postDelayed(this, STATS_INTERVAL_MS)
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (BuildConfig.DEBUG) handler.postDelayed(logStats, STATS_INTERVAL_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        guarded("handle an event") {
            if (preferences.enabled) {
                handler.removeCallbacks(processWindow)
                handler.postDelayed(processWindow, DEBOUNCE_MS)
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
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
                AppKind.CHAT -> ScreenBlocks.chat(nodes, resources.displayMetrics.widthPixels)
                AppKind.FEED -> ScreenBlocks.feed(nodes)
            }
        send(packageName, blocks)
    }

    private fun send(
        packageName: String,
        blocks: List<String>,
    ) {
        val pipeline = engine.loadedPipeline
        if (pipeline == null) {
            // Until the model is loaded, blocks are dropped, not queued. They are read again on a later change.
            engine.start()
        } else {
            blocks.filter { sent.add("$packageName\n$it".hashCode()) }.forEach { check(packageName, it, pipeline) }
            while (sent.size > MAX_REMEMBERED) sent.remove(sent.first())
        }
    }

    private fun check(
        packageName: String,
        block: String,
        pipeline: ScamPipeline,
    ) {
        scope.launch(engine.modelDispatcher) {
            guarded("check a block") {
                val report = pipeline.inspect(block)
                record(report)
                if (BuildConfig.DEBUG) logBlock(packageName, block, report)
            }
        }
    }

    private fun record(report: PipelineReport) {
        blocksSeen.incrementAndGet()
        if (report.aiScore != null) aiRuns.incrementAndGet()
        when {
            report.rule != null -> ruleWarnings.incrementAndGet()
            report.warning?.type == WarningType.AI_SCAM -> aiWarnings.incrementAndGet()
        }
    }

    private fun logBlock(
        packageName: String,
        block: String,
        report: PipelineReport,
    ) {
        val preview = block.replace('\n', ' ').take(PREVIEW_LENGTH)
        val ai = if (report.aiScore != null) "AI run" else "AI skipped"
        Log.d(TAG, "$packageName | $preview | $ai | ${report.warning?.type?.key ?: "none"}")
    }

    // The service must never crash, so this catches everything except coroutine cancellation.
    @Suppress("TooGenericExceptionCaught")
    private inline fun guarded(
        action: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not $action", e)
        }
    }

    companion object {
        private const val TAG = "SG"
        private const val DEBOUNCE_MS = 1_500L
        private const val STATS_INTERVAL_MS = 5 * 60 * 1_000L
        private const val MAX_REMEMBERED = 500
        private const val PREVIEW_LENGTH = 60

        /** True when the user has switched this service on in the system accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            return manager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.name == ScamAccessibilityService::class.java.name }
        }
    }
}
