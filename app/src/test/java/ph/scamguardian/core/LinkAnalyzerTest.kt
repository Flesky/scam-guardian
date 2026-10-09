package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkAnalyzerTest {
    private val analyzer = Fixtures.linkAnalyzer()

    private data class Expected(
        val host: String,
        val registrableDomain: String,
        val shortener: Boolean = false,
        val riskyTld: Boolean = false,
        val numericHost: Boolean = false,
    )

    @Test
    fun analyze_findsAndFlagsEachSampleLink() {
        val cases =
            mapOf(
                "Please visit https://bpivipe.com/ph redeem your rewards!" to Expected("bpivipe.com", "bpivipe.com"),
                "favorite gift: https://www-bpia.com/" to Expected("www-bpia.com", "www-bpia.com", numericHost = true),
                "Click now:https://bdo-bd0.cc/ph" to Expected("bdo-bd0.cc", "bdo-bd0.cc", riskyTld = true),
                "Visit https://bdo-t2u.cc/ph to redeem your points." to
                    Expected("bdo-t2u.cc", "bdo-t2u.cc", riskyTld = true),
                "channel:https://cashs-bdo.is Redemption code: FSGNN-8878" to
                    Expected("cashs-bdo.is", "cashs-bdo.is", riskyTld = true),
                "log in again to https://onlinebdo.icu/ to complete" to
                    Expected("onlinebdo.icu", "onlinebdo.icu", riskyTld = true),
                "Join now: https://new.gcashoz-ph.com Thank you!" to Expected("new.gcashoz-ph.com", "gcashoz-ph.com"),
                "update via https://bit.ly/Aug-SSS" to Expected("bit.ly", "bit.ly", shortener = true),
                "losing your points!https://smart.txlgv.icu/ph" to
                    Expected("smart.txlgv.icu", "txlgv.icu", riskyTld = true),
                "favorite gift: https://globece-ph.com" to Expected("globece-ph.com", "globece-ph.com"),
                "Good Luck: tableph111.com" to Expected("tableph111.com", "tableph111.com", numericHost = true),
                "Good Luck: luckysaya.com" to Expected("luckysaya.com", "luckysaya.com"),
                "Good luck and enjoy: tbplus99.com" to Expected("tbplus99.com", "tbplus99.com"),
                "handog na P888: www005.eightvvvipb.mx" to
                    Expected("www005.eightvvvipb.mx", "eightvvvipb.mx", riskyTld = true, numericHost = true),
                "initial bonus. https://evojili.bio/l26" to Expected("evojili.bio", "evojili.bio", riskyTld = true),
                "na bonus >> https://llsms14.phtala.site" to
                    Expected("llsms14.phtala.site", "phtala.site", riskyTld = true),
                "na bonus >>> http://122101.22jl03.vip" to
                    Expected("122101.22jl03.vip", "22jl03.vip", riskyTld = true, numericHost = true),
                "P777 na bonus >>> 121501.22jl03.vip" to
                    Expected("121501.22jl03.vip", "22jl03.vip", riskyTld = true, numericHost = true),
                "Play or withdraw: https://bit.ly/3DSGi5T" to Expected("bit.ly", "bit.ly", shortener = true),
            )

        cases.forEach { (text, expected) ->
            val link = analyzer.analyze(text).single()
            val actual = Expected(link.host, link.registrableDomain, link.shortener, link.riskyTld, link.numericHost)
            assertEquals(text, expected, actual)
            assertEquals(text, expected.host.substringAfterLast('.'), link.tld)
        }
    }

    @Test
    fun analyze_ignoresTextThatOnlyLooksLikeALink() {
        val cases =
            listOf(
                "Big winner of 865.3K today",
                "2.7 percent rebate everyday",
                "You have received P4700.00 from Juan",
                "Salamat Mr.Juan sa tulong",
                "email me at juan@example.com",
                "10:06 to 11:32",
            )

        cases.forEach { text -> assertEquals(text, emptyList<Link>(), analyzer.analyze(text)) }
    }

    @Test
    fun analyze_marksLinksOnADetectedBrandsDomainAsOfficial() {
        val bdo = Fixtures.data().brands.first { it.name == "BDO" }

        assertTrue(analyzer.analyze("Log in at https://online.bdo.com.ph/login", listOf(bdo)).single().official)
        assertTrue(analyzer.analyze("Visit bdo.com.ph today", listOf(bdo)).single().official)
        assertFalse(analyzer.analyze("Visit https://bdo.com.ph.evil.cc", listOf(bdo)).single().official)
        assertFalse(analyzer.analyze("Log in at https://online.bdo.com.ph/login").single().official)
    }

    @Test
    fun replaceLinks_swapsEveryLinkForThePlaceholder() {
        val text = "Click now:https://bdo-bd0.cc/ph or luckysaya.com"

        assertEquals("Click now: [link] or [link]", analyzer.replaceLinks(text, "[link]"))
    }
}
