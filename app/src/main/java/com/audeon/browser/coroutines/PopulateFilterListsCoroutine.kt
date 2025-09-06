/*
 * Copyright 2019, 2021-2024 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android>.
 *
 * Privacy Browser Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.audeon.browser.coroutines

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout

import com.audeon.browser.R
import com.audeon.browser.helpers.ParseFilterListHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.ArrayList

class PopulateFilterListsCoroutine(context: Context) {
    // The public interface is used to send information back to the parent activity.
    interface PopulateFilterListsListener {
        fun finishedPopulatingFilterLists(combinedFilterLists: ArrayList<ArrayList<List<Array<String>>>>)
    }

    // Define the class variables.
    private val context: Context
    private val populateFilterListsListener: PopulateFilterListsListener

    // The public constructor.
    init {
        // Get a handle for the populate filter lists listener from the launching activity.
        populateFilterListsListener = context as PopulateFilterListsListener

        // Store the context.
        this.context = context
    }

    fun populateFilterLists(activity: Activity) {
        // Use a coroutine to populate the filter lists.
        CoroutineScope(Dispatchers.Main).launch {
            // Get handles for the views.
            val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerlayout)
            val loadingFilterListsRelativeLayout = activity.findViewById<RelativeLayout>(R.id.loading_filterlists_relativelayout)
            val loadingFilterListTextView = activity.findViewById<TextView>(R.id.loading_filterlist_textview)

            // Show the loading filter lists screen.
            loadingFilterListsRelativeLayout.visibility = View.VISIBLE

            // Instantiate the filter list helper.
            val parseFilterListHelper = ParseFilterListHelper()

            // Create a combined array list.
            val combinedFilterLists = ArrayList<ArrayList<List<Array<String>>>>()

            // Advertise the loading of EasyList.
            loadingFilterListTextView.text = context.getString(R.string.loading_easylist)

            // Populate the filter lists on the IO thread.
            withContext(Dispatchers.IO) {
                // Populate EasyList.
                val easyList = parseFilterListHelper.parseFilterList(context.assets, "filterlists/easylist.txt")

                // Advertise the loading of EasyPrivacy.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_easyprivacy)
                }

                // Populate EasyPrivacy.
                val easyPrivacy = parseFilterListHelper.parseFilterList(context.assets, "filterlists/easyprivacy.txt")

                // Advertise the loading of Fanboy's Annoyance List.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_fanboys_annoyance_list)
                }

                // Populate Fanboy's Annoyance List.
                val fanboysAnnoyanceList = parseFilterListHelper.parseFilterList(context.assets, "filterlists/fanboy-annoyance.txt")

                // Advertise the loading of Fanboy's social blocking list.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_fanboys_social_blocking_list)
                }

                // Populate Fanboy's Social Blocking List.
                val fanboysSocialList = parseFilterListHelper.parseFilterList(context.assets, "filterlists/fanboy-social.txt")

                // Advertise the loading of UltraList
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_ultralist)
                }

                // Populate UltraList.
                val ultraList = parseFilterListHelper.parseFilterList(context.assets, "filterlists/ultralist.txt")

                // Advertise the loading of UltraPrivacy.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_ultraprivacy)
                }

                // Populate UltraPrivacy.
                val ultraPrivacy = parseFilterListHelper.parseFilterList(context.assets, "filterlists/ultraprivacy.txt")

                // Populate the combined array list.
                combinedFilterLists.add(easyList)
                combinedFilterLists.add(easyPrivacy)
                combinedFilterLists.add(fanboysAnnoyanceList)
                combinedFilterLists.add(fanboysSocialList)
                combinedFilterLists.add(ultraList)
                combinedFilterLists.add(ultraPrivacy)

                // Update the UI.
                withContext(Dispatchers.Main) {
                    // Show the drawer layout.
                    drawerLayout.visibility = View.VISIBLE

                    // Hide the loading filter lists screen.
                    loadingFilterListsRelativeLayout.visibility = View.GONE

                    // Enable the sliding drawers.
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                    // Continue loading the app.
                    populateFilterListsListener.finishedPopulatingFilterLists(combinedFilterLists)
                }
            }
        }
    }
}
