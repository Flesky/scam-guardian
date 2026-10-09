package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ScamPipelineTest {
    private val anchorsJson = Fixtures.text("anchors_test.json")
    private val anchors: Anchors = Fixtures.json.decodeFromString(anchorsJson)

    @Test
    fun check_ruleWarning_givesTheTypeSeverityBrandAndEvidence() {
        val warning = Fixtures.rulesOnlyPipeline().check("[BDO] Last chance to redeem. Click now:https://bdo-bd0.cc/ph")

        assertEquals(
            ScamWarning(
                type = WarningType.FAKE_LINK,
                severity = Severity.RED,
                brand = "BDO",
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
    fun check_hostSplitByAUnicodeDot_isNotTheOfficialDomain() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        assertNull(pipeline.check("GCash verify https://gcash.com/login"))
        assertEquals(WarningType.FAKE_LINK, pipeline.check("GCash verify https://gca\u3002sh.com/login")?.type)
        assertEquals(WarningType.FAKE_LINK, pipeline.check("GCash verify https://gca🙂sh.com/login")?.type)
    }

    @Test
    fun check_rulesRunEvenWhenThePrefilterWouldSkipTheMessage() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val cases =
            mapOf(
                "Please give passcode" to WarningType.OTP_REQUEST,
                "Please tell me the one time password" to WarningType.OTP_REQUEST,
                "Please s\u0435nd your \u041ETP to me" to WarningType.OTP_REQUEST,
                "Visit https://gcash-win.cc" to WarningType.FAKE_LINK,
            )

        cases.forEach { (text, expected) -> assertEquals(text, expected, pipeline.check(text)?.type) }
    }

    @Test
    fun check_punycodeLookAlikeHost_isAFakeLink() {
        val warning = Fixtures.rulesOnlyPipeline().check("Verify your account at https://xn--gcsh-63d.com/login")

        assertEquals(WarningType.FAKE_LINK, warning?.type)
        assertEquals("GCash name used in the link; link goes to xn--gcsh-63d.com", warning?.evidence)
    }

    @Test
    fun check_moneyRequestWithAnUnrelatedLink_stillWarns() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        assertEquals(WarningType.MONEY_REQUEST, pipeline.check("Pahiram naman kailangan ko agad")?.type)
        assertEquals(
            WarningType.MONEY_REQUEST,
            pipeline.check("Pahiram naman kailangan ko agad https://example.com")?.type,
        )
    }

    @Test
    fun check_ipAddressLink_isARiskyLink() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        assertEquals(WarningType.RISKY_LINK, pipeline.check("Claim your prize at http://11.22.33.44")?.type)
        assertEquals(WarningType.RISKY_LINK, pipeline.check("Claim your prize at 11.22.33.44/win")?.type)
    }

    @Test
    fun check_riskyLink_needsAGeneralKeywordToo() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        assertNull(pipeline.check("Tingnan mo ito https://bit.ly/abc"))
        assertNull(pipeline.check("Ito yung picture natin kahapon: tableph111.com"))
        assertEquals(WarningType.RISKY_LINK, pipeline.check("Tingnan mo ito, may prize https://bit.ly/abc")?.type)
    }

    @Test
    fun check_moneyRequest_acceptsAPromiseToPayBackAsPressure() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        val warning = pipeline.check("anak padalhan mo naman ako\n5kyaw babalik ko rin bukas\nsensya na.")

        assertEquals(WarningType.MONEY_REQUEST, warning?.type)
        assertEquals("Asks for money (padalhan) and promises to pay it back (babalik)", warning?.evidence)
        assertNull(pipeline.check("anak padalhan mo naman ako ng picture"))
        assertNull(pipeline.check("babalik ako bukas, hintayin mo ako"))
    }

    @Test
    fun inspect_ruleMatch_reportsTheRuleAndSkipsTheAi() {
        val embedder = FakeEmbedder()
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), embedder)
        val callsForAnchors = embedder.calls

        val report = pipeline.inspect("[BDO] Last chance to redeem. Click now:https://bdo-bd0.cc/ph")

        assertEquals(WarningType.FAKE_LINK, report.rule?.type)
        assertEquals(WarningType.FAKE_LINK, report.warning?.type)
        assertEquals(GateResult(true, listOf("link: bdo-bd0.cc", "keywords: last chance, redeem")), report.gate)
        assertNull(report.aiScore)
        assertEquals(0, report.modelCalls)
        assertEquals(callsForAnchors, embedder.calls)
    }

    @Test
    fun inspect_noRule_reportsTheAiScoresAndModelCalls() {
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), FakeEmbedder())

        val short = pipeline.inspect(anchors.scam.first())
        val long = pipeline.inspect("Balance mo sa load ay 5 pesos na lang. ".repeat(100))

        assertNull(short.rule)
        assertEquals(WarningType.AI_SCAM, short.warning?.type)
        assertEquals(1f, checkNotNull(short.aiScore).scam, 1e-5f)
        assertEquals(AiCheck.DEFAULT_THRESHOLD, short.aiThreshold)
        assertEquals(1, short.modelCalls)
        assertNull(long.warning)
        assertEquals(AiCheck.MAX_CHUNKS, long.modelCalls)
    }

    @Test
    fun inspect_messageThatFailsThePrefilter_saysWhy() {
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), FakeEmbedder())

        assertEquals(GateResult(false, listOf("no link, money or keywords")), pipeline.inspect("Kain na tayo").gate)
        assertEquals(GateResult(false, listOf("fewer than 2 words")), pipeline.inspect("ok").gate)
        assertEquals(0, pipeline.inspect("Kain na tayo").modelCalls)
    }

    @Test
    fun inspect_doesNotUseTheCache() {
        val embedder = FakeEmbedder()
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), embedder)

        pipeline.inspect(anchors.scam.first())
        val callsAfterFirst = embedder.calls
        pipeline.inspect(anchors.scam.first())

        assertEquals(callsAfterFirst + 1, embedder.calls)
    }

    @Test
    fun inspectNew_reportsATextOnlyTheFirstTime() {
        val embedder = FakeEmbedder()
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), embedder)

        val first = pipeline.inspectNew(anchors.scam.first())
        val callsAfterFirst = embedder.calls

        assertEquals(WarningType.AI_SCAM, first?.warning?.type)
        assertNull(pipeline.inspectNew(anchors.scam.first()))
        assertNull(pipeline.inspectNew("  " + anchors.scam.first() + " 🙂"))
        assertEquals(callsAfterFirst, embedder.calls)
        assertEquals(WarningType.AI_SCAM, pipeline.check(anchors.scam.first())?.type)
    }

    @Test
    fun inspectNew_textWithoutAWarning_isAlsoReportedOnce() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        assertNull(checkNotNull(pipeline.inspectNew("Kain na tayo")).warning)
        assertNull(pipeline.inspectNew("Kain na tayo"))
    }

    @Test
    fun inspectNew_sameWordsWithAnotherLink_isANewText() {
        val pipeline = Fixtures.rulesOnlyPipeline()

        assertEquals(WarningType.FAKE_LINK, pipeline.inspectNew("GCash verify https://gcash-win.cc")?.warning?.type)
        assertNull(checkNotNull(pipeline.inspectNew("GCash verify https://gcash.com")).warning)
    }

    @Test
    fun forget_letsInspectNewReportTheTextAgain() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val text = "[BDO] Last chance to redeem. Click now:https://bdo-bd0.cc/ph"
        pipeline.inspectNew(text)

        pipeline.forget(text)

        assertEquals(WarningType.FAKE_LINK, pipeline.inspectNew(text)?.warning?.type)
        assertNull(pipeline.inspectNew(text))
    }

    @Test
    fun markSafe_ruleWarning_neverWarnsForThatTextAgain() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val text = "[BDO] Last chance to redeem. Click now:https://bdo-bd0.cc/ph"
        assertEquals(WarningType.FAKE_LINK, pipeline.check(text)?.type)

        pipeline.markSafe(text)
        pipeline.forget(text)

        assertNull(pipeline.check(text))
        assertNull(pipeline.inspectNew(text))
        assertEquals(WarningType.FAKE_LINK, pipeline.check("[BDO] Click now:https://bdo-bd0.cc/ph")?.type)
    }

    @Test
    fun markSafe_aiWarning_addsASafeAnchor() {
        val pipeline = ScamPipeline(Fixtures.data(anchorsJson), FakeEmbedder())
        val text = anchors.scam.first()
        assertEquals(WarningType.AI_SCAM, pipeline.inspect(text).warning?.type)

        pipeline.markSafe(text)

        val report = pipeline.inspect(text)
        assertNull(report.warning)
        assertEquals(1f, checkNotNull(report.aiScore).safe, 1e-5f)
        assertEquals(WarningType.AI_SCAM, pipeline.inspect(anchors.scam.last()).warning?.type)
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
        assertEquals(Severity.AMBER, warning?.severity)
        assertNull(warning?.brand)
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
