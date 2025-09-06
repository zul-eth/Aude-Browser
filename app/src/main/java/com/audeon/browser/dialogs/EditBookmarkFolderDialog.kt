/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2025 Soren Stoutner <soren@stoutner.com>
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.google.android.material.snackbar.Snackbar

import com.audeon.browser.R
import com.audeon.browser.helpers.BOOKMARK_NAME
import com.audeon.browser.helpers.FAVORITE_ICON
import com.audeon.browser.helpers.BookmarksDatabaseHelper

import java.io.ByteArrayOutputStream

// Define the class constants.
private const val DATABASE_ID = "A"
private const val FAVORITE_ICON_BYTE_ARRAY = "B"

class EditBookmarkFolderDialog : DialogFragment() {
    companion object {
        fun editFolder(databaseId: Int, favoriteIconBitmap: Bitmap): EditBookmarkFolderDialog {
            // Create a favorite icon byte array output stream.
            val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon to a PNG and place it in the byte array output stream.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

            // Convert the byte array output stream to a byte array.
            val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

            // Create an arguments bundle
            val argumentsBundle = Bundle()

            // Store the variables in the bundle.
            argumentsBundle.putInt(DATABASE_ID, databaseId)
            argumentsBundle.putByteArray(FAVORITE_ICON_BYTE_ARRAY, favoriteIconByteArray)

            // Create a new instance of the dialog.
            val editBookmarkFolderDialog = EditBookmarkFolderDialog()

            // Add the arguments bundle to the dialog.
            editBookmarkFolderDialog.arguments = argumentsBundle

            // Return the new dialog.
            return editBookmarkFolderDialog
        }
    }

    private val browseActivityResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri: Uri? ->
        // Only do something if the user didn't press back from the file picker.
        if (imageUri != null) {
            // Get a handle for the content resolver.
            val contentResolver = requireContext().contentResolver

            // Get the image MIME type.
            val mimeType = contentResolver.getType(imageUri)

            // Decode the image according to the type.
            if (mimeType == "image/svg+xml") {  // The image is an SVG.
                // Display a snackbar.
                Snackbar.make(customIconImageView, getString(R.string.cannot_use_svg), Snackbar.LENGTH_LONG).show()
            } else {  // The image is not an SVG.
                // Get an input stream for the image URI.
                val inputStream = contentResolver.openInputStream(imageUri)

                // Get the bitmap from the URI.
                // `ImageDecoder.decodeBitmap` can't be used, because when running `Drawable.toBitmap` later the `Software rendering doesn't support hardware bitmaps` error message might be produced.
                var imageBitmap = BitmapFactory.decodeStream(inputStream)

                // Scale the image down if it is greater than 64 pixels in either direction.
                if ((imageBitmap != null) && ((imageBitmap.height > 128) || (imageBitmap.width > 128)))
                    imageBitmap = imageBitmap.scale(128, 128)

                // Display the new custom favorite icon.
                customIconImageView.setImageBitmap(imageBitmap)

                // Select the custom icon radio button.
                customIconLinearLayout.performClick()
            }
        }
    }

    // Declare the class views.
    private lateinit var currentIconRadioButton: RadioButton
    private lateinit var customIconLinearLayout: LinearLayout
    private lateinit var customIconImageView: ImageView
    private lateinit var folderNameEditText: EditText
    private lateinit var saveButton: Button

    // Declare the class variables.
    private lateinit var currentFolderName: String
    private lateinit var editBookmarkFolderListener: EditBookmarkFolderListener

    // The public interface is used to send information back to the parent activity.
    interface EditBookmarkFolderListener {
        fun saveBookmarkFolder(dialogFragment: DialogFragment, selectedFolderDatabaseId: Int)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the edit bookmark folder listener from the launching context.
        editBookmarkFolderListener = context as EditBookmarkFolderListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get a handle for the arguments.
        val arguments = requireArguments()

        // Get the variables from the arguments.
        val selectedFolderDatabaseId = arguments.getInt(DATABASE_ID)
        val favoriteIconByteArray = arguments.getByteArray(FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Initialize the database helper.
        val bookmarksDatabaseHelper = BookmarksDatabaseHelper(requireContext())

        // Get a cursor with the selected folder.
        val folderCursor = bookmarksDatabaseHelper.getBookmark(selectedFolderDatabaseId)

        // Move the cursor to the first position.
        folderCursor.moveToFirst()

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.edit_folder)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.folder)

        // Set the view.
        dialogBuilder.setView(R.layout.edit_bookmark_folder_dialog)

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the save button listener.
        dialogBuilder.setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity on save.
            editBookmarkFolderListener.saveBookmarkFolder(this, selectedFolderDatabaseId)
        }

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

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get handles for the views in the alert dialog.
        val currentIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.current_icon_linearlayout)!!
        currentIconRadioButton = alertDialog.findViewById(R.id.current_icon_radiobutton)!!
        val currentIconImageView = alertDialog.findViewById<ImageView>(R.id.current_icon_imageview)!!
        val defaultFolderIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.default_folder_icon_linearlayout)!!
        val defaultFolderIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.default_folder_icon_radiobutton)!!
        val webpageFavoriteIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.webpage_favorite_icon_linearlayout)!!
        val webpageFavoriteIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.webpage_favorite_icon_radiobutton)!!
        val webpageFavoriteIconImageView = alertDialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)!!
        customIconLinearLayout = alertDialog.findViewById(R.id.custom_icon_linearlayout)!!
        val customIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.custom_icon_radiobutton)!!
        customIconImageView = alertDialog.findViewById(R.id.custom_icon_imageview)!!
        val browseButton = alertDialog.findViewById<Button>(R.id.browse_button)!!
        folderNameEditText = alertDialog.findViewById(R.id.folder_name_edittext)!!
        saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Get the current favorite icon byte array from the cursor.
        val currentIconByteArray = folderCursor.getBlob(folderCursor.getColumnIndexOrThrow(FAVORITE_ICON))

        // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
        val currentIconBitmap = BitmapFactory.decodeByteArray(currentIconByteArray, 0, currentIconByteArray.size)

        // Get the current folder name.
        currentFolderName = folderCursor.getString(folderCursor.getColumnIndexOrThrow(BOOKMARK_NAME))

        // Populate the views.
        currentIconImageView.setImageBitmap(currentIconBitmap)
        webpageFavoriteIconImageView.setImageBitmap(favoriteIconBitmap)
        customIconImageView.setImageBitmap(AppCompatResources.getDrawable(requireContext(), R.drawable.folder)!!.toBitmap(128, 128, Bitmap.Config.ARGB_8888))
        folderNameEditText.setText(currentFolderName)

        // Set the radio button listeners.  These perform a click on the linear layout, which contains the necessary logic.
        currentIconRadioButton.setOnClickListener { currentIconLinearLayout.performClick() }
        defaultFolderIconRadioButton.setOnClickListener { defaultFolderIconLinearLayout.performClick() }
        webpageFavoriteIconRadioButton.setOnClickListener { webpageFavoriteIconLinearLayout.performClick() }
        customIconRadioButton.setOnClickListener { customIconLinearLayout.performClick() }

        // Set the current icon linear layout click listener.
        currentIconLinearLayout.setOnClickListener {
            // Check the current icon radio button.
            currentIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            defaultFolderIconRadioButton.isChecked = false
            webpageFavoriteIconRadioButton.isChecked = false
            customIconRadioButton.isChecked = false

            // Update the save button.
            updateSaveButton()
        }

        // Set the default folder icon linear layout click listener.
        defaultFolderIconLinearLayout.setOnClickListener {
            // Check the default icon radio button.
            defaultFolderIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            currentIconRadioButton.isChecked = false
            webpageFavoriteIconRadioButton.isChecked = false
            customIconRadioButton.isChecked = false

            // Update the save button.
            updateSaveButton()
        }

        // Set the webpage favorite icon linear layout click listener.
        webpageFavoriteIconLinearLayout.setOnClickListener {
            // Check the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            currentIconRadioButton.isChecked = false
            defaultFolderIconRadioButton.isChecked = false
            customIconRadioButton.isChecked = false

            // Update the save button.
            updateSaveButton()
        }

        // Set the custom icon linear layout click listener.
        customIconLinearLayout.setOnClickListener {
            // Check the current icon radio button.
            customIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            currentIconRadioButton.isChecked = false
            defaultFolderIconRadioButton.isChecked = false
            webpageFavoriteIconRadioButton.isChecked = false

            // Update the save button.
            updateSaveButton()
        }

        browseButton.setOnClickListener {
            // Open the file picker.
            browseActivityResultLauncher.launch("image/*")
        }

        // Update the status of the save button when the folder name is changed.
        folderNameEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable?) {
                // Update the save button.
                updateSaveButton()
            }
        })

        // Allow the enter key on the keyboard to save the bookmark from the edit bookmark name edit text.
        folderNameEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            // Check the key code, event, and button status.
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && saveButton.isEnabled) {  // The enter key was pressed and the save button is enabled.
                // Trigger the listener and return the dialog fragment to the parent activity.
                editBookmarkFolderListener.saveBookmarkFolder(this, selectedFolderDatabaseId)

                // Manually dismiss the the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // If any other key was pressed, or if the save button is currently disabled, do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Initially disable the save button.
        saveButton.isEnabled = false

        // Return the alert dialog.
        return alertDialog
    }

    private fun updateSaveButton() {
        // Get the new folder name.
        val newFolderName = folderNameEditText.text.toString()

        // Has the folder been renamed?
        val folderRenamed = (newFolderName != currentFolderName)

        // Has the favorite icon changed?
        val iconChanged = !currentIconRadioButton.isChecked

        // Enable the save button if something has been edited and the new folder name is not valid.
        saveButton.isEnabled = newFolderName.isNotBlank() && (folderRenamed || iconChanged)
    }
}
