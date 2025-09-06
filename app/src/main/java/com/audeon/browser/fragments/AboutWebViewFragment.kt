/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2023 Soren Stoutner <soren@stoutner.com>
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.audeon.browser.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.fragment.app.Fragment
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewFeature

import com.audeon.browser.R

// Define the class constants.
private const val TAB_NUMBER = "tab_number"
private const val SCROLL_X = "scroll_x"
private const val SCROLL_Y = "scroll_y"

class AboutWebViewFragment : Fragment() {
    // Define the class variables.
    private var tabNumber = 0

    // Declare the class views.
    private lateinit var webViewLayout: View

    companion object {
        fun createTab(tabNumber: Int): AboutWebViewFragment {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putInt(TAB_NUMBER, tabNumber)

            // Create a new instance of the tab fragment.
            val aboutWebViewFragment = AboutWebViewFragment()

            // Add the arguments bundle to the fragment.
            aboutWebViewFragment.arguments = argumentsBundle

            // Return the new fragment.
            return aboutWebViewFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Store the tab number in a class variable.
        tabNumber = requireArguments().getInt(TAB_NUMBER)
    }

    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout.  The fragment will take care of attaching the root automatically.
        webViewLayout = layoutInflater.inflate(R.layout.bare_webview, container, false)

        // Get a handle for tab WebView.
        val tabWebView = webViewLayout as WebView

        // Create a WebView asset loader.
        val webViewAssetLoader = WebViewAssetLoader.Builder().addPathHandler("/assets/", AssetsPathHandler(requireContext())).build()

        // Set a WebView client.
        tabWebView.webViewClient = object : WebViewClient() {
            // Send external links back to the main Privacy Browser WebView.
            override fun shouldOverrideUrlLoading(view: WebView, webResourceRequest: WebResourceRequest): Boolean {
                // Create an intent to view the URL.
                val urlIntent = Intent(Intent.ACTION_VIEW)

                // Add the URL to the intent.
                urlIntent.data = webResourceRequest.url

                // Make it so.
                startActivity(urlIntent)

                // Consume the click.
                return true
            }

            // Process asset requests with the asset loader.
            override fun shouldInterceptRequest(webView: WebView, webResourceRequest: WebResourceRequest): WebResourceResponse? {
                // This allows using the `appassets.androidplatform.net` URL, which handles the loading of SVG files, which otherwise is prevented by the CORS policy.
                return webViewAssetLoader.shouldInterceptRequest(webResourceRequest.url)
            }
        }

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Check to see if the app is in night mode.  This can be removed once the minimum API >= 33.
        if ((Build.VERSION.SDK_INT < 33) && (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES) && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {  // The app is in night mode.
            // Apply the dark WebView theme.
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(tabWebView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }

        // Load the indicated tab.  The tab numbers start at 0, with the WebView tabs starting at 1.
        when (tabNumber) {
            1 -> tabWebView.loadUrl("https://appassets.androidplatform.net/assets/" + getString(R.string.android_asset_path) + "/about_permissions.html")
            2 -> tabWebView.loadUrl("https://appassets.androidplatform.net/assets/" + getString(R.string.android_asset_path) + "/about_privacy_policy.html")
            3 -> tabWebView.loadUrl("https://appassets.androidplatform.net/assets/" + getString(R.string.android_asset_path) + "/about_licenses.html")
        }

        // Scroll the tab if the saved instance state is not null.
        if (savedInstanceState != null) {
            tabWebView.post {
                tabWebView.scrollX = savedInstanceState.getInt(SCROLL_X)
                tabWebView.scrollY = savedInstanceState.getInt(SCROLL_Y)
            }
        }

        // Return the formatted WebView layout.
        return webViewLayout
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Get a handle for the tab WebView.  A class variable cannot be used because it gets out of sync when restarting.
        val tabWebView = webViewLayout as WebView?

        // Save the scroll positions if the layout is not null, which can happen if a tab is not currently selected.
        if (tabWebView != null) {
            savedInstanceState.putInt(SCROLL_X, tabWebView.scrollX)
            savedInstanceState.putInt(SCROLL_Y, tabWebView.scrollY)
        }
    }
}
