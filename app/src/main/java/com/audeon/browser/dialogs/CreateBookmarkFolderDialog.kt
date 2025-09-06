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

import java.io.ByteArrayOutputStream

// Define the class constants.
private const val FAVORITE_ICON_BYTE_ARRAY = "A"

class CreateBookmarkFolderDialog : DialogFragment() {
    companion object {
        fun createBookmarkFolder(favoriteIconBitmap: Bitmap): CreateBookmarkFolderDialog {
            // Create a favorite icon byte array output stream.
            val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon to a PNG and place it in the byte array output stream.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

            // Convert the byte array output stream to a byte array.
            val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the favorite icon in the bundle.
            argumentsBundle.putByteArray(FAVORITE_ICON_BYTE_ARRAY, favoriteIconByteArray)

            // Create a new instance of the dialog.
            val createBookmarkFolderDialog = CreateBookmarkFolderDialog()

            // Add the bundle to the dialog.
            createBookmarkFolderDialog.arguments = argumentsBundle

            // Return the new dialog.
            return createBookmarkFolderDialog
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

                // Scale the image down if it is greater than 128 pixels in either direction.
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
    private lateinit var customIconImageView: ImageView
    private lateinit var customIconLinearLayout: LinearLayout

    // Declare the class variables.
    private lateinit var createBookmarkFolderListener: CreateBookmarkFolderListener

    // The public interface is used to send information back to the parent activity.
    interface CreateBookmarkFolderListener {
        fun createBookmarkFolder(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the create bookmark folder listener from the launching context.
        createBookmarkFolderListener = context as CreateBookmarkFolderListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments.
        val arguments = requireArguments()

        // Get the favorite icon byte array.
        val favoriteIconByteArray = arguments.getByteArray(FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Use an alert dialog builder to create the dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.create_folder)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.folder)

        // Set the view.
        dialogBuilder.setView(R.layout.create_bookmark_folder_dialog)

        // Set a listener on the cancel button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the create button listener.
        dialogBuilder.setPositiveButton(R.string.create) { _: DialogInterface, _: Int ->
            // Return the dialog fragment to the parent activity on create.
            createBookmarkFolderListener.createBookmarkFolder(this)
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

        // Display the keyboard.
        alertDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // The alert dialog must be shown before the content can be modified.
        alertDialog.show()

        // Get handles for the views in the dialog.
        val defaultFolderIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.default_folder_icon_linearlayout)!!
        val defaultFolderIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.default_folder_icon_radiobutton)!!
        val webpageFavoriteIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.webpage_favorite_icon_linearlayout)!!
        val webpageFavoriteIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.webpage_favorite_icon_radiobutton)!!
        val webpageFavoriteIconImageView = alertDialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)!!
        customIconLinearLayout = alertDialog.findViewById(R.id.custom_icon_linearlayout)!!
        val customIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.custom_icon_radiobutton)!!
        customIconImageView = alertDialog.findViewById(R.id.custom_icon_imageview)!!
        val browseButton = alertDialog.findViewById<Button>(R.id.browse_button)!!
        val folderNameEditText = alertDialog.findViewById<EditText>(R.id.folder_name_edittext)!!
        val createButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Populate the views.  The vectored drawable must be converted to a bitmap or the save function will fail later.  `Bitmap.Config.RGBA_1010102` can be used once the minimum API >= 33.
        webpageFavoriteIconImageView.setImageBitmap(favoriteIconBitmap)
        customIconImageView.setImageBitmap(AppCompatResources.getDrawable(requireContext(), R.drawable.folder)!!.toBitmap(128, 128, Bitmap.Config.ARGB_8888))

        // Set the radio button listeners.  These perform a click on the linear layout, which contains the necessary logic.
        defaultFolderIconRadioButton.setOnClickListener { defaultFolderIconLinearLayout.performClick() }
        webpageFavoriteIconRadioButton.setOnClickListener { webpageFavoriteIconLinearLayout.performClick() }
        customIconRadioButton.setOnClickListener { customIconLinearLayout.performClick() }

        // Set the default icon linear layout click listener.
        defaultFolderIconLinearLayout.setOnClickListener {
            // Check the default icon radio button.
            defaultFolderIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            webpageFavoriteIconRadioButton.isChecked = false
            customIconRadioButton.isChecked = false
        }

        // Set the webpage favorite icon linear layout click listener.
        webpageFavoriteIconLinearLayout.setOnClickListener {
            // Check the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            defaultFolderIconRadioButton.isChecked = false
            customIconRadioButton.isChecked = false
        }

        // Set the custom icon linear layout click listener.
        customIconLinearLayout.setOnClickListener {
            // Check the custom icon radio button.
            customIconRadioButton.isChecked = true

            // Uncheck the other radio buttons.
            defaultFolderIconRadioButton.isChecked = false
            webpageFavoriteIconRadioButton.isChecked = false
        }

        browseButton.setOnClickListener {
            // Open the file picker.
            browseActivityResultLauncher.launch("image/*")
        }

        // Enable the create button if the folder name is populated.
        folderNameEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable?) {
                // Convert the current text to a string.
                val folderName = editable.toString()

                // Enable the create button if the new folder name is not empty.
                createButton.isEnabled = folderName.isNotEmpty()
            }
        })

        // Set the enter key on the keyboard to create the folder from the edit text.
        folderNameEditText.setOnKeyListener { _: View?, keyCode: Int, keyEvent: KeyEvent ->
            // Check the key code, event, and button status.
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && createButton.isEnabled) {  // The event is a key-down on the enter key and the create button is enabled.
                // Trigger the create bookmark folder listener and return the dialog fragment to the parent activity.
                createBookmarkFolderListener.createBookmarkFolder(this)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // Some other key was pressed or the create button is disabled.
                return@setOnKeyListener false
            }
        }

        // Initially disable the create button.
        createButton.isEnabled = false

        // Return the alert dialog.
        return alertDialog
    }
}
