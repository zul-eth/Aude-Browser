/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2019-2023 Soren Stoutner <soren@stoutner.com>
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

package com.audeon.browser.adapters

import android.os.Bundle
import android.widget.FrameLayout

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

import com.audeon.browser.R
import com.audeon.browser.fragments.WebViewTabFragment
import com.audeon.browser.views.NestedScrollWebView

import java.util.LinkedList

class WebViewStateAdapter(fragmentActivity: FragmentActivity, private val bottomAppBar: Boolean) : FragmentStateAdapter(fragmentActivity) {
    // Define the class variables.
    private val webViewFragmentsList = LinkedList<WebViewTabFragment>()

    // Get a page fragment.
    override fun createFragment(pageNumber: Int): Fragment {
        // Get the fragment for a particular page.  Page numbers are 0 indexed.
        return webViewFragmentsList[pageNumber]
    }

    override fun containsItem(itemId: Long): Boolean {
        // Initialize the position variable.
        var position = -1

        // Initialize the while counter.
        var i = 0

        // Find the current position of the WebView fragment with the given ID.
        while ((position < 0) && (i < webViewFragmentsList.size)) {
            // Check to see if the tab ID of this WebView matches the page ID.
            if (webViewFragmentsList[i].fragmentId == itemId) {
                // Store the position if they are a match.
                position = i
            }

            // Increment the counter.
            i++
        }

        // Return true if the item was found in the WebView fragments list.
        return (position != -1)
    }

    // Get the number of tabs.
    override fun getItemCount(): Int {
        // Return the number of pages.
        return webViewFragmentsList.size
    }

    // Get the unique ID for the item.
    override fun getItemId(position: Int): Long {
        // Return the unique ID for this page.
        return if ((position >= 0) && (position < webViewFragmentsList.size))  // The position is 0 based, so it is contained in the WebView fragment list.
            webViewFragmentsList[position].fragmentId
        else  // The item does not exist.
            NO_ID
    }

    fun addPage(pagePosition: Int, url: String) {
        // Add a new page.
        webViewFragmentsList.add(pagePosition, WebViewTabFragment.createPage(pagePosition, url, bottomAppBar))

        // Update the view pager.
        notifyItemInserted(pagePosition)
    }

    fun deletePage(pageNumber: Int, webViewPager2: ViewPager2): Boolean {
        // Get the WebView tab fragment.
        val webViewTabFragment = webViewFragmentsList[pageNumber]

        // Get the WebView frame layout.
        val webViewFrameLayout = (webViewTabFragment.view as FrameLayout)

        // Get a handle for the nested scroll WebView.
        val nestedScrollWebView = webViewFrameLayout.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

        // Pause the current WebView.
        nestedScrollWebView.onPause()

        // Remove all the views from the frame layout.
        webViewFrameLayout.removeAllViews()

        // Destroy the current WebView.
        nestedScrollWebView.destroy()

        // Delete the page.
        webViewFragmentsList.removeAt(pageNumber)

        // Update the view pager.
        notifyItemRemoved(pageNumber)

        // Return true if the selected page number did not change after the delete (because the newly selected tab has has same number as the previously deleted tab).
        // This will cause the calling method to reset the current WebView to the new contents of this page number.
        return (webViewPager2.currentItem == pageNumber)
    }

    fun getPageFragment(pageNumber: Int): WebViewTabFragment {
        // Return the page fragment.
        return webViewFragmentsList[pageNumber]
    }

    fun getPositionForId(fragmentId: Long): Int {
        // Initialize the position variable.
        var position = -1

        // Initialize the while counter.
        var i = 0

        // Find the current position of the WebView fragment with the given ID.
        while ((position < 0) && (i < webViewFragmentsList.size)) {
            // Check to see if the tab ID of this WebView matches the page ID.
            if (webViewFragmentsList[i].fragmentId == fragmentId) {
                // Store the position if they are a match.
                position = i
            }

            // Increment the counter.
            i++
        }

        // Set the position to be the last tab if it is not found.
        // Sometimes there is a race condition in populating the webView fragments list when resuming Privacy Browser and displaying an SSL certificate error while loading a new intent.
        // In that case, the last tab should be the one it is looking for, which is one less than the size because it is zero based.
        if (position == -1)
            position = (webViewFragmentsList.size - 1)

        // Return the position.
        return position
    }

    fun restorePage(savedState: Bundle, savedNestedScrollWebViewState: Bundle) {
        // Restore the page.
        webViewFragmentsList.add(WebViewTabFragment.restorePage(savedState, savedNestedScrollWebViewState, bottomAppBar))

        // Update the view pager.  The position is zero indexed.
        notifyItemInserted(webViewFragmentsList.size - 1)
    }
}
