package ph.scamguardian.core

import com.google.common.net.InternetDomainName
import com.linkedin.urls.Url
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions

data class Link(
    val url: String,
    val host: String,
    val registrableDomain: String,
    val tld: String,
    val shortener: Boolean,
    val riskyTld: Boolean,
    val numericHost: Boolean,
    /** Names of the catalog brands that own this link's domain. */
    val owners: List<String> = emptyList(),
    val official: Boolean = false,
) {
    val risky: Boolean get() = shortener || riskyTld || numericHost

    /** True when the link's host is one of [domains] or a subdomain of one. */
    fun isOn(domains: List<String>): Boolean =
        domains.any { domain -> host == domain.lowercase() || host.endsWith(".${domain.lowercase()}") }
}

/** Finds links in text, with or without a scheme, and flags the risky ones. */
class LinkAnalyzer(
    rules: UrlRules,
    private val catalog: List<Brand> = emptyList(),
) {
    private val shorteners = rules.shorteners.map(String::lowercase).toSet()
    private val riskyTlds = rules.riskyTlds.map(String::lowercase).toSet()

    /**
     * Links in [text]. Each link knows which catalog brands own its domain. It is official when one of
     * its owners is among the brands [named] in the message, or when the message names no brand at all.
     */
    fun analyze(
        text: String,
        named: List<Brand> = emptyList(),
    ): List<Link> =
        detect(text)
            .distinctBy { it.originalUrl }
            .map { url -> toLink(url, named) }

    /** Returns [text] with every link replaced by [replacement]. */
    fun replaceLinks(
        text: String,
        replacement: String,
    ): String = detect(text).fold(separateSchemes(text)) { result, url -> result.replace(url.originalUrl, replacement) }

    private fun detect(text: String): List<Url> =
        UrlDetector(separateSchemes(text), UrlDetectorOptions.Default).detect().filter(::isLink)

    // "Click now:https://..." has no space before the scheme; the detector needs one.
    private fun separateSchemes(text: String): String = gluedScheme.replace(text, " $1")

    private fun isLink(url: Url): Boolean {
        val host = hostOf(url)
        val hasScheme = "://" in url.originalUrl
        return when {
            host.isEmpty() -> false

            hasScheme -> true

            '@' in url.originalUrl -> false

            // Without a scheme, only accept hosts under a known suffix, so "Mr.Juan" or "2.7" are not links.
            else -> InternetDomainName.isValid(host) && InternetDomainName.from(host).isUnderPublicSuffix
        }
    }

    private fun toLink(
        url: Url,
        named: List<Brand>,
    ): Link {
        val host = hostOf(url)
        val labels = host.split('.')
        val link =
            Link(
                url = url.originalUrl,
                host = host,
                registrableDomain = registrableDomain(host, labels),
                tld = labels.last(),
                shortener = false,
                riskyTld = labels.last() in riskyTlds,
                numericHost =
                    labels.any { label ->
                        label.count(Char::isDigit) >= MIN_DIGITS ||
                            wwwVariant.containsMatchIn(label)
                    },
            )
        val owners = catalog.filter { link.isOn(it.domains) }.map { it.name }
        return link.copy(
            shortener = link.registrableDomain in shorteners,
            owners = owners,
            official = owners.isNotEmpty() && (named.isEmpty() || named.any { it.name in owners }),
        )
    }

    private fun hostOf(url: Url): String =
        url.host
            .orEmpty()
            .lowercase()
            .trim('.')

    private fun registrableDomain(
        host: String,
        labels: List<String>,
    ): String {
        val known =
            if (InternetDomainName.isValid(
                    host,
                )
            ) {
                InternetDomainName.from(host).takeIf { it.isUnderPublicSuffix }
            } else {
                null
            }
        return known?.topPrivateDomain()?.toString() ?: labels.takeLast(2).joinToString(".")
    }

    private companion object {
        const val MIN_DIGITS = 3
        val gluedScheme = Regex("(?<=\\S)(https?://)", RegexOption.IGNORE_CASE)
        val wwwVariant = Regex("^www[\\d-]")
    }
}
