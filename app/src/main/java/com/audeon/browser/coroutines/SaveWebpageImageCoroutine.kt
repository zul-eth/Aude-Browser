/*
 * Copyright0 2019-2024 Soren Stoutner <soren@stoutner.com>.
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

import com.google.android.material.snackbar.Snackbar

import com.audeon.browser.R
import com.audeon.browser.views.NestedScrollWebView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream

class SaveWebpageImageCoroutine {
    fun save(activity: Activity, fileUri: Uri, nestedScrollWebView: NestedScrollWebView) {
        // Use a coroutine to save the webpage image.
        CoroutineScope(Dispatchers.Main).launch {
            // Get a cursor from the content resolver.
            val contentResolverCursor = activity.contentResolver.query(fileUri, null, null, null)

            // Move to the first row.
            contentResolverCursor!!.moveToFirst()

            // Get the file name from the cursor.
            val fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

            // Close the cursor.
            contentResolverCursor.close()

            // Create a saving image snackbar.
            val savingImageSnackbar = Snackbar.make(nestedScrollWebView, activity.getString(R.string.processing_image, fileNameString), Snackbar.LENGTH_INDEFINITE)

            // Display the saving image snackbar.
            savingImageSnackbar.show()

            // Create a webpage bitmap.  Once the Minimum API >= 33 Bitmap.Config.RGBA_1010102 can be used instead of ARGB_8888.
            // RGBA_F16 can't be used because it produces black output for the part of page not currently visible on the screen.
            val webpageBitmap = Bitmap.createBitmap(nestedScrollWebView.getHorizontalScrollRange(), nestedScrollWebView.getVerticalScrollRange(), Bitmap.Config.ARGB_8888)

            // Create a canvas.
            val webpageCanvas = Canvas(webpageBitmap)

            // Draw the current webpage onto the bitmap.  The nested scroll WebView commands must be run on the UI thread.
            nestedScrollWebView.draw(webpageCanvas)

            // Compress the image on the IO thread.
            withContext(Dispatchers.IO) {
                // Create a webpage PNG byte array output stream.
                val webpageByteArrayOutputStream = ByteArrayOutputStream()

                // Convert the bitmap to a PNG.  `0` is for lossless compression (the only option for a PNG).  This compression takes a long time.
                // Once the minimum API >= 30 this could be replaced with WEBP_LOSSLESS.
                webpageBitmap.compress(Bitmap.CompressFormat.PNG, 0, webpageByteArrayOutputStream)

                try {
                    // Create an image file output stream.
                    val imageFileOutputStream = activity.contentResolver.openOutputStream(fileUri)!!

                            // Write the webpage image to the image file.
                            webpageByteArrayOutputStream.writeTo(imageFileOutputStream)

                    // Close the output stream.
                    imageFileOutputStream.close()

                    // Update the UI.
                    withContext(Dispatchers.Main) {
                        // Dismiss the saving image snackbar.
                        savingImageSnackbar.dismiss()

                        // Display the image saved snackbar.
                        Snackbar.make(nestedScrollWebView, activity.getString(R.string.saved, fileNameString), Snackbar.LENGTH_SHORT).show()
                    }
                } catch (exception: Exception) {
                    // Update the UI.
                    withContext(Dispatchers.Main) {
                        // Dismiss the saving image snackbar.
                        savingImageSnackbar.dismiss()

                        // Display the file saving error.
                        Snackbar.make(nestedScrollWebView, activity.getString(R.string.error_saving_file, fileNameString, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }
                }
            }
        }
    }
}
