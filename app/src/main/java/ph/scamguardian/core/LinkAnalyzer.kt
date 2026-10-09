package ph.scamguardian.core

import com.google.common.net.InetAddresses
import com.google.common.net.InternetDomainName
import com.ibm.icu.text.IDNA
import com.linkedin.urls.Url
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions

data class Link(
    val url: String,
    /** The canonical host: lowercase ASCII, with international names in Punycode. */
    val host: String,
    val registrableDomain: String,
    val tld: String,
    val shortener: Boolean,
    val riskyTld: Boolean,
    val numericHost: Boolean,
    /** The host as a person sees it, with Punycode labels decoded. Used to spot imitated names. */
    val unicodeHost: String = host,
    /** Names of the catalog brands that own this link's domain. */
    val owners: List<String> = emptyList(),
    val official: Boolean = false,
) {
    val risky: Boolean get() = shortener || riskyTld || numericHost

    /** True when the link's host is one of [domains] or a subdomain of one. */
    fun isOn(domains: List<String>): Boolean =
        domains.any { domain -> host == domain.lowercase() || host.endsWith(".${domain.lowercase()}") }
}

/**
 * Finds links in text, with or without a scheme, and flags the risky ones. Pass text from
 * [Sanitizer.prepare]: cleaning it further first could join pieces of text into a different host.
 */
class LinkAnalyzer(
    rules: UrlRules,
    private val catalog: List<Brand> = emptyList(),
) {
    private val shorteners = rules.shorteners.map(String::lowercase).toSet()
    private val riskyTlds = rules.riskyTlds.map(String::lowercase).toSet()
    private val idna =
        IDNA.getUTS46Instance(
            IDNA.NONTRANSITIONAL_TO_ASCII or IDNA.NONTRANSITIONAL_TO_UNICODE or IDNA.CHECK_BIDI or IDNA.CHECK_CONTEXTJ,
        )

    /**
     * Links in [text]. Each link knows which catalog brands own its domain. It is official when one of
     * its owners is among the brands [named] in the message, or when the message names no brand at all.
     */
    fun analyze(
        text: String,
        named: List<Brand> = emptyList(),
    ): List<Link> =
        detect(text)
            .distinctBy { it.first.originalUrl }
            .map { (url, host) -> toLink(url, host, named) }

    /** Returns [text] with every link replaced by [replacement]. */
    fun replaceLinks(
        text: String,
        replacement: String,
    ): String =
        detect(text).fold(separateSchemes(text)) { result, (url, _) -> result.replace(url.originalUrl, replacement) }

    private fun detect(text: String): List<Pair<Url, Host>> =
        UrlDetector(separateSchemes(text), UrlDetectorOptions.Default)
            .detect()
            .mapNotNull { url -> hostOf(url)?.let { url to it } }
            .filter { (url, host) -> isLink(url, host) }

    // "Click now:https://..." has no space before the scheme; the detector needs one.
    private fun separateSchemes(text: String): String = gluedScheme.replace(text, " $1")

    private fun isLink(
        url: Url,
        host: Host,
    ): Boolean =
        when {
            "://" in url.originalUrl -> true

            '@' in url.originalUrl -> false

            host.isAddress -> host.labels.size == IPV4_PARTS

            // Without a scheme, only accept hosts under a known suffix, so "Mr.Juan" or "2.7" are not links.
            else -> InternetDomainName.isValid(host.ascii) && InternetDomainName.from(host.ascii).isUnderPublicSuffix
        }

    private fun toLink(
        url: Url,
        host: Host,
        named: List<Brand>,
    ): Link {
        val tld = if (host.isAddress) "" else host.labels.last()
        val link =
            Link(
                url = url.originalUrl,
                host = host.ascii,
                registrableDomain = registrableDomain(host),
                tld = tld,
                shortener = false,
                riskyTld = tld in riskyTlds,
                numericHost =
                    host.isAddress ||
                        host.labels.any { it.count(Char::isDigit) >= MIN_DIGITS || wwwVariant.containsMatchIn(it) },
                unicodeHost = host.unicode,
            )
        // A host that is not a valid international name cannot belong to anyone.
        val owners = if (host.valid) catalog.filter { link.isOn(it.domains) }.map { it.name } else emptyList()
        return link.copy(
            shortener = link.registrableDomain in shorteners,
            owners = owners,
            official = owners.isNotEmpty() && (named.isEmpty() || named.any { it.name in owners }),
        )
    }

    private fun hostOf(url: Url): Host? {
        val raw =
            url.host
                .orEmpty()
                .trim('.')
                .lowercase()
        return when {
            raw.isEmpty() -> null
            isAddress(raw) -> Host(ascii = raw, unicode = raw, valid = true, isAddress = true)
            else -> domainHost(raw)
        }
    }

    private fun isAddress(host: String): Boolean =
        InetAddresses.isInetAddress(host.removeSurrounding("[", "]")) || host.split('.').all(addressPart::matches)

    private fun domainHost(raw: String): Host {
        val info = IDNA.Info()
        val ascii = idna.nameToASCII(raw, StringBuilder(), info).toString().lowercase()
        val valid = !info.hasErrors() && ascii.isNotEmpty()
        return if (valid) {
            Host(ascii, idna.nameToUnicode(ascii, StringBuilder(), IDNA.Info()).toString().lowercase(), valid = true)
        } else {
            Host(ascii = raw, unicode = raw, valid = false)
        }
    }

    private fun registrableDomain(host: Host): String {
        if (host.isAddress) return host.ascii
        val known =
            if (InternetDomainName.isValid(host.ascii)) {
                InternetDomainName.from(host.ascii).takeIf { it.isUnderPublicSuffix }
            } else {
                null
            }
        return known?.topPrivateDomain()?.toString() ?: host.labels.takeLast(2).joinToString(".")
    }

    private class Host(
        val ascii: String,
        val unicode: String,
        val valid: Boolean,
        /** An IP address in any spelling browsers accept: dotted, decimal or hexadecimal. */
        val isAddress: Boolean = false,
    ) {
        val labels: List<String> = ascii.split('.')
    }

    private companion object {
        const val MIN_DIGITS = 3
        const val IPV4_PARTS = 4
        val gluedScheme = Regex("(?<=\\S)(https?://)", RegexOption.IGNORE_CASE)
        val wwwVariant = Regex("^www[\\d-]")
        val addressPart = Regex("0x[0-9a-f]+|\\d+")
    }
}
