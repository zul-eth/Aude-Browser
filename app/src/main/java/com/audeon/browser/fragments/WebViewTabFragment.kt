/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2019-2020, 2022-2023, 2025 Soren Stoutner <soren@stoutner.com>
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
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar

import androidx.fragment.app.Fragment

import com.audeon.browser.R
import com.audeon.browser.views.NestedScrollWebView

import java.util.Calendar

// Define the class constants.
private const val CREATE_NEW_PAGE = "A"
private const val BOTTOM_APP_BAR = "B"
private const val PAGE_POSITION = "C"
private const val SAVED_NESTED_SCROLL_WEBVIEW_STATE = "D"
private const val SAVED_STATE = "E"
private const val URL = "F"

class WebViewTabFragment : Fragment() {
    // Define the public variables.
    var fragmentId = Calendar.getInstance().timeInMillis

    // The public interface is used to send information back to the parent activity.
    interface NewTabListener {
        @SuppressLint("ClickableViewAccessibility")
        fun initializeWebView(nestedScrollWebView: NestedScrollWebView, pagePosition: Int, progressBar: ProgressBar, urlString: String, restoringState: Boolean)
    }

    // Declare the class variables.
    private lateinit var newTabListener: NewTabListener

    // Declare the class views.
    private lateinit var nestedScrollWebView: NestedScrollWebView

    companion object {
        fun createPage(pageNumber: Int, url: String?, bottomAppBar: Boolean): WebViewTabFragment {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the argument in the bundle.
            argumentsBundle.putBoolean(CREATE_NEW_PAGE, true)
            argumentsBundle.putInt(PAGE_POSITION, pageNumber)
            argumentsBundle.putString(URL, url)
            argumentsBundle.putBoolean(BOTTOM_APP_BAR, bottomAppBar)

            // Create a new instance of the WebView tab fragment.
            val webViewTabFragment = WebViewTabFragment()

            // Add the arguments bundle to the fragment.
            webViewTabFragment.arguments = argumentsBundle

            // Return the new fragment.
            return webViewTabFragment
        }

        fun restorePage(savedState: Bundle, savedNestedScrollWebViewState: Bundle, bottomAppBar: Boolean): WebViewTabFragment {
            // Create an arguments bundle
            val argumentsBundle = Bundle()

            // Store the saved states in the arguments bundle.
            argumentsBundle.putBundle(SAVED_STATE, savedState)
            argumentsBundle.putBundle(SAVED_NESTED_SCROLL_WEBVIEW_STATE, savedNestedScrollWebViewState)
            argumentsBundle.putBoolean(BOTTOM_APP_BAR, bottomAppBar)

            // Create a new instance of the WebView tab fragment.
            val webViewTabFragment = WebViewTabFragment()

            // Add the arguments bundle to the fragment.
            webViewTabFragment.arguments = argumentsBundle

            // Return the new fragment.
            return webViewTabFragment
        }
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the new tab listener from the launching context.
        newTabListener = context as NewTabListener
    }

    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Get the bottom app bar status from the arguments.
        val bottomAppBar = requireArguments().getBoolean(BOTTOM_APP_BAR)

        // Inflate the tab's WebView according to the app bar position.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.
        // The fragment will take care of attaching the root automatically.
        val newPageView = if (bottomAppBar)
            layoutInflater.inflate(R.layout.webview_framelayout_bottom_appbar, container, false)
        else
            layoutInflater.inflate(R.layout.webview_framelayout_top_appbar, container, false)

        // Get handles for the views.
        nestedScrollWebView = newPageView.findViewById(R.id.nestedscroll_webview)
        val progressBar = newPageView.findViewById<ProgressBar>(R.id.progress_bar)

        // Store the WebView fragment ID in the nested scroll WebView.
        nestedScrollWebView.webViewFragmentId = fragmentId

        // Check to see if the fragment is being restarted without the app being killed.
        return if (savedInstanceState == null) {  // The fragment is not being restarted.  It is either new or is being restored after the app was killed.
            // Check to see if a new page is being created.
            if (requireArguments().getBoolean(CREATE_NEW_PAGE)) {  // A new page is being created.
                // Get the variables from the arguments
                val pagePosition = requireArguments().getInt(PAGE_POSITION)
                val url = requireArguments().getString(URL)!!

                // Request the main activity initialize the WebView.
                newTabListener.initializeWebView(nestedScrollWebView, pagePosition, progressBar, url, false)

                // Return the new page view.
                newPageView
            } else {  // A page is being restored after the app was killed.
                // Get the saved states from the arguments.
                val savedState = requireArguments().getBundle(SAVED_STATE)!!
                val savedNestedScrollWebViewState = requireArguments().getBundle(SAVED_NESTED_SCROLL_WEBVIEW_STATE)!!

                // Restore the WebView state.
                nestedScrollWebView.restoreState(savedState)

                // Restore the nested scroll WebView state.
                nestedScrollWebView.restoreNestedScrollWebViewState(savedNestedScrollWebViewState)

                // Initialize the WebView.
                newTabListener.initializeWebView(nestedScrollWebView, 0, progressBar, "", true)

                // Return the new page view.
                newPageView
            }
        } else {  // The fragment is being restarted.
            // Return null.  Otherwise, the fragment will be inflated and initialized by the OS on a restart, discarded, and then recreated with saved settings by Privacy Browser.
            null
        }
    }
}
