/*
 * Copyright 2016-2024 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
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
 * along with Privacy Browser Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.audeon.browser.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.MatrixCursor
import android.database.MergeCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import com.audeon.browser.activities.HOME_FOLDER_ID

import java.util.Date

// Define the class constants.
private const val SCHEMA_VERSION = 2

// Define the public database constants.
const val BOOKMARKS_DATABASE = "bookmarks.db"
const val BOOKMARKS_TABLE = "bookmarks"

// Define the public schema constants.
const val BOOKMARK_NAME = "bookmarkname"
const val BOOKMARK_URL = "bookmarkurl"
const val DISPLAY_ORDER = "displayorder"
const val FAVORITE_ICON = "favoriteicon"
const val FOLDER_ID = "folder_id"
const val IS_FOLDER = "isfolder"
const val PARENT_FOLDER_ID = "parent_folder_id"

// Define the public table creation constant.
const val CREATE_BOOKMARKS_TABLE = "CREATE TABLE $BOOKMARKS_TABLE (" +
        "$ID INTEGER PRIMARY KEY, " +
        "$BOOKMARK_NAME TEXT, " +
        "$BOOKMARK_URL TEXT, " +
        "$PARENT_FOLDER_ID INTEGER, " +
        "$DISPLAY_ORDER INTEGER, " +
        "$IS_FOLDER BOOLEAN, " +
        "$FOLDER_ID INTEGER, " +
        "$FAVORITE_ICON BLOB)"

class BookmarksDatabaseHelper(context: Context) : SQLiteOpenHelper(context, BOOKMARKS_DATABASE, null, SCHEMA_VERSION) {
    override fun onCreate(bookmarksDatabase: SQLiteDatabase) {
        // Create the bookmarks table.
        bookmarksDatabase.execSQL(CREATE_BOOKMARKS_TABLE)
    }

    override fun onUpgrade(bookmarksDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade from schema version 1, first used in Privacy Browser 1.8, to schema version 2, first used in Privacy Browser 3.15.
        if (oldVersion < 2) {
            // Add the folder ID column.
            bookmarksDatabase.execSQL("ALTER TABLE $BOOKMARKS_TABLE ADD COLUMN $FOLDER_ID INTEGER")

            // Get a cursor with all the folders.
            val foldersCursor = bookmarksDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $IS_FOLDER = 1", null)

            // Get the folders cursor ID column index.
            val foldersCursorIdColumnIndex = foldersCursor.getColumnIndexOrThrow(ID)

            // Add a folder ID to each folder.
            while(foldersCursor.moveToNext()) {
                // Get the current folder database ID.
                val databaseId = foldersCursor.getInt(foldersCursorIdColumnIndex)

                // Generate a folder ID.
                val folderId = Date().time

                // Create a folder content values.
                val folderContentValues = ContentValues()

                // Store the new folder ID in the content values.
                folderContentValues.put(FOLDER_ID, folderId)

                // Update the folder with the new folder ID.
                bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

                // Wait 2 milliseconds to ensure that the next folder ID is unique.
                Thread.sleep(2)
            }

            // Close the folders cursor.
            foldersCursor.close()


            // Add the parent folder ID column.
            bookmarksDatabase.execSQL("ALTER TABLE $BOOKMARKS_TABLE ADD COLUMN $PARENT_FOLDER_ID INTEGER")

            // Get a cursor with all the bookmarks.
            val bookmarksCursor = bookmarksDatabase.rawQuery("SELECT $ID, parentfolder FROM $BOOKMARKS_TABLE", null)

            // Get the bookmarks cursor ID column index.
            val bookmarksCursorIdColumnIndex = bookmarksCursor.getColumnIndexOrThrow(ID)
            val bookmarksCursorParentFolderColumnIndex = bookmarksCursor.getColumnIndexOrThrow("parentfolder")

            // Populate the parent folder ID for each bookmark.
            while(bookmarksCursor.moveToNext()) {
                // Get the information from the cursor.
                val databaseId = bookmarksCursor.getInt(bookmarksCursorIdColumnIndex)
                val oldParentFolderString = bookmarksCursor.getString(bookmarksCursorParentFolderColumnIndex)

                // Initialize the new parent folder ID.
                var newParentFolderId = HOME_FOLDER_ID

                // Get the parent folder ID if the bookmark is not in the home folder.
                if (oldParentFolderString.isNotEmpty()) {
                    // SQL escape the old parent folder string.
                    val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(oldParentFolderString)

                    // Get the parent folder cursor.
                    val parentFolderCursor = bookmarksDatabase.rawQuery("SELECT $FOLDER_ID FROM $BOOKMARKS_TABLE WHERE $BOOKMARK_NAME = $sqlEscapedFolderName AND $IS_FOLDER = 1", null)

                    // Get the new parent folder ID if it exists.
                    if (parentFolderCursor.count > 0) {
                        // Move to the first entry.
                        parentFolderCursor.moveToFirst()

                        // Get the new parent folder ID.
                        newParentFolderId = parentFolderCursor.getLong(parentFolderCursor.getColumnIndexOrThrow(FOLDER_ID))
                    }

                    // Close the parent folder cursor.
                    parentFolderCursor.close()
                }

                // Create a bookmark content values.
                val bookmarkContentValues = ContentValues()

                // Store the new parent folder ID in the content values.
                bookmarkContentValues.put(PARENT_FOLDER_ID, newParentFolderId)

                // Update the folder with the new folder ID.
                bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)
            }

            // Close the bookmarks cursor.
            bookmarksCursor.close()

            // This upgrade removed the old `parentfolder` string column.
            // SQLite amazingly only added a command to drop a column in version 3.35.0.  <https://www.sqlite.org/changes.html>
            // It will be a while before that is supported in Android.  <https://developer.android.com/reference/android/database/sqlite/package-summary>
            // Although a new table could be created and all the data copied to it, I think I will just leave the old parent folder column.  It will be wiped out the next time an import is run.
        }
    }

    // Get a cursor for all bookmarks and folders.
    val allBookmarks: Cursor
        get() {
            // Get a readable database handle.
            val bookmarksDatabase = this.readableDatabase

            // Return a cursor with the entire contents of the bookmarks table.  The cursor cannot be closed because it is used in the parent activity.
            return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE", null)
        }

    // Get a cursor with just the database IDs of all the bookmarks and folders.  This is useful for counting the number of bookmarks imported.
    val allBookmarkAndFolderIds: Cursor
        get() {
            // Get a readable database handle.
            val bookmarksDatabase = this.readableDatabase

            // Return a cursor with all the database IDs.  The cursor cannot be closed because it is used in the parent activity.
            return bookmarksDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE", null)
        }

    // Get a cursor for all bookmarks and folders ordered by display order.
    val allBookmarksByDisplayOrder: Cursor
        get() {
            // Get a readable database handle.
            val bookmarksDatabase = this.readableDatabase

            // Return a cursor with the entire contents of the bookmarks table ordered by the display order.  The cursor cannot be closed because it is used in the parent activity.
            return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE ORDER BY $DISPLAY_ORDER ASC", null)
        }

    // Create a bookmark.
    fun createBookmark(bookmarkName: String, bookmarkUrl: String, parentFolderId: Long, displayOrder: Int, favoriteIcon: ByteArray) {
        // Store the bookmark data in a content values.
        val bookmarkContentValues = ContentValues()

        // The ID is created automatically.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(PARENT_FOLDER_ID, parentFolderId)
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkContentValues.put(IS_FOLDER, false)
        bookmarkContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Insert a new row.
        bookmarksDatabase.insert(BOOKMARKS_TABLE, null, bookmarkContentValues)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Create a bookmark from content values.
    fun createBookmark(contentValues: ContentValues) {
        // Get a writable database.
        val bookmarksDatabase = this.writableDatabase

        // Insert a new row.
        bookmarksDatabase.insert(BOOKMARKS_TABLE, null, contentValues)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Create a folder.
    fun createFolder(folderName: String, parentFolderId: Long, displayOrder: Int, favoriteIcon: ByteArray): Long {
        // Create a bookmark folder content values.
        val bookmarkFolderContentValues = ContentValues()

        // Generate the folder ID.
        val folderId = generateFolderId()

        // The ID is created automatically.  Folders are always created at the top of the list.
        bookmarkFolderContentValues.put(BOOKMARK_NAME, folderName)
        bookmarkFolderContentValues.put(PARENT_FOLDER_ID, parentFolderId)
        bookmarkFolderContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkFolderContentValues.put(IS_FOLDER, true)
        bookmarkFolderContentValues.put(FOLDER_ID, folderId)
        bookmarkFolderContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Insert the new folder.
        bookmarksDatabase.insert(BOOKMARKS_TABLE, null, bookmarkFolderContentValues)

        // Close the database handle.
        bookmarksDatabase.close()

        // Return the new folder ID.
        return folderId
    }

    // Delete one bookmark.
    fun deleteBookmark(databaseId: Int) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Deletes the row with the given database ID.
        bookmarksDatabase.delete(BOOKMARKS_TABLE, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Get a cursor for the bookmark with the specified database ID.
    fun getBookmark(databaseId: Int): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return the cursor for the database ID.  The cursor can't be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)
    }

    // Get a cursor for all bookmarks and folders by display order except those with the specified IDs.
    fun getAllBookmarksByDisplayOrderExcept(exceptIdLongArray: LongArray): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Return a cursor with all the bookmarks except those specified ordered by display order.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID NOT IN ($idsNotToGetStringBuilder) ORDER BY $DISPLAY_ORDER ASC", null)
    }

    // Get a cursor for all bookmarks and folders except those with the specified IDs.
    fun getAllBookmarksExcept(exceptIdLongArray: LongArray): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Return a cursor with all the bookmarks except those specified.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID NOT IN ($idsNotToGetStringBuilder)", null)
    }

    // Get a cursor with just the database IDs of bookmarks and folders in the specified folder.  This is useful for deleting folders with bookmarks that have favorite icons too large to fit in a cursor.
    fun getBookmarkAndFolderIds(parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return a cursor with all the database IDs.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder.
    fun getBookmarks(parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return a cursor with all the bookmarks in a specified folder.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder ordered by display order.
    fun getBookmarksByDisplayOrder(parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return a cursor with all the bookmarks in the specified folder ordered by display order.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId ORDER BY $DISPLAY_ORDER ASC", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder by display order except those with the specified IDs.
    fun getBookmarksByDisplayOrderExcept(exceptIdLongArray: LongArray, parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Return a cursor with all the bookmarks in the specified folder except for those database IDs specified ordered by display order.  The cursor cannot be closed because it will be used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $ID NOT IN ($idsNotToGetStringBuilder) ORDER BY $DISPLAY_ORDER ASC", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder except those with the specified IDs.
    fun getBookmarksExcept(exceptIdLongArray: LongArray, parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Return a cursor with all the bookmarks in the specified folder except for those database IDs specified.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $ID NOT IN ($idsNotToGetStringBuilder)", null)
    }

    fun getBookmarksSortedAlphabetically(parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get the folders sorted alphabetically.
        val foldersCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $IS_FOLDER = 1 ORDER BY $BOOKMARK_NAME ASC", null)

        // Get the bookmarks sorted alphabetically.
        val bookmarksCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $IS_FOLDER = 0 ORDER BY $BOOKMARK_NAME ASC", null)

        // Return the merged cursors.
        return MergeCursor(arrayOf(foldersCursor, bookmarksCursor))
    }

    fun getBookmarksSortedAlphabeticallyExcept(exceptIdLongArray: LongArray, parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Get the folders sorted alphabetically except for those database IDs specified, ordered by name.
        val foldersCursor = bookmarksDatabase.rawQuery(
            "SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $IS_FOLDER = 1 AND $ID NOT IN ($idsNotToGetStringBuilder) ORDER BY $BOOKMARK_NAME ASC", null)

        // Get the bookmarks sorted alphabetically except for those database IDs specified, ordered by name.
        val bookmarksCursor = bookmarksDatabase.rawQuery(
            "SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $IS_FOLDER = 0 AND $ID NOT IN ($idsNotToGetStringBuilder) ORDER BY $BOOKMARK_NAME ASC", null)

        // Return the merged cursors.
        return MergeCursor(arrayOf(foldersCursor, bookmarksCursor))
    }

    fun getFolderBookmarks(parentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return a cursor with all the bookmarks in the folder.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $parentFolderId AND $IS_FOLDER = 0 ORDER BY $DISPLAY_ORDER ASC", null)
    }

    // Get the database ID for the specified folder name.
    fun getFolderDatabaseId(folderId: Long): Int {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Initialize the database ID.
        var databaseId = 0

        // Get the cursor for the folder with the specified name.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $FOLDER_ID = $folderId", null)

        // Get the database ID if it exists.
        if (folderCursor.count > 0) {
            // Move to the first record.
            folderCursor.moveToFirst()

            // Get the database ID.
            databaseId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(ID))
        }

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the database ID.
        return databaseId
    }

    // Get the folder ID for the specified folder database ID.
    fun getFolderId(folderDatabaseId: Int): Long {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get the cursor for the folder with the specified database ID.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT $FOLDER_ID FROM $BOOKMARKS_TABLE WHERE $ID = $folderDatabaseId", null)

        // Move to the first record.
        folderCursor.moveToFirst()

        // Get the folder ID.
        val folderId = folderCursor.getLong(folderCursor.getColumnIndexOrThrow(FOLDER_ID))

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the folder ID.
        return folderId
    }

    // Get the folder name for the specified folder ID.
    fun getFolderName(folderId: Long): String {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Initialize the folder name.
        var folderName = ""

        // Get the cursor for the folder with the specified folder ID.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT $BOOKMARK_NAME FROM $BOOKMARKS_TABLE WHERE $FOLDER_ID = $folderId", null)

        // Get the folder name if it exists.
        if (folderCursor.count > 0) {
            // Move to the first record.
            folderCursor.moveToFirst()

            // Get the folder name.
            folderName = folderCursor.getString(folderCursor.getColumnIndexOrThrow(BOOKMARK_NAME))
        }

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the folder name.
        return folderName
    }

    // Get a cursor of all the folders except those specified.
    fun getFoldersExcept(exceptFolderIdLongList: List<Long>): Cursor {
        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val folderIdsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (folderId in exceptFolderIdLongList) {
            // Check to see if there is already a number in the builder.
            if (folderIdsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                folderIdsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            folderIdsNotToGetStringBuilder.append(folderId)
        }

        // Get an array list with all of the requested subfolders.
        val subfoldersCursorArrayList = getSubfoldersExcept(HOME_FOLDER_ID, folderIdsNotToGetStringBuilder.toString())

        // Return a cursor.
        return if (subfoldersCursorArrayList.isEmpty()) {  // There are no folders.  Return an empty cursor.
            // A matrix cursor requires the definition of at least one column.
            MatrixCursor(arrayOf(ID))
        } else {  // There is at least one folder.
            // Use a merge cursor to return the folders.
            MergeCursor(subfoldersCursorArrayList.toTypedArray())
        }
    }

    // Determine if any folders exist beside the specified database IDs.  The array of database IDs can include both bookmarks and folders.
    fun hasFoldersExceptDatabaseId(exceptDatabaseIdLongArray: LongArray): Boolean {
        // Create a folder ID long list.
        val folderIdLongList = mutableListOf<Long>()

        // Populate the list.
        for (databaseId in exceptDatabaseIdLongArray) {
            // Convert the database ID to an Int.
            val databaseIdInt = databaseId.toInt()

            // Only process database IDs that are folders.
            if (isFolder(databaseIdInt)) {
                // Add the folder ID to the list.
                folderIdLongList.add(getFolderId(databaseIdInt))
            }
        }

        // Get a lit of all the folders except those specified and their subfolders.
        val foldersCursor = getFoldersExcept(folderIdLongList)

        // Determine if any other folders exists.
        val hasFolder = (foldersCursor.count > 0)

        // Close the cursor.
        foldersCursor.close()

        // Return the folder status.
        return hasFolder
    }

    // Get the name of the parent folder
    fun getParentFolderId(currentFolderId: Long): Long {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor for the current folder.
        val bookmarkCursor = bookmarksDatabase.rawQuery("SELECT $PARENT_FOLDER_ID FROM $BOOKMARKS_TABLE WHERE $FOLDER_ID = $currentFolderId", null)

        // Move to the first record.
        bookmarkCursor.moveToFirst()

        // Store the parent folder ID.
        val parentFolderId = bookmarkCursor.getLong(bookmarkCursor.getColumnIndexOrThrow(PARENT_FOLDER_ID))

        // Close the cursor and the database.
        bookmarkCursor.close()
        bookmarksDatabase.close()

        // Return the parent folder string ID.
        return parentFolderId
    }

    // Get the name of the parent folder.
    fun getParentFolderId(databaseId: Int): Long {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor for the specified database ID.
        val bookmarkCursor = bookmarksDatabase.rawQuery("SELECT $PARENT_FOLDER_ID FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)

        // Move to the first record.
        bookmarkCursor.moveToFirst()

        // Store the name of the parent folder.
        val parentFolderId = bookmarkCursor.getLong(bookmarkCursor.getColumnIndexOrThrow(PARENT_FOLDER_ID))

        // Close the cursor and the database.
        bookmarkCursor.close()
        bookmarksDatabase.close()

        // Return the parent folder string.
        return parentFolderId
    }

    // Get a cursor with the names and folder IDs of all the subfolders of the specified folder.
    fun getSubfolderNamesAndFolderIds(currentFolderId: Long): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return the cursor with the subfolders.  The cursor can't be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT $BOOKMARK_NAME, $FOLDER_ID FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $currentFolderId AND $IS_FOLDER = 1", null)
    }

    fun getSubfolderSpacer(folderId: Long): String {
        // Create a spacer string
        var spacerString = ""

        // Get the parent folder ID.
        val parentFolderId = getParentFolderId(folderId)

        // Check to see if the parent folder is not in the home folder.
        if (parentFolderId != HOME_FOLDER_ID) {
            // Add two spaces to the spacer string.
            spacerString += "  "

            // Check the parent folder recursively.
            spacerString += getSubfolderSpacer(parentFolderId)
        }

        // Return the spacer string.
        return spacerString
    }

    private fun getSubfoldersExcept(folderId: Long, exceptFolderIdString: String): ArrayList<Cursor> {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Create a cursor array list.
        val cursorArrayList = ArrayList<Cursor>()

        // Create a matrix cursor column names.
        val matrixCursorColumnNames = arrayOf(ID, BOOKMARK_NAME, FAVORITE_ICON, PARENT_FOLDER_ID, FOLDER_ID)

        // Get a cursor with the subfolders.
        val subfolderCursor = bookmarksDatabase.rawQuery(
            "SELECT * FROM $BOOKMARKS_TABLE WHERE $IS_FOLDER = 1 AND $PARENT_FOLDER_ID = $folderId AND $FOLDER_ID NOT IN ($exceptFolderIdString) ORDER BY $DISPLAY_ORDER ASC", null)

        // Get the subfolder cursor column indexes.
        val idColumnIndex = subfolderCursor.getColumnIndexOrThrow(ID)
        val nameColumnIndex = subfolderCursor.getColumnIndexOrThrow(BOOKMARK_NAME)
        val favoriteIconColumnIndex = subfolderCursor.getColumnIndexOrThrow(FAVORITE_ICON)
        val parentFolderIdColumnIndex = subfolderCursor.getColumnIndexOrThrow(PARENT_FOLDER_ID)
        val folderIdColumnIndex = subfolderCursor.getColumnIndexOrThrow(FOLDER_ID)

        while (subfolderCursor.moveToNext()) {
            // Create an array list.
            val matrixCursor = MatrixCursor(matrixCursorColumnNames)

            // Add the subfolder to the matrix cursor.
            matrixCursor.addRow(arrayOf<Any>(subfolderCursor.getInt(idColumnIndex), subfolderCursor.getString(nameColumnIndex), subfolderCursor.getBlob(favoriteIconColumnIndex),
                subfolderCursor.getLong(parentFolderIdColumnIndex), subfolderCursor.getLong(folderIdColumnIndex)))

            // Add the matrix cursor to the array list.
            cursorArrayList.add(matrixCursor)

            // Get all the sub-subfolders recursively
            cursorArrayList.addAll(getSubfoldersExcept(subfolderCursor.getLong(folderIdColumnIndex), exceptFolderIdString))
        }

        // Close the subfolder cursor.
        subfolderCursor.close()

        // Return the matrix cursor.
        return cursorArrayList
    }

    // Check if a database ID is a folder.
    fun isFolder(databaseId: Int): Boolean {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor with the is folder field for the specified database ID.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT $IS_FOLDER FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)

        // Move to the first record.
        folderCursor.moveToFirst()

        // Ascertain if this database ID is a folder.
        val isFolder = (folderCursor.getInt(folderCursor.getColumnIndexOrThrow(IS_FOLDER)) == 1)

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the folder status.
        return isFolder
    }

    // Move one bookmark or folder to a new folder.
    fun moveToFolder(databaseId: Int, newFolderId: Long) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Get a cursor for all the bookmarks in the new folder ordered by display order.
        val newFolderCursor = bookmarksDatabase.rawQuery("SELECT $DISPLAY_ORDER FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $newFolderId ORDER BY $DISPLAY_ORDER ASC", null)

        // Set the new display order.
        val displayOrder: Int = if (newFolderCursor.count > 0) {  // There are already bookmarks in the folder.
            // Move to the last bookmark.
            newFolderCursor.moveToLast()

            // Set the display order to be one greater that the last bookmark.
            newFolderCursor.getInt(newFolderCursor.getColumnIndexOrThrow(DISPLAY_ORDER)) + 1
        } else {  // There are no bookmarks in the new folder.
            // Set the display order to be `0`.
            0
        }

        // Close the cursor.
        newFolderCursor.close()

        // Create a content values.
        val bookmarkContentValues = ContentValues()

        // Store the new values.
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkContentValues.put(PARENT_FOLDER_ID, newFolderId)

        // Update the database.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    fun recalculateFolderContentsDisplayOrder(folderId: Long) {
        // Get a readable database.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor with the current content of the folder.
        val folderContentsCursor = bookmarksDatabase.rawQuery("SELECT $ID, $DISPLAY_ORDER FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER_ID = $folderId ORDER BY $DISPLAY_ORDER ASC", null)

        // Get the count of the folder contents.
        val folderContentsCount = folderContentsCursor.count

        // Get the query columns.
        val databaseIdColumnInt = folderContentsCursor.getColumnIndexOrThrow(ID)
        val displayOrderColumnInt = folderContentsCursor.getColumnIndexOrThrow(DISPLAY_ORDER)

        // Move to the first entry.
        folderContentsCursor.moveToFirst()

        // Update the display order if it isn't currently correct.
        for (i in 0 until folderContentsCount) {
            // Use the current value for `i` as the display order if it isn't currently so.
            if (i != folderContentsCursor.getInt(displayOrderColumnInt))
                updateDisplayOrder(folderContentsCursor.getInt(databaseIdColumnInt), i)

            // Move to the next entry.
            folderContentsCursor.moveToNext()
        }

        // Close the cursor.
        folderContentsCursor.close()
    }

    // Update the bookmark name and URL.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the bookmark name, URL, parent folder, and display order.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String, parentFolderId: Long, displayOrder: Int) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(PARENT_FOLDER_ID, parentFolderId)
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the bookmark name, URL, and favorite icon.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String, favoriteIcon: ByteArray) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the bookmark name, URL, parent folder, display order, and favorite icon.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String, parentFolderId: Long, displayOrder: Int, favoriteIcon: ByteArray) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(PARENT_FOLDER_ID, parentFolderId)
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the display order for one bookmark or folder.
    fun updateDisplayOrder(databaseId: Int, displayOrder: Int) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a content values.
        val bookmarkContentValues = ContentValues()

        // Store the new display order.
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)

        // Update the database.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name.
    fun updateFolder(databaseId: Int, newFolderName: String) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the new folder name.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name, parent folder, and display order.
    fun updateFolder(databaseId: Int, newFolderName: String, parentFolderId: Long, displayOrder: Int) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the new folder values.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)
        folderContentValues.put(PARENT_FOLDER_ID, parentFolderId)
        folderContentValues.put(DISPLAY_ORDER, displayOrder)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name and icon.
    fun updateFolder(databaseId: Int, newFolderName: String, folderIcon: ByteArray) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the updated values.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)
        folderContentValues.put(FAVORITE_ICON, folderIcon)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name and icon.
    fun updateFolder(databaseId: Int, newFolderName: String, parentFolderId: Long, displayOrder: Int, folderIcon: ByteArray) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the updated values.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)
        folderContentValues.put(PARENT_FOLDER_ID, parentFolderId)
        folderContentValues.put(DISPLAY_ORDER, displayOrder)
        folderContentValues.put(FAVORITE_ICON, folderIcon)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    private fun generateFolderId(): Long {
        // Get the current time in epoch format (in milliseconds).
        val possibleFolderId = Date().time

        // Get a readable database.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor with any folders that already have this folder ID.
        val existingFolderCursor = bookmarksDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $FOLDER_ID = $possibleFolderId", null)

        // Check if the folder ID is unique.
        val folderIdIsUnique = (existingFolderCursor.count == 0)

        // Close the cursor.
        existingFolderCursor.close()

        // Either return the folder ID or test a new one.
        return if (folderIdIsUnique)
            possibleFolderId
        else
            generateFolderId()
    }
}
