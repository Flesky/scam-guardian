package ph.scamguardian.core

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Every real scam message must get a rule warning of one of its listed types. */
@RunWith(Parameterized::class)
class RealScamsTest(
    @Suppress("unused") private val label: String,
    private val case: ScamCase,
) {
    @Test
    fun check_givesAListedRuleWarning() {
        val warning = Fixtures.rulesOnlyPipeline().check(case.text)

        assertTrue(
            "expected ${case.types} but got ${warning?.type?.key}: ${case.text}",
            warning?.type?.key in case.types,
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            Fixtures.realScams().mapIndexed { index, case ->
                arrayOf("${index + 1} ${case.types.first()}", case)
            }
    }
}
