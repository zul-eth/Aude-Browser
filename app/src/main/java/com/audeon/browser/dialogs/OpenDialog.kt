/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2019-2023, 2025 Soren Stoutner <soren@stoutner.com>
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R

// Define the class constants.
private const val MHT_EXPLANATION_VISIBILITY = "mht_explanation_visibility"

class OpenDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var openListener: OpenListener

    // Declare the class views.
    private lateinit var fileNameEditText: EditText
    private lateinit var mhtExplanationTextView: TextView

    // The public interface is used to send information back to the parent activity.
    interface OpenListener {
        fun openFile(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the open listener from the launching context.
        openListener = context as OpenListener
    }

    // Define the browse activity result launcher.
    private val browseActivityResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { fileUri: Uri? ->
        // Only do something if the user didn't press back from the file picker.
        if (fileUri != null) {
            // Get the file name string from the URI.
            val fileNameString = fileUri.toString()

            // Set the file name text.
            fileNameEditText.setText(fileNameString)

            // Move the cursor to the end of the file name edit text.
            fileNameEditText.setSelection(fileNameString.length)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.proxy_enabled)

        // Set the title.
        dialogBuilder.setTitle(R.string.open)

        // Set the view.
        dialogBuilder.setView(R.layout.open_dialog)

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the open button listener.
        dialogBuilder.setPositiveButton(R.string.open) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity.
            openListener.openFile(this)
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get handles for the layout items.
        fileNameEditText = alertDialog.findViewById(R.id.file_name_edittext)!!
        val browseButton = alertDialog.findViewById<Button>(R.id.browse_button)!!
        val mhtCheckBox = alertDialog.findViewById<CheckBox>(R.id.mht_checkbox)!!
        mhtExplanationTextView = alertDialog.findViewById(R.id.mht_explanation_textview)!!
        val openButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Initially disable the open button.
        openButton.isEnabled = false

        // Only display the MHT check box if the API <= 34 (Android 14).
        if (Build.VERSION.SDK_INT <= 34)
            mhtCheckBox.visibility = View.VISIBLE
        else
            mhtCheckBox.visibility = View.GONE

        // Update the status of the open button when the file name changes.
        fileNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable) {
                // Get the current file name.
                val fileNameString = fileNameEditText.text.toString()

                // Enable the open button if the file name is populated.
                openButton.isEnabled = fileNameString.isNotEmpty()
            }
        })

        // Handle clicks on the browse button.
        browseButton.setOnClickListener {
            // Launch the file picker.
            browseActivityResultLauncher.launch("*/*")
        }

        // Handle clicks on the MHT checkbox.
        mhtCheckBox.setOnClickListener {
            // Update the visibility of the MHT explanation text view.
            if (mhtCheckBox.isChecked) {
                mhtExplanationTextView.visibility = View.VISIBLE
            } else {
                mhtExplanationTextView.visibility = View.GONE
            }
        }

        // Restore the MHT explanation text view visibility if the saved instance state is not null.
        if (savedInstanceState != null) {
            // Restore the MHT explanation text view visibility.
            if (savedInstanceState.getBoolean(MHT_EXPLANATION_VISIBILITY)) {
                mhtExplanationTextView.visibility = View.VISIBLE
            } else {
                mhtExplanationTextView.visibility = View.GONE
            }
        }

        // Return the alert dialog.
        return alertDialog
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Add the MHT explanation visibility status to the bundle.
        savedInstanceState.putBoolean(MHT_EXPLANATION_VISIBILITY, mhtExplanationTextView.isVisible)
    }
}
