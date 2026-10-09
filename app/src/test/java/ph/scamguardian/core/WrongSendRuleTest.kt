package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WrongSendRuleTest {
    private val pipeline = Fixtures.rulesOnlyPipeline()

    @Test
    fun check_moneySentByMistakeAndWantedBack_isARefundRequest() {
        val messages =
            listOf(
                "Magandang hapon po. Pasensya na po nagkamali ako send gcash. Pablik na lang poh. " +
                    "Send ko dapat sa 0999 999 9994 eh. Sana maintindihan nyo slamat.",
                "Wrong send po sa GCash, pakibalik naman po",
                "Namali ako ng number, pa-gcash ibalik mo na lang",
                "Sorry sent by mistake, please send back the money",
                "Na xsend ko po sa inyo yung 500, pakibalik naman po",
                "Xsend po, pabalik na lang sa number na to",
                "Sorry po na-wrong send, paki refund naman",
                "Wrongsend po ako sa gcash nyo, pasauli po",
                "Missent po yung pera, please return",
                "Naipadala ko po sa maling number, ibalik nyo na lang po",
                "Hindi sinasadya, napasend ko sa inyo. Pakibalik po",
                "Accidentally sent you 1,000 pesos, please refund",
            )

        messages.forEach { assertEquals(it, WarningType.REFUND_REQUEST, pipeline.check(it)?.type) }
    }

    @Test
    fun check_evidenceNamesTheMistakeAndTheReturn() {
        val warning = pipeline.check("Wrong send po sa GCash, pakibalik naman po")

        assertEquals(
            "Says money was sent by mistake (wrong send); asks you to send it back (pakibalik)",
            warning?.evidence,
        )
        assertEquals(Severity.AMBER, warning?.severity)
        assertEquals(
            "Do not entertain people who ask you to send money back. " +
                "Let them contact customer service to get their money back.",
            Fixtures.data().warnings.message(WarningType.REFUND_REQUEST, Language.ENGLISH, null),
        )
    }

    @Test
    fun check_refundRequestComesBeforeMoneyRequest() {
        assertEquals(
            WarningType.REFUND_REQUEST,
            pipeline.check("Nagkamali ako ng send, pakibalik agad kailangan ko na")?.type,
        )
        assertEquals(WarningType.MONEY_REQUEST, pipeline.check("Pahiram muna ng 500 kailangan ko agad")?.type)
    }

    @Test
    fun check_aMistakeOrAReturnAlone_isNotEnough() {
        val messages =
            listOf(
                "Nagkamali ako ng send kanina, sorry ha",
                "Mali yung address, send ko ulit mamaya",
                "Pakibalik ng libro bukas, salamat",
                "Send mo yung picture, babalik ako bukas",
                // A plain "send" is not "xsend".
                "Send mo na, ibalik ko bukas",
                "Mali yung sagot ko, balik tayo sa simula",
                "Sent na po, salamat",
            )

        messages.forEach { assertNull(it, pipeline.check(it)) }
    }
}
