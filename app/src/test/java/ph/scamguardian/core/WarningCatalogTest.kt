package ph.scamguardian.core

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class WarningCatalogTest {
    private val catalog = parseWarnings(Fixtures.text("warnings_test.json"))

    @Test
    fun severity_isRedForSureScamsAndAmberForPossibleScams() {
        assertEquals(
            mapOf(
                WarningType.FAKE_LINK to Severity.RED,
                WarningType.OTP_REQUEST to Severity.RED,
                WarningType.RISKY_LINK to Severity.AMBER,
                WarningType.MONEY_REQUEST to Severity.AMBER,
                WarningType.AI_SCAM to Severity.AMBER,
            ),
            WarningType.entries.associateWith(catalog::severity),
        )
    }

    @Test
    fun title_followsTheSeverity() {
        WarningType.entries.forEach { type ->
            val expected = if (catalog.severity(type) == Severity.RED) "Scam detected" else "Possible scam detected"
            assertEquals(type.key, expected, catalog.title(type))
        }
    }

    @Test
    fun message_fillsTheBrandInBothLanguages() {
        assertEquals(
            "Hindi ito ang tunay na link ng BDO. Huwag magbigay ng OTP o personal na impormasyon.",
            catalog.message(WarningType.FAKE_LINK, Language.FILIPINO, "BDO"),
        )
        assertEquals(
            "Not a real link of BDO. Do not give your OTP or personal info.",
            catalog.message(WarningType.FAKE_LINK, Language.ENGLISH, "BDO"),
        )
    }

    @Test
    fun brandRange_pointsAtTheBrandInTheMessage() {
        Language.entries.forEach { language ->
            val message = catalog.message(WarningType.FAKE_LINK, language, "BDO Unibank")
            val range = checkNotNull(catalog.brandRange(WarningType.FAKE_LINK, language, "BDO Unibank"))

            assertEquals("BDO Unibank", message.substring(range))
        }
    }

    @Test
    fun brandRange_withoutABrandOrAPlaceholder_isNull() {
        assertNull(catalog.brandRange(WarningType.FAKE_LINK, Language.ENGLISH, null))
        assertNull(catalog.brandRange(WarningType.FAKE_LINK, Language.ENGLISH, ""))
        assertNull(catalog.brandRange(WarningType.OTP_REQUEST, Language.ENGLISH, "BDO"))
    }

    @Test
    fun message_withoutABrand_usesTheSelectedLanguage() {
        assertEquals(
            "Huwag ibigay ang OTP. Hindi kailanman manghihingi ng OTP ang mga bangko o kumpanya.",
            catalog.message(WarningType.OTP_REQUEST, Language.FILIPINO),
        )
        assertEquals(
            "Do not give your OTP. Banks and companies will never ask for your OTP.",
            catalog.message(WarningType.OTP_REQUEST, Language.ENGLISH),
        )
        assertEquals(
            "Mag-ingat. Kahina-hinala ang link na ito.",
            catalog.message(WarningType.RISKY_LINK, Language.FILIPINO),
        )
        assertEquals(
            "Call the person first and make sure it is really them before you send money.",
            catalog.message(WarningType.MONEY_REQUEST, Language.ENGLISH),
        )
        assertEquals(
            "Do not send money, OTP, or personal information.",
            catalog.message(WarningType.AI_SCAM, Language.ENGLISH),
        )
    }

    @Test
    fun message_neverLeavesThePlaceholder() {
        WarningType.entries.forEach { type ->
            Language.entries.forEach { language ->
                assertFalse(catalog.message(type, language).contains(WarningCatalog.BRAND_PLACEHOLDER))
            }
        }
    }

    @Test
    fun parseWarnings_aMissingType_fails() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                parseWarnings(
                    """{ "fake_link": { "severity": "red", "title": "T", "message": { "fil": "F", "en": "E" } } }""",
                )
            }

        assertEquals("No warning text for otp_request", error.message)
    }

    @Test
    fun parseWarnings_theOldFormatOrAnUnknownSeverity_fails() {
        assertThrows(SerializationException::class.java) {
            parseWarnings("""{ "fake_link": { "title": "T", "message": "M" } }""")
        }
        assertThrows(SerializationException::class.java) {
            parseWarnings(
                """{ "fake_link": { "severity": "green", "title": "T", "message": { "fil": "F", "en": "E" } } }""",
            )
        }
    }

    @Test
    fun fixture_hasExactlyTheWarningTypes() {
        val keys = Fixtures.json.decodeFromString<Map<String, WarningText>>(Fixtures.text("warnings_test.json")).keys

        assertEquals(WarningType.entries.map { it.key }.toSet(), keys)
    }

    @Test
    fun language_fromCode_defaultsToFilipino() {
        assertEquals(Language.FILIPINO, Language.DEFAULT)
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.FILIPINO, Language.fromCode("fil"))
        assertEquals(Language.FILIPINO, Language.fromCode(null))
        assertEquals(Language.FILIPINO, Language.fromCode("xx"))
    }
}
