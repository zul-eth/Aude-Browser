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

package com.audeon.browser.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R

// Define the class constants.
private const val FONT_SIZE = "font_size"

class FontSizeDialog : DialogFragment() {
    companion object {
        fun displayDialog(fontSize: Int): FontSizeDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the font size in the bundle.
            argumentsBundle.putInt(FONT_SIZE, fontSize)

            // Create a new instance of the dialog.
            val fontSizeDialog = FontSizeDialog()

            // Add the bundle to the dialog.
            fontSizeDialog.arguments = argumentsBundle

            // Return the new dialog.
            return fontSizeDialog
        }
    }

    // Declare the class variables.
    private lateinit var updateFontSizeListener: UpdateFontSizeListener

    // The public interface is used to send information back to the parent activity.
    interface UpdateFontSizeListener {
        fun updateFontSize(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the update font size listener from the launching context.
        updateFontSizeListener = context as UpdateFontSizeListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the current font size from the arguments.
        val currentFontSize = requireArguments().getInt(FONT_SIZE)

        // Use a builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the icon according to the theme.
        dialogBuilder.setIcon(R.drawable.font_size)

        // Set the title.
        dialogBuilder.setTitle(R.string.font_size)

        // Set the view.
        dialogBuilder.setView(R.layout.font_size_dialog)

        // Set the close button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.close, null)

        // Set the apply button listener.
        dialogBuilder.setPositiveButton(R.string.apply) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity.
            updateFontSizeListener.updateFontSize(this)
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Get the alert dialog window.
        val dialogWindow = alertDialog.window!!

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Display the keyboard.
        dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get a handle for the font size edit text.
        val fontSizeEditText = alertDialog.findViewById<EditText>(R.id.font_size_edittext)!!

        // Display the current font size.
        fontSizeEditText.setText(currentFontSize.toString())

        // Request focus on the font size edit text.
        fontSizeEditText.requestFocus()

        // Set the enter key on the keyboard to update the font size.
        fontSizeEditText.setOnKeyListener { _: View?, keyCode: Int, keyEvent: KeyEvent ->
            // Check the key code, event, and button status.
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {  // The enter key was pressed.
                // Trigger the update font size listener and return the dialog fragment to the parent activity.
                updateFontSizeListener.updateFontSize(this)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                //Consume the event.
                return@setOnKeyListener true
            } else {  // If any other key was pressed do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Return the alert dialog.
        return alertDialog
    }
}
