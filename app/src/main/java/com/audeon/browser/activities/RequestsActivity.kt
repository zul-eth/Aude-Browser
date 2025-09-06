/*
 * Copyright 2018-2024 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
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

package com.audeon.browser.activities

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.ResourceCursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.adapters.RequestsArrayAdapter
import com.audeon.browser.dialogs.ViewRequestDialog.Companion.request
import com.audeon.browser.dialogs.ViewRequestDialog.ViewRequestListener
import com.audeon.browser.helpers.REQUEST_ALLOWED
import com.audeon.browser.helpers.REQUEST_BLOCKED
import com.audeon.browser.helpers.REQUEST_DEFAULT
import com.audeon.browser.helpers.REQUEST_DISPOSITION
import com.audeon.browser.helpers.REQUEST_THIRD_PARTY

// Define the public constants.
const val BLOCK_ALL_THIRD_PARTY_REQUESTS = "block_all_third_party_requests"

// Define the private class constants.
private const val LISTVIEW_POSITION = "listview_position"

class RequestsActivity : AppCompatActivity(), ViewRequestListener {
    companion object {
        // The resource requests are populated by `MainWebViewActivity` before `RequestsActivity` is launched.
        var resourceRequests: List<Array<String>>? = null
    }

    // Define the class views.
    private lateinit var requestsListView: ListView

    public override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Get the launching intent
        val intent = intent

        // Get the status of the third-party filter list.
        val blockAllThirdPartyRequests = intent.getBooleanExtra(BLOCK_ALL_THIRD_PARTY_REQUESTS, false)

        // Set the content view.
        if (bottomAppBar) {
            setContentView(R.layout.requests_bottom_appbar)
        } else {
            setContentView(R.layout.requests_top_appbar)
        }

        // Get a handle for the toolbar.
        val toolbar = findViewById<Toolbar>(R.id.requests_toolbar)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the app bar.
        val appBar = supportActionBar!!

        // Set the app bar custom view.
        appBar.setCustomView(R.layout.spinner)

        // Display the back arrow in the app bar.
        appBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_HOME_AS_UP

        // Get handles for the views.
        val appBarSpinner = findViewById<Spinner>(R.id.spinner)
        requestsListView = findViewById(R.id.requests_listview)

        // Initialize the resource array lists.  A list is needed for all the resource requests, or the activity can crash if `MainWebViewActivity.resourceRequests` is modified after the activity loads.
        val allResourceRequests: MutableList<Array<String>> = ArrayList()
        val defaultResourceRequests: MutableList<Array<String>> = ArrayList()
        val allowedResourceRequests: MutableList<Array<String>> = ArrayList()
        val thirdPartyResourceRequests: MutableList<Array<String>> = ArrayList()
        val blockedResourceRequests: MutableList<Array<String>> = ArrayList()

        // Populate the resource array lists.
        for (request in resourceRequests!!) {
            // Add the request to the list of all requests.
            allResourceRequests.add(request)

            when (request[REQUEST_DISPOSITION]) {
                REQUEST_DEFAULT -> {
                    // Add the request to the list of default requests.
                    defaultResourceRequests.add(request)
                }

                REQUEST_ALLOWED -> {
                    // Add the request to the list of allowed requests.
                    allowedResourceRequests.add(request)
                }

                REQUEST_THIRD_PARTY -> {
                    // Add the request to the list of third-party requests.
                    thirdPartyResourceRequests.add(request)
                }

               REQUEST_BLOCKED -> {
                    // Add the request to the list of blocked requests.
                    blockedResourceRequests.add(request)
                }
            }
        }

        // Setup a matrix cursor for the resource lists.
        val spinnerCursor = MatrixCursor(arrayOf("_id", "Requests"))
        spinnerCursor.addRow(arrayOf<Any>(0, getString(R.string.all) + " - " + allResourceRequests.size))
        spinnerCursor.addRow(arrayOf<Any>(1, getString(R.string.default_label) + " - " + defaultResourceRequests.size))
        spinnerCursor.addRow(arrayOf<Any>(2, getString(R.string.allowed_plural) + " - " + allowedResourceRequests.size))
        if (blockAllThirdPartyRequests)
            spinnerCursor.addRow(arrayOf<Any>(3, getString(R.string.third_party_plural) + " - " + thirdPartyResourceRequests.size))
        spinnerCursor.addRow(arrayOf<Any>(4, getString(R.string.blocked_plural) + " - " + blockedResourceRequests.size))

        // Create a resource cursor adapter for the spinner.
        val spinnerCursorAdapter: ResourceCursorAdapter = object : ResourceCursorAdapter(this, R.layout.requests_appbar_spinner_item, spinnerCursor, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get a handle for the spinner item text view.
                val spinnerItemTextView = view.findViewById<TextView>(R.id.spinner_item_textview)

                // Set the text view to display the resource list.
                spinnerItemTextView.text = cursor.getString(1)
            }
        }

        // Set the resource cursor adapter drop down view resource.
        spinnerCursorAdapter.setDropDownViewResource(R.layout.requests_appbar_spinner_dropdown_item)

        // Set the app bar spinner adapter.
        appBarSpinner.adapter = spinnerCursorAdapter

        // Get a handle for the context.
        val context: Context = this

        // Handle clicks on the spinner dropdown.
        appBarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (id.toInt()) {
                    0 -> {
                        // Get an adapter for all the requests.
                        val allResourceRequestsArrayAdapter: ArrayAdapter<Array<String>> = RequestsArrayAdapter(context, allResourceRequests)

                        // Display the adapter in the list view.
                        requestsListView.adapter = allResourceRequestsArrayAdapter
                    }

                    1 -> {
                        // Get an adapter for the default requests.
                        val defaultResourceRequestsArrayAdapter: ArrayAdapter<Array<String>> = RequestsArrayAdapter(context, defaultResourceRequests)

                        // Display the adapter in the list view.
                        requestsListView.adapter = defaultResourceRequestsArrayAdapter
                    }

                    2 -> {
                        // Get an adapter for the allowed requests.
                        val allowedResourceRequestsArrayAdapter: ArrayAdapter<Array<String>> = RequestsArrayAdapter(context, allowedResourceRequests)

                        // Display the adapter in the list view.
                        requestsListView.adapter = allowedResourceRequestsArrayAdapter
                    }

                    3 -> {
                        // Get an adapter for the third-party requests.
                        val thirdPartyResourceRequestsArrayAdapter: ArrayAdapter<Array<String>> = RequestsArrayAdapter(context, thirdPartyResourceRequests)

                        //Display the adapter in the list view.
                        requestsListView.adapter = thirdPartyResourceRequestsArrayAdapter
                    }

                    4 -> {
                        // Get an adapter for the blocked requests.
                        val blockedResourceRequestsArrayAdapter: ArrayAdapter<Array<String>> = RequestsArrayAdapter(context, blockedResourceRequests)

                        // Display the adapter in the list view.
                        requestsListView.adapter = blockedResourceRequestsArrayAdapter
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Create an array adapter with the list of the resource requests.
        val resourceRequestsArrayAdapter: ArrayAdapter<Array<String>> = RequestsArrayAdapter(context, allResourceRequests)

        // Populate the list view with the resource requests adapter.
        requestsListView.adapter = resourceRequestsArrayAdapter

        // Listen for taps on entries in the list view.
        requestsListView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            // Display the view request dialog.  The list view is 0 based, so the position must be incremented by 1.
            launchViewRequestDialog(position + 1)
        }

        // Check to see if the activity has been restarted.
        if (savedInstanceState != null) {
            // Scroll to the saved position.
            requestsListView.post { requestsListView.setSelection(savedInstanceState.getInt(LISTVIEW_POSITION)) }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(outState)

        // Get the listview position.
        val listViewPosition = requestsListView.firstVisiblePosition

        // Store the listview position in the bundle.
        outState.putInt(LISTVIEW_POSITION, listViewPosition)
    }

    override fun onPrevious(currentId: Int) {
        // Show the previous dialog.
        launchViewRequestDialog(currentId - 1)
    }

    override fun onNext(currentId: Int) {
        // Show the next dialog.
        launchViewRequestDialog(currentId + 1)
    }

    private fun launchViewRequestDialog(id: Int) {
        // Determine if this is the last request in the list.
        val isLastRequest = (id == requestsListView.count)

        // Get the string array for the selected resource request.  The resource requests list view is zero based.  There is no need to check to make sure each string is not null because
        @Suppress("UNCHECKED_CAST") val selectedRequestStringArray = (requestsListView.getItemAtPosition(id - 1) as Array<String>)

        // Create a view request dialog.
        val viewRequestDialogFragment: DialogFragment = request(id, isLastRequest, selectedRequestStringArray)

        // Make it so.
        viewRequestDialogFragment.show(supportFragmentManager, getString(R.string.request_details))
    }
}
