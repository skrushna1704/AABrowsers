package com.kododake.aabrowser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.kododake.aabrowser.R
import com.kododake.aabrowser.data.BrowserPreferences
import com.kododake.aabrowser.databinding.ActivityMainBinding
import com.kododake.aabrowser.permissions.PermissionManager
import com.kododake.aabrowser.startpage.StartPageManager
import com.kododake.aabrowser.tabs.TabManager
import com.kododake.aabrowser.ui.BrowserUIManager

class NavigationManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val tabManager: TabManager,
    private val permissionManager: PermissionManager,
    private val startPageManager: StartPageManager,
    private val uiManager: BrowserUIManager,
    private val callbacks: NavigationCallbacks,
    private val urlSafetyCoordinator: UrlSafetyCoordinator = UrlSafetyCoordinator()
) {

    interface NavigationCallbacks {
        fun onNavigationStarted(url: String)
        fun onNavigationFinished(url: String)
        fun onHideStartPage()
        fun getCurrentUrl(): String
        fun setCurrentUrl(url: String)
        fun setCurrentPageTitle(title: String)
    }

    fun extractBrowsableUrl(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val raw = data.toString()
        if (urlSafetyCoordinator.isBlockedScheme(raw)) {
            return null
        }
        return urlSafetyCoordinator.normalizeUrl(raw)
    }

    fun loadUrlFromIntent(rawUrl: String) {
        val navigable = BrowserPreferences.formatNavigableUrl(rawUrl.trim())
        if (navigable.isNotEmpty()) {
            navigateActiveTabTo(navigable, closeMenuAfterNavigate = true)
        }
    }

    fun navigateToAddress(raw: String, closeMenuAfterNavigate: Boolean) {
        val navigable = BrowserPreferences.formatNavigableUrl(raw)
        if (navigable.isNotEmpty()) {
            navigateActiveTabTo(navigable, closeMenuAfterNavigate)
        }
    }

    private fun navigateActiveTabTo(navigable: String, closeMenuAfterNavigate: Boolean) {
        if (urlSafetyCoordinator.isBlockedScheme(navigable)) {
            return
        }
        val normalizedNavigable = urlSafetyCoordinator.normalizeUrl(navigable) ?: return

        var targetTab = tabManager.activeTab
        if (targetTab == null) {
            targetTab = tabManager.createNewTab(activate = true)
        }
        if (targetTab == null) {
            return
        }
        
        val targetWebView = targetTab.webView
        val uri = runCatching { Uri.parse(normalizedNavigable) }.getOrNull()
        if (uri == null) {
            return
        }

        val finishNavigation: (() -> Unit) -> Unit = { loadAction ->
            targetTab.currentUrl = normalizedNavigable
            targetTab.currentTitle = ""
            
            if (targetTab.id == tabManager.activeTabId) {
                callbacks.setCurrentUrl(normalizedNavigable)
                callbacks.setCurrentPageTitle("")
                if (binding.addressEdit.text?.toString() != normalizedNavigable) {
                    binding.addressEdit.setText(normalizedNavigable)
                    binding.addressEdit.setSelection(normalizedNavigable.length)
                }
            }
            
            BrowserPreferences.persistUrl(activity, normalizedNavigable)
            tabManager.persistTabSession()
            callbacks.onHideStartPage()
            loadAction()
            
            if (closeMenuAfterNavigate && binding.menuOverlay.isVisible) {
                uiManager.hideMenuOverlay()
            } else {
                uiManager.hideKeyboard(binding.persistentAddressEdit)
                binding.persistentAddressEdit.clearFocus()
            }
        }

        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        
        val userAllowedCleartext = BrowserPreferences.isHostAllowedCleartext(activity, host)
        val canUseCleartext = urlSafetyCoordinator.allowsCleartext(normalizedNavigable, userAllowedCleartext)

        if (scheme == "http" && !canUseCleartext) {
            permissionManager.showCleartextNavigationDialog(
                uri = uri,
                onAllowOnce = {
                    finishNavigation {
                        targetWebView.setTag(R.id.webview_allow_once_uri_tag, normalizedNavigable)
                        targetWebView.post { 
                            targetWebView.loadUrl(normalizedNavigable) 
                        }
                    }
                },
                onAllowHost = {
                    if (host != null) {
                        BrowserPreferences.addAllowedCleartextHost(activity, host)
                        urlSafetyCoordinator.registerTrustedCleartextHost(host)
                    }
                    finishNavigation {
                        targetWebView.setTag(R.id.webview_allow_once_uri_tag, normalizedNavigable)
                        targetWebView.post { 
                            targetWebView.loadUrl(normalizedNavigable) 
                        }
                    }
                },
                onCancel = {
                    if (closeMenuAfterNavigate && binding.menuOverlay.isVisible) {
                        uiManager.hideMenuOverlay()
                    }
                }
            )
            return
        }

        finishNavigation { 
            targetWebView.loadUrl(normalizedNavigable) 
        }
    }

    fun followRedirectForActiveTab(currentUrl: String, redirectTarget: String): Boolean {
        urlSafetyCoordinator.recordRedirect(currentUrl, redirectTarget)
        if (urlSafetyCoordinator.exceedsRedirectLimit()) {
            urlSafetyCoordinator.clearRedirectChain()
            return false
        }
        if (urlSafetyCoordinator.isBlockedScheme(redirectTarget)) {
            return false
        }
        navigateActiveTabTo(redirectTarget, closeMenuAfterNavigate = false)
        return true
    }
}
