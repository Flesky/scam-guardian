package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepaymentRuleTest {
    @Test
    fun check_returningOrBorrowingObjects_doesNotTriggerMoneyRequest() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val messages =
            listOf(
                "Send mo yung picture, babalik ako bukas",
                "Padalhan mo ako ng picture, babalik ako bukas",
                "Pahiram ng libro, ibabalik ko bukas",
            )

        messages.forEach { assertNull(it, pipeline.check(it)) }
    }

    @Test
    fun check_repaymentWithMoneyContext_stillWarns() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val messages =
            listOf(
                "anak padalhan mo naman ako 5kyaw babalik ko rin bukas sensya na",
                "Send mo P500, ibabalik ko bukas",
                "Pahiram ng pera, babayaran kita bukas",
                "Pa-gcash naman, babayaran kita bukas",
                "Utang muna, babayaran kita bukas",
            )

        messages.forEach { assertEquals(it, WarningType.MONEY_REQUEST, pipeline.check(it)?.type) }
    }

    @Test
    fun check_urgencyStillWorksWithoutAnAmount() {
        val warning = Fixtures.rulesOnlyPipeline().check("Pahiram naman kailangan ko agad")

        assertEquals(WarningType.MONEY_REQUEST, warning?.type)
    }
}
