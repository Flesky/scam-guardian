package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ScamPipelineTest {
    private val anchorsJson = Fixtures.text("anchors_test.json")
    private val anchors: Anchors = Fixtures.json.decodeFromString(anchorsJson)

    @Test
    fun check_ruleWarning_fillsTheBrandAndGivesEvidence() {
        val warning = Fixtures.rulesOnlyPipeline().check("[BDO] Last chance to redeem. Click now:https://bdo-bd0.cc/ph")

        assertEquals(
            ScamWarning(
                type = WarningType.FAKE_LINK,
                title = "Scam detected",
                message = "Hindi ito tunay na link ng BDO. Huwag maglagay ng OTP o personal na impormasyon.",
                evidence = "BDO mentioned; link goes to bdo-bd0.cc",
            ),
            warning,
        )
    }

    @Test
    fun check_brandOnlyInTheLink_namesThatBrand() {
        val warning = Fixtures.rulesOnlyPipeline().check("Earn up to ₱10,000. Join now: https://new.gcashoz-ph.com")

        assertEquals(WarningType.FAKE_LINK, warning?.type)
        assertEquals("GCash name used in the link; link goes to gcashoz-ph.com", warning?.evidence)
    }

    @Test
    fun check_officialBrandLink_givesNoWarning() {
        assertNull(
            Fixtures.rulesOnlyPipeline().check("BDO: view your rewards points at https://www.bdo.com.ph/rewards"),
        )
    }

    @Test
    fun check_linkOwnedByAnotherBrandThanTheOneNamed_isAFakeLink() {
        val pipeline = ScamPipeline(Fixtures.data(brands = "brands_ownership_test.json"), FakeEmbedder())

        val warning = pipeline.check("Please verify your BPI account at https://docs.google.com/forms/d/attacker")

        assertEquals(WarningType.FAKE_LINK, warning?.type)
        assertEquals("BPI mentioned; link goes to google.com", warning?.evidence)
    }

    @Test
    fun check_linkOnACatalogDomain_isNotAnImitationOfAnotherBrand() {
        val pipeline = ScamPipeline(Fixtures.data(brands = "brands_ownership_test.json"), FakeEmbedder())
        val cases =
            listOf(
                "Please visit https://tiktokglobalshop.com to update your account",
                "TikTok Shop: Please visit https://tiktokglobalshop.com to update your account",
                "Open the form at https://docs.google.com/forms/d/abc to update your account",
                "Google: review your account at https://docs.google.com/settings",
                "BPI and Google Pay: verify your account at https://docs.google.com/help",
            )

        cases.forEach { text -> assertNull(text, pipeline.check(text)) }
    }

    @Test
    fun check_otpRequest_firesOnRequestsEvenNextToNumbersOrWarnings() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val cases =
            listOf(
                "Please send your OTP to 12345",
                "Your OTP is required. Please send it to our support agent.",
                "Please send the code we sent you. Do not share it with anyone else.",
                "Paki-send po ng verification code, 5 minutes lang po ito",
            )

        cases.forEach { text -> assertEquals(text, WarningType.OTP_REQUEST, pipeline.check(text)?.type) }
    }

    @Test
    fun check_otpRequest_ignoresCodeDeliveryAndWarnings() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val cases =
            listOf(
                "Use code 482913 to log in. Never share it with anyone.",
                "482913 is your BDO OTP. Do not give it to anyone, even bank staff.",
                "Not you? Don't enter your OTP on any site or send it to anyone. Your OTP is 204816.",
                "Your code: 7719. Huwag ibigay ang code kahit kanino.",
            )

        cases.forEach { text -> assertNull(text, pipeline.check(text)) }
    }

    @Test
    fun check_rulesAreTriedInOrder() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val cases =
            mapOf(
                "GCash: send the verification code to claim your bonus at https://gcash-win.cc" to
                    WarningType.FAKE_LINK,
                "Paki-send ng code para sa bonus at jackpot, P500 agad" to WarningType.OTP_REQUEST,
                "Claim your prize now at https://bit.ly/abc" to WarningType.RISKY_LINK,
                "Pahiram naman, kailangan ko lang agad" to WarningType.MONEY_REQUEST,
            )

        cases.forEach { (text, expected) -> assertEquals(text, expected, pipeline.check(text)?.type) }
    }

    @Test
    fun check_messageLikeAScamAnchor_givesAiWarning() {
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), FakeEmbedder())

        val warning = pipeline.check(anchors.scam.first())

        assertEquals(WarningType.AI_SCAM, warning?.type)
        assertEquals("Mukhang scam", warning?.title)
        assertEquals("Similar to known scam messages (score 1.00)", warning?.evidence)
    }

    @Test
    fun check_unrelatedMessage_givesNoAiWarning() {
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), FakeEmbedder())

        assertNull(pipeline.check("Balance mo sa load ay 5 pesos na lang"))
    }

    @Test
    fun check_messageThatFailsThePrefilter_skipsTheAiCheck() {
        val embedder = FakeEmbedder()
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), embedder)
        val callsForAnchors = embedder.calls

        assertNull(pipeline.check("Kain na tayo mamaya, libre ko"))
        assertEquals(callsForAnchors, embedder.calls)
    }

    @Test
    fun check_repeatedMessage_usesTheCache() {
        val embedder = FakeEmbedder()
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), embedder)

        val first = pipeline.check(anchors.scam.first())
        val callsAfterFirst = embedder.calls
        val second = pipeline.check("  ${anchors.scam.first()}​ ")

        assertSame(first, second)
        assertEquals(callsAfterFirst, embedder.calls)
    }
}
