/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2023 Soren Stoutner <soren@stoutner.com>
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
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.activities.HOME_FOLDER_DATABASE_ID
import com.audeon.browser.activities.HOME_FOLDER_ID
import com.audeon.browser.helpers.BOOKMARK_NAME
import com.audeon.browser.helpers.FAVORITE_ICON
import com.audeon.browser.helpers.FOLDER_ID
import com.audeon.browser.helpers.ID
import com.audeon.browser.helpers.PARENT_FOLDER_ID
import com.audeon.browser.helpers.BookmarksDatabaseHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream

// Define the class constants.
private const val CURRENT_FOLDER_ID = "A"
private const val SELECTED_BOOKMARKS_LONG_ARRAY = "B"

class MoveToFolderDialog : DialogFragment() {
    companion object {
        fun moveBookmarks(currentFolderId: Long, selectedBookmarksLongArray: LongArray): MoveToFolderDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putLong(CURRENT_FOLDER_ID, currentFolderId)
            argumentsBundle.putLongArray(SELECTED_BOOKMARKS_LONG_ARRAY, selectedBookmarksLongArray)

            // Create a new instance of the dialog.
            val moveToFolderDialog = MoveToFolderDialog()

            // And the bundle to the dialog.
            moveToFolderDialog.arguments = argumentsBundle

            // Return the new dialog.
            return moveToFolderDialog
        }
    }

    // Declare the class variables.
    private lateinit var moveToFolderListener: MoveToFolderListener
    private lateinit var bookmarksDatabaseHelper: BookmarksDatabaseHelper

    // The public interface is used to send information back to the parent activity.
    interface MoveToFolderListener {
        fun onMoveToFolder(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the move to folder listener from the launching context.
        moveToFolderListener = context as MoveToFolderListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the data from the arguments.
        val currentFolderId = requireArguments().getLong(CURRENT_FOLDER_ID, HOME_FOLDER_ID)
        val selectedBookmarksLongArray = requireArguments().getLongArray(SELECTED_BOOKMARKS_LONG_ARRAY)!!

        // Initialize the database helper.
        bookmarksDatabaseHelper = BookmarksDatabaseHelper(requireContext())

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.move_to_folder_blue)

        // Set the title.
        dialogBuilder.setTitle(R.string.move_to_folder)

        // Set the view.
        dialogBuilder.setView(R.layout.move_to_folder_dialog)

        // Set the listener for the cancel button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the listener fo the move button.
        dialogBuilder.setPositiveButton(R.string.move) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity on move.
            moveToFolderListener.onMoveToFolder(this)
        }

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Disable screenshots.
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get a handle for the positive button.
        val moveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Initially disable the positive button.
        moveButton.isEnabled = false

        // Create a list of folders not to display.
        val folderIdsNotToDisplay = mutableListOf<Long>()

        // Add any selected folders and their subfolders to the list of folders not to display.
        for (databaseIdLong in selectedBookmarksLongArray) {
            // Get the database ID int for each selected bookmark.
            val databaseIdInt = databaseIdLong.toInt()

            // Check to see if the bookmark is a folder.
            if (bookmarksDatabaseHelper.isFolder(databaseIdInt)) {
                // Add the folder to the list of folders not to display.
                folderIdsNotToDisplay.add(bookmarksDatabaseHelper.getFolderId(databaseIdInt))
            }
        }

        // Check to see if the bookmark is currently in the home folder.
        if (currentFolderId == HOME_FOLDER_ID) {  // The bookmark is currently in the home folder.  Don't display `Home Folder` at the top of the list view.
            // Get a cursor containing the folders to display.
            val foldersCursor = bookmarksDatabaseHelper.getFoldersExcept(folderIdsNotToDisplay)

            // Populate the folders cursor adapter.
            val foldersCursorAdapter = populateFoldersCursorAdapter(requireContext(), foldersCursor)

            // Get a handle for the folders list view.
            val foldersListView = alertDialog.findViewById<ListView>(R.id.move_to_folder_listview)!!

            // Set the folder list view adapter.
            foldersListView.adapter = foldersCursorAdapter

            // Enable the move button when a folder is selected.
            foldersListView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
                // Enable the move button.
                moveButton.isEnabled = true
            }
        } else {  // The current folder is not directly in the home folder.  Display `Home Folder` at the top of the list view.
            // Get the home folder icon drawable.
            val homeFolderIconDrawable = ContextCompat.getDrawable(requireActivity().applicationContext, R.drawable.folder_gray_bitmap)

            // Convert the home folder icon drawable to a bitmap drawable.
            val homeFolderIconBitmapDrawable = homeFolderIconDrawable as BitmapDrawable

            // Convert the home folder bitmap drawable to a bitmap.
            val homeFolderIconBitmap = homeFolderIconBitmapDrawable.bitmap

            // Create a home folder icon byte array output stream.
            val homeFolderIconByteArrayOutputStream = ByteArrayOutputStream()

            // Compress the bitmap using a coroutine with Dispatchers.Default.
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.Default) {
                    // Convert the home folder bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
                    homeFolderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, homeFolderIconByteArrayOutputStream)

                    // Convert the home folder icon byte array output stream to a byte array.
                    val homeFolderIconByteArray = homeFolderIconByteArrayOutputStream.toByteArray()

                    // Setup the home folder matrix cursor column names.
                    val homeFolderMatrixCursorColumnNames = arrayOf(ID, BOOKMARK_NAME, FAVORITE_ICON, PARENT_FOLDER_ID)

                    // Setup a matrix cursor for the `Home Folder`.
                    val homeFolderMatrixCursor = MatrixCursor(homeFolderMatrixCursorColumnNames)

                    // Add the home folder to the home folder matrix cursor.
                    homeFolderMatrixCursor.addRow(arrayOf<Any>(HOME_FOLDER_DATABASE_ID, getString(R.string.home_folder), homeFolderIconByteArray, HOME_FOLDER_ID))

                    // Add the current folder to the list of folders not to display.
                    folderIdsNotToDisplay.add(currentFolderId)

                    // Get a cursor containing the folders to display.
                    val foldersCursor = bookmarksDatabaseHelper.getFoldersExcept(folderIdsNotToDisplay)

                    // Combine the home folder matrix cursor and the folders cursor.
                    val foldersMergeCursor = MergeCursor(arrayOf(homeFolderMatrixCursor, foldersCursor))

                    // Populate the folders cursor on the main thread.
                    withContext(Dispatchers.Main) {
                        // Populate the folders cursor adapter.
                        val foldersCursorAdapter = populateFoldersCursorAdapter(requireContext(), foldersMergeCursor)

                        // Get a handle for the folders list view.
                        val foldersListView = alertDialog.findViewById<ListView>(R.id.move_to_folder_listview)!!

                        // Set the folder list view adapter.
                        foldersListView.adapter = foldersCursorAdapter

                        // Enable the move button when a folder is selected.
                        foldersListView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
                            // Enable the move button.
                            moveButton.isEnabled = true
                        }
                    }
                }
            }
        }

        // Return the alert dialog.
        return alertDialog
    }

    private fun populateFoldersCursorAdapter(context: Context, cursor: Cursor): CursorAdapter {
        // Return the folders cursor adapter.
        return object : CursorAdapter(context, cursor, false) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                // Inflate the individual item layout.
                return requireActivity().layoutInflater.inflate(R.layout.move_to_folder_item_linearlayout, parent, false)
            }

            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get the data from the cursor.
                val folderIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(FAVORITE_ICON))
                val folderName = cursor.getString(cursor.getColumnIndexOrThrow(BOOKMARK_NAME))

                // Get handles for the views.
                val subfolderSpacerTextView = view.findViewById<TextView>(R.id.subfolder_spacer_textview)
                val folderIconImageView = view.findViewById<ImageView>(R.id.folder_icon_imageview)
                val folderNameTextView = view.findViewById<TextView>(R.id.folder_name_textview)

                // Populate the subfolder spacer.
                if (cursor.getLong(cursor.getColumnIndexOrThrow(PARENT_FOLDER_ID)) != HOME_FOLDER_ID) {  // The folder is not in the home folder.
                    // Get the subfolder spacer.
                    subfolderSpacerTextView.text = bookmarksDatabaseHelper.getSubfolderSpacer(cursor.getLong(cursor.getColumnIndexOrThrow(FOLDER_ID)))
                } else {  // The folder is in the home folder.
                    // Reset the subfolder spacer.
                    subfolderSpacerTextView.text = ""
                }

                // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
                val folderIconBitmap = BitmapFactory.decodeByteArray(folderIconByteArray, 0, folderIconByteArray.size)

                // Display the folder icon bitmap.
                folderIconImageView.setImageBitmap(folderIconBitmap)

                // Display the folder name.
                folderNameTextView.text = folderName
            }
        }
    }
}
