/*
 * Copyright 2023 Soren Stoutner <soren@stoutner.com>.
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

package com.audeon.browser.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import android.widget.ScrollView

import androidx.appcompat.content.res.AppCompatResources

import com.google.android.material.snackbar.Snackbar
import com.audeon.browser.BuildConfig

import com.audeon.browser.R

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.nio.charset.StandardCharsets

class ImportExportBookmarksHelper {
    // Define the class variables.
    private var bookmarksAndFolderExported = 0

    fun importBookmarks(fileNameString: String, context: Context, scrollView: ScrollView) {
        try {
            // Get an input stream for the file name.
            val inputStream = context.contentResolver.openInputStream(Uri.parse(fileNameString))!!

            // Load the bookmarks input stream into a buffered reader.
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            // Instantiate the bookmarks database helper.
            val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context)

            // Get the default icon drawables.
            val defaultFavoriteIconDrawable = AppCompatResources.getDrawable(context, R.drawable.world)
            val defaultFolderIconDrawable = AppCompatResources.getDrawable(context, R.drawable.folder_blue_bitmap)

            // Cast the default icon drawables to bitmap drawables.
            val defaultFavoriteIconBitmapDrawable = (defaultFavoriteIconDrawable as BitmapDrawable?)!!
            val defaultFolderIconBitmapDrawable = (defaultFolderIconDrawable as BitmapDrawable)

            // Get the default icon bitmaps.
            val defaultFavoriteIconBitmap = defaultFavoriteIconBitmapDrawable.bitmap
            val defaultFolderIconBitmap = defaultFolderIconBitmapDrawable.bitmap

            // Create the default icon byte array output streams.
            val defaultFavoriteIconByteArrayOutputStream = ByteArrayOutputStream()
            val defaultFolderIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the default icon bitmaps to byte array streams.  `0` is for lossless compression (the only option for a PNG).
            defaultFavoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, defaultFavoriteIconByteArrayOutputStream)
            defaultFolderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, defaultFolderIconByteArrayOutputStream)

            // Convert the icon byte array streams to byte array stream to byte arrays.
            val defaultFavoriteIconByteArray = defaultFavoriteIconByteArrayOutputStream.toByteArray()
            val defaultFolderIconByteArray = defaultFolderIconByteArrayOutputStream.toByteArray()

            // Get a cursor with all the bookmarks.
            val initialNumberOfBookmarksAndFoldersCursor = bookmarksDatabaseHelper.allBookmarkAndFolderIds

            // Get an initial count of the folders and bookmarks.
            val initialNumberOfFoldersAndBookmarks = initialNumberOfBookmarksAndFoldersCursor.count

            // Close the cursor.
            initialNumberOfBookmarksAndFoldersCursor.close()

            // Get a cursor with the contents of the home folder.
            val homeFolderContentCursor = bookmarksDatabaseHelper.getBookmarkAndFolderIds(0L)

            // Initialize the variables.
            var parentFolderId = 0L
            var displayOrder = homeFolderContentCursor.count

            // Close the cursor.
            homeFolderContentCursor.close()

            // Parse the bookmarks.
            bufferedReader.forEachLine {
                // Trim the string.
                var line = it.trimStart()

                // Only process interesting lines.
                if (line.startsWith("<DT>")) {  // This is a bookmark or a folder.
                    // Remove the initial `<DT>`
                    line = line.substring(4)

                    // Check to see if this is a bookmark or a folder.
                    if (line.contains("HREF=\"")) {  // This is a bookmark.
                        // Remove the text before the bookmark name.
                        var bookmarkName = line.substring(line.indexOf(">") + 1)

                        // Remove the text after the bookmark name.
                        bookmarkName = bookmarkName.substring(0, bookmarkName.indexOf("<"))

                        // Remove the text before the bookmark URL.
                        var bookmarkUrl = line.substring(line.indexOf("HREF=\"") + 6)

                        // Remove the text after the bookmark URL.
                        bookmarkUrl = bookmarkUrl.substring(0, bookmarkUrl.indexOf("\""))

                        // Initialize the favorite icon string.
                        var favoriteIconString = ""

                        // Populate the favorite icon string.
                        if (line.contains("ICON=\"data:image/png;base64,")) {  // The `ICON` attribute contains a Base64 encoded icon.
                            // Remove the text before the icon string.
                            favoriteIconString = line.substring(line.indexOf("ICON=\"data:image/png;base64,") + 28)

                            // Remove the text after the icon string.
                            favoriteIconString = favoriteIconString.substring(0, favoriteIconString.indexOf("\""))
                        } else if (line.contains("ICON_URI=\"data:image/png;base64,")) {  // The `ICON_URI` attribute contains a Base64 encoded icon.
                            // Remove the text before the icon string.
                            favoriteIconString = line.substring(line.indexOf("ICON_URI=\"data:image/png;base64,") + 32)

                            // Remove the text after the icon string.
                            favoriteIconString = favoriteIconString.substring(0, favoriteIconString.indexOf("\""))
                        }

                        // Populate the favorite icon byte array.
                        val favoriteIconByteArray = if (favoriteIconString.isEmpty())  // The favorite icon string is empty.  Use the default favorite icon.
                            defaultFavoriteIconByteArray
                        else  // The favorite icon string is populated.  Decode it to a byte array.
                            Base64.decode(favoriteIconString, Base64.DEFAULT)

                        // Add the bookmark.
                        bookmarksDatabaseHelper.createBookmark(bookmarkName, bookmarkUrl, parentFolderId, displayOrder, favoriteIconByteArray)

                        // Increment the display order.
                        ++displayOrder
                    } else {  // This is a folder.  The following lines will be contain in this folder until a `</DL>` is encountered.
                        // Remove the text before the folder name.
                        var folderName = line.substring(line.indexOf(">") + 1)

                        // Remove the text after the folder name.
                        folderName = folderName.substring(0, folderName.indexOf("<"))

                        // Add the folder and set it as the new parent folder ID.
                        parentFolderId = bookmarksDatabaseHelper.createFolder(folderName, parentFolderId, displayOrder, defaultFolderIconByteArray)

                        // Reset the display order.
                        displayOrder = 0
                    }
                } else if (line.startsWith("</DL>")) {  // This is the end of a folder's contents.
                    // Reset the parent folder id if it isn't 0.
                    if (parentFolderId != 0L)
                        parentFolderId = bookmarksDatabaseHelper.getParentFolderId(parentFolderId)

                    // Get a cursor with the contents of the new parent folder.
                    val folderContentCursor = bookmarksDatabaseHelper.getBookmarkAndFolderIds(parentFolderId)

                    // Reset the display order.
                    displayOrder = folderContentCursor.count

                    // Close the cursor.
                    folderContentCursor.close()
                }
            }

            // Get a cursor with all the bookmarks.
            val finalNumberOfBookmarksAndFoldersCursor = bookmarksDatabaseHelper.allBookmarkAndFolderIds

            // Get the final count of the folders and bookmarks.
            val finalNumberOfFoldersAndBookmarks = finalNumberOfBookmarksAndFoldersCursor.count

            // Close the cursor.
            finalNumberOfBookmarksAndFoldersCursor.close()

            // Close the bookmarks database helper.
            bookmarksDatabaseHelper.close()

            // Close the input stream.
            inputStream.close()

            // Display a snackbar with the number of folders and bookmarks imported.
            Snackbar.make(scrollView, context.getString(R.string.bookmarks_imported, finalNumberOfFoldersAndBookmarks - initialNumberOfFoldersAndBookmarks), Snackbar.LENGTH_LONG).show()
        } catch (exception: Exception) {
            // Display a snackbar with the error message.
            Snackbar.make(scrollView, context.getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    fun exportBookmarks(fileNameString: String, context: Context, scrollView: ScrollView) {
        // Create a bookmarks string builder
        val bookmarksStringBuilder = StringBuilder()

        // Populate the headers.
        bookmarksStringBuilder.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
        bookmarksStringBuilder.append("\n\n")
        bookmarksStringBuilder.append("<!-- These bookmarks were exported from Privacy Browser Android ${BuildConfig.VERSION_NAME}. -->")
        bookmarksStringBuilder.append("\n\n")
        bookmarksStringBuilder.append("<meta HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
        bookmarksStringBuilder.append("\n\n")

        // Begin the bookmarks.
        bookmarksStringBuilder.append("<DL><p>")
        bookmarksStringBuilder.append("\n")

        // Instantiate the bookmarks database helper.
        val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context)

        // Initialize the indent string.
        val indentString = "    "

        // Populate the bookmarks, starting with the home folder.
        bookmarksStringBuilder.append(populateBookmarks(bookmarksDatabaseHelper, 0L, indentString))

        // End the bookmarks.
        bookmarksStringBuilder.append("</DL>")
        bookmarksStringBuilder.append("\n")

        try {
            // Get an output stream for the file name, truncating any existing content.
            val outputStream = context.contentResolver.openOutputStream(Uri.parse(fileNameString), "wt")!!

            // Write the bookmarks string to the output stream.
            outputStream.write(bookmarksStringBuilder.toString().toByteArray(StandardCharsets.UTF_8))

            // Close the output stream.
            outputStream.close()

            // Display a snackbar with the number of folders and bookmarks exported.
            Snackbar.make(scrollView, context.getString(R.string.bookmarks_exported, bookmarksAndFolderExported), Snackbar.LENGTH_LONG).show()
        } catch (exception: Exception) {
            // Display a snackbar with the error message.
            Snackbar.make(scrollView, context.getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
        }

        // Close the bookmarks database helper.
        bookmarksDatabaseHelper.close()
    }

    private fun populateBookmarks(bookmarksDatabaseHelper: BookmarksDatabaseHelper, folderId: Long, indentString: String): String {
        // Create a bookmarks string builder.
        val bookmarksStringBuilder = StringBuilder()

        // Get all the bookmarks in the current folder.
        val bookmarksCursor = bookmarksDatabaseHelper.getBookmarks(folderId)

        // Process each bookmark.
        while (bookmarksCursor.moveToNext()) {
            if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(IS_FOLDER)) == 1) {  // This is a folder.
                // Export the folder.
                bookmarksStringBuilder.append(indentString)
                bookmarksStringBuilder.append("<DT><H3>")
                bookmarksStringBuilder.append(bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BOOKMARK_NAME)))
                bookmarksStringBuilder.append("</H3>")
                bookmarksStringBuilder.append("\n")

                // Begin the folder contents.
                bookmarksStringBuilder.append(indentString)
                bookmarksStringBuilder.append("<DL><p>")
                bookmarksStringBuilder.append("\n")

                // Populate the folder contents.
                bookmarksStringBuilder.append(populateBookmarks(bookmarksDatabaseHelper, bookmarksCursor.getLong(bookmarksCursor.getColumnIndexOrThrow(FOLDER_ID)), "    $indentString"))

                // End the folder contents.
                bookmarksStringBuilder.append(indentString)
                bookmarksStringBuilder.append("</DL><p>")
                bookmarksStringBuilder.append("\n")

                // Increment the bookmarks and folders exported counter.
                ++bookmarksAndFolderExported
            } else {  // This is a bookmark.
                // Export the bookmark.
                bookmarksStringBuilder.append(indentString)
                bookmarksStringBuilder.append("<DT><A HREF=\"")
                bookmarksStringBuilder.append(bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BOOKMARK_URL)))
                bookmarksStringBuilder.append("\" ICON=\"data:image/png;base64,")
                bookmarksStringBuilder.append(Base64.encodeToString(bookmarksCursor.getBlob(bookmarksCursor.getColumnIndexOrThrow(FAVORITE_ICON)), Base64.NO_WRAP))
                bookmarksStringBuilder.append("\">")
                bookmarksStringBuilder.append(bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BOOKMARK_NAME)))
                bookmarksStringBuilder.append("</A>")
                bookmarksStringBuilder.append("\n")

                // Increment the bookmarks and folders exported counter.
                ++bookmarksAndFolderExported
            }
        }

        // Return the bookmarks string.
        return bookmarksStringBuilder.toString()
    }
}
