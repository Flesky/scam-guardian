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
        val warning = Fixtures.rulesOnlyPipeline().check("Pahiram naman ng pera kailangan ko agad")

        assertEquals(WarningType.MONEY_REQUEST, warning?.type)
    }

    @Test
    fun check_borrowingWithNowNaOrAPromiseToReturn_warns() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val cases =
            mapOf(
                "Pautang nga ko 10k please now na balik ko sa katapusan" to
                    "Asks for money (pautang) and says it is urgent (now na) and promises to pay it back (balik)",
                "Pahiram ako 10k please now na" to "Asks for money (pahiram) and says it is urgent (now na)",
                "Pahiram ako please now na\n10k" to "Asks for money (pahiram) and says it is urgent (now na)",
                "Pahiram muna 5,000 urgent lang" to "Asks for money (pahiram) and says it is urgent (urgent)",
                "Pautang muna 2k, ibalik ko sa sahod" to
                    "Asks for money (pautang) and promises to pay it back (ibabalik)",
                "Pahiramin mo ko 500 asap" to "Asks for money (pahiramin) and says it is urgent (asap)",
            )

        cases.forEach { (text, evidence) ->
            val warning = pipeline.check(text)
            assertEquals(text, WarningType.MONEY_REQUEST, warning?.type)
            assertEquals(text, evidence, warning?.evidence)
        }
    }

    @Test
    fun check_borrowingWithoutPressure_doesNotWarn() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val messages =
            listOf(
                "Pautang naman minsan, joke lang",
                "Balik ka na dito, pahiram ng payong",
                "Pahiram ng charger mamaya",
                // Borrowing a thing in a hurry is not a money request.
                "Pahiram ng charger now na",
                "Pahiram ako please now na",
                "Pahiram naman kailangan ko agad",
            )

        messages.forEach { assertNull(it, pipeline.check(it)) }
    }
}
