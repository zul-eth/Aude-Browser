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
import androidx.core.graphics.scale
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.google.android.material.snackbar.Snackbar

import com.audeon.browser.R

import java.io.ByteArrayOutputStream

// Define the class constants.
private const val URL_STRING = "A"
private const val TITLE = "B"
private const val FAVORITE_ICON_BYTE_ARRAY = "C"

class CreateBookmarkDialog : DialogFragment() {
    companion object {
        fun createBookmark(urlString: String, title: String, favoriteIconBitmap: Bitmap): CreateBookmarkDialog {
            // Create a favorite icon byte array output stream.
            val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon to a PNG and place it in the byte array output stream.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

            // Convert the byte array output stream to a byte array.
            val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the variables in the bundle.
            argumentsBundle.putString(URL_STRING, urlString)
            argumentsBundle.putString(TITLE, title)
            argumentsBundle.putByteArray(FAVORITE_ICON_BYTE_ARRAY, favoriteIconByteArray)

            // Create a new instance of the dialog.
            val createBookmarkDialog = CreateBookmarkDialog()

            // Add the bundle to the dialog.
            createBookmarkDialog.arguments = argumentsBundle

            // Return the new dialog.
            return createBookmarkDialog
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
                Snackbar.make(bookmarkNameEditText, getString(R.string.cannot_use_svg), Snackbar.LENGTH_LONG).show()
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
    private lateinit var bookmarkNameEditText: EditText
    private lateinit var bookmarkUrlEditText: EditText
    private lateinit var createButton: Button
    private lateinit var customIconImageView: ImageView
    private lateinit var customIconLinearLayout: LinearLayout

    // Declare the class variables
    private lateinit var createBookmarkListener: CreateBookmarkListener

    // The public interface is used to send information back to the parent activity.
    interface CreateBookmarkListener {
        fun createBookmark(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the create bookmark listener from the launching context.
        createBookmarkListener = context as CreateBookmarkListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments.
        val arguments = requireArguments()

        // Get the contents of the arguments.
        val urlString = arguments.getString(URL_STRING)
        val title = arguments.getString(TITLE)
        val favoriteIconByteArray = arguments.getByteArray(FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Use an alert dialog builder to create the dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.create_bookmark)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.bookmark)

        // Set the view.
        dialogBuilder.setView(R.layout.create_bookmark_dialog)

        // Set a listener on the cancel button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set a listener on the create button.
        dialogBuilder.setPositiveButton(R.string.create) { _: DialogInterface, _: Int ->
            // Return the dialog fragment and the favorite icon bitmap to the parent activity.
            createBookmarkListener.createBookmark(this)
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

        // The alert dialog needs to be shown before the contents can be modified.
        alertDialog.show()

        // Get a handle for the edit texts.
        val webpageFavoriteIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.webpage_favorite_icon_linearlayout)!!
        val webpageFavoriteIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.webpage_favorite_icon_radiobutton)!!
        val webpageFavoriteIconImageView = alertDialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)!!
        customIconLinearLayout = alertDialog.findViewById(R.id.custom_icon_linearlayout)!!
        val customIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.custom_icon_radiobutton)!!
        customIconImageView = alertDialog.findViewById(R.id.custom_icon_imageview)!!
        val browseButton = alertDialog.findViewById<Button>(R.id.browse_button)!!
        bookmarkNameEditText = alertDialog.findViewById(R.id.bookmark_name_edittext)!!
        bookmarkUrlEditText = alertDialog.findViewById(R.id.bookmark_url_edittext)!!
        createButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Populate the views.
        webpageFavoriteIconImageView.setImageBitmap(favoriteIconBitmap)
        customIconImageView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.world))
        bookmarkNameEditText.setText(title)
        bookmarkUrlEditText.setText(urlString)

        // Set the radio button listeners.  These perform a click on the linear layout, which contains the necessary logic.
        webpageFavoriteIconRadioButton.setOnClickListener { webpageFavoriteIconLinearLayout.performClick() }
        customIconRadioButton.setOnClickListener { customIconLinearLayout.performClick() }

        // Set the webpage favorite icon linear layout click listener.
        webpageFavoriteIconLinearLayout.setOnClickListener {
            // Check the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = true

            // Uncheck the custom icon radio button.
            customIconRadioButton.isChecked = false
        }

        // Set the custom icon linear layout click listener.
        customIconLinearLayout.setOnClickListener {
            // Check the custom icon radio button.
            customIconRadioButton.isChecked = true

            // Uncheck the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = false
        }

        browseButton.setOnClickListener {
            // Open the file picker.
            browseActivityResultLauncher.launch("image/*")
        }

        // Update the UI when the bookmark name changes.
        bookmarkNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable?) {
                // Update the UI.
                updateUi()
            }
        })

        // Update the UI when the bookmark name changes.
        bookmarkUrlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, befire: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable?) {
                // Update the UI.
                updateUi()
            }
        })

        // Allow the enter key on the keyboard to create the bookmark from the create bookmark name edit text.
        bookmarkNameEditText.setOnKeyListener { _: View, keyCode: Int, keyEvent: KeyEvent ->
            // Check the key code and event.
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {  // The event is a key-down on the enter key.
                // Trigger the create bookmark listener and return the dialog fragment and the favorite icon bitmap to the parent activity.
                createBookmarkListener.createBookmark(this)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // Some other key was pressed.
                // Do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Allow the enter key on the keyboard to create the bookmark from create bookmark URL edit text.
        bookmarkUrlEditText.setOnKeyListener { _: View, keyCode: Int, keyEvent: KeyEvent ->
            // Check the key code and event.
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {  // The event is a key-down on the enter key.
                // Trigger the create bookmark listener and return the dialog fragment and the favorite icon bitmap to the parent activity.
                createBookmarkListener.createBookmark(this)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else { // Some other key was pressed.
                // Do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Populate the UI.
        updateUi()

        // Return the alert dialog.
        return alertDialog
    }

    private fun updateUi() {
        // Get the contents of the edit texts.
        val bookmarkName = bookmarkNameEditText.text.toString()
        val bookmarkUrl = bookmarkUrlEditText.text.toString()

        // Enable the create button if the edit texts are populated.
        createButton.isEnabled = bookmarkName.isNotBlank() && bookmarkUrl.isNotBlank()
    }
}
