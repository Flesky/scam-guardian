@file:OptIn(ExperimentalSerializationApi::class)

package ph.scamguardian.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class Brand(
    val name: String,
    val aliases: List<String> = listOf(name.lowercase()),
    @JsonNames("officialDomains") val domains: List<String>,
    val contextWords: List<String> = emptyList(),
)

@Serializable
data class Keywords(
    val general: List<String> = emptyList(),
)

@Serializable
data class UrlRules(
    val shorteners: List<String> = emptyList(),
    val riskyTlds: List<String> = emptyList(),
)

@Serializable
data class Anchors(
    val scam: List<String> = emptyList(),
    val safe: List<String> = emptyList(),
)

/** The JSON text of each data file, as read from assets or test fixtures. */
data class PipelineJson(
    val brands: String,
    val keywords: String,
    val shortcuts: String,
    val urlRules: String,
    val warnings: String,
    val anchors: String,
) {
    fun parse(): PipelineData =
        PipelineData(
            brands = parseBrands(brands),
            keywords = json.decodeFromString(keywords),
            shortcuts = parseShortcuts(shortcuts),
            urlRules = json.decodeFromString(urlRules),
            warnings = parseWarnings(warnings),
            anchors = json.decodeFromString(anchors),
        )
}

data class PipelineData(
    val brands: List<Brand>,
    val keywords: Keywords,
    val shortcuts: Map<String, String>,
    val urlRules: UrlRules,
    val warnings: WarningCatalog,
    val anchors: Anchors,
)

fun parseBrands(text: String): List<Brand> = json.decodeFromString(text)

fun parseShortcuts(text: String): Map<String, String> = json.decodeFromString(text)

fun parseWarnings(text: String): WarningCatalog = WarningCatalog(json.decodeFromString<Map<String, WarningText>>(text))
