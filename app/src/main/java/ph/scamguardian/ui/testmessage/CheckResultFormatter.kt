package ph.scamguardian.ui.testmessage

import ph.scamguardian.core.PipelineReport
import java.util.Locale

/** One pipeline run on the test screen. */
data class CheckResult(
    val report: PipelineReport,
    val totalMs: Long,
)

/** One labelled line of the result. */
data class ResultRow(
    val label: String,
    val value: String,
)

/** Turns a pipeline report into the rows shown on the test screen. */
object CheckResultFormatter {
    const val NO_WARNING = "Walang babala"

    fun rows(result: CheckResult): List<ResultRow> {
        val report = result.report
        return listOf(
            ResultRow("AI gate (prefilter)", aiGate(report)),
            ResultRow("Rule result", report.rule?.type?.key ?: "none"),
            ResultRow("AI scores", aiScores(report)),
            ResultRow("Final result", report.warning?.let { "${it.title}\n${it.message}" } ?: NO_WARNING),
            ResultRow(
                "Evidence",
                report.warning
                    ?.evidence
                    ?.split("; ")
                    ?.joinToString("\n") ?: "none",
            ),
            ResultRow("Model calls", report.modelCalls.toString()),
            ResultRow("Total time", "${result.totalMs} ms"),
        )
    }

    private fun aiGate(report: PipelineReport): String {
        val reasons = report.gate.reasons.joinToString("; ")
        return when {
            report.aiScore != null -> "AI run: $reasons"
            report.rule != null -> "AI skipped: a rule matched"
            !report.gate.passes -> "AI skipped: $reasons"
            else -> "AI skipped: no scam anchors"
        }
    }

    private fun aiScores(report: PipelineReport): String =
        report.aiScore?.let { score ->
            String.format(Locale.US, "scam %.2f, safe %.2f, threshold %.2f", score.scam, score.safe, report.aiThreshold)
        } ?: "AI not run"
}
