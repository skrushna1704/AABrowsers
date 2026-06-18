package com.kododake.aabrowser.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UrlSafetyCoordinatorTest {

    private lateinit var coordinator: UrlSafetyCoordinator

    @Before
    fun setUp() {
        coordinator = UrlSafetyCoordinator()
    }

    @Test
    fun normalizeUrl_stripsTrailingSlashFromPath() {
        assertEquals(
            "https://example.com/page",
            coordinator.normalizeUrl("https://example.com/page/")
        )
    }

    @Test
    fun normalizeUrl_addsHttpsWhenSchemeMissing() {
        assertEquals("https://example.com", coordinator.normalizeUrl("example.com"))
    }

    @Test
    fun isBlockedScheme_rejectsJavascriptAndDataUrls() {
        assertTrue(coordinator.isBlockedScheme("javascript:alert(1)"))
        assertTrue(coordinator.isBlockedScheme("data:text/html,hello"))
        assertFalse(coordinator.isBlockedScheme("https://example.com"))
    }

    @Test
    fun allowsCleartext_requiresTrustedHostOrUserAllowance() {
        assertTrue(coordinator.allowsCleartext("http://127.0.0.1", userAllowedHost = false))
        assertFalse(coordinator.allowsCleartext("http://unknown.example", userAllowedHost = false))
        assertTrue(coordinator.allowsCleartext("http://unknown.example", userAllowedHost = true))
    }

    @Test
    fun redirectChain_tracksHopsAndDetectsLimit() {
        repeat(UrlSafetyCoordinator.MAX_REDIRECT_DEPTH) { index ->
            coordinator.recordRedirect("https://a.com/$index", "https://b.com/$index")
        }
        assertFalse(coordinator.exceedsRedirectLimit())
        coordinator.recordRedirect("https://c.com", "https://d.com")
        assertTrue(coordinator.exceedsRedirectLimit())
    }

    @Test
    fun extractHttpUrlsFromText_parsesMultipleLinks() {
        val text = "Check https://one.com/a and http://two.com/b?x=1 for details."
        val urls = coordinator.extractHttpUrlsFromText(text)
        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://one.com/a"))
        assertTrue(urls.contains("http://two.com/b?x=1"))
    }

    @Test
    fun deduplicateByNormalized_collapsesTrailingSlashVariants() {
        val deduped = coordinator.deduplicateByNormalized(
            listOf(
                "https://example.com/docs",
                "https://example.com/docs/",
                "javascript:alert(1)"
            )
        )
        assertEquals(1, deduped.size)
        assertEquals("https://example.com/docs", deduped.first())
    }
}
