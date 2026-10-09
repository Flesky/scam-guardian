package ph.scamguardian.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How sure a warning is: red for "Scam detected", amber for "Possible scam detected". */
@Serializable
enum class Severity {
    @SerialName("red")
    RED,

    @SerialName("amber")
    AMBER,
}

/** The language of the warning messages. [code] is the key in warnings.json and the saved setting. */
enum class Language(
    val code: String,
) {
    FILIPINO("fil"),
    ENGLISH("en"),
    ;

    companion object {
        val DEFAULT = FILIPINO

        /** The language with this [code], or the default for null and unknown codes. */
        fun fromCode(code: String?): Language = entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}

@Serializable
data class WarningMessage(
    val fil: String,
    val en: String,
) {
    fun inLanguage(language: Language): String =
        when (language) {
            Language.FILIPINO -> fil
            Language.ENGLISH -> en
        }
}

/** One entry of warnings.json. The message may contain the placeholder {brand}. */
@Serializable
data class WarningText(
    val severity: Severity,
    val title: String,
    val message: WarningMessage,
)

/** The text and severity of every warning type. Fails at creation if a type has no entry. */
class WarningCatalog(
    texts: Map<String, WarningText>,
) {
    private val texts =
        WarningType.entries.associateWith { type ->
            requireNotNull(texts[type.key]) { "No warning text for ${type.key}" }
        }

    fun severity(type: WarningType): Severity = texts.getValue(type).severity

    /** The title is the same in both languages. */
    fun title(type: WarningType): String = texts.getValue(type).title

    /** The message in [language], with [brand] in place of {brand}. */
    fun message(
        type: WarningType,
        language: Language,
        brand: String? = null,
    ): String =
        texts
            .getValue(type)
            .message
            .inLanguage(language)
            .replace(BRAND_PLACEHOLDER, brand.orEmpty())

    companion object {
        const val BRAND_PLACEHOLDER = "{brand}"
    }
}
