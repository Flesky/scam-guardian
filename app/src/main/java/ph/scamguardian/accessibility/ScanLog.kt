package ph.scamguardian.accessibility

import android.util.Log
import ph.scamguardian.core.PipelineReport
import ph.scamguardian.core.WarningType
import java.util.concurrent.atomic.AtomicInteger

/** The debug log of the service: one line per checked block, and the totals of the last 5 minutes. */
internal class ScanLog {
    private val blocksSeen = AtomicInteger()
    private val ruleWarnings = AtomicInteger()
    private val aiRuns = AtomicInteger()
    private val aiWarnings = AtomicInteger()

    fun record(report: PipelineReport) {
        blocksSeen.incrementAndGet()
        if (report.aiScore != null) aiRuns.incrementAndGet()
        when {
            report.rule != null -> ruleWarnings.incrementAndGet()
            report.warning?.type == WarningType.AI_SCAM -> aiWarnings.incrementAndGet()
        }
    }

    fun logBlock(
        packageName: String,
        block: String,
        report: PipelineReport,
    ) {
        val preview = block.replace('\n', ' ').take(PREVIEW_LENGTH)
        val ai = if (report.aiScore != null) "AI run" else "AI skipped"
        Log.d(TAG, "$packageName | $preview | $ai | ${report.warning?.type?.key ?: "none"}")
    }

    /** Logs the totals and starts counting again. */
    fun logTotals() {
        Log.d(
            TAG,
            "last 5 min: blocks seen=${blocksSeen.getAndSet(0)}, rule warnings=${ruleWarnings.getAndSet(0)}, " +
                "AI runs=${aiRuns.getAndSet(0)}, AI warnings=${aiWarnings.getAndSet(0)}",
        )
    }

    private companion object {
        const val TAG = "SG"
        const val PREVIEW_LENGTH = 60
    }
}
