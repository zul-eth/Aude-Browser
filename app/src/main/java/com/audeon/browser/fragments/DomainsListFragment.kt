/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2017-2020, 2022, 2024 Soren Stoutner <soren@stoutner.com>
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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView

import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow

import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.audeon.browser.R
import com.audeon.browser.activities.DOMAIN_SETTINGS_FRAGMENT_TAG
import com.audeon.browser.activities.DomainsActivity

class DomainsListFragment : Fragment() {
    // Declare the class variables.
    private lateinit var dismissSnackbarInterface: DismissSnackbarInterface
    private lateinit var saveDomainSettingsInterface: SaveDomainSettingsInterface

    // Define the public dismiss snackbar interface.
    interface DismissSnackbarInterface {
        fun dismissSnackbar()
    }

    // Define the public save domain interface.
    interface SaveDomainSettingsInterface {
        fun saveDomainSettings(view: View)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Populate the interfaces.
        dismissSnackbarInterface = context as DismissSnackbarInterface
        saveDomainSettingsInterface = context as SaveDomainSettingsInterface
    }

    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout.  The fragment will take care of attaching the root automatically.
        val domainsListFragmentView = layoutInflater.inflate(R.layout.domains_list_fragment, container, false)

        // Get a handle for the domains list view.
        val domainsListView = domainsListFragmentView.findViewById<ListView>(R.id.domains_listview)

        // Get a handle for the support fragment manager.
        val supportFragmentManager = requireActivity().supportFragmentManager

        // Handle clicks on the domains list view.
        domainsListView.onItemClickListener = OnItemClickListener { _: AdapterView<*>, _: View, _: Int, id: Long ->
            // Dismiss the snackbar if it is visible.
            dismissSnackbarInterface.dismissSnackbar()

            // Get a handle for the old domain settings fragment.
            val oldDomainSettingsFragment = supportFragmentManager.findFragmentById(R.id.domain_settings_fragment_container)

            // Save the current domain settings if operating in two-paned mode and a domain is currently selected.
            if (DomainsActivity.twoPanedMode && (oldDomainSettingsFragment != null)) {
                // Get a handle for the old domain settings fragment view.
                val oldDomainSettingsFragmentView = oldDomainSettingsFragment.requireView()

                // Save the domain settings.
                saveDomainSettingsInterface.saveDomainSettings(oldDomainSettingsFragmentView)
            }

            // Store the new current domain database ID, converting it from long to int to match the format of the domains database.
            DomainsActivity.currentDomainDatabaseId = id.toInt()

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Add the current domain database ID to the arguments bundle.
            argumentsBundle.putInt(DomainSettingsFragment.DATABASE_ID, DomainsActivity.currentDomainDatabaseId)

            // Create a new domains settings fragment instance.
            val domainSettingsFragment = DomainSettingsFragment()

            // Add the arguments bundle to the domain settings fragment.
            domainSettingsFragment.arguments = argumentsBundle

            // Check to see if the device is in two paned mode.
            if (DomainsActivity.twoPanedMode) {  // The device in in two-paned mode.
                // Display the domain settings fragment.
                supportFragmentManager.commitNow {
                    replace(R.id.domain_settings_fragment_container, domainSettingsFragment, DOMAIN_SETTINGS_FRAGMENT_TAG)
                }
            } else { // The device in in single-paned mode
                // Save the domains listview position.
                DomainsActivity.domainsListViewPosition = domainsListView.firstVisiblePosition

                // Get a handle for the add domain floating action button.
                val addDomainFab = requireActivity().findViewById<FloatingActionButton>(R.id.add_domain_fab)

                // Hide the add domain FAB.
                addDomainFab.hide()

                // Display the domain settings fragment.
                supportFragmentManager.commitNow {
                    replace(R.id.domains_listview_fragment_container, domainSettingsFragment, DOMAIN_SETTINGS_FRAGMENT_TAG)
                }
            }
        }

        // Return the domains list fragment.
        return domainsListFragmentView
    }
}
