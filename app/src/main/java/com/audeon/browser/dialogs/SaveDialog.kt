/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2019-2024 Soren Stoutner <soren@stoutner.com>
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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.helpers.UrlHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Define the private class constants.
private const val URL_STRING = "A"
private const val FILE_SIZE_STRING = "B"
private const val FILE_NAME_STRING = "C"
private const val USER_AGENT_STRING = "D"
private const val COOKIES_ENABLED = "E"

class SaveDialog : DialogFragment() {
    companion object {
        fun saveUrl(urlString: String, fileNameString: String, fileSizeString: String, userAgentString: String, cookiesEnabled: Boolean): SaveDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putString(URL_STRING, urlString)
            argumentsBundle.putString(FILE_NAME_STRING, fileNameString)
            argumentsBundle.putString(FILE_SIZE_STRING, fileSizeString)
            argumentsBundle.putString(USER_AGENT_STRING, userAgentString)
            argumentsBundle.putBoolean(COOKIES_ENABLED, cookiesEnabled)

            // Create a new instance of the save webpage dialog.
            val saveDialog = SaveDialog()

            // Add the arguments bundle to the new dialog.
            saveDialog.arguments = argumentsBundle

            // Return the new dialog.
            return saveDialog
        }
    }

    // Declare the class variables.
    private lateinit var saveListener: SaveListener

    // The public interface is used to send information back to the parent activity.
    interface SaveListener {
        // Save with Android's download manager.
        fun saveWithAndroidDownloadManager(dialogFragment: DialogFragment)

        // Save with Privacy Browser.
        fun saveWithaudeonbrowser(originalUrlString: String, fileNameString: String, dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the save webpage listener from the launching context.
        saveListener = context as SaveListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments
        val arguments = requireArguments()

        // Get the arguments from the bundle.
        val originalUrlString = arguments.getString(URL_STRING)!!
        var fileNameString = arguments.getString(FILE_NAME_STRING)!!
        val fileSizeString = arguments.getString(FILE_SIZE_STRING)!!
        val userAgentString = arguments.getString(USER_AGENT_STRING)!!
        val cookiesEnabled = arguments.getBoolean(COOKIES_ENABLED)

        // Get the download provider entry values string array.
        val downloadProviderEntryValuesStringArray = resources.getStringArray(R.array.download_provider_entry_values)

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val downloadProvider = sharedPreferences.getString(getString(R.string.download_provider_key), getString(R.string.download_provider_default_value))!!

        // Determine the download provider.
        val audeonbrowserDownloadProvider = downloadProvider == downloadProviderEntryValuesStringArray[0]

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.save_url)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.download)

        // Set the view.
        dialogBuilder.setView(R.layout.save_dialog)

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the save button listener.
        dialogBuilder.setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
            // Save the URL with the selected download provider.
            if (audeonbrowserDownloadProvider)  // Download with Privacy Browser.
                saveListener.saveWithaudeonbrowser(originalUrlString, fileNameString, this)
            else  // Download with Android's download manager.
                saveListener.saveWithAndroidDownloadManager(this)
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get handles for the layout items.
        val urlEditText = alertDialog.findViewById<EditText>(R.id.url_edittext)!!
        val fileSizeTextView = alertDialog.findViewById<TextView>(R.id.file_size_textview)!!
        val blobUrlWarningTextView = alertDialog.findViewById<TextView>(R.id.blob_url_warning_textview)!!
        val dataUrlWarningTextView = alertDialog.findViewById<TextView>(R.id.data_url_warning_textview)!!
        val androidDownloadManagerLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.android_download_manager_linearlayout)!!
        val fileNameEditText = alertDialog.findViewById<TextView>(R.id.file_name_edittext)!!
        val saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Display the extra views if using Android's download manager.
        if (!audeonbrowserDownloadProvider)
            androidDownloadManagerLinearLayout.visibility = View.VISIBLE

        // Populate the views.
        fileSizeTextView.text = fileSizeString
        fileNameEditText.text = fileNameString

        // Populate the URL edit text according to the type.  This must be done before the text change listener is created below so that the file size isn't requested again.
        if (originalUrlString.startsWith("data:")) {  // The URL contains the entire data of an image.
            // Get a substring of the data URL with the first 100 characters.  Otherwise, the user interface will freeze while trying to layout the edit text.
            val urlSubstring = originalUrlString.substring(0, 100) + "…"

            // Populate the URL edit text with the truncated URL.
            urlEditText.setText(urlSubstring)

            // Disable the editing of the URL edit text.
            urlEditText.inputType = InputType.TYPE_NULL

            // Display the warning if using Android's download manager.
            if (!audeonbrowserDownloadProvider) {
                // Display the data URL warning.
                dataUrlWarningTextView.visibility = View.VISIBLE

                // Disable the save button.
                saveButton.isEnabled = false
            }
        } else {  // The URL contains a reference to the location of the data.
            // Populate the URL edit text with the full URL.
            urlEditText.setText(originalUrlString)
        }

        // Handle blob URLs.
        if (originalUrlString.startsWith("blob:")) {
            // Display the blob URL warning.
            blobUrlWarningTextView.visibility = View.VISIBLE

            // Disable the save button.
            saveButton.isEnabled = false
        }

        // Update the UI when the URL changes.
        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable?) {
                // Get the contents of the edit texts.
                val urlToSave = urlEditText.text.toString()
                val fileName = fileNameEditText.text.toString()

                // Determine if this is a blob URL.
                val blobUrl = urlToSave.startsWith("blob:")

                // Set the display status of the blob warning.
                if (blobUrl)
                    blobUrlWarningTextView.visibility = View.VISIBLE
                else
                    blobUrlWarningTextView.visibility = View.GONE

                // Enable the save button if the edit texts are populated and this isn't a blob URL.
                saveButton.isEnabled = urlToSave.isNotBlank() && fileName.isNotBlank() && !blobUrl

                // Determine if this is a data URL.
                val dataUrl = urlToSave.startsWith("data:")

                // Only process the URL if it is not a data URL.
                if (!dataUrl) {
                    CoroutineScope(Dispatchers.Main).launch {
                        // Create a URL size string.
                        var fileNameAndSize: Pair<String, String>

                        // Get the URL size on the IO thread.
                        withContext(Dispatchers.IO) {
                            // Get the updated file name and size.
                            fileNameAndSize = UrlHelper.getNameAndSize(requireContext(), urlToSave, userAgentString, cookiesEnabled)

                            // Save the updated file name.
                            fileNameString = fileNameAndSize.first
                        }

                        // Display the updated file size.
                        fileSizeTextView.text = fileNameAndSize.second
                    }
                }
            }
        })

        // Update the UI when the file name changes.
        fileNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable?) {
                // Get the contents of the edit texts.
                val urlToSave = urlEditText.text.toString()
                val fileName = fileNameEditText.text.toString()

                // Determine if this is a blob URL.
                val blobUrl = urlToSave.startsWith("blob:")

                // Enable the save button if the edit texts are populated and this isn't a blob URL (or a data URL using Android's download manager).
                saveButton.isEnabled = urlToSave.isNotBlank() && fileName.isNotBlank() && !blobUrl && !dataUrlWarningTextView.isVisible
            }
        })

        // Return the alert dialog.
        return alertDialog
    }
}
