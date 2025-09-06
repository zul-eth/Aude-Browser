/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2024 Soren Stoutner <soren@stoutner.com>
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

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.ArrayAdapter

import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import com.audeon.browser.R
import com.audeon.browser.activities.SETTINGS_CUSTOM_USER_AGENT
import com.audeon.browser.activities.SETTINGS_WEBVIEW_DEFAULT_USER_AGENT
import com.audeon.browser.activities.UNRECOGNIZED_USER_AGENT
import com.audeon.browser.helpers.ProxyHelper
import kotlin.system.exitProcess

// Define the class constants.
private const val SCROLL_Y = "scroll_y"

class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        // Declare the private static class variables.  For some reason (I'm looking at you Android's Activity Lifecycle) this only works if these are static.
        private var fragmentRestarted: Boolean = false
        private var scrollY: Int = 0
    }

    // Declare the class variables.
    private lateinit var allowScreenshotsPreference: Preference
    private lateinit var ampRedirectsPreference: Preference
    private lateinit var appThemeEntriesStringArray: Array<String>
    private lateinit var appThemeEntryValuesStringArray: Array<String>
    private lateinit var appThemePreference: Preference
    private lateinit var blockAllThirdPartyRequestsPreference: Preference
    private lateinit var bottomAppBarPreference: Preference
    private lateinit var clearCachePreference: Preference
    private lateinit var clearCookiesPreference: Preference
    private lateinit var clearDomStoragePreference: Preference
    private lateinit var clearEverythingPreference: Preference
    private lateinit var clearLogcatPreference: Preference
    private lateinit var cookiesPreference: Preference
    private lateinit var customUserAgentPreference: Preference
    private lateinit var defaultUserAgent: String
    private lateinit var displayAdditionalAppBarIconsPreference: Preference
    private lateinit var displayWebpageImagesPreference: Preference
    private lateinit var domStoragePreference: Preference
    private lateinit var downloadProviderEntryValuesStringArray: Array<String>
    private lateinit var downloadProviderPreference: Preference
    private lateinit var easyListPreference: Preference
    private lateinit var easyPrivacyPreference: Preference
    private lateinit var fanboyAnnoyanceListPreference: Preference
    private lateinit var fanboySocialBlockingListPreference: Preference
    private lateinit var fontSizePreference: Preference
    private lateinit var fullScreenBrowsingModePreference: Preference
    private lateinit var hideAppBarPreference: Preference
    private lateinit var displayUnderCutoutsPreference: Preference
    private lateinit var homepagePreference: Preference
    private lateinit var incognitoModePreference: Preference
    private lateinit var javaScriptPreference: Preference
    private lateinit var openIntentsInNewTabPreference: Preference
    private lateinit var proxyCustomUrlPreference: Preference
    private lateinit var proxyPreference: Preference
    private lateinit var scrollAppBarPreference: Preference
    private lateinit var searchCustomURLPreference: Preference
    private lateinit var searchPreference: Preference
    private lateinit var sharedPreferenceChangeListener: OnSharedPreferenceChangeListener
    private lateinit var sortBookmarksAlphabeticallyPreference: Preference
    private lateinit var swipeToRefreshPreference: Preference
    private lateinit var trackingQueriesPreference: Preference
    private lateinit var translatedUserAgentNamesArray: Array<String>
    private lateinit var ultraListPreference: Preference
    private lateinit var ultraPrivacyPreference: Preference
    private lateinit var userAgentDataArray: Array<String>
    private lateinit var userAgentPreference: Preference
    private lateinit var userAgentNamesArray: ArrayAdapter<CharSequence>
    private lateinit var webViewThemeEntriesStringArray: Array<String>
    private lateinit var webViewThemeEntryValuesStringArray: Array<String>
    private lateinit var webViewThemePreference: Preference
    private lateinit var wideViewportPreference: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Check if the fragment has been restarted.
        if (savedInstanceState != null) {
            // Set the fragment restored flag.
            fragmentRestarted = true

            // Save the scroll Y.
            scrollY = savedInstanceState.getInt(SCROLL_Y)
        }

        // Load the preferences from the XML file.
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Get a handle for the shared preferences.
        val sharedPreferences = preferenceScreen.sharedPreferences!!

        // Get handles for the preferences.
        javaScriptPreference = findPreference(getString(R.string.javascript_key))!!
        cookiesPreference = findPreference(getString(R.string.cookies_key))!!
        domStoragePreference = findPreference(getString(R.string.dom_storage_key))!!
        userAgentPreference = findPreference(getString(R.string.user_agent_key))!!
        customUserAgentPreference = findPreference(getString(R.string.custom_user_agent_key))!!
        incognitoModePreference = findPreference(getString(R.string.incognito_mode_key))!!
        allowScreenshotsPreference = findPreference(getString(R.string.allow_screenshots_key))!!
        easyListPreference = findPreference(getString(R.string.easylist_key))!!
        easyPrivacyPreference = findPreference(getString(R.string.easyprivacy_key))!!
        fanboyAnnoyanceListPreference = findPreference(getString(R.string.fanboys_annoyance_list_key))!!
        fanboySocialBlockingListPreference = findPreference(getString(R.string.fanboys_social_blocking_list_key))!!
        ultraListPreference = findPreference(getString(R.string.ultralist_key))!!
        ultraPrivacyPreference = findPreference(getString(R.string.ultraprivacy_key))!!
        blockAllThirdPartyRequestsPreference = findPreference(getString(R.string.block_all_third_party_requests_key))!!
        trackingQueriesPreference = findPreference(getString(R.string.tracking_queries_key))!!
        ampRedirectsPreference = findPreference(getString(R.string.amp_redirects_key))!!
        searchPreference = findPreference(getString(R.string.search_key))!!
        searchCustomURLPreference = findPreference(getString(R.string.search_custom_url_key))!!
        proxyPreference = findPreference(getString(R.string.proxy_key))!!
        proxyCustomUrlPreference = findPreference(getString(R.string.proxy_custom_url_key))!!
        fullScreenBrowsingModePreference = findPreference(getString(R.string.full_screen_browsing_mode_key))!!
        hideAppBarPreference = findPreference(getString(R.string.hide_app_bar_key))!!
        displayUnderCutoutsPreference = findPreference(getString(R.string.display_under_cutouts_key))!!
        clearEverythingPreference = findPreference(getString(R.string.clear_everything_key))!!
        clearCookiesPreference = findPreference(getString(R.string.clear_cookies_key))!!
        clearDomStoragePreference = findPreference(getString(R.string.clear_dom_storage_key))!!
        clearLogcatPreference = findPreference(getString(R.string.clear_logcat_key))!!
        clearCachePreference = findPreference(getString(R.string.clear_cache_key))!!
        homepagePreference = findPreference(getString(R.string.homepage_key))!!
        fontSizePreference = findPreference(getString(R.string.font_size_key))!!
        openIntentsInNewTabPreference = findPreference(getString(R.string.open_intents_in_new_tab_key))!!
        swipeToRefreshPreference = findPreference(getString(R.string.swipe_to_refresh_key))!!
        downloadProviderPreference = findPreference(getString(R.string.download_provider_key))!!
        scrollAppBarPreference = findPreference(getString(R.string.scroll_app_bar_key))!!
        bottomAppBarPreference = findPreference(getString(R.string.bottom_app_bar_key))!!
        displayAdditionalAppBarIconsPreference = findPreference(getString(R.string.display_additional_app_bar_icons_key))!!
        sortBookmarksAlphabeticallyPreference = findPreference(getString(R.string.sort_bookmarks_alphabetically_key))!!
        appThemePreference = findPreference(getString(R.string.app_theme_key))!!
        webViewThemePreference = findPreference(getString(R.string.webview_theme_key))!!
        wideViewportPreference = findPreference(getString(R.string.wide_viewport_key))!!
        displayWebpageImagesPreference = findPreference(getString(R.string.display_webpage_images_key))!!

        // Set the preference dependencies.
        domStoragePreference.dependency = getString(R.string.javascript_key)
        hideAppBarPreference.dependency = getString(R.string.full_screen_browsing_mode_key)
        displayUnderCutoutsPreference.dependency = getString(R.string.full_screen_browsing_mode_key)

        // Get strings from the preferences.
        val userAgentName = sharedPreferences.getString(getString(R.string.user_agent_key), getString(R.string.user_agent_default_value))
        val searchString = sharedPreferences.getString(getString(R.string.search_key), getString(R.string.search_default_value))
        val proxyString = sharedPreferences.getString(getString(R.string.proxy_key), getString(R.string.proxy_default_value))

        // Get booleans that are used in multiple places from the preferences.
        val javaScriptEnabled = sharedPreferences.getBoolean(getString(R.string.javascript_key), false)
        val fanboyAnnoyanceListEnabled = sharedPreferences.getBoolean(getString(R.string.fanboys_annoyance_list_key), true)
        val fanboySocialBlockingEnabled = sharedPreferences.getBoolean(getString(R.string.fanboys_social_blocking_list_key), true)
        val fullScreenBrowsingMode = sharedPreferences.getBoolean(getString(R.string.full_screen_browsing_mode_key), false)
        val clearEverything = sharedPreferences.getBoolean(getString(R.string.clear_everything_key), true)

        // Only enable Fanboy's social blocking list preference if Fanboy's annoyance list is disabled.
        fanboySocialBlockingListPreference.isEnabled = !fanboyAnnoyanceListEnabled


        // Inflate a WebView to get the default user agent.
        val inflater = requireActivity().layoutInflater

        // `@SuppressLint("InflateParams")` removes the warning about using `null` as the `ViewGroup`, which in this case makes sense because the `bare_webview` will not be displayed.
        @SuppressLint("InflateParams") val bareWebViewLayout = inflater.inflate(R.layout.bare_webview, null, false)

        // Get a handle for the bare WebView.
        val bareWebView = bareWebViewLayout.findViewById<WebView>(R.id.bare_webview)

        // Get the default user agent.
        defaultUserAgent = bareWebView.settings.userAgentString

        // Get the user agent arrays.
        userAgentNamesArray = ArrayAdapter.createFromResource(requireContext(), R.array.user_agent_names, R.layout.spinner_item)
        translatedUserAgentNamesArray = resources.getStringArray(R.array.translated_user_agent_names)
        userAgentDataArray = resources.getStringArray(R.array.user_agent_data)

        // Populate the user agent summary.
        when (val userAgentArrayPosition = userAgentNamesArray.getPosition(userAgentName)) {
            // The user agent name is not on the canonical list.
            // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.  Use the current user agent entry name as the summary.
            UNRECOGNIZED_USER_AGENT -> userAgentPreference.summary = userAgentName

            // Get the user agent text from the webview (which changes based on the version of Android and WebView installed).
            SETTINGS_WEBVIEW_DEFAULT_USER_AGENT -> userAgentPreference.summary = "${translatedUserAgentNamesArray[userAgentArrayPosition]}:\n$defaultUserAgent"

            // Display the custom user agent.
            SETTINGS_CUSTOM_USER_AGENT -> userAgentPreference.setSummary(R.string.custom_user_agent)

            // Get the user agent summary from the user agent data array.
            else -> userAgentPreference.summary = "${translatedUserAgentNamesArray[userAgentArrayPosition]}:\n${userAgentDataArray[userAgentArrayPosition]}"
        }

        // Set the summary text for the custom user agent preference.
        customUserAgentPreference.summary = sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value))

        // Only enable the custom user agent preference if the user agent is set to custom.
        customUserAgentPreference.isEnabled = (userAgentPreference.summary == getString(R.string.custom_user_agent))

        // Set the search URL as the summary text for the search preference when the preference screen is loaded.
        if (searchString == getString(R.string.custom_url_item)) {
            // Use R.string.custom_url, which will be translated, instead of the array value, which will not.
            searchPreference.setSummary(R.string.custom_url)
        } else {
            // Set the array value as the summary text.
            searchPreference.summary = searchString
        }

        // Set the summary text for the search custom URL (the default is `""`).
        searchCustomURLPreference.summary = sharedPreferences.getString(getString(R.string.search_custom_url_key), getString(R.string.search_custom_url_default_value))

        // Only enable the search custom URL preference if the search is set to a custom URL.
        searchCustomURLPreference.isEnabled = (searchString == getString(R.string.custom_url_item))

        // Set the summary text for the proxy preference.
        proxyPreference.summary = when (proxyString) {
            ProxyHelper.NONE -> getString(R.string.no_proxy_enabled)
            ProxyHelper.TOR -> getString(R.string.tor_enabled)
            ProxyHelper.I2P -> getString(R.string.i2p_enabled)
            ProxyHelper.CUSTOM -> getString(R.string.custom_proxy)
            else -> getString(R.string.no_proxy_enabled)
        }

        // Set the summary text for the custom proxy URL.
        proxyCustomUrlPreference.summary = sharedPreferences.getString(getString(R.string.proxy_custom_url_key), getString(R.string.proxy_custom_url_default_value))

        // Only enable the custom proxy URL if a custom proxy is selected.
        proxyCustomUrlPreference.isEnabled = proxyString == ProxyHelper.CUSTOM

        // Set the status of the clear and exit preferences.
        clearCookiesPreference.isEnabled = !clearEverything
        clearDomStoragePreference.isEnabled = !clearEverything
        clearLogcatPreference.isEnabled = !clearEverything
        clearCachePreference.isEnabled = !clearEverything

        // Set the homepage URL as the summary text for the homepage preference.
        homepagePreference.summary = sharedPreferences.getString(getString(R.string.homepage_key), getString(R.string.homepage_default_value))

        // Set the font size as the summary text for the preference.
        fontSizePreference.summary = sharedPreferences.getString(getString(R.string.font_size_key), getString(R.string.font_size_default_value)) + "%"

        // Get the download provider entry values string array
        downloadProviderEntryValuesStringArray = resources.getStringArray(R.array.download_provider_entry_values)

        // Set the summary text for the download provider preference.
        downloadProviderPreference.summary = when (sharedPreferences.getString(getString(R.string.download_provider_key), getString(R.string.download_provider_default_value))) {
            downloadProviderEntryValuesStringArray[0] -> getString(R.string.download_with_audeon_browser)  // Privacy Browser is selected.
            downloadProviderEntryValuesStringArray[1] -> getString(R.string.download_with_android_download_manager)  // Android download manager is selected.
            else -> getString(R.string.download_with_external_app)  // External app is selected.
        }

        // Get the app theme string arrays.
        appThemeEntriesStringArray = resources.getStringArray(R.array.app_theme_entries)
        appThemeEntryValuesStringArray = resources.getStringArray(R.array.app_theme_entry_values)

        // Get the app theme entry number that matches the current app theme.
        val appThemeEntryNumber: Int = when (sharedPreferences.getString(getString(R.string.app_theme_key), getString(R.string.app_theme_default_value))) {
            appThemeEntryValuesStringArray[1] -> 1  // The light theme is selected.
            appThemeEntryValuesStringArray[2] -> 2  // The dark theme is selected.
            else -> 0  // The system default theme is selected.
        }

        // Set the current theme as the summary text for the preference.
        appThemePreference.summary = appThemeEntriesStringArray[appThemeEntryNumber]

        // Enable the WebView theme preference if the app theme is not set to light.  Google does not allow light themes to display dark WebViews.
        webViewThemePreference.isEnabled = (appThemeEntryNumber != 1)

        // Get the WebView theme string arrays.
        webViewThemeEntriesStringArray = resources.getStringArray(R.array.webview_theme_entries)
        webViewThemeEntryValuesStringArray = resources.getStringArray(R.array.webview_theme_entry_values)

        // Get the WebView theme entry number that matches the current WebView theme.
        val webViewThemeEntryNumber: Int = when (sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value))) {
            webViewThemeEntryValuesStringArray[1] -> 1  // The light theme is selected.
            webViewThemeEntryValuesStringArray[2] -> 2  // The dark theme is selected.
            else -> 0  // The system default theme is selected.
        }

        // Set the current theme as the summary text for the preference.
        webViewThemePreference.summary = webViewThemeEntriesStringArray[webViewThemeEntryNumber]

        // Set the JavaScript icon.
        if (javaScriptEnabled)
            javaScriptPreference.setIcon(R.drawable.javascript_enabled)
        else
            javaScriptPreference.setIcon(R.drawable.privacy_mode)

        // Set the cookies icon.
        if (sharedPreferences.getBoolean(getString(R.string.cookies_key), false))
            cookiesPreference.setIcon(R.drawable.cookies_enabled)
        else
            cookiesPreference.setIcon(R.drawable.cookies_disabled)

        // Set the DOM storage icon.
        if (javaScriptEnabled) {  // JavaScript is enabled.
            if (sharedPreferences.getBoolean(getString(R.string.dom_storage_key), false))  // DOM storage is enabled.
                domStoragePreference.setIcon(R.drawable.dom_storage_enabled)
            else  // DOM storage is disabled.
                domStoragePreference.setIcon(R.drawable.dom_storage_disabled)
        } else {  // JavaScript is disabled.  DOM storage should be ghosted.
            domStoragePreference.setIcon(R.drawable.dom_storage_ghosted)
        }

        // Set the custom user agent icon.
        if (customUserAgentPreference.isEnabled)
            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_enabled)
        else
            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_ghosted)

        // Set the incognito mode icon.
        if (sharedPreferences.getBoolean(getString(R.string.incognito_mode_key), false))
            incognitoModePreference.setIcon(R.drawable.incognito_mode_enabled)
        else
            incognitoModePreference.setIcon(R.drawable.incognito_mode_disabled)

        // Set the allow screenshots icon.
        if (sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false))
            allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_enabled)
        else
            allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_disabled)

        // Set the EasyList icon.
        if (sharedPreferences.getBoolean(getString(R.string.easylist_key), true))
            easyListPreference.setIcon(R.drawable.block_ads_enabled)
        else
            easyListPreference.setIcon(R.drawable.block_ads_disabled)

        // Set the EasyPrivacy icon.
        if (sharedPreferences.getBoolean(getString(R.string.easyprivacy_key), true))
            easyPrivacyPreference.setIcon(R.drawable.block_tracking_enabled)
        else
            easyPrivacyPreference.setIcon(R.drawable.block_tracking_disabled)

        // Set the Fanboy lists icons.
        if (fanboyAnnoyanceListEnabled) {
            // Set the Fanboy annoyance list icon.
            fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_enabled)

            // Set the Fanboy social blocking list icon.
            fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_ghosted)
        } else {
            // Set the Fanboy annoyance list icon.
            fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_disabled)

            // Set the Fanboy social blocking list icon.
            if (fanboySocialBlockingEnabled)
                fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_enabled)
            else
                fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_disabled)
        }

        // Set the UltraList icon.
        if (sharedPreferences.getBoolean(getString(R.string.ultralist_key), true))
            ultraListPreference.setIcon(R.drawable.block_ads_enabled)
        else
            ultraListPreference.setIcon(R.drawable.block_ads_disabled)

        // Set the UltraPrivacy icon.
        if (sharedPreferences.getBoolean(getString(R.string.ultraprivacy_key), true))
            ultraPrivacyPreference.setIcon(R.drawable.block_tracking_enabled)
        else
            ultraPrivacyPreference.setIcon(R.drawable.block_tracking_disabled)

        // Set the block all third-party requests icon.
        if (sharedPreferences.getBoolean(getString(R.string.block_all_third_party_requests), false))
            blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_enabled)
        else
            blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_disabled)

        // Set the Tracking Queries icon.
        if (sharedPreferences.getBoolean(getString(R.string.tracking_queries_key), true))
            trackingQueriesPreference.setIcon(R.drawable.modify_url_enabled)
        else
            trackingQueriesPreference.setIcon(R.drawable.modify_url_disabled)

        // Set the AMP Redirects icon.
        if (sharedPreferences.getBoolean(getString(R.string.amp_redirects_key), true))
            ampRedirectsPreference.setIcon(R.drawable.modify_url_enabled)
        else
            ampRedirectsPreference.setIcon(R.drawable.modify_url_disabled)

        // Set the search custom URL icon.
        if (searchCustomURLPreference.isEnabled)
            searchCustomURLPreference.setIcon(R.drawable.search_custom_enabled)
        else
            searchCustomURLPreference.setIcon(R.drawable.search_custom_ghosted)

        // Set the proxy icons according to the theme and status.
        if (proxyString == ProxyHelper.NONE) {  // Proxying is disabled.
            // Set the main proxy icon to be disabled.
            proxyPreference.setIcon(R.drawable.proxy_disabled)

            // Set the custom proxy URL icon to be ghosted.
            proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted)
        } else {  // Proxying is enabled.
            // Set the main proxy icon to be enabled.
            proxyPreference.setIcon(R.drawable.proxy_enabled)

            // Set the custom proxy URL icon according to its status.
            if (proxyCustomUrlPreference.isEnabled)
                proxyCustomUrlPreference.setIcon(R.drawable.proxy_enabled)
            else
                proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted)
        }

        // Set the full-screen browsing mode icons.
        if (fullScreenBrowsingMode) {  // Full-screen browsing mode is enabled.
            // Set the full screen browsing mode preference icon.
            fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_enabled)

            // Set the hide app bar icon.
            if (sharedPreferences.getBoolean(getString(R.string.hide_app_bar_key), true))
                hideAppBarPreference.setIcon(R.drawable.app_bar_enabled)
            else
                hideAppBarPreference.setIcon(R.drawable.app_bar_disabled)
        } else {  // Full screen browsing mode is disabled.
            // Set the icons.
            fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_disabled)
            hideAppBarPreference.setIcon(R.drawable.app_bar_ghosted)
        }

        // Set the display under cutouts icon.
        if (sharedPreferences.getBoolean(getString(R.string.display_under_cutouts_key), false))
            displayUnderCutoutsPreference.setIcon(R.drawable.display_under_cutouts_enabled)
        else
            displayUnderCutoutsPreference.setIcon(R.drawable.display_under_cutouts_disabled)

        // Set the clear everything icon.
        if (clearEverything) {
            clearEverythingPreference.setIcon(R.drawable.clear_everything_enabled)
        } else {
            clearEverythingPreference.setIcon(R.drawable.clear_everything_disabled)
        }

        // Set the clear cookies icon.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_cookies_key), true))
            clearCookiesPreference.setIcon(R.drawable.clear_cookies_enabled)
        else
            clearCookiesPreference.setIcon(R.drawable.clear_cookies_disabled)

        // Set the clear DOM storage icon.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_dom_storage_key), true))
            clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_enabled)
        else
            clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_disabled)

        // Set the clear logcat icon.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_logcat_key), true))
            clearLogcatPreference.setIcon(R.drawable.clear_logcat_enabled)
        else
            clearLogcatPreference.setIcon(R.drawable.clear_logcat_disabled)

        // Set the clear cache icon.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_cache_key), true))
            clearCachePreference.setIcon(R.drawable.clear_cache_enabled)
        else
            clearCachePreference.setIcon(R.drawable.clear_cache_disabled)

        // Set the open intents in new tab icon.
        if (sharedPreferences.getBoolean(getString(R.string.open_intents_in_new_tab_key), true))
            openIntentsInNewTabPreference.setIcon(R.drawable.tab_enabled)
        else
            openIntentsInNewTabPreference.setIcon(R.drawable.tab_disabled)

        // Set the swipe to refresh icon.
        if (sharedPreferences.getBoolean(getString(R.string.swipe_to_refresh_key), true))
            swipeToRefreshPreference.setIcon(R.drawable.refresh_enabled)
        else
            swipeToRefreshPreference.setIcon(R.drawable.refresh_disabled)

        // Set the scroll app bar icon.
        if (sharedPreferences.getBoolean(getString(R.string.scroll_app_bar_key), false))
            scrollAppBarPreference.setIcon(R.drawable.app_bar_enabled)
        else
            scrollAppBarPreference.setIcon(R.drawable.app_bar_disabled)

        // Set the bottom app bar icon.
        if (sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false))
            bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_enabled)
        else
            bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_disabled)

        // Set the display additional app bar icons icon.
        if (sharedPreferences.getBoolean(getString(R.string.display_additional_app_bar_icons_key), false))
            displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_enabled)
        else
            displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_disabled)

        // Set the sort bookmarks alphabetically icon.
        if (sharedPreferences.getBoolean(getString(R.string.sort_bookmarks_alphabetically_key), false))
            sortBookmarksAlphabeticallyPreference.setIcon(R.drawable.sort_by_alpha_enabled)
        else
            sortBookmarksAlphabeticallyPreference.setIcon(R.drawable.sort_by_alpha_disabled)

        // Set the WebView theme icon.
        if (webViewThemePreference.isEnabled) {  // The WebView theme preference is enabled.
            when (webViewThemeEntryNumber) {
                // The system default WebView theme is selected.
                0 -> {
                    // Get the current theme status.
                    val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                    // Set the icon according to the app theme.
                    if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)
                        webViewThemePreference.setIcon(R.drawable.webview_light_theme)
                    else
                        webViewThemePreference.setIcon(R.drawable.webview_dark_theme)
                }

                // The light WebView theme is selected.
                1 -> {
                    // Set the icon.
                    webViewThemePreference.setIcon(R.drawable.webview_light_theme)
                }

                // The dark WebView theme is selected.
                2 -> {
                    // Set the icon.
                    webViewThemePreference.setIcon(R.drawable.webview_dark_theme)
                }
            }
        } else {  // The WebView theme preference is disabled.
            webViewThemePreference.setIcon(R.drawable.webview_theme_ghosted)
        }

        // Set the wide viewport icon.
        if (sharedPreferences.getBoolean(getString(R.string.wide_viewport_key), true))
            wideViewportPreference.setIcon(R.drawable.wide_viewport_enabled)
        else
            wideViewportPreference.setIcon(R.drawable.wide_viewport_disabled)

        // Set the display webpage images icon.
        if (sharedPreferences.getBoolean(getString(R.string.display_webpage_images_key), true))
            displayWebpageImagesPreference.setIcon(R.drawable.images_enabled)
        else
            displayWebpageImagesPreference.setIcon(R.drawable.images_disabled)
    }

    // The listener should be unregistered when the app is paused.
    override fun onPause() {
        // Run the default commands.
        super.onPause()

        // Get a handle for the shared preferences.
        val sharedPreferences = preferenceScreen.sharedPreferences!!

        // Unregister the shared preference listener.
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    // The listener should be re-registered when the app is resumed.
    override fun onResume() {
        // Run the default commands.
        super.onResume()

        // Get a new shared preference change listener.
        sharedPreferenceChangeListener = getSharedPreferenceChangeListener()

        // Get a handle for the shared preferences.
        val sharedPreferences = preferenceScreen.sharedPreferences!!

        // Re-register the shared preference listener.
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        // Restore the scroll position if the fragment has been restarted.
        if (fragmentRestarted) {
            // Reset the fragment restarted flag.
            fragmentRestarted = false

            // Set the scroll position.
            listView.post { listView.smoothScrollBy(0, scrollY) }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Save the scroll position.
        savedInstanceState.putInt(SCROLL_Y, listView.computeVerticalScrollOffset())
    }

    private fun getSharedPreferenceChangeListener(): OnSharedPreferenceChangeListener {
        // Return the shared preference change listener.
        return OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String? ->
            when (key) {
                getString(R.string.javascript_key) -> {
                    // Update the icons and the DOM storage preference status.
                    if (sharedPreferences.getBoolean(getString(R.string.javascript_key), false)) {  // The JavaScript preference is enabled.
                        // Update the icon for the JavaScript preference.
                        javaScriptPreference.setIcon(R.drawable.javascript_enabled)

                        // Update the status of the DOM storage preference.
                        domStoragePreference.isEnabled = true

                        // Update the icon for the DOM storage preference.
                        if (sharedPreferences.getBoolean(getString(R.string.dom_storage_key), false))
                            domStoragePreference.setIcon(R.drawable.dom_storage_enabled)
                        else
                            domStoragePreference.setIcon(R.drawable.dom_storage_disabled)
                    } else {  // The JavaScript preference is disabled.
                        // Update the icon for the JavaScript preference.
                        javaScriptPreference.setIcon(R.drawable.privacy_mode)

                        // Update the status of the DOM storage preference.
                        domStoragePreference.isEnabled = false

                        // Set the icon for DOM storage preference to be ghosted.
                        domStoragePreference.setIcon(R.drawable.dom_storage_ghosted)
                    }
                }

                getString(R.string.cookies_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.cookies_key), false))
                        cookiesPreference.setIcon(R.drawable.cookies_enabled)
                    else
                        cookiesPreference.setIcon(R.drawable.cookies_disabled)
                }

                getString(R.string.dom_storage_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.dom_storage_key), false))
                        domStoragePreference.setIcon(R.drawable.dom_storage_enabled)
                    else
                        domStoragePreference.setIcon(R.drawable.dom_storage_disabled)
                }

                getString(R.string.user_agent_key) -> {
                    // Get the new user agent name.
                    val newUserAgentName = sharedPreferences.getString(getString(R.string.user_agent_key), getString(R.string.user_agent_default_value))

                    // Get the array position for the new user agent name.
                    val newUserAgentArrayPosition = userAgentNamesArray.getPosition(newUserAgentName)

                    // Get the translated new user agent name.
                    val translatedNewUserAgentName = translatedUserAgentNamesArray[newUserAgentArrayPosition]

                    // Populate the user agent summary.
                    when (newUserAgentArrayPosition) {
                        SETTINGS_WEBVIEW_DEFAULT_USER_AGENT -> {
                            // Get the user agent text from the webview (which changes based on the version of Android and WebView installed).
                            userAgentPreference.summary = "$translatedNewUserAgentName:\n$defaultUserAgent"

                            // Disable the custom user agent preference.
                            customUserAgentPreference.isEnabled = false

                            // Set the custom user agent preference icon.
                            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_ghosted)
                        }

                        SETTINGS_CUSTOM_USER_AGENT -> {
                            // Set the summary text.
                            userAgentPreference.setSummary(R.string.custom_user_agent)

                            // Enable the custom user agent preference.
                            customUserAgentPreference.isEnabled = true

                            // Set the custom user agent preference icon.
                            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_enabled)
                        }

                        else -> {
                            // Get the user agent summary from the user agent data array.
                            userAgentPreference.summary = "$translatedNewUserAgentName:\n${userAgentDataArray[newUserAgentArrayPosition]}"

                            // Disable the custom user agent preference.
                            customUserAgentPreference.isEnabled = false

                            // Set the custom user agent preference icon.
                            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_ghosted)
                        }
                    }
                }

                getString(R.string.custom_user_agent_key) -> {
                    // Set the new custom user agent as the summary text for the preference.
                    customUserAgentPreference.summary = sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value))
                }

                getString(R.string.incognito_mode_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.incognito_mode_key), false))
                        incognitoModePreference.setIcon(R.drawable.incognito_mode_enabled)
                    else
                        incognitoModePreference.setIcon(R.drawable.incognito_mode_disabled)
                }

                getString(R.string.allow_screenshots_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false))
                        allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_enabled)
                    else
                        allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_disabled)

                    // Restart Privacy Browser.
                    restartPrivacyBrowser()
                }

                getString(R.string.easylist_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.easylist_key), true))
                        easyListPreference.setIcon(R.drawable.block_ads_enabled)
                    else
                        easyListPreference.setIcon(R.drawable.block_ads_disabled)
                }

                getString(R.string.easyprivacy_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.easyprivacy_key), true))
                        easyPrivacyPreference.setIcon(R.drawable.block_tracking_enabled)
                    else
                        easyPrivacyPreference.setIcon(R.drawable.block_tracking_disabled)
                }

                getString(R.string.fanboys_annoyance_list_key) -> {
                    // Get the current Fanboy settings.
                    val currentFanboyAnnoyanceList = sharedPreferences.getBoolean(getString(R.string.fanboys_annoyance_list_key), true)
                    val currentFanboySocialBlockingList = sharedPreferences.getBoolean(getString(R.string.fanboys_social_blocking_list_key), true)

                    // Update the Fanboy icons.
                    if (currentFanboyAnnoyanceList) {  // Fanboy's annoyance list is enabled.
                        // Update the Fanboy's annoyance list icon.
                        fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_enabled)

                        // Update the Fanboy's social blocking list icon.
                        fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_ghosted)
                    } else {  // Fanboy's annoyance list is disabled.
                        // Update the Fanboy's annoyance list icon.
                        fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_disabled)

                        // Update the Fanboy's social blocking list icon.
                        if (currentFanboySocialBlockingList)
                            fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_enabled)
                        else
                            fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_disabled)
                    }

                    // Only enable Fanboy's social blocking list preference if Fanboy's annoyance list preference is disabled.
                    fanboySocialBlockingListPreference.isEnabled = !currentFanboyAnnoyanceList
                }

                getString(R.string.fanboys_social_blocking_list_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.fanboys_social_blocking_list_key), true))
                        fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_enabled)
                    else
                        fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_disabled)
                }

                getString(R.string.ultralist_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.ultralist_key), true))
                        ultraListPreference.setIcon(R.drawable.block_ads_enabled)
                    else
                        ultraListPreference.setIcon(R.drawable.block_ads_disabled)
                }

                getString(R.string.ultraprivacy_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.ultraprivacy_key), true))
                        ultraPrivacyPreference.setIcon(R.drawable.block_tracking_enabled)
                    else
                        ultraPrivacyPreference.setIcon(R.drawable.block_tracking_disabled)
                }

                getString(R.string.block_all_third_party_requests_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.block_all_third_party_requests_key), false)) {
                        blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_enabled)
                    } else {
                        blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_disabled)
                    }
                }

                getString(R.string.tracking_queries_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.tracking_queries_key), true))
                        trackingQueriesPreference.setIcon(R.drawable.modify_url_enabled)
                    else
                        trackingQueriesPreference.setIcon(R.drawable.modify_url_disabled)
                }

                getString(R.string.amp_redirects_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.amp_redirects_key), true))
                        ampRedirectsPreference.setIcon(R.drawable.modify_url_enabled)
                    else
                        ampRedirectsPreference.setIcon(R.drawable.modify_url_disabled)
                }

                getString(R.string.search_key) -> {
                    // Store the new search string.
                    val newSearchString = sharedPreferences.getString(getString(R.string.search_key), getString(R.string.search_default_value))

                    // Update the search and search custom URL preferences.
                    if (newSearchString == getString(R.string.custom_url_item)) {  // A custom URL is selected.
                        // Set the summary text to `R.string.custom_url`, which is translated.
                        searchPreference.setSummary(R.string.custom_url)

                        // Enable the search custom URL preference.
                        searchCustomURLPreference.isEnabled = true

                        // Set the search custom URL preference icon.
                        searchCustomURLPreference.setIcon(R.drawable.search_custom_enabled)
                    } else {  // A custom URL is not selected.
                        // Set the summary text to the new search string.
                        searchPreference.summary = newSearchString

                        // Disable the search custom URL Preference.
                        searchCustomURLPreference.isEnabled = false

                        // Set the search custom URL preference icon.
                        searchCustomURLPreference.setIcon(R.drawable.search_custom_ghosted)
                    }
                }

                getString(R.string.search_custom_url_key) -> {
                    // Set the new search custom URL as the summary text for the preference.
                    searchCustomURLPreference.summary = sharedPreferences.getString(getString(R.string.search_custom_url_key), getString(R.string.search_custom_url_default_value))
                }

                getString(R.string.proxy_key) -> {
                    // Get the current proxy string.
                    val currentProxyString = sharedPreferences.getString(getString(R.string.proxy_key), getString(R.string.proxy_default_value))

                    // Update the proxy preference summary text.
                    proxyPreference.summary = when (currentProxyString) {
                        ProxyHelper.NONE -> getString(R.string.no_proxy_enabled)
                        ProxyHelper.TOR -> getString(R.string.tor_enabled)
                        ProxyHelper.I2P -> getString(R.string.i2p_enabled)
                        ProxyHelper.CUSTOM -> getString(R.string.custom_proxy)
                        else -> getString(R.string.no_proxy_enabled)
                    }

                    // Update the status of the custom URL preference.
                    proxyCustomUrlPreference.isEnabled = currentProxyString == ProxyHelper.CUSTOM

                    // Update the icons.
                    if (currentProxyString == ProxyHelper.NONE) {  // Proxying is disabled.
                        // Set the main proxy icon to be disabled
                        proxyPreference.setIcon(R.drawable.proxy_disabled)

                        // Set the custom proxy URL icon to be ghosted.
                        proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted)
                    } else {  // Proxying is enabled.
                        // Set the main proxy icon to be enabled.
                        proxyPreference.setIcon(R.drawable.proxy_enabled)

                        /// Set the custom proxy URL icon according to its status.
                        if (proxyCustomUrlPreference.isEnabled)
                            proxyCustomUrlPreference.setIcon(R.drawable.proxy_enabled)
                        else
                            proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted)
                    }
                }

                getString(R.string.proxy_custom_url_key) -> {
                    // Set the summary text for the proxy custom URL.
                    proxyCustomUrlPreference.summary = sharedPreferences.getString(getString(R.string.proxy_custom_url_key), getString(R.string.proxy_custom_url_default_value))
                }

                getString(R.string.full_screen_browsing_mode_key) -> {
                    // Update the icons.
                    if (sharedPreferences.getBoolean(getString(R.string.full_screen_browsing_mode_key), false)) {  // Full screen browsing is enabled.
                        // Set the full screen browsing mode preference icon.
                        fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_enabled)

                        // Set the hide app bar preference icon.
                        if (sharedPreferences.getBoolean(getString(R.string.hide_app_bar_key), true))
                            hideAppBarPreference.setIcon(R.drawable.app_bar_enabled)
                        else
                            hideAppBarPreference.setIcon(R.drawable.app_bar_disabled)
                    } else {  // Full screen browsing is disabled.
                        // Update the icons.
                        fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_disabled)
                        hideAppBarPreference.setIcon(R.drawable.app_bar_ghosted)
                    }
                }

                getString(R.string.hide_app_bar_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.hide_app_bar_key), true))
                        hideAppBarPreference.setIcon(R.drawable.app_bar_enabled)
                    else
                        hideAppBarPreference.setIcon(R.drawable.app_bar_disabled)
                }

                getString(R.string.display_under_cutouts_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.display_under_cutouts_key), false))
                        displayUnderCutoutsPreference.setIcon(R.drawable.display_under_cutouts_enabled)
                    else
                        displayUnderCutoutsPreference.setIcon(R.drawable.display_under_cutouts_disabled)

                    // Restart Privacy Browser if the API < 35.
                    if (Build.VERSION.SDK_INT < 35)
                        restartPrivacyBrowser()
                }

                getString(R.string.clear_everything_key) -> {
                    // Store the new clear everything status
                    val newClearEverythingBoolean = sharedPreferences.getBoolean(getString(R.string.clear_everything_key), true)

                    // Update the status of the clear and exit preferences.
                    clearCookiesPreference.isEnabled = !newClearEverythingBoolean
                    clearDomStoragePreference.isEnabled = !newClearEverythingBoolean
                    clearLogcatPreference.isEnabled = !newClearEverythingBoolean
                    clearCachePreference.isEnabled = !newClearEverythingBoolean

                    // Update the clear everything preference icon.
                    if (newClearEverythingBoolean)
                        clearEverythingPreference.setIcon(R.drawable.clear_everything_enabled)
                    else
                        clearEverythingPreference.setIcon(R.drawable.clear_everything_disabled)

                    // Update the clear cookies preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean(getString(R.string.clear_cookies_key), true))
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_enabled)
                    else
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_disabled)

                    // Update the clear dom storage preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean(getString(R.string.clear_dom_storage_key), true))
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_enabled)
                    else
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_disabled)

                    // Update the clear logcat preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean(getString(R.string.clear_logcat_key), true))
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_enabled)
                    else
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_disabled)

                    // Update the clear cache preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean(getString(R.string.clear_cache_key), true))
                        clearCachePreference.setIcon(R.drawable.clear_cache_enabled)
                    else
                        clearCachePreference.setIcon(R.drawable.clear_cache_disabled)
                }

                getString(R.string.clear_cookies_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.clear_cookies_key), true))
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_enabled)
                    else
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_disabled)
                }

                getString(R.string.clear_dom_storage_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.clear_dom_storage_key), true))
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_enabled)
                    else
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_disabled)
                }

                getString(R.string.clear_logcat_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.clear_logcat_key), true))
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_enabled)
                    else
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_disabled)
                }

                getString(R.string.clear_cache_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.clear_cache_key), true))
                        clearCachePreference.setIcon(R.drawable.clear_cache_enabled)
                    else
                        clearCachePreference.setIcon(R.drawable.clear_cache_disabled)
                }

                getString(R.string.homepage_key) -> {
                    // Set the new homepage URL as the summary text for the Homepage preference.
                    homepagePreference.summary = sharedPreferences.getString(getString(R.string.homepage_key), getString(R.string.homepage_default_value))
                }

                getString(R.string.font_size_key) -> {
                    // Update the font size summary text.
                    fontSizePreference.summary = sharedPreferences.getString(getString(R.string.font_size_key), getString(R.string.font_size_default_value)) + "%"
                }

                getString(R.string.open_intents_in_new_tab_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.open_intents_in_new_tab_key), true))
                        openIntentsInNewTabPreference.setIcon(R.drawable.tab_enabled)
                    else
                        openIntentsInNewTabPreference.setIcon(R.drawable.tab_disabled)
                }

                getString(R.string.swipe_to_refresh_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.swipe_to_refresh_key), true))
                        swipeToRefreshPreference.setIcon(R.drawable.refresh_enabled)
                    else
                        swipeToRefreshPreference.setIcon(R.drawable.refresh_disabled)
                }

                getString(R.string.download_provider_key) -> {
                    // Set the summary text for the download provider preference.
                    downloadProviderPreference.summary = when (sharedPreferences.getString(getString(R.string.download_provider_key), getString(R.string.download_provider_default_value))) {
                        downloadProviderEntryValuesStringArray[0] -> getString(R.string.download_with_audeon_browser)  // Privacy Browser is selected.
                        downloadProviderEntryValuesStringArray[1] -> getString(R.string.download_with_android_download_manager)  // Android download manager is selected.
                        else -> getString(R.string.download_with_external_app)  // External app is selected.
                    }
                }

                getString(R.string.scroll_app_bar_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.scroll_app_bar_key), false))
                        scrollAppBarPreference.setIcon(R.drawable.app_bar_enabled)
                    else
                        scrollAppBarPreference.setIcon(R.drawable.app_bar_disabled)
                }

                getString(R.string.bottom_app_bar_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false))
                        bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_enabled)
                    else
                        bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_disabled)

                    // Restart Privacy Browser.
                    restartPrivacyBrowser()
                }

                getString(R.string.display_additional_app_bar_icons_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.display_additional_app_bar_icons_key), false))
                        displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_enabled)
                    else
                        displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_disabled)

                    // Restart Privacy Browser.
                    restartPrivacyBrowser()
                }

                getString(R.string.sort_bookmarks_alphabetically_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.sort_bookmarks_alphabetically_key), false))
                        sortBookmarksAlphabeticallyPreference.setIcon(R.drawable.sort_by_alpha_enabled)
                    else
                        sortBookmarksAlphabeticallyPreference.setIcon(R.drawable.sort_by_alpha_disabled)
                }

                getString(R.string.app_theme_key) -> {
                    // Get the app theme entry number that matches the current app theme.
                    val appThemeEntryNumber: Int = when (sharedPreferences.getString(getString(R.string.app_theme_key), getString(R.string.app_theme_default_value))) {
                        appThemeEntryValuesStringArray[1] -> 1  // The light theme is selected.
                        appThemeEntryValuesStringArray[2] -> 2  // The dark theme is selected.
                        else -> 0  // The system default theme is selected.
                    }

                    // Update the system according to the new theme.
                    when (appThemeEntryNumber) {
                        0 -> {  // The system default theme is selected.
                            // Update the theme preference summary text.
                            appThemePreference.summary = appThemeEntriesStringArray[0]

                            // Apply the new theme.
                            if (Build.VERSION.SDK_INT >= 28) {  // The system default theme is supported.
                                // Follow the system default theme.
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            } else { // The system default theme is not supported.
                                // Follow the battery saver mode.
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                            }
                        }

                        1 -> {  // The light theme is selected.
                            // Update the theme preference summary text.
                            appThemePreference.summary = appThemeEntriesStringArray[1]

                            // Apply the new theme.
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }

                        2 -> {  // The dark theme is selected.
                            // Update the theme preference summary text.
                            appThemePreference.summary = appThemeEntriesStringArray[2]

                            // Apply the new theme.
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                    }

                    // Enable the WebView theme preference if the app theme is not set to light.  Google does not allow light themes to display dark WebViews.
                    webViewThemePreference.isEnabled = (appThemeEntryNumber != 1)

                    // Get the webView theme entry number that matches the new WebView theme.
                    val webViewThemeEntryNumber: Int = when (sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value))) {
                        webViewThemeEntriesStringArray[1] -> 1  // The light theme is selected.
                        webViewThemeEntryValuesStringArray[2] -> 2  // The dark theme is selected.
                        else -> 0  // The system default theme is selected.
                    }

                    // Update the WebView theme icon.
                    if (webViewThemePreference.isEnabled) {  // The WebView theme preference is enabled.
                        when (webViewThemeEntryNumber) {
                            // The system default WebView theme is selected.
                            0 -> {
                                // Get the current theme status.
                                val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                                // Set the icon according to the app theme.
                                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)
                                    webViewThemePreference.setIcon(R.drawable.webview_light_theme)
                                else
                                    webViewThemePreference.setIcon(R.drawable.webview_dark_theme)
                            }

                            // The light WebView theme is selected.
                            1 -> {
                                // Set the icon.
                                webViewThemePreference.setIcon(R.drawable.webview_light_theme)
                            }

                            // The dark WebView theme is selected.
                            2 -> {
                                // Set the icon.
                                webViewThemePreference.setIcon(R.drawable.webview_dark_theme)
                            }
                        }
                    } else {  // The WebView theme preference is disabled.
                        webViewThemePreference.setIcon(R.drawable.webview_theme_ghosted)
                    }
                }

                getString(R.string.webview_theme_key) -> {
                    // Get the webView theme entry number that matches the new WebView theme.
                    val newWebViewThemeEntryNumber: Int = when (sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value))) {
                        webViewThemeEntriesStringArray[1] -> 1  // The light theme is selected.
                        webViewThemeEntryValuesStringArray[2] -> 2  // The dark theme is selected.
                        else -> 0  // The system default theme is selected.
                    }

                    // Update the WebView theme icon.
                    when (newWebViewThemeEntryNumber) {
                        // The system default WebView theme is selected.
                        0 -> {
                            // Get the current theme status.
                            val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                            // Set the icon.
                            if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)
                                webViewThemePreference.setIcon(R.drawable.webview_light_theme)
                            else
                                webViewThemePreference.setIcon(R.drawable.webview_dark_theme)
                        }

                        // The light WebView theme is selected.
                        1 -> {
                            // Set the icon.
                            webViewThemePreference.setIcon(R.drawable.webview_light_theme)
                        }

                        // The dark WebView theme is selected.
                        2 -> {
                            // Set the icon.
                            webViewThemePreference.setIcon(R.drawable.webview_dark_theme)
                        }
                    }

                    // Set the current theme as the summary text for the preference.
                    webViewThemePreference.summary = webViewThemeEntriesStringArray[newWebViewThemeEntryNumber]
                }

                getString(R.string.wide_viewport_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.wide_viewport_key), true))
                        wideViewportPreference.setIcon(R.drawable.wide_viewport_enabled)
                    else
                        wideViewportPreference.setIcon(R.drawable.wide_viewport_disabled)
                }

                getString(R.string.display_webpage_images_key) -> {
                    // Update the icon.
                    if (sharedPreferences.getBoolean(getString(R.string.display_webpage_images_key), true))
                        displayWebpageImagesPreference.setIcon(R.drawable.images_enabled)
                    else
                        displayWebpageImagesPreference.setIcon(R.drawable.images_disabled)
                }
            }
        }
    }

    private fun restartPrivacyBrowser() {
        // Create an intent to restart Privacy Browser.
        val restartIntent = requireActivity().parentActivityIntent!!

        // `Intent.FLAG_ACTIVITY_CLEAR_TASK` removes all activities from the stack.  It requires `Intent.FLAG_ACTIVITY_NEW_TASK`.
        restartIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Create a handler to restart the activity.
        val restartHandler = Handler(Looper.getMainLooper())

        // Create a runnable to restart the activity.
        val restartRunnable = Runnable {
            // Restart the activity.
            startActivity(restartIntent)

            // Kill this instance of Privacy Browser.  Otherwise, the app exhibits sporadic behavior after the restart.
            exitProcess(0)
        }

        // Restart the activity after 400 milliseconds, so that the app has enough time to save the change to the preference.
        restartHandler.postDelayed(restartRunnable, 400)
    }
}
