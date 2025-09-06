/*
 * Copyright 2020-2022, 2024 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android>.
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

package com.audeon.browser.coroutines

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.LinearLayout

import com.google.android.material.snackbar.Snackbar

import com.audeon.browser.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.lang.Exception

// Declare the class constants.
private const val SUCCESS = "Success"

object SaveAboutVersionImageCoroutine {
    fun saveImage(activity: Activity, fileUri: Uri, aboutVersionLinearLayout: LinearLayout) {
        // Save the image using a coroutine.
        CoroutineScope(Dispatchers.Main).launch {
            // Create a saving image snackbar.
            val savingImageSnackbar: Snackbar

            // Process the image on the IO thread.
            withContext(Dispatchers.IO) {
                // Instantiate a file name string.
                val fileNameString: String

                // Get a cursor from the content resolver.
                val contentResolverCursor = activity.contentResolver.query(fileUri, null, null, null)

                // Get the file display name if the content resolve cursor is not null.
                if (contentResolverCursor != null) {  // The content resolve cursor is not null.
                    // Move to the first row.
                    contentResolverCursor.moveToFirst()

                    // Get the file name from the cursor.
                    fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

                    // Close the cursor.
                    contentResolverCursor.close()
                } else {  // The content resolve cursor is null.
                    // Use the URI last path segment as the file name string.
                    fileNameString = fileUri.lastPathSegment.toString()
                }

                // Use the main thread to display a snackbar.
                withContext(Dispatchers.Main) {
                    // Create a saving image snackbar.
                    savingImageSnackbar = Snackbar.make(aboutVersionLinearLayout, activity.getString(R.string.processing_image, fileNameString), Snackbar.LENGTH_INDEFINITE)

                    // Display the saving image snackbar.
                    savingImageSnackbar.show()
                }

                // Create an empty bitmap with the dimensions of the linear layout.  Once the minimum API >= 33 Bitmap.Config.RGBA_1010102 can be used instead of RBGA_F16.
                val aboutVersionBitmap = Bitmap.createBitmap(aboutVersionLinearLayout.width, aboutVersionLinearLayout.height, Bitmap.Config.RGBA_F16)

                // Create a canvas.
                val aboutVersionCanvas = Canvas(aboutVersionBitmap)

                // Use the main thread to interact with the linear layout.
                withContext(Dispatchers.Main) {
                    // Draw the current about version onto the bitmap.  It might be possible to do this with PixelCopy, but I am not sure that would be any better.
                    aboutVersionLinearLayout.draw(aboutVersionCanvas)
                }

                // Create an about version PNG byte array output stream.
                val aboutVersionByteArrayOutputStream = ByteArrayOutputStream()

                // Convert the bitmap to a PNG.  `0` is for lossless compression (the only option for a PNG).  This compression takes a long time.
                // Once the minimum API >= 30 this can be replaced with WEBP_LOSSLESS.
                aboutVersionBitmap.compress(Bitmap.CompressFormat.PNG, 0, aboutVersionByteArrayOutputStream)

                // Create a file creation disposition string.
                var fileCreationDisposition = SUCCESS

                // Write the image inside a try block to capture any write exceptions.
                try {
                    // Open an output stream.
                    val outputStream = activity.contentResolver.openOutputStream(fileUri)!!

                    // Write the webpage image to the image file.
                    aboutVersionByteArrayOutputStream.writeTo(outputStream)

                    // Close the output stream.
                    outputStream.close()
                } catch (exception: Exception) {
                    // Store the error in the file creation disposition string.
                    fileCreationDisposition = exception.toString()
                }

                // Use the main thread to update the snackbars.
                withContext(Dispatchers.Main) {
                    // Dismiss the saving image snackbar.
                    savingImageSnackbar.dismiss()

                    // Display a file creation disposition snackbar.
                    if (fileCreationDisposition == SUCCESS) {
                        // Create a file saved snackbar.
                        Snackbar.make(aboutVersionLinearLayout, activity.getString(R.string.saved, fileNameString), Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(aboutVersionLinearLayout, activity.getString(R.string.error_saving_file, fileNameString, fileCreationDisposition), Snackbar.LENGTH_INDEFINITE).show()
                    }
                }
            }
        }
    }
}
