/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2023 Soren Stoutner <soren@stoutner.com>
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
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R

// Define the public class constants.
const val AVAILABLE_CIPHERS = 0
const val SSL_CERTIFICATE = 1

// Define the private class constants.
private const val DIALOG_TYPE = "A"
private const val MESSAGE = "B"
private const val APPLIED_CIPHER_STRING = "C"

class ViewHeadersDetailDialog : DialogFragment() {
    companion object {
        fun displayDialog(dialogType: Int, message: String, appliedCipherString: String = ""): ViewHeadersDetailDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the SSL error message components in the bundle.
            argumentsBundle.putInt(DIALOG_TYPE, dialogType)
            argumentsBundle.putString(MESSAGE, message)
            argumentsBundle.putString(APPLIED_CIPHER_STRING, appliedCipherString)

            // Create a new instance of the SSL certificate error dialog.
            val thisHeadersSslCertificateDialog = ViewHeadersDetailDialog()

            // Add the arguments bundle to the new dialog.
            thisHeadersSslCertificateDialog.arguments = argumentsBundle

            // Return the new dialog.
            return thisHeadersSslCertificateDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val dialogType = requireArguments().getInt(DIALOG_TYPE)
        val message = requireArguments().getString(MESSAGE)!!
        val appliedCipherString = requireArguments().getString(APPLIED_CIPHER_STRING)!!

        // Use a builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the icon according to the theme.
        dialogBuilder.setIcon(R.drawable.ssl_certificate)

        // Set the title and message according to the type.
        if (dialogType == AVAILABLE_CIPHERS) {  // A cipher suite dialog is displayed.
            // Set the title
            dialogBuilder.setTitle(R.string.available_ciphers)

            // Create a message spannable string builder with the applied cipher bolded.
            val messageSpannableStringBuilder = SpannableStringBuilder(message)

            // Get the applied cipher index.
            val appliedCipherIndex = message.indexOf(appliedCipherString)

            // Set the applied cipher to be bold.
            messageSpannableStringBuilder.setSpan(StyleSpan(Typeface.BOLD), appliedCipherIndex, appliedCipherIndex + appliedCipherString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set the message.
            dialogBuilder.setMessage(messageSpannableStringBuilder)
        } else {  // An SSL certificate dialog is displayed.
            // Set the title and message.
            dialogBuilder.setTitle(R.string.ssl_certificate)
            dialogBuilder.setMessage(message)
        }

        // Set the close button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.close, null)

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Return the alert dialog.
        return alertDialog
    }
}
