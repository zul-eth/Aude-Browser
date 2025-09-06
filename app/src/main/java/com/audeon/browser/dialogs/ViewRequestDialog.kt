/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2023 Soren Stoutner <soren@stoutner.com>
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

package com.audeon.browser.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.helpers.DOMAIN_ALLOWLIST
import com.audeon.browser.helpers.DOMAIN_BLOCKLIST
import com.audeon.browser.helpers.DOMAIN_FINAL_ALLOWLIST
import com.audeon.browser.helpers.DOMAIN_FINAL_BLOCKLIST
import com.audeon.browser.helpers.DOMAIN_INITIAL_ALLOWLIST
import com.audeon.browser.helpers.DOMAIN_INITIAL_BLOCKLIST
import com.audeon.browser.helpers.DOMAIN_REGULAR_EXPRESSION_BLOCKLIST
import com.audeon.browser.helpers.INITIAL_BLOCKLIST
import com.audeon.browser.helpers.REQUEST_ALLOWED
import com.audeon.browser.helpers.REQUEST_BLOCKED
import com.audeon.browser.helpers.REQUEST_BLOCKLIST
import com.audeon.browser.helpers.REQUEST_BLOCKLIST_ENTRIES
import com.audeon.browser.helpers.REQUEST_BLOCKLIST_ORIGINAL_ENTRY
import com.audeon.browser.helpers.REQUEST_DEFAULT
import com.audeon.browser.helpers.REQUEST_DISPOSITION
import com.audeon.browser.helpers.FINAL_ALLOWLIST
import com.audeon.browser.helpers.FINAL_BLOCKLIST
import com.audeon.browser.helpers.MAIN_ALLOWLIST
import com.audeon.browser.helpers.MAIN_BLOCKLIST
import com.audeon.browser.helpers.REGULAR_EXPRESSION_BLOCKLIST
import com.audeon.browser.helpers.REQUEST_SUBLIST
import com.audeon.browser.helpers.REQUEST_THIRD_PARTY
import com.audeon.browser.helpers.REQUEST_URL
import com.audeon.browser.helpers.THIRD_PARTY_BLOCKLIST
import com.audeon.browser.helpers.THIRD_PARTY_DOMAIN_BLOCKLIST
import com.audeon.browser.helpers.THIRD_PARTY_DOMAIN_INITIAL_ALLOWLIST
import com.audeon.browser.helpers.THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST
import com.audeon.browser.helpers.THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLOCKLIST
import com.audeon.browser.helpers.THIRD_PARTY_DOMAIN_ALLOWLIST
import com.audeon.browser.helpers.THIRD_PARTY_INITIAL_BLOCKLIST
import com.audeon.browser.helpers.THIRD_PARTY_ALLOWLIST
import com.audeon.browser.helpers.THIRD_PARTY_REGULAR_EXPRESSION_BLOCKLIST

// Define the class constants.
private const val ID = "id"
private const val IS_LAST_REQUEST = "is_last_request"
private const val REQUEST_DETAILS = "request_details"

class ViewRequestDialog : DialogFragment() {
    companion object {
        fun request(id: Int, isLastRequest: Boolean, requestDetails: Array<String>): ViewRequestDialog {
            // Create a bundle.
            val bundle = Bundle()

            // Store the request details.
            bundle.putInt(ID, id)
            bundle.putBoolean(IS_LAST_REQUEST, isLastRequest)
            bundle.putStringArray(REQUEST_DETAILS, requestDetails)

            // Create a new instance of the view request dialog.
            val viewRequestDialog = ViewRequestDialog()

            // Add the arguments to the new dialog.
            viewRequestDialog.arguments = bundle

            // Return the new dialog.
            return viewRequestDialog
        }
    }

    // Define the class variables.
    private lateinit var viewRequestListener: ViewRequestListener

    // The public interface is used to send information back to the parent activity.
    interface ViewRequestListener {
        // Show the previous request.
        fun onPrevious(currentId: Int)

        // Show the next request.
        fun onNext(currentId: Int)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener from the launching context.
        viewRequestListener = context as ViewRequestListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val id = requireArguments().getInt(ID)
        val isLastRequest = requireArguments().getBoolean(IS_LAST_REQUEST)
        val requestDetails = requireArguments().getStringArray(REQUEST_DETAILS)!!

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.block_ads_enabled)

        // Set the title.
        dialogBuilder.setTitle(resources.getString(R.string.request_details) + " - " + id)

        // Set the view.
        dialogBuilder.setView(R.layout.view_request_dialog)

        // Set the close button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNeutralButton(R.string.close, null)

        // Set the previous button.
        dialogBuilder.setNegativeButton(R.string.previous) { _: DialogInterface?, _: Int ->
            // Load the previous request.
            viewRequestListener.onPrevious(id)
        }

        // Set the next button.
        dialogBuilder.setPositiveButton(R.string.next) { _: DialogInterface?, _: Int ->
            // Load the next request.
            viewRequestListener.onNext(id)
        }

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Disable screenshots.
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        //The alert dialog must be shown before the contents can be modified.
        alertDialog.show()

        // Get handles for the dialog views.
        val requestDisposition = alertDialog.findViewById<TextView>(R.id.request_disposition)!!
        val requestUrl = alertDialog.findViewById<TextView>(R.id.request_url)!!
        val requestFilterListLabel = alertDialog.findViewById<TextView>(R.id.request_filterlist_label)!!
        val requestFilterList = alertDialog.findViewById<TextView>(R.id.request_filterlist)!!
        val requestSubListLabel = alertDialog.findViewById<TextView>(R.id.request_sublist_label)!!
        val requestSubList = alertDialog.findViewById<TextView>(R.id.request_sublist)!!
        val requestFilterListEntriesLabel = alertDialog.findViewById<TextView>(R.id.request_filterlist_entries_label)!!
        val requestFilterListEntries = alertDialog.findViewById<TextView>(R.id.request_filterlist_entries)!!
        val requestFilterListOriginalEntryLabel = alertDialog.findViewById<TextView>(R.id.request_filterlist_original_entry_label)!!
        val requestFilterListOriginalEntry = alertDialog.findViewById<TextView>(R.id.request_filterlist_original_entry)!!
        val previousButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val nextButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        // Disable the previous button if the first resource request is displayed.
        previousButton.isEnabled = (id != 1)

        // Disable the next button if the last resource request is displayed.
        nextButton.isEnabled = !isLastRequest

        // Set the request action text.
        when (requestDetails[REQUEST_DISPOSITION]) {
            REQUEST_DEFAULT -> {
                // Set the text.
                requestDisposition.setText(R.string.default_allowed)

                // Set the background color to be transparent.
                requestDisposition.setBackgroundColor(getColor(requireContext(), R.color.transparent))
            }

            REQUEST_ALLOWED -> {
                // Set the text.
                requestDisposition.setText(R.string.allowed)

                // Set the background color to be blue.
                requestDisposition.setBackgroundColor(getColor(requireContext(), R.color.requests_blue_background))
            }

            REQUEST_THIRD_PARTY -> {
                // Set the text.
                requestDisposition.setText(R.string.third_party_blocked)

                // Set the background color to be yellow.
                requestDisposition.setBackgroundColor(getColor(requireContext(), R.color.yellow_background))
            }

            REQUEST_BLOCKED -> {
                // Set the text.
                requestDisposition.setText(R.string.blocked)

                // Set the background color to be red.
                requestDisposition.setBackgroundColor(getColor(requireContext(), R.color.red_background))
            }
        }

        // Display the request URL.
        requestUrl.text = requestDetails[REQUEST_URL]

        // Modify the dialog based on the request action.
        if (requestDetails.size == 2) {  // A default request.
            // Hide the unused views.
            requestFilterListLabel.visibility = View.GONE
            requestFilterList.visibility = View.GONE
            requestSubListLabel.visibility = View.GONE
            requestSubList.visibility = View.GONE
            requestFilterListEntriesLabel.visibility = View.GONE
            requestFilterListEntries.visibility = View.GONE
            requestFilterListOriginalEntryLabel.visibility = View.GONE
            requestFilterListOriginalEntry.visibility = View.GONE
        } else {  // A blocked or allowed request.
            // Set the text on the text views.
            requestFilterList.text = requestDetails[REQUEST_BLOCKLIST]
            requestFilterListEntries.text = requestDetails[REQUEST_BLOCKLIST_ENTRIES]
            requestFilterListOriginalEntry.text = requestDetails[REQUEST_BLOCKLIST_ORIGINAL_ENTRY]
            when (requestDetails[REQUEST_SUBLIST]) {
                MAIN_ALLOWLIST -> requestSubList.setText(R.string.main_allowlist)
                FINAL_ALLOWLIST -> requestSubList.setText(R.string.final_allowlist)
                DOMAIN_ALLOWLIST -> requestSubList.setText(R.string.domain_allowlist)
                DOMAIN_INITIAL_ALLOWLIST -> requestSubList.setText(R.string.domain_initial_allowlist)
                DOMAIN_FINAL_ALLOWLIST -> requestSubList.setText(R.string.domain_final_allowlist)
                THIRD_PARTY_ALLOWLIST -> requestSubList.setText(R.string.third_party_allowlist)
                THIRD_PARTY_DOMAIN_ALLOWLIST -> requestSubList.setText(R.string.third_party_domain_allowlist)
                THIRD_PARTY_DOMAIN_INITIAL_ALLOWLIST -> requestSubList.setText(R.string.third_party_domain_initial_allowlist)
                MAIN_BLOCKLIST -> requestSubList.setText(R.string.main_blocklist)
                INITIAL_BLOCKLIST -> requestSubList.setText(R.string.initial_blocklist)
                FINAL_BLOCKLIST -> requestSubList.setText(R.string.final_blocklist)
                DOMAIN_BLOCKLIST -> requestSubList.setText(R.string.domain_blocklist)
                DOMAIN_INITIAL_BLOCKLIST -> requestSubList.setText(R.string.domain_initial_blocklist)
                DOMAIN_FINAL_BLOCKLIST -> requestSubList.setText(R.string.domain_final_blocklist)
                DOMAIN_REGULAR_EXPRESSION_BLOCKLIST -> requestSubList.setText(R.string.domain_regular_expression_blocklist)
                THIRD_PARTY_BLOCKLIST -> requestSubList.setText(R.string.third_party_blocklist)
                THIRD_PARTY_INITIAL_BLOCKLIST -> requestSubList.setText(R.string.third_party_initial_blocklist)
                THIRD_PARTY_DOMAIN_BLOCKLIST -> requestSubList.setText(R.string.third_party_domain_blocklist)
                THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST -> requestSubList.setText(R.string.third_party_domain_initial_blocklist)
                THIRD_PARTY_REGULAR_EXPRESSION_BLOCKLIST -> requestSubList.setText(R.string.third_party_regular_expression_blocklist)
                THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLOCKLIST -> requestSubList.setText(R.string.third_party_domain_regular_expression_blocklist)
                REGULAR_EXPRESSION_BLOCKLIST -> requestSubList.setText(R.string.regular_expression_blocklist)
            }
        }

        // Return the alert dialog.
        return alertDialog
    }
}
