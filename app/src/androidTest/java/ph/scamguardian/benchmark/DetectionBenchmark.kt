package ph.scamguardian.benchmark

import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.scamguardian.EngineState
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.core.PipelineJson
import ph.scamguardian.core.PipelineReport
import ph.scamguardian.core.ScamPipeline
import ph.scamguardian.embedding.LiteRtEmbedder
import java.io.File

/**
 * Runs the held-out messages in androidTest/assets/benchmark_messages.json through the real pipeline
 * and the real model on the phone, and writes what happened to files/benchmark/results.json.
 *
 * It is a measurement, not a pass/fail test. It waits for the app to copy the model to app storage.
 * The pipeline here is a fresh one: messages the user marked "Not a scam" in the app are not part of it.
 */
class DetectionBenchmark {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as ScamGuardianApp
    private val modelFile = File(app.noBackupFilesDir, MODEL_PATH)

    @Test
    fun measure() {
        waitForAppEngine()
        assumeTrue("The app could not load the model", app.engine.state.value == EngineState.READY)

        val embedder = LiteRtEmbedder(modelFile.path)
        val loadMs = timeMs { embedder.load() }
        lateinit var pipeline: ScamPipeline
        val anchorsMs = timeMs { pipeline = ScamPipeline(pipelineData(), embedder) }
        val embedMs = List(EMBED_RUNS) { timeMs { embedder.embed(EMBED_TEXT) } }

        val results = JSONArray()
        val cases = JSONArray(testAsset("benchmark_messages.json"))
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            lateinit var report: PipelineReport
            val ms = timeMs { report = pipeline.inspect(case.getString("text")) }
            results.put(result(case, report, ms))
        }
        embedder.close()

        val output =
            JSONObject()
                .put("modelLoadMs", loadMs)
                .put("anchorsMs", anchorsMs)
                .put("embedMs", JSONArray(embedMs))
                .put("results", results)
        val file = File(app.getExternalFilesDir("benchmark"), "results.json")
        file.writeText(output.toString(2))
        Log.i(TAG, "wrote ${results.length()} results to ${file.path}")
    }

    // Debug builds load the app's own engine at start. That also copies the model to app storage, and
    // waiting for it keeps the app's engine out of the timings.
    private fun waitForAppEngine() {
        app.engine.start()
        val deadline = SystemClock.elapsedRealtime() + APP_ENGINE_WAIT_MS
        while (app.engine.state.value == EngineState.LOADING && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(POLL_MS)
        }
    }

    private fun result(
        case: JSONObject,
        report: PipelineReport,
        ms: Double,
    ): JSONObject =
        JSONObject()
            .put("label", case.getString("label"))
            .put("category", case.getString("category"))
            .put("text", case.getString("text"))
            .put("warning", report.warning?.type?.key ?: JSONObject.NULL)
            .put("rule", report.rule?.type?.key ?: JSONObject.NULL)
            .put("gatePasses", report.gate.passes)
            .put("gateReasons", JSONArray(report.gate.reasons))
            .put("aiScam", report.aiScore?.scam?.toDouble() ?: JSONObject.NULL)
            .put("aiSafe", report.aiScore?.safe?.toDouble() ?: JSONObject.NULL)
            .put("aiThreshold", report.aiThreshold.toDouble())
            .put("modelCalls", report.modelCalls)
            .put("ms", ms)

    private fun pipelineData() =
        PipelineJson(
            brands = appAsset("brands.json"),
            keywords = appAsset("keywords.json"),
            shortcuts = appAsset("shortcuts.json"),
            urlRules = appAsset("url_rules.json"),
            warnings = appAsset("warnings.json"),
            anchors = appAsset("anchors.json"),
        ).parse()

    private fun appAsset(name: String): String =
        app.assets
            .open(name)
            .bufferedReader()
            .use { it.readText() }

    private fun testAsset(name: String): String =
        instrumentation.context.assets
            .open(name)
            .bufferedReader()
            .use { it.readText() }

    private inline fun timeMs(block: () -> Unit): Double {
        val start = SystemClock.elapsedRealtimeNanos()
        block()
        return (SystemClock.elapsedRealtimeNanos() - start) / NANOS_PER_MS
    }

    private companion object {
        const val TAG = "SG"
        const val MODEL_PATH = "models/embeddinggemma-2-740m.litertlm"
        const val EMBED_TEXT =
            "BDO: You withdrew PHP 5,000.00 from your account ending 8812. If this was not you, call our hotline."
        const val EMBED_RUNS = 20
        const val APP_ENGINE_WAIT_MS = 120_000L
        const val POLL_MS = 250L
        const val NANOS_PER_MS = 1_000_000.0
    }
}
