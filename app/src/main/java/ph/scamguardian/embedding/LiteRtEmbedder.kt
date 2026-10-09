package ph.scamguardian.embedding

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.InputData
import ph.scamguardian.core.Embedder
import ph.scamguardian.core.truncatedAndNormalized
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * [Embedder] backed by EmbeddingGemma 2 running on-device through LiteRT-LM.
 *
 * The model is loaded on first use and kept in memory. Every model call runs on one dedicated
 * thread; [embed] blocks the caller until the result is ready, so do not call it on the main thread.
 */
class LiteRtEmbedder(
    private val modelPath: String,
) : Embedder,
    AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor { Thread(it, THREAD_NAME) }

    // Only read and written on the executor thread.
    private var engine: EmbeddingEngine? = null

    override fun embed(text: String): FloatArray =
        try {
            executor.submit(Callable { compute(text) }).get()
        } catch (e: ExecutionException) {
            throw IllegalStateException("Embedding failed: ${e.cause?.message}", e)
        }

    override fun close() {
        executor.execute {
            engine?.close()
            engine = null
        }
        executor.shutdown()
    }

    private fun compute(text: String): FloatArray {
        val input = InputData.Text(CLASSIFICATION_PREFIX + text.trim())
        return loadedEngine().computeEmbedding(listOf(input)).embedding.truncatedAndNormalized(DIMENSIONS)
    }

    private fun loadedEngine(): EmbeddingEngine {
        engine?.let { return it }
        check(File(modelPath).isFile) { "Model file not found at $modelPath" }
        val created = EmbeddingEngine(EmbeddingEngineConfig(modelPath = modelPath, backend = Backend.GPU()))
        created.initialize()
        engine = created
        return created
    }

    private companion object {
        const val THREAD_NAME = "litert-embedder"
        const val CLASSIFICATION_PREFIX = "task: classification | query: "
        const val DIMENSIONS = 256
    }
}
