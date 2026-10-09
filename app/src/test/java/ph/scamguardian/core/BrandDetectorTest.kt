package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandDetectorTest {
    private val detector = Fixtures.brandDetector()

    @Test
    fun inDomain_findsTheBrandInEachSampleHost() {
        val cases =
            mapOf(
                "bpivipe.com" to listOf("BPI"),
                "www-bpia.com" to listOf("BPI"),
                "bdo-bd0.cc" to listOf("BDO"),
                "bdo-t2u.cc" to listOf("BDO"),
                "cashs-bdo.is" to listOf("BDO"),
                "onlinebdo.icu" to listOf("BDO"),
                "new.gcashoz-ph.com" to listOf("GCash"),
                "smart.txlgv.icu" to listOf("Smart"),
                "globece-ph.com" to listOf("Globe"),
                "bd0-rewards.com" to listOf("BDO"),
                "g-cash.help" to listOf("GCash"),
                "bit.ly" to emptyList(),
                "tableph111.com" to emptyList(),
                "luckysaya.com" to emptyList(),
                "tbplus99.com" to emptyList(),
                "www005.eightvvvipb.mx" to emptyList(),
                "evojili.bio" to emptyList(),
                "llsms14.phtala.site" to emptyList(),
                "122101.22jl03.vip" to emptyList(),
                "121501.22jl03.vip" to emptyList(),
            )

        cases.forEach { (host, expected) -> assertEquals(host, expected, detector.inDomain(host).map { it.name }) }
    }

    @Test
    fun inText_findsBrandsAsWholeWords() {
        val cases =
            mapOf(
                "[BDO] Last Chance!" to listOf("BDO"),
                "BPl reminds you" to listOf("BPI"),
                "your bd0 account" to listOf("BDO"),
                "gcаsh verify" to listOf("GCash"),
                "Banco de Oro advisory" to listOf("BDO"),
                "send to my G-Cash" to listOf("GCash"),
                "from SSS plan via GCash" to listOf("GCash", "SSS"),
                "Smart promo: free load" to listOf("Smart"),
                "smart kid si Juan, top 1 sa klase" to emptyList(),
                "the globe is round" to emptyList(),
                "bdohan mo na" to emptyList(),
            )

        cases.forEach { (text, expected) -> assertEquals(text, expected, detector.inText(text).map { it.name }) }
    }

    @Test
    fun detect_reportsWhereTheBrandWasFound() {
        val matches = detector.detect("Earn up to 10,000. Join now", listOf("new.gcashoz-ph.com"))

        assertEquals(
            listOf(BrandMatch(matches.single().brand, inText = false, hosts = listOf("new.gcashoz-ph.com"))),
            matches,
        )
        assertEquals("GCash", matches.single().brand.name)
    }
}
