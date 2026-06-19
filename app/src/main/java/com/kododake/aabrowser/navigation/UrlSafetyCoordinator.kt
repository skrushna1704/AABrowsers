package com.kododake.aabrowser.navigation

import java.net.URI

/**
 * Centralizes URL normalization, redirect-chain tracking, and trust checks
 * shared by navigation, tab redirects, and bookmark import flows.
 */
class UrlSafetyCoordinator {

    private val trustedCleartextHosts = mutableSetOf("localhost", "127.0.0.1")
    private val redirectChain = ArrayDeque<String>()

    fun normalizeUrl(raw: String): String? {
        if (raw.isBlank()) {
            return null
        }
        val trimmed = raw.trim()
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        return try {
            val uri = URI(withScheme)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") {
                return null
            }
            val host = uri.host?.lowercase() ?: return null
            val path = uri.path.orEmpty().trimEnd('/')
            val query = uri.rawQuery?.let { "?$it" }.orEmpty()
            val fragment = uri.rawFragment?.let { "#$it" }.orEmpty()
            "$scheme://$host$path$query$fragment"
        } catch (_: Exception) {
            null
        }
    }

    fun isBlockedScheme(raw: String): Boolean {
        val lower = raw.trim().lowercase()
        return lower.startsWith("javascript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("file:") ||
            lower.startsWith("intent:")
    }

    fun registerTrustedCleartextHost(host: String) {
        val normalized = host.trim().lowercase().removePrefix("www.")
        if (normalized.isNotBlank()) {
            trustedCleartextHosts.add(normalized)
        }
    }

    fun isTrustedCleartextHost(host: String?): Boolean {
        if (host.isNullOrBlank()) {
            return false
        }
        val normalized = host.lowercase().removePrefix("www.")
        return trustedCleartextHosts.any { trusted ->
            normalized == trusted || normalized.endsWith(".$trusted")
        }
    }

    fun allowsCleartext(raw: String, userAllowedHost: Boolean): Boolean {
        val normalized = normalizeUrl(raw) ?: return false
        if (normalized.startsWith("https://")) {
            return true
        }
        if (!normalized.startsWith("http://")) {
            return false
        }
        if (userAllowedHost) {
            return true
        }
        return isTrustedCleartextHost(URI(normalized).host)
    }

    fun recordRedirect(fromUrl: String, toUrl: String) {
        val from = normalizeUrl(fromUrl) ?: fromUrl
        val to = normalizeUrl(toUrl) ?: toUrl
        redirectChain.addLast(from)
        redirectChain.addLast(to)
        while (redirectChain.size > MAX_REDIRECT_CHAIN_ENTRIES) {
            redirectChain.removeFirst()
        }
    }

    fun redirectHopCount(): Int = redirectChain.size / 2

    fun exceedsRedirectLimit(): Boolean = redirectHopCount() > MAX_REDIRECT_DEPTH

    fun clearRedirectChain() {
        redirectChain.clear()
    }

    fun extractHttpUrlsFromText(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }
        val pattern = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        val seen = linkedSetOf<String>()
        pattern.findAll(text).forEach { match ->
            val candidate = match.value.trim().trimEnd('.', ',', ';', ')')
            val normalized = normalizeUrl(candidate) ?: return@forEach
            if (!isBlockedScheme(normalized)) {
                seen.add(normalized)
            }
        }
        return seen.toList()
    }

    fun deduplicateByNormalized(urls: List<String>): List<String> {
        val result = linkedSetOf<String>()
        urls.forEach { raw ->
            val normalized = normalizeUrl(raw) ?: return@forEach
            if (!isBlockedScheme(normalized)) {
                result.add(normalized)
            }
        }
        return result.toList()
    }

    companion object {
        const val MAX_REDIRECT_DEPTH = 5
        private const val MAX_REDIRECT_CHAIN_ENTRIES = MAX_REDIRECT_DEPTH * 2
    }
}
