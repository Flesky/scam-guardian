package ph.scamguardian.ui.testmessage

import ph.scamguardian.core.Language
import ph.scamguardian.core.PipelineReport
import ph.scamguardian.core.ScamWarning
import ph.scamguardian.core.WarningCatalog
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
    const val NO_WARNING = "No warning"

    /** The warning text comes from [warnings] in the selected [language]. */
    fun rows(
        result: CheckResult,
        warnings: WarningCatalog,
        language: Language,
    ): List<ResultRow> {
        val report = result.report
        return listOf(
            ResultRow("AI gate (prefilter)", aiGate(report)),
            ResultRow("Rule result", report.rule?.type?.key ?: "none"),
            ResultRow("AI scores", aiScores(report)),
            ResultRow("Final result", report.warning?.let { warningText(it, warnings, language) } ?: NO_WARNING),
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

    private fun warningText(
        warning: ScamWarning,
        warnings: WarningCatalog,
        language: Language,
    ): String = "${warnings.title(warning.type)}\n${warnings.message(warning.type, language, warning.brand)}"

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
