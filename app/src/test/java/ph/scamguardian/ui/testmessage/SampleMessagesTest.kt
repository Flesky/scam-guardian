package ph.scamguardian.ui.testmessage

import org.junit.Assert.assertEquals
import org.junit.Test
import ph.scamguardian.core.Fixtures
import ph.scamguardian.core.GateResult
import ph.scamguardian.core.Language
import ph.scamguardian.core.PipelineReport

class SampleMessagesTest {
    private val warnings = Fixtures.data().warnings

    private fun rows(
        result: CheckResult,
        language: Language = Language.FILIPINO,
    ): Map<String, String> = CheckResultFormatter.rows(result, warnings, language).associate { it.label to it.value }

    @Test
    fun sampleMessages_getTheExpectedRuleResult() {
        val pipeline = Fixtures.rulesOnlyPipeline()
        val expected = listOf("fake_link", "otp_request", "money_request", null, "money_request")

        assertEquals(
            expected,
            sampleMessages.map {
                pipeline
                    .inspect(it.text)
                    .rule
                    ?.type
                    ?.key
            },
        )
    }

    @Test
    fun rows_ruleMatch_showsTheWarningAndSkipsTheAi() {
        val report = Fixtures.rulesOnlyPipeline().inspect(sampleMessages.first().text)

        val rows = rows(CheckResult(report, totalMs = 12))

        assertEquals("AI skipped: a rule matched", rows["AI gate (prefilter)"])
        assertEquals("fake_link", rows["Rule result"])
        assertEquals("AI not run", rows["AI scores"])
        assertEquals(
            "Scam detected\nHindi ito ang tunay na link ng BDO. Huwag magbigay ng OTP o personal na impormasyon.",
            rows["Final result"],
        )
        assertEquals("BDO mentioned\nlink goes to bdo-bd0.cc", rows["Evidence"])
        assertEquals("0", rows["Model calls"])
        assertEquals("12 ms", rows["Total time"])
    }

    @Test
    fun rows_english_showsTheWarningInEnglish() {
        val fakeLink = Fixtures.rulesOnlyPipeline().inspect(sampleMessages.first().text)
        val moneyRequest = Fixtures.rulesOnlyPipeline().inspect("Pahiram naman, kailangan ko lang agad")

        assertEquals(
            "Scam detected\nNot a real link of BDO. Do not give your OTP or personal info.",
            rows(CheckResult(fakeLink, totalMs = 1), Language.ENGLISH)["Final result"],
        )
        assertEquals(
            "Possible scam detected\n" +
                "Call the person first and make sure it is really them before you send money.",
            rows(CheckResult(moneyRequest, totalMs = 1), Language.ENGLISH)["Final result"],
        )
    }

    @Test
    fun rows_noWarning_showsNoWarningAndWhyTheAiWasSkipped() {
        val report =
            PipelineReport(
                warning = null,
                rule = null,
                gate = GateResult(false, listOf("no link, money or keywords")),
                aiScore = null,
                aiThreshold = 0.6f,
                modelCalls = 0,
            )

        val rows = rows(CheckResult(report, totalMs = 1))

        assertEquals("AI skipped: no link, money or keywords", rows["AI gate (prefilter)"])
        assertEquals("none", rows["Rule result"])
        assertEquals("No warning", rows["Final result"])
        assertEquals("none", rows["Evidence"])
    }

    @Test
    fun rows_aiRun_showsScoresWithTwoDecimals() {
        val report =
            PipelineReport(
                warning = null,
                rule = null,
                gate = GateResult(true, listOf("money: p500", "keywords: bayad")),
                aiScore = ph.scamguardian.core.AiScore(scam = 0.4567f, safe = 0.5f),
                aiThreshold = 0.6f,
                modelCalls = 2,
            )

        val rows = rows(CheckResult(report, totalMs = 140))

        assertEquals("AI run: money: p500; keywords: bayad", rows["AI gate (prefilter)"])
        assertEquals("scam 0.46, safe 0.50, threshold 0.60", rows["AI scores"])
        assertEquals("2", rows["Model calls"])
    }
}
