package ph.scamguardian.core

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Ordinary messages must not get a rule warning. */
@RunWith(Parameterized::class)
class SafeMessagesTest(
    @Suppress("unused") private val label: String,
    private val text: String,
) {
    @Test
    fun check_givesNoRuleWarning() {
        val warning = Fixtures.rulesOnlyPipeline().check(text)

        assertNull("unexpected ${warning?.type?.key} (${warning?.evidence}): $text", warning)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            Fixtures.safeMessages().mapIndexed { index, text -> arrayOf("${index + 1}", text) }
    }
}
