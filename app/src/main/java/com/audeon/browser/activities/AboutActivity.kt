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

package com.audeon.browser.activities

import android.os.Bundle
import android.view.WindowManager

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

import com.audeon.browser.R
import com.audeon.browser.adapters.AboutStateAdapter

const val FILTERLIST_VERSIONS = "filterlist_versions"

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Get the intent that launched the activity.
        val launchingIntent = intent

        // Get the filter list versions.
        val filterListVersions = launchingIntent.getStringArrayExtra(FILTERLIST_VERSIONS)!!

        // Set the content view.
        if (bottomAppBar)
            setContentView(R.layout.about_bottom_appbar)
        else
            setContentView(R.layout.about_top_appbar)

        // Get handles for the views.
        val toolbar = findViewById<Toolbar>(R.id.about_toolbar)
        val aboutTabLayout = findViewById<TabLayout>(R.id.about_tablayout)
        val aboutViewPager2 = findViewById<ViewPager2>(R.id.about_viewpager2)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Display the home arrow on the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Initialize the about state adapter.
        val aboutStateAdapter = AboutStateAdapter(this, filterListVersions)

        // Set the view pager adapter.
        aboutViewPager2.adapter = aboutStateAdapter

        // Disable swiping between pages in the view pager.
        aboutViewPager2.isUserInputEnabled = false

        // Create a tab layout mediator.  Tab numbers start at 0.
        TabLayoutMediator(aboutTabLayout, aboutViewPager2) { tab, position ->
            // Set the tab text based on the position.
            tab.text = when (position) {
                0 -> getString(R.string.version)
                1 -> getString(R.string.permissions)
                2 -> getString(R.string.privacy_policy)
                3 -> getString(R.string.licenses)
                else -> ""
            }
        }.attach()
    }
}
