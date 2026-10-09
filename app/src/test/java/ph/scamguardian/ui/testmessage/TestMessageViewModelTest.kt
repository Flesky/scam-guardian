package ph.scamguardian.ui.testmessage

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ph.scamguardian.core.Fixtures
import ph.scamguardian.core.PipelineReport
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TestMessageViewModelTest {
    private val safeReport = Fixtures.rulesOnlyPipeline().inspect("Kain na tayo")

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onTextChange_clearsACompletedVerdict() =
        runTest {
            val viewModel = model { safeReport }
            runCurrent()
            viewModel.onTextChange("Kain na tayo")
            viewModel.check()
            runCurrent()
            assertSame(safeReport, checkNotNull(viewModel.state.value.result).report)

            viewModel.onTextChange(sampleMessages.first().text)

            assertNull(viewModel.state.value.result)
            assertTrue(viewModel.state.value.canCheck)
        }

    @Test
    fun check_ignoresACompletionAfterAnEditAndAllowsAnotherCheck() =
        runTest {
            val pending = CompletableDeferred<PipelineReport>()
            val checkedTexts = mutableListOf<String>()
            val viewModel =
                model { text ->
                    checkedTexts += text
                    pending.await()
                }
            runCurrent()
            viewModel.onTextChange("first message")
            viewModel.check()
            runCurrent()

            viewModel.onTextChange("second message")
            viewModel.check()
            assertFalse(viewModel.state.value.canCheck)
            pending.complete(safeReport)
            runCurrent()

            assertNull(viewModel.state.value.result)
            assertFalse(viewModel.state.value.checking)
            assertTrue(viewModel.state.value.canCheck)
            assertEquals(listOf("first message"), checkedTexts)

            viewModel.check()
            runCurrent()
            assertEquals(listOf("first message", "second message"), checkedTexts)
            assertSame(safeReport, checkNotNull(viewModel.state.value.result).report)
        }

    @Test
    fun check_ignoresACompletionEvenIfTheUserRestoresTheOriginalText() =
        runTest {
            val pending = CompletableDeferred<PipelineReport>()
            val viewModel = model { pending.await() }
            runCurrent()
            viewModel.onTextChange("original message")
            viewModel.check()
            runCurrent()

            viewModel.onTextChange("edited message")
            viewModel.onTextChange("original message")
            pending.complete(safeReport)
            runCurrent()

            assertNull(viewModel.state.value.result)
            assertTrue(viewModel.state.value.canCheck)
        }

    @Test
    fun onTextChange_sameTextKeepsTheVerdict() =
        runTest {
            val viewModel = model { safeReport }
            runCurrent()
            viewModel.onTextChange("Kain na tayo")
            viewModel.check()
            runCurrent()

            viewModel.onTextChange("Kain na tayo")

            assertSame(safeReport, checkNotNull(viewModel.state.value.result).report)
        }

    @Test
    fun check_ignoresErrorsFromAnObsoleteMessage() =
        runTest {
            val pending = CompletableDeferred<PipelineReport>()
            val viewModel = model { pending.await() }
            runCurrent()
            viewModel.onTextChange("first message")
            viewModel.check()
            runCurrent()

            viewModel.onTextChange("second message")
            pending.completeExceptionally(IOException("Check failed"))
            runCurrent()

            assertNull(viewModel.state.value.error)
            assertNull(viewModel.state.value.result)
            assertTrue(viewModel.state.value.canCheck)
        }

    @Test
    fun onTextChange_clearsAnErrorForThePreviousMessage() =
        runTest {
            val viewModel = model { throw IOException("Check failed") }
            runCurrent()
            viewModel.onTextChange("first message")
            viewModel.check()
            runCurrent()
            assertEquals("Check failed", viewModel.state.value.error)

            viewModel.onTextChange("second message")

            assertNull(viewModel.state.value.error)
            assertTrue(viewModel.state.value.canCheck)
        }

    @Test
    fun onTextChange_preservesALoadingFailure() =
        runTest {
            val viewModel =
                TestMessageViewModel(
                    awaitReady = { throw IOException("Model missing") },
                    inspect = { error("Must not inspect while loading") },
                    elapsedRealtime = { 0L },
                )
            runCurrent()

            viewModel.onTextChange("new message")
            viewModel.check()
            runCurrent()

            assertEquals("Model missing", viewModel.state.value.error)
            assertTrue(viewModel.state.value.loading)
            assertFalse(viewModel.state.value.canCheck)
        }

    private fun model(inspect: suspend (String) -> PipelineReport): TestMessageViewModel =
        TestMessageViewModel(awaitReady = {}, inspect = inspect, elapsedRealtime = { 0L })
}
