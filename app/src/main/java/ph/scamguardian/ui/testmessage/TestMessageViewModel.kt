package ph.scamguardian.ui.testmessage

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ph.scamguardian.ScamEngine
import ph.scamguardian.core.PipelineReport
import java.io.IOException

data class TestMessageState(
    val text: String = "",
    val loading: Boolean = true,
    val checking: Boolean = false,
    val error: String? = null,
    val result: CheckResult? = null,
) {
    val canCheck: Boolean get() = !loading && !checking && text.isNotBlank()
}

class TestMessageViewModel internal constructor(
    private val awaitReady: suspend () -> Unit,
    private val inspect: suspend (String) -> PipelineReport,
    private val elapsedRealtime: () -> Long,
) : ViewModel() {
    constructor(engine: ScamEngine) : this({ engine.pipeline() }, engine::inspect, SystemClock::elapsedRealtime)

    private val mutableState = MutableStateFlow(TestMessageState())
    val state: StateFlow<TestMessageState> = mutableState.asStateFlow()
    private var textRevision = 0L

    init {
        // Stays in the loading state, with the reason shown, if the model or data cannot be loaded.
        viewModelScope.launch {
            val error = failureOf { awaitReady() }
            mutableState.update { it.copy(loading = error != null, error = error) }
        }
    }

    fun onTextChange(text: String) {
        if (text == state.value.text) return
        textRevision++
        mutableState.update { current ->
            current.copy(text = text, result = null, error = if (current.loading) current.error else null)
        }
    }

    fun check() {
        val text = state.value.text
        if (!state.value.canCheck) return
        val revision = textRevision
        mutableState.update { it.copy(checking = true, error = null, result = null) }
        viewModelScope.launch {
            val start = elapsedRealtime()
            var result: CheckResult? = null
            val error = failureOf { result = CheckResult(inspect(text), elapsedRealtime() - start) }
            mutableState.update {
                if (revision == textRevision) {
                    it.copy(checking = false, error = error, result = result)
                } else {
                    it.copy(checking = false)
                }
            }
        }
    }

    private suspend fun failureOf(block: suspend () -> Unit): String? =
        try {
            block()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            e.message ?: e.toString()
        } catch (e: IOException) {
            e.message ?: e.toString()
        }
}
