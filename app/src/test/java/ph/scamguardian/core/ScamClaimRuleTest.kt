package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScamClaimRuleTest {
    private val pipeline = Fixtures.rulesOnlyPipeline()

    private fun typeOf(text: String): WarningType? = pipeline.inspect(text).rule?.type

    @Test
    fun scamClaim_needsAReassuranceAndMoney() {
        val cases =
            listOf(
                "baron send 500 and pero dont worry ITS NOT A SCAM because ill send u 5000 first",
                "Legit po ito 100%, mag-send lang kayo ng 300 processing fee",
                "Hindi po ito scam, promise. Padala lang 1k para ma-release ang prize mo",
                "Trust me, walang scam dito. Deposit ka lang muna",
                "Guaranteed payout, P500 lang ang puhunan",
                "I'll send first, then you transfer the rest",
            )

        cases.forEach { text -> assertEquals(text, WarningType.SCAM_CLAIM, typeOf(text)) }
    }

    @Test
    fun scamClaim_ignoresQuestionsAndDoubt() {
        val cases =
            listOf(
                "scam ba to? may nagtext sakin na nanalo daw ako",
                "Baka scam yan, wag mo i-send",
                "Legit ba yan? Magkano ang bayad",
                "Is this legit, 500 pesos lang daw",
                "Is it not a scam if they ask for a fee",
                "Legit kaya yung nag-aalok ng loan",
                "Not a scam? They want a deposit first",
            )

        cases.forEach { text -> assertNull(text, typeOf(text)) }
    }

    @Test
    fun scamClaim_ignoresReassuranceWithoutMoney() {
        val cases =
            listOf(
                "Legit yung shop na yan, nakabili na ako dati",
                "Promise, pupunta ako bukas",
                "Trust me, maganda yung pelikula",
                // "promos" is not a misspelled "promise".
                "Check out our new promos! Load P50 for 3 days",
                "Enjoy exclusive promos and earn points today",
            )

        cases.forEach { text -> assertNull(text, typeOf(text)) }
    }

    @Test
    fun scamClaim_doubtInAnotherSentenceDoesNotCancelTheClaim() {
        assertEquals(WarningType.SCAM_CLAIM, typeOf("Scam ba? Hindi ito scam. Padala ka lang ng 500 pesos"))
    }

    @Test
    fun scamClaim_comesAfterRiskyLinkAndBeforeMoneyRequest() {
        assertEquals(WarningType.RISKY_LINK, typeOf("Legit ito, claim your prize at https://bit.ly/abc"))
        assertEquals(WarningType.SCAM_CLAIM, typeOf("Legit ito promise, pahiram muna ng 500 kailangan ko agad"))
        assertEquals(WarningType.MONEY_REQUEST, typeOf("Pahiram muna ng 500 kailangan ko agad"))
    }

    @Test
    fun scamClaim_warningIsAmberWithItsEvidence() {
        val warning =
            checkNotNull(pipeline.inspect("Legit po ito 100%, mag-send lang kayo ng 300 processing fee").warning)

        assertEquals(Severity.AMBER, warning.severity)
        assertEquals("Insists it is not a scam (legit); talks about money (words: send, fee)", warning.evidence)
        assertEquals(
            "Be careful. Messages that say \"this is not a scam\" are often scams.",
            Fixtures.data().warnings.message(WarningType.SCAM_CLAIM, Language.ENGLISH, null),
        )
    }
}
