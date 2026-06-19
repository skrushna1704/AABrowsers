package com.kododake.aabrowser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kododake.aabrowser.data.BrowserPreferences
import com.kododake.aabrowser.navigation.UrlSafetyCoordinator

class ShareBookmarkActivity : AppCompatActivity() {

    private val urlSafetyCoordinator = UrlSafetyCoordinator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
        finish()
    }

    private fun handleShareIntent(intent: Intent?) {
        val urls = extractSharedUrls(intent)
        if (urls.isEmpty()) {
            Toast.makeText(this, R.string.bookmark_share_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        val added = importSharedBookmarkBatch(urls)
        when {
            added == urls.size -> Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
            added > 0 -> Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, R.string.bookmark_exists, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSharedBookmarkBatch(urls: List<String>): Int {
        val normalized = urlSafetyCoordinator.deduplicateByNormalized(
            urls.map { BrowserPreferences.formatNavigableUrl(it) }
        )
        var added = 0
        normalized.forEach { url ->
            if (BrowserPreferences.addBookmark(this, url)) {
                added++
            }
        }
        return added
    }

    private fun extractSharedUrls(intent: Intent?): List<String> {
        if (intent == null) return emptyList()

        val collected = linkedSetOf<String>()

        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            urlSafetyCoordinator.extractHttpUrlsFromText(text.orEmpty()).forEach { collected.add(it) }
            urlSafetyCoordinator.extractHttpUrlsFromText(subject.orEmpty()).forEach { collected.add(it) }
            extractFirstUrl(text)?.let { collected.add(it) }
            extractFirstUrl(subject)?.let { collected.add(it) }
        }

        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val texts = intent.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT)
            texts?.forEach { entry ->
                urlSafetyCoordinator.extractHttpUrlsFromText(entry?.toString().orEmpty())
                    .forEach { collected.add(it) }
            }
        }

        val dataUri = intent.data
        if (dataUri?.scheme?.lowercase() in listOf("http", "https")) {
            urlSafetyCoordinator.normalizeUrl(dataUri.toString())?.let { collected.add(it) }
        }

        return urlSafetyCoordinator.deduplicateByNormalized(collected.toList())
    }

    private fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group().trim()
            if (candidate.isNotEmpty()) {
                val parsed = runCatching { Uri.parse(candidate) }.getOrNull() ?: continue
                val scheme = parsed.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    return urlSafetyCoordinator.normalizeUrl(candidate) ?: candidate
                }
            }
        }
        return null
    }
}
