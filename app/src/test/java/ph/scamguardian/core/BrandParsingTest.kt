package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandParsingTest {
    @Test
    fun parseBrands_acceptsOfficialDomainsAsTheDomainField() {
        val brands = parseBrands("""[{ "name": "BDO", "aliases": ["bdo"], "officialDomains": ["bdo.com.ph"] }]""")

        assertEquals(listOf("bdo.com.ph"), brands.single().domains)
    }

    @Test
    fun parseBrands_withoutContextWords_defaultsToEmpty() {
        val brands = parseBrands("""[{ "name": "BDO", "aliases": ["bdo"], "domains": ["bdo.com.ph"] }]""")

        assertEquals(emptyList<String>(), brands.single().contextWords)
    }

    @Test
    fun parseBrands_withoutAliases_usesTheLowercaseName() {
        val brands = parseBrands("""[{ "name": "GCash", "domains": ["gcash.com"] }]""")

        assertEquals(listOf("gcash"), brands.single().aliases)
    }

    @Test
    fun parseBrands_ignoresUnknownKeys() {
        val brands = parseBrands("""[{ "name": "BPI", "domains": ["bpi.com.ph"], "category": "bank" }]""")

        assertEquals("BPI", brands.single().name)
    }

    @Test
    fun parseBrands_readsTheTestFixture() {
        val names = parseBrands(Fixtures.text("brands_test.json")).map { it.name }

        assertEquals(listOf("BDO", "BPI", "GCash", "SSS", "Globe", "Smart"), names)
    }
}
