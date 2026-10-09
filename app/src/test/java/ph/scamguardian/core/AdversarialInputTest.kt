package ph.scamguardian.core

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Long hostile inputs must be handled in time that grows with their length, not with its square. */
class AdversarialInputTest {
    private val inputs =
        mapOf(
            "digits" to "8".repeat(SIZE) + " verify",
            "digits with commas" to "8,".repeat(SIZE / 2) + " pesos",
            "spaced letters" to "p ".repeat(SIZE / 2) + "verify",
            "one stretched word" to "a".repeat(SIZE) + " verify",
            "dots" to "a.".repeat(SIZE / 2) + " verify",
            "code words" to "otp ".repeat(SIZE / 4) + "send",
        )

    @Test(timeout = TIME_LIMIT_MS)
    fun moneyDetector_handlesLongInputs() {
        inputs.values.forEach { MoneyDetector.find(it) }
    }

    @Test(timeout = TIME_LIMIT_MS)
    fun pipeline_handlesLongInputs() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        inputs.values.forEach { pipeline.check(it) }
    }

    @Test
    fun moneyDetector_timeGrowsWithLengthNotItsSquare() {
        fun time(size: Int): Long {
            val text = "8".repeat(size) + " verify"
            return (1..RUNS).minOf {
                val start = System.nanoTime()
                MoneyDetector.find(text)
                System.nanoTime() - start
            }
        }
        time(SIZE)

        val small = time(SIZE)
        val large = time(SIZE * GROWTH)

        // Quadratic time would be GROWTH squared (64) times slower; allow generous noise above linear (8).
        assertTrue("small=${small}ns large=${large}ns", large < small * GROWTH * GROWTH / 2)
    }

    @Test
    fun pipeline_longDigitRun_givesNoWarning() {
        assertNull(Fixtures.rulesOnlyPipeline().check("8".repeat(SIZE) + " verify"))
    }

    private companion object {
        const val SIZE = 16_000
        const val GROWTH = 8
        const val RUNS = 5
        const val TIME_LIMIT_MS = 3_000L
    }
}
