/*
 * Copyright 2018-2024 Soren Stoutner <soren@stoutner.com>.
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
 * along with Privacy Browser Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.audeon.browser.helpers

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase

import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.activities.HOME_FOLDER_ID

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

import java.util.Date

// Define the public constants.
const val IMPORT_EXPORT_SCHEMA_VERSION = 20
const val EXPORT_SUCCESSFUL = "export_successful"
const val IMPORT_SUCCESSFUL = "import_successful"

// Define the private class constants.
private const val ALLOW_SCREENSHOTS = "allow_screenshots"
private const val AMP_REDIRECTS = "amp_redirects"
private const val APP_THEME = "app_theme"
private const val BOTTOM_APP_BAR = "bottom_app_bar"
private const val CLEAR_CACHE = "clear_cache"
private const val CLEAR_COOKIES = "clear_cookies"
private const val CLEAR_DOM_STORAGE = "clear_dom_storage"
private const val CLEAR_EVERYTHING = "clear_everything"
private const val CLEAR_LOGCAT = "clear_logcat"
private const val CUSTOM_USER_AGENT = "custom_user_agent"
private const val DISPLAY_ADDITIONAL_APP_BAR_ICONS = "display_additional_app_bar_icons"
private const val DISPLAY_UNDER_CUTOUTS = "display_under_cutouts"
private const val DISPLAY_WEBPAGE_IMAGES = "display_webpage_images"
private const val DOM_STORAGE = "dom_storage"
private const val DOWNLOAD_PROVIDER = "download_provider"
private const val EASYLIST = "easylist"
private const val EASYPRIVACY = "easyprivacy"
private const val FANBOYS_ANNOYANCE_LIST = "fanboys_annoyance_list"
private const val FANBOYS_SOCIAL_BLOCKING_LIST = "fanboys_social_blocking_list"
private const val FULL_SCREEN_BROWSING_MODE = "full_screen_browsing_mode"
private const val HIDE_APP_BAR = "hide_app_bar"
private const val HOMEPAGE = "homepage"
private const val INCOGNITO_MODE = "incognito_mode"
private const val JAVASCRIPT = "javascript"
private const val OPEN_INTENTS_IN_NEW_TAB = "open_intents_in_new_tab"
private const val PREFERENCES_BLOCK_ALL_THIRD_PARTY_REQUESTS = "block_all_third_party_requests"
private const val PREFERENCES_FONT_SIZE = "font_size"
private const val PREFERENCES_TABLE = "preferences"
private const val PREFERENCES_USER_AGENT = "user_agent"
private const val PROXY = "proxy"
private const val PROXY_CUSTOM_URL = "proxy_custom_url"
private const val SEARCH = "search"
private const val SEARCH_CUSTOM_URL = "search_custom_url"
private const val SCROLL_APP_BAR = "scroll_app_bar"
private const val SORT_BOOKMARKS_ALPHABETICALLY = "sort_bookmarks_alphabetically"
private const val PREFERENCES_SWIPE_TO_REFRESH = "swipe_to_refresh"
private const val TRACKING_QUERIES = "tracking_queries"
private const val ULTRAPRIVACY = "ultraprivacy"

class ImportExportDatabaseHelper {
    fun importUnencrypted(importFileInputStream: InputStream, context: Context): String {
        return try {
            // Create a temporary import file.
            val temporaryImportFile = File.createTempFile("temporary_import_file", null, context.cacheDir)

            // The file may be copied directly in Kotlin using `File.copyTo`.  <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/java.io.-file/copy-to.html>
            // It can be copied in Android using `Files.copy` once the minimum API >= 26.
            // <https://developer.android.com/reference/java/nio/file/Files#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...)>
            // However, the file cannot be acquired from the content URI until the minimum API >= 29.  <https://developer.android.com/reference/kotlin/android/content/ContentResolver#openfile>

            // Create a temporary file output stream.
            val temporaryImportFileOutputStream = FileOutputStream(temporaryImportFile)

            // Create a transfer byte array.
            val transferByteArray = ByteArray(1024)

            // Create an integer to track the number of bytes read.
            var bytesRead: Int

            // Copy the import file to the temporary import file.
            while (importFileInputStream.read(transferByteArray).also { bytesRead = it } > 0) {
                temporaryImportFileOutputStream.write(transferByteArray, 0, bytesRead)
            }

            // Flush the temporary import file output stream.
            temporaryImportFileOutputStream.flush()

            // Close the file streams.
            importFileInputStream.close()
            temporaryImportFileOutputStream.close()


            // Get a handle for the shared preference.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Open the import database.  Once the minimum API >= 27 the file can be opened directly without using the string.
            val importDatabase = SQLiteDatabase.openDatabase(temporaryImportFile.toString(), null, SQLiteDatabase.OPEN_READWRITE)

            // Get the database version.
            val importDatabaseVersion = importDatabase.version

            // Upgrade from schema version 1, first used in Privacy Browser 2.13, to schema version 2, first used in Privacy Browser 2.14.
            // Previously this upgrade added `download_with_external_app` to the Preferences table.  But that is now removed in schema version 10.

            // Upgrade from schema version 2, first used in Privacy Browser 2.14, to schema version 3, first used in Privacy Browser 2.15.
            if (importDatabaseVersion < 3) {
                // `default_font_size` was renamed `font_size`.
                // Once the SQLite version is >= 3.25.0 (Android API >= 30) `ALTER TABLE RENAME COLUMN` can be used.  <https://www.sqlite.org/lang_altertable.html> <https://www.sqlite.org/changes.html>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                // In the meantime, a new column must be created with the new name.  There is no need to delete the old column on the temporary import database.

                // Create the new font size column.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $PREFERENCES_FONT_SIZE TEXT")

                // Populate the preferences table with the current font size value.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $PREFERENCES_FONT_SIZE = default_font_size")
            }

            // Upgrade from schema version 3, first used in Privacy Browser 2.15, to schema version 4, first used in Privacy Browser 2.16.
            if (importDatabaseVersion < 4) {
                // Add the Pinned IP Addresses columns to the domains table.
                importDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $PINNED_IP_ADDRESSES  BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $IP_ADDRESSES TEXT")
            }

            // Upgrade from schema version 4, first used in Privacy Browser 2.16, to schema version 5, first used in Privacy Browser 2.17.
            if (importDatabaseVersion < 5) {
                // Add the hide and scroll app bar columns to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $HIDE_APP_BAR BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $SCROLL_APP_BAR BOOLEAN")

                // Get the current hide and scroll app bar settings.
                val hideAppBar = sharedPreferences.getBoolean(HIDE_APP_BAR, true)
                val scrollAppBar = sharedPreferences.getBoolean(SCROLL_APP_BAR, true)

                // Populate the preferences table with the current app bar values.
                // This can switch to using the variables directly once the minimum API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (hideAppBar)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $HIDE_APP_BAR = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $HIDE_APP_BAR = 0")

                if (scrollAppBar)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $SCROLL_APP_BAR = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $SCROLL_APP_BAR = 0")
            }

            // Upgrade from schema version 5, first used in Privacy Browser 2.17, to schema version 6, first used in Privacy Browser 3.0.
            if (importDatabaseVersion < 6) {
                // Add the open intents in new tab column to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $OPEN_INTENTS_IN_NEW_TAB BOOLEAN")

                // Get the current open intents in new tab preference.
                val openIntentsInNewTab = sharedPreferences.getBoolean(OPEN_INTENTS_IN_NEW_TAB, true)

                // Populate the preferences table with the current open intents value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (openIntentsInNewTab)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $OPEN_INTENTS_IN_NEW_TAB = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $OPEN_INTENTS_IN_NEW_TAB = 0")
            }

            // Upgrade from schema version 6, first used in Privacy Browser 3.0, to schema version 7, first used in Privacy Browser 3.1.
            if (importDatabaseVersion < 7) {
                // Previously this upgrade added `facebook_click_ids` to the Preferences table.  But that is now removed in schema version 15.

                // Add the wide viewport column to the domains table.
                importDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $WIDE_VIEWPORT INTEGER")

                // Add the Google Analytics, Twitter AMP redirects, and wide viewport columns to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN google_analytics BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN twitter_amp_redirects BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $WIDE_VIEWPORT BOOLEAN")

                // Get the current preference values.
                val trackingQueries = sharedPreferences.getBoolean(TRACKING_QUERIES, true)
                val ampRedirects = sharedPreferences.getBoolean(AMP_REDIRECTS, true)
                val wideViewport = sharedPreferences.getBoolean(WIDE_VIEWPORT, true)

                // Populate the preferences with the current Tracking Queries value.  Google Analytics was renamed Tracking Queries in schema version 15.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (trackingQueries)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET google_analytics = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET google_analytics = 0")

                // Populate the preferences table with the current AMP Redirects value.  Twitter AMP Redirects was renamed AMP Redirects in schema version 15.
                if (ampRedirects)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET twitter_amp_redirects = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET twitter_amp_redirects = 0")

                // Populate the preferences table with the current wide viewport value.
                if (wideViewport)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $WIDE_VIEWPORT = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $WIDE_VIEWPORT = 0")
            }

            // Upgrade from schema version 7, first used in Privacy Browser 3.1, to schema version 8, first used in Privacy Browser 3.2.
            if (importDatabaseVersion < 8) {
                // Add the UltraList column to the tables.
                importDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ULTRALIST INTEGER")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $ULTRALIST BOOLEAN")

                // Get the current preference values.
                val ultraList = sharedPreferences.getBoolean(ULTRALIST, true)

                // Populate the tables with the current UltraList value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (ultraList) {
                    importDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ULTRALIST = 1")
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $ULTRALIST = 1")
                } else {
                    importDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ULTRALIST = 0")
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $ULTRALIST = 0")
                }
            }

            // Upgrade from schema version 8, first used in Privacy Browser 3.2, to schema version 9, first used in Privacy Browser 3.3.
            if (importDatabaseVersion < 9) {
                // Add the new proxy columns to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $PROXY TEXT")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $PROXY_CUSTOM_URL TEXT")

                // Get the current proxy values.
                val proxy = sharedPreferences.getString(PROXY, context.getString(R.string.proxy_default_value))
                var proxyCustomUrl = sharedPreferences.getString(PROXY_CUSTOM_URL, context.getString(R.string.proxy_custom_url_default_value))

                // SQL escape the proxy custom URL string.
                proxyCustomUrl = DatabaseUtils.sqlEscapeString(proxyCustomUrl)

                // Populate the preferences table with the current proxy values. The proxy custom URL does not need to be surrounded by `'` because it was SLQ escaped above.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $PROXY = '$proxy'")
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $PROXY_CUSTOM_URL = $proxyCustomUrl")
            }

            // Upgrade from schema version 9, first used in Privacy Browser 3.3, to schema version 10, first used in Privacy Browser 3.4.
            // Previously this upgrade added `download_location` and `download_custom_location` to the Preferences table.  But they were removed in schema version 13.

            // Upgrade from schema version 10, first used in Privacy Browser 3.4, to schema version 11, first used in Privacy Browser 3.5.
            if (importDatabaseVersion < 11) {
                // Add the app theme column to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $APP_THEME TEXT")

                // Get a cursor for the dark theme preference.
                val darkThemePreferencesCursor = importDatabase.rawQuery("SELECT dark_theme FROM $PREFERENCES_TABLE", null)

                // Move to the first entry.
                darkThemePreferencesCursor.moveToFirst()

                // Get the old dark theme value, which is in column 0.
                val darkTheme = darkThemePreferencesCursor.getInt(0)

                // Close the dark theme preference cursor.
                darkThemePreferencesCursor.close()

                // Get the system default string.
                val systemDefault = context.getString(R.string.app_theme_default_value)

                // Get the theme entry values string array.
                val appThemeEntryValuesStringArray: Array<String> = context.resources.getStringArray(R.array.app_theme_entry_values)

                // Get the dark string.
                val dark = appThemeEntryValuesStringArray[2]

                // Populate the app theme according to the old dark theme preference.
                if (darkTheme == 0) {  // A light theme was selected.
                    // Set the app theme to be the system default.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $APP_THEME = '$systemDefault'")
                } else {  // A dark theme was selected.
                    // Set the app theme to be dark.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $APP_THEME = '$dark'")
                }

                // Add the WebView theme to the domains table.  This defaults to 0, which is `System default`, so a separate step isn't needed to populate the database.
                importDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $WEBVIEW_THEME INTEGER")

                // Add the WebView theme to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $WEBVIEW_THEME TEXT")

                // Get the WebView theme default value string.
                val webViewThemeDefaultValue = context.getString(R.string.webview_theme_default_value)

                // Set the WebView theme in the preferences table to be the default.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $WEBVIEW_THEME = '$webViewThemeDefaultValue'")
            }

            // Upgrade from schema version 11, first used in Privacy Browser 3.5, to schema version 12, first used in Privacy Browser 3.6.
            if (importDatabaseVersion < 12) {
                // Add the clear logcat column to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $CLEAR_LOGCAT BOOLEAN")

                // Get the current clear logcat value.
                val clearLogcat = sharedPreferences.getBoolean(CLEAR_LOGCAT, true)

                // Populate the preferences table with the current clear logcat value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (clearLogcat)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $CLEAR_LOGCAT = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $CLEAR_LOGCAT = 0")
            }

            // Upgrade from schema version 12, first used in Privacy Browser 3.6, to schema version 13, first used in Privacy Browser 3.7.
            // Do nothing.  `download_location` and `download_custom_location` were removed from the preferences table, but they can be left in the temporary import database without issue.

            // Upgrade from schema version 13, first used in Privacy Browser 3.7, to schema version 14, first used in Privacy Browser 3.8.
            if (importDatabaseVersion < 14) {
                // `enabledthirdpartycookies` was removed from the domains table.  `do_not_track` and `third_party_cookies` were removed from the preferences table.
                // There is no need to delete the columns as they will simply be ignored by the import.

                // `enablefirstpartycookies` was renamed `cookies`.
                // Once the SQLite version is >= 3.25.0 (Android API >= 30) `ALTER TABLE RENAME COLUMN` can be used.  <https://www.sqlite.org/lang_altertable.html> <https://www.sqlite.org/changes.html>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                // In the meantime, a new column must be created with the new name.  There is no need to delete the old column on the temporary import database.

                // Create the new cookies columns.
                importDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $COOKIES INTEGER")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $COOKIES BOOLEAN")

                // Copy the data from the old cookies columns to the new ones.
                importDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $COOKIES = enablefirstpartycookies")
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $COOKIES = first_party_cookies")

                // Create the new download with external app and bottom app bar columns.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN download_with_external_app BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $BOTTOM_APP_BAR BOOLEAN")

                // Get the current values for the new columns.
                val bottomAppBar = sharedPreferences.getBoolean(BOTTOM_APP_BAR, false)
                val downloadProviderString = sharedPreferences.getString(DOWNLOAD_PROVIDER, context.getString(R.string.download_provider_default_value))

                // Get the download provider entry values string array.
                val tempDownloadProviderEntryValuesStringArray = context.resources.getStringArray(R.array.download_provider_entry_values)

                // Populate the new download with external app preference.  It was added in this version of the schema, but removed in version 18.
                // The new preference, `download_provider`, converts `download_with_external_app`, so it needs to exist.
                // This code sets `download_with_external_app` to be as similar as possible to the current preference in the settings.
                if (downloadProviderString == tempDownloadProviderEntryValuesStringArray[0])  // Download with Privacy Browser.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET download_with_external_app = 0")
                else  // Download with external app.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET download_with_external_app = 1")

                // Populate the preferences table with the current bottom app bar value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (bottomAppBar)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $BOTTOM_APP_BAR = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $BOTTOM_APP_BAR = 0")
            }

            // Upgrade from schema version 14, first used in Privacy Browser 3.8, to schema version 15, first used in Privacy Browser 3.11.
            if (importDatabaseVersion < 15) {
                // `facebook_click_ids` was removed from the preferences table.
                // There is no need to delete the columns as they will simply be ignored by the import.

                // `x_requested_with_header` was previously added to the preferences and domains tables in this version, but it was removed later in schema version 16.

                // Create the new URL modification columns.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $TRACKING_QUERIES BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $AMP_REDIRECTS BOOLEAN")

                // Copy the data from the old columns to the new ones.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $TRACKING_QUERIES = google_analytics")
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $AMP_REDIRECTS = twitter_amp_redirects")
            }

            // Upgrade from schema version 15, first used in Privacy Browser 3.11, to schema version 16, first used in Privacy Browser 3.12.
            // This upgrade removed `x_requested_with_header` from the domains and preferences tables.
            // There is no need to delete the columns as they will simply be ignored by the import.

            // Upgrade from schema version 16, first used in Privacy Browser 3.12, to schema version 17, first used in Privacy Browser 3.15.
            if (importDatabaseVersion < 17) {
                // This upgrade removed `parentfolder` from the Bookmarks table.
                // There is no need to delete the column as they will simply be ignored by the import.

                // Add the folder ID column.
                importDatabase.execSQL("ALTER TABLE $BOOKMARKS_TABLE ADD COLUMN $FOLDER_ID INTEGER")

                // Get a cursor with all the folders.
                val foldersCursor = importDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $IS_FOLDER = 1", null)

                // Get the folders cursor ID column index.
                val foldersCursorIdColumnIndex = foldersCursor.getColumnIndexOrThrow(ID)

                // Add a folder ID to each folder.
                while(foldersCursor.moveToNext()) {
                    // Get the current folder database ID.
                    val databaseId = foldersCursor.getInt(foldersCursorIdColumnIndex)

                    // Generate a folder ID.
                    val folderId = generateFolderId(importDatabase)

                    // Create a folder content values.
                    val folderContentValues = ContentValues()

                    // Store the new folder ID in the content values.
                    folderContentValues.put(FOLDER_ID, folderId)

                    // Update the folder with the new folder ID.
                    importDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)
                }

                // Close the folders cursor.
                foldersCursor.close()


                // Add the parent folder ID column.
                importDatabase.execSQL("ALTER TABLE $BOOKMARKS_TABLE ADD COLUMN $PARENT_FOLDER_ID INTEGER")

                // Get a cursor with all the bookmarks.
                val bookmarksCursor = importDatabase.rawQuery("SELECT $ID, parentfolder FROM $BOOKMARKS_TABLE", null)

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
                        val parentFolderCursor = importDatabase.rawQuery("SELECT $FOLDER_ID FROM $BOOKMARKS_TABLE WHERE $BOOKMARK_NAME = $sqlEscapedFolderName AND $IS_FOLDER = 1", null)

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
                    importDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)
                }

                // Close the bookmarks cursor.
                bookmarksCursor.close()

                // Get the current switch default values.
                val javaScriptDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.javascript_key), false)
                val cookiesDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.cookies_key), false)
                val domStorageDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.dom_storage_key), false)
                val easyListDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.easylist_key), true)
                val easyPrivacyDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.easyprivacy_key), true)
                val fanboysAnnoyanceListDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.fanboys_annoyance_list_key), true)
                val fanboysSocialBlockingListDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.fanboys_social_blocking_list), true)
                val ultraListDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.ultralist_key), true)
                val ultraPrivacyDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.ultraprivacy_key), true)
                val blockAllThirdPartyRequestsDefaultValue = sharedPreferences.getBoolean(context.getString(R.string.block_all_third_party_requests_key), false)

                // Get a domains cursor.
                val importDomainsConversionCursor = importDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE", null)

                // Get the domains column indexes.
                val javaScriptColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_JAVASCRIPT)
                val cookiesColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(COOKIES)
                val domStorageColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_DOM_STORAGE)
                val easyListColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_EASYLIST)
                val easyPrivacyColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY)
                val fanboysAnnoyanceListColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_ANNOYANCE_LIST)
                val fanboysSocialBlockingListColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)
                val ultraListColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ULTRALIST)
                val ultraPrivacyColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(ENABLE_ULTRAPRIVACY)
                val blockAllThirdPartyRequestsColumnIndex = importDomainsConversionCursor.getColumnIndexOrThrow(BLOCK_ALL_THIRD_PARTY_REQUESTS)

                // Convert the domain from the switch booleans to the spinner integers.
                for (i in 0 until importDomainsConversionCursor.count) {
                    // Move to the current record.
                    importDomainsConversionCursor.moveToPosition(i)

                    // Get the domain current values.
                    val javaScriptDomainCurrentValue = importDomainsConversionCursor.getInt(javaScriptColumnIndex)
                    val cookiesDomainCurrentValue = importDomainsConversionCursor.getInt(cookiesColumnIndex)
                    val domStorageDomainCurrentValue = importDomainsConversionCursor.getInt(domStorageColumnIndex)
                    val easyListDomainCurrentValue = importDomainsConversionCursor.getInt(easyListColumnIndex)
                    val easyPrivacyDomainCurrentValue = importDomainsConversionCursor.getInt(easyPrivacyColumnIndex)
                    val fanboysAnnoyanceListCurrentValue = importDomainsConversionCursor.getInt(fanboysAnnoyanceListColumnIndex)
                    val fanboysSocialBlockingListCurrentValue = importDomainsConversionCursor.getInt(fanboysSocialBlockingListColumnIndex)
                    val ultraListCurrentValue = importDomainsConversionCursor.getInt(ultraListColumnIndex)
                    val ultraPrivacyCurrentValue = importDomainsConversionCursor.getInt(ultraPrivacyColumnIndex)
                    val blockAllThirdPartyRequestsCurrentValue = importDomainsConversionCursor.getInt(blockAllThirdPartyRequestsColumnIndex)

                    // Instantiate a domain content values.
                    val domainContentValues = ContentValues()

                    // Populate the domain content values.
                    domainContentValues.put(ENABLE_JAVASCRIPT, convertFromSwitchToSpinner(javaScriptDefaultValue, javaScriptDomainCurrentValue))
                    domainContentValues.put(COOKIES, convertFromSwitchToSpinner(cookiesDefaultValue, cookiesDomainCurrentValue))
                    domainContentValues.put(ENABLE_DOM_STORAGE, convertFromSwitchToSpinner(domStorageDefaultValue, domStorageDomainCurrentValue))
                    domainContentValues.put(ENABLE_EASYLIST, convertFromSwitchToSpinner(easyListDefaultValue, easyListDomainCurrentValue))
                    domainContentValues.put(ENABLE_EASYPRIVACY, convertFromSwitchToSpinner(easyPrivacyDefaultValue, easyPrivacyDomainCurrentValue))
                    domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, convertFromSwitchToSpinner(fanboysAnnoyanceListDefaultValue, fanboysAnnoyanceListCurrentValue))
                    domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, convertFromSwitchToSpinner(fanboysSocialBlockingListDefaultValue, fanboysSocialBlockingListCurrentValue))
                    domainContentValues.put(ULTRALIST, convertFromSwitchToSpinner(ultraListDefaultValue, ultraListCurrentValue))
                    domainContentValues.put(ENABLE_ULTRAPRIVACY, convertFromSwitchToSpinner(ultraPrivacyDefaultValue, ultraPrivacyCurrentValue))
                    domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, convertFromSwitchToSpinner(blockAllThirdPartyRequestsDefaultValue, blockAllThirdPartyRequestsCurrentValue))

                    // Get the current database ID.
                    val currentDatabaseId = importDomainsConversionCursor.getInt(importDomainsConversionCursor.getColumnIndexOrThrow(ID))

                    // Update the row for the specified database ID.
                    importDatabase.update(DOMAINS_TABLE, domainContentValues, "$ID = $currentDatabaseId", null)
                }

                // Close the cursor.
                importDomainsConversionCursor.close()
            }


            // Upgrade from schema version 17, first used in Privacy Browser 3.15, to schema version 18, first used in Privacy Browser 3.17.
            if (importDatabaseVersion < 18) {
                // Create the new columns.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $DISPLAY_UNDER_CUTOUTS BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $DOWNLOAD_PROVIDER TEXT")

                // Get the current display under cutout value.
                val displayUnderCutouts = sharedPreferences.getBoolean(DISPLAY_UNDER_CUTOUTS, false)

                // Populate the preferences table with the current display under cutouts value.
                // This can switch to using the variables directly once the minimum API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (displayUnderCutouts)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $DISPLAY_UNDER_CUTOUTS = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $DISPLAY_UNDER_CUTOUTS = 0")

                // Get the download with external app cursor.
                val downloadWithExternalAppCursor = importDatabase.rawQuery("SELECT download_with_external_app FROM $PREFERENCES_TABLE", null)

                // Move to the first entry.
                downloadWithExternalAppCursor.moveToFirst()

                // Get the old download with external app setting.
                val downloadWithExternalApp = (downloadWithExternalAppCursor.getInt(downloadWithExternalAppCursor.getColumnIndexOrThrow("download_with_external_app")) == 1)

                // Close the cursor.
                downloadWithExternalAppCursor.close()

                // Get the download provider entry values string array.
                val downloadProviderEntryValuesStringArray = context.resources.getStringArray(R.array.download_provider_entry_values)

                // Populate the new download provider preference.
                if (downloadWithExternalApp)  // Download with external app.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $DOWNLOAD_PROVIDER = '${downloadProviderEntryValuesStringArray[2]}'")
                else  // Download with Privacy Browser.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $DOWNLOAD_PROVIDER = '${downloadProviderEntryValuesStringArray[0]}'")
            }

            // Upgrade from schema version 18, first used in Privacy Browser 3.17, to schema version 19, first used in Privacy Browser 3.18.
            // This upgrade removed `enableformdata` from the Domains table and `save_form_data` and `clear_form_data` from the Preferences table.
            // There is no need to delete the columns as they will simply be ignored by the import.

            // Upgrade from schema version 19, first used in Privacy Browser 3.18, to schema version 20, first used in Privacy Browser 3.19.
            if (importDatabaseVersion < 20) {
                // Create the new sort bookmarks alphabetically column.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $SORT_BOOKMARKS_ALPHABETICALLY BOOLEAN")

                // Get the current sort bookmarks alphabetically column.
                val sortBookmarksAlphabetically = sharedPreferences.getBoolean(SORT_BOOKMARKS_ALPHABETICALLY, false)

                // Populate the preferences table with the current sort bookmarks alphabetically value.
                // This can switch to using the variables directly once the minimum API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (sortBookmarksAlphabetically)
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $SORT_BOOKMARKS_ALPHABETICALLY = 1")
                else
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $SORT_BOOKMARKS_ALPHABETICALLY = 0")
            }


            /* End of database upgrade logic. */

            // Get a cursor for the bookmarks table.
            val importBookmarksCursor = importDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE", null)

            // Delete the current bookmarks database.
            context.deleteDatabase(BOOKMARKS_DATABASE)

            // Create a new bookmarks database.
            val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context)

            // Move to the first record.
            importBookmarksCursor.moveToFirst()

            // Get the bookmarks colum indexes.
            val bookmarkNameColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(BOOKMARK_NAME)
            val bookmarkUrlColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(BOOKMARK_URL)
            val bookmarkParentFolderIdColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(PARENT_FOLDER_ID)
            val bookmarkDisplayOrderColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(DISPLAY_ORDER)
            val bookmarkIsFolderColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(IS_FOLDER)
            val bookmarkFolderIdColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(FOLDER_ID)
            val bookmarkFavoriteIconColumnIndex = importBookmarksCursor.getColumnIndexOrThrow(FAVORITE_ICON)

            // Copy the data from the import bookmarks cursor into the bookmarks database.
            for (i in 0 until importBookmarksCursor.count) {
                // Create a bookmark content values.
                val bookmarkContentValues = ContentValues()

                // Add the information for this bookmark to the content values.
                bookmarkContentValues.put(BOOKMARK_NAME, importBookmarksCursor.getString(bookmarkNameColumnIndex))
                bookmarkContentValues.put(BOOKMARK_URL, importBookmarksCursor.getString(bookmarkUrlColumnIndex))
                bookmarkContentValues.put(PARENT_FOLDER_ID, importBookmarksCursor.getLong(bookmarkParentFolderIdColumnIndex))
                bookmarkContentValues.put(DISPLAY_ORDER, importBookmarksCursor.getInt(bookmarkDisplayOrderColumnIndex))
                bookmarkContentValues.put(IS_FOLDER, importBookmarksCursor.getInt(bookmarkIsFolderColumnIndex))
                bookmarkContentValues.put(FOLDER_ID, importBookmarksCursor.getLong(bookmarkFolderIdColumnIndex))
                bookmarkContentValues.put(FAVORITE_ICON, importBookmarksCursor.getBlob(bookmarkFavoriteIconColumnIndex))

                // Insert the content values into the bookmarks database.
                bookmarksDatabaseHelper.createBookmark(bookmarkContentValues)

                // Advance to the next record.
                importBookmarksCursor.moveToNext()
            }

            // Close the bookmarks cursor and database.
            importBookmarksCursor.close()
            bookmarksDatabaseHelper.close()


            // Get a cursor for the domains table.
            val importDomainsCursor = importDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE ORDER BY $DOMAIN_NAME ASC", null)

            // Delete the current domains database.
            context.deleteDatabase(DOMAINS_DATABASE)

            // Create a new domains database.
            val domainsDatabaseHelper = DomainsDatabaseHelper(context)

            // Move to the first record.
            importDomainsCursor.moveToFirst()

            // Get the domain column indexes.
            val domainNameColumnIndex = importDomainsCursor.getColumnIndexOrThrow(DOMAIN_NAME)
            val domainJavaScriptColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_JAVASCRIPT)
            val domainCookiesColumnIndex = importDomainsCursor.getColumnIndexOrThrow(COOKIES)
            val domainDomStorageColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_DOM_STORAGE)
            val domainEasyListColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_EASYLIST)
            val domainEasyPrivacyColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY)
            val domainFanboysAnnoyanceListColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_ANNOYANCE_LIST)
            val domainFanboysSocialBlockingListColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)
            val domainUltraListColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ULTRALIST)
            val domainUltraPrivacyColumnIndex = importDomainsCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY)
            val domainBlockAllThirdPartyRequestsColumnIndex = importDomainsCursor.getColumnIndexOrThrow(BLOCK_ALL_THIRD_PARTY_REQUESTS)
            val domainUserAgentColumnIndex = importDomainsCursor.getColumnIndexOrThrow(USER_AGENT)
            val domainFontSizeColumnIndex = importDomainsCursor.getColumnIndexOrThrow(FONT_SIZE)
            val domainSwipeToRefreshColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SWIPE_TO_REFRESH)
            val domainWebViewThemeColumnIndex = importDomainsCursor.getColumnIndexOrThrow(WEBVIEW_THEME)
            val domainWideViewportColumnIndex = importDomainsCursor.getColumnIndexOrThrow(WIDE_VIEWPORT)
            val domainDisplayImagesColumnIndex = importDomainsCursor.getColumnIndexOrThrow(DISPLAY_IMAGES)
            val domainPinnedSslCertificateColumnIndex = importDomainsCursor.getColumnIndexOrThrow(PINNED_SSL_CERTIFICATE)
            val domainSslIssuedToCommonNameColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_COMMON_NAME)
            val domainSslIssuedToOrganizationColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_ORGANIZATION)
            val domainSslIssuedToOrganizationalUnitColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT)
            val domainSslIssuedByCommonNameColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_COMMON_NAME)
            val domainSslIssuedByOrganizationColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_ORGANIZATION)
            val domainSslIssuedByOrganizationalUnitColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT)
            val domainSslStartDateColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_START_DATE)
            val domainSslEndDateColumnIndex = importDomainsCursor.getColumnIndexOrThrow(SSL_END_DATE)
            val domainPinnedIpAddressesColumnIndex = importDomainsCursor.getColumnIndexOrThrow(PINNED_IP_ADDRESSES)
            val domainIpAddressesColumnIndex = importDomainsCursor.getColumnIndexOrThrow(IP_ADDRESSES)

            // Copy the data from the import domains cursor into the domains database.
            for (i in 0 until importDomainsCursor.count) {
                // Create a domain content values.
                val domainContentValues = ContentValues()

                // Populate the domain content values.
                domainContentValues.put(DOMAIN_NAME, importDomainsCursor.getString(domainNameColumnIndex))
                domainContentValues.put(ENABLE_JAVASCRIPT, importDomainsCursor.getInt(domainJavaScriptColumnIndex))
                domainContentValues.put(COOKIES, importDomainsCursor.getInt(domainCookiesColumnIndex))
                domainContentValues.put(ENABLE_DOM_STORAGE, importDomainsCursor.getInt(domainDomStorageColumnIndex))
                domainContentValues.put(ENABLE_EASYLIST, importDomainsCursor.getInt(domainEasyListColumnIndex))
                domainContentValues.put(ENABLE_EASYPRIVACY, importDomainsCursor.getInt(domainEasyPrivacyColumnIndex))
                domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, importDomainsCursor.getInt(domainFanboysAnnoyanceListColumnIndex))
                domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, importDomainsCursor.getInt(domainFanboysSocialBlockingListColumnIndex))
                domainContentValues.put(ULTRALIST, importDomainsCursor.getInt(domainUltraListColumnIndex))
                domainContentValues.put(ENABLE_ULTRAPRIVACY, importDomainsCursor.getInt(domainUltraPrivacyColumnIndex))
                domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, importDomainsCursor.getInt(domainBlockAllThirdPartyRequestsColumnIndex))
                domainContentValues.put(USER_AGENT, importDomainsCursor.getString(domainUserAgentColumnIndex))
                domainContentValues.put(FONT_SIZE, importDomainsCursor.getInt(domainFontSizeColumnIndex))
                domainContentValues.put(SWIPE_TO_REFRESH, importDomainsCursor.getInt(domainSwipeToRefreshColumnIndex))
                domainContentValues.put(WEBVIEW_THEME, importDomainsCursor.getInt(domainWebViewThemeColumnIndex))
                domainContentValues.put(WIDE_VIEWPORT, importDomainsCursor.getInt(domainWideViewportColumnIndex))
                domainContentValues.put(DISPLAY_IMAGES, importDomainsCursor.getInt(domainDisplayImagesColumnIndex))
                domainContentValues.put(PINNED_SSL_CERTIFICATE, importDomainsCursor.getInt(domainPinnedSslCertificateColumnIndex))
                domainContentValues.put(SSL_ISSUED_TO_COMMON_NAME, importDomainsCursor.getString(domainSslIssuedToCommonNameColumnIndex))
                domainContentValues.put(SSL_ISSUED_TO_ORGANIZATION, importDomainsCursor.getString(domainSslIssuedToOrganizationColumnIndex))
                domainContentValues.put(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT, importDomainsCursor.getString(domainSslIssuedToOrganizationalUnitColumnIndex))
                domainContentValues.put(SSL_ISSUED_BY_COMMON_NAME, importDomainsCursor.getString(domainSslIssuedByCommonNameColumnIndex))
                domainContentValues.put(SSL_ISSUED_BY_ORGANIZATION, importDomainsCursor.getString(domainSslIssuedByOrganizationColumnIndex))
                domainContentValues.put(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT, importDomainsCursor.getString(domainSslIssuedByOrganizationalUnitColumnIndex))
                domainContentValues.put(SSL_START_DATE, importDomainsCursor.getLong(domainSslStartDateColumnIndex))
                domainContentValues.put(SSL_END_DATE, importDomainsCursor.getLong(domainSslEndDateColumnIndex))
                domainContentValues.put(PINNED_IP_ADDRESSES, importDomainsCursor.getInt(domainPinnedIpAddressesColumnIndex))
                domainContentValues.put(IP_ADDRESSES, importDomainsCursor.getString(domainIpAddressesColumnIndex))

                // Insert the content values into the domains database.
                domainsDatabaseHelper.addDomain(domainContentValues)

                // Advance to the next record.
                importDomainsCursor.moveToNext()
            }

            // Close the domains cursor and database.
            importDomainsCursor.close()
            domainsDatabaseHelper.close()


            // Get a cursor for the preferences table.
            val importPreferencesCursor = importDatabase.rawQuery("SELECT * FROM $PREFERENCES_TABLE", null)

            // Move to the first record.
            importPreferencesCursor.moveToFirst()

            // Import the preference data.
            sharedPreferences.edit()
                .putBoolean(JAVASCRIPT, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(JAVASCRIPT)) == 1)
                .putBoolean(COOKIES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(COOKIES)) == 1)
                .putBoolean(DOM_STORAGE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DOM_STORAGE)) == 1)
                .putString(PREFERENCES_USER_AGENT, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(PREFERENCES_USER_AGENT)))
                .putString(CUSTOM_USER_AGENT, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(CUSTOM_USER_AGENT)))
                .putBoolean(INCOGNITO_MODE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(INCOGNITO_MODE)) == 1)
                .putBoolean(ALLOW_SCREENSHOTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(ALLOW_SCREENSHOTS)) == 1)
                .putBoolean(EASYLIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(EASYLIST)) == 1)
                .putBoolean(EASYPRIVACY, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(EASYPRIVACY)) == 1)
                .putBoolean(FANBOYS_ANNOYANCE_LIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FANBOYS_ANNOYANCE_LIST)) == 1)
                .putBoolean(FANBOYS_SOCIAL_BLOCKING_LIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FANBOYS_SOCIAL_BLOCKING_LIST)) == 1)
                .putBoolean(ULTRALIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(ULTRALIST)) == 1)
                .putBoolean(ULTRAPRIVACY, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(ULTRAPRIVACY)) == 1)
                .putBoolean(PREFERENCES_BLOCK_ALL_THIRD_PARTY_REQUESTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(PREFERENCES_BLOCK_ALL_THIRD_PARTY_REQUESTS)) == 1)
                .putBoolean(TRACKING_QUERIES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(TRACKING_QUERIES)) == 1)
                .putBoolean(AMP_REDIRECTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(AMP_REDIRECTS)) == 1)
                .putString(SEARCH, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(SEARCH)))
                .putString(SEARCH_CUSTOM_URL, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(SEARCH_CUSTOM_URL)))
                .putString(PROXY, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(PROXY)))
                .putString(PROXY_CUSTOM_URL, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(PROXY_CUSTOM_URL)))
                .putBoolean(FULL_SCREEN_BROWSING_MODE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FULL_SCREEN_BROWSING_MODE)) == 1)
                .putBoolean(HIDE_APP_BAR, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(HIDE_APP_BAR)) == 1)
                .putBoolean(DISPLAY_UNDER_CUTOUTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DISPLAY_UNDER_CUTOUTS)) == 1)
                .putBoolean(CLEAR_EVERYTHING, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_EVERYTHING)) == 1)
                .putBoolean(CLEAR_COOKIES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_COOKIES)) == 1)
                .putBoolean(CLEAR_DOM_STORAGE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_DOM_STORAGE)) == 1)
                .putBoolean(CLEAR_LOGCAT, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_LOGCAT)) == 1)
                .putBoolean(CLEAR_CACHE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_CACHE)) == 1)
                .putString(HOMEPAGE, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(HOMEPAGE)))
                .putString(PREFERENCES_FONT_SIZE, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(PREFERENCES_FONT_SIZE)))
                .putBoolean(OPEN_INTENTS_IN_NEW_TAB, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(OPEN_INTENTS_IN_NEW_TAB)) == 1)
                .putBoolean(PREFERENCES_SWIPE_TO_REFRESH, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(PREFERENCES_SWIPE_TO_REFRESH)) == 1)
                .putString(DOWNLOAD_PROVIDER, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(DOWNLOAD_PROVIDER)))
                .putBoolean(SCROLL_APP_BAR, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(SCROLL_APP_BAR)) == 1)
                .putBoolean(BOTTOM_APP_BAR, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(BOTTOM_APP_BAR)) == 1)
                .putBoolean(DISPLAY_ADDITIONAL_APP_BAR_ICONS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DISPLAY_ADDITIONAL_APP_BAR_ICONS)) == 1)
                .putBoolean(SORT_BOOKMARKS_ALPHABETICALLY, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(SORT_BOOKMARKS_ALPHABETICALLY)) == 1)
                .putString(APP_THEME, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(APP_THEME)))
                .putString(WEBVIEW_THEME, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(WEBVIEW_THEME)))
                .putBoolean(WIDE_VIEWPORT, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(WIDE_VIEWPORT)) == 1)
                .putBoolean(DISPLAY_WEBPAGE_IMAGES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DISPLAY_WEBPAGE_IMAGES)) == 1)
                .apply()

            // Close the preferences cursor and database.
            importPreferencesCursor.close()
            importDatabase.close()

            // Delete the temporary import file database, journal, and other related auxiliary files.
            SQLiteDatabase.deleteDatabase(temporaryImportFile)

            // Return the import successful string.
            IMPORT_SUCCESSFUL
        } catch (exception: Exception) {
            // Return the import error.
            exception.toString()
        }
    }

    fun exportUnencrypted(exportFileOutputStream: OutputStream, context: Context): String {
        return try {
            // Create a temporary export file.
            val temporaryExportFile = File.createTempFile("temporary_export_file", null, context.cacheDir)

            // Create the temporary export database.
            val temporaryExportDatabase = SQLiteDatabase.openOrCreateDatabase(temporaryExportFile, null)

            // Set the temporary export database version number.
            temporaryExportDatabase.version = IMPORT_EXPORT_SCHEMA_VERSION


            // Create the temporary export database bookmarks table.
            temporaryExportDatabase.execSQL(CREATE_BOOKMARKS_TABLE)

            // Open the bookmarks database.
            val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context)

            // Get a full bookmarks cursor.
            val bookmarksCursor = bookmarksDatabaseHelper.allBookmarks

            // Move to the first record.
            bookmarksCursor.moveToFirst()

            // Get the bookmarks colum indexes.
            val bookmarkNameColumnIndex = bookmarksCursor.getColumnIndexOrThrow(BOOKMARK_NAME)
            val bookmarkUrlColumnIndex = bookmarksCursor.getColumnIndexOrThrow(BOOKMARK_URL)
            val bookmarkParentFolderIdColumnIndex = bookmarksCursor.getColumnIndexOrThrow(PARENT_FOLDER_ID)
            val bookmarkDisplayOrderColumnIndex = bookmarksCursor.getColumnIndexOrThrow(DISPLAY_ORDER)
            val bookmarkIsFolderColumnIndex = bookmarksCursor.getColumnIndexOrThrow(IS_FOLDER)
            val bookmarkFolderIdColumnIndex = bookmarksCursor.getColumnIndexOrThrow(FOLDER_ID)
            val bookmarkFavoriteIconColumnIndex = bookmarksCursor.getColumnIndexOrThrow(FAVORITE_ICON)

            // Copy the data from the bookmarks cursor into the export database.
            for (i in 0 until bookmarksCursor.count) {
                // Create a bookmark content values.
                val bookmarkContentValues = ContentValues()

                // Populate the bookmark content values.
                bookmarkContentValues.put(BOOKMARK_NAME, bookmarksCursor.getString(bookmarkNameColumnIndex))
                bookmarkContentValues.put(BOOKMARK_URL, bookmarksCursor.getString(bookmarkUrlColumnIndex))
                bookmarkContentValues.put(PARENT_FOLDER_ID, bookmarksCursor.getLong(bookmarkParentFolderIdColumnIndex))
                bookmarkContentValues.put(DISPLAY_ORDER, bookmarksCursor.getInt(bookmarkDisplayOrderColumnIndex))
                bookmarkContentValues.put(IS_FOLDER, bookmarksCursor.getInt(bookmarkIsFolderColumnIndex))
                bookmarkContentValues.put(FOLDER_ID, bookmarksCursor.getLong(bookmarkFolderIdColumnIndex))
                bookmarkContentValues.put(FAVORITE_ICON, bookmarksCursor.getBlob(bookmarkFavoriteIconColumnIndex))

                // Insert the content values into the temporary export database.
                temporaryExportDatabase.insert(BOOKMARKS_TABLE, null, bookmarkContentValues)

                // Advance to the next record.
                bookmarksCursor.moveToNext()
            }

            // Close the bookmarks cursor and database.
            bookmarksCursor.close()
            bookmarksDatabaseHelper.close()


            // Create the temporary export database domains table.
            temporaryExportDatabase.execSQL(CREATE_DOMAINS_TABLE)

            // Open the domains database.
            val domainsDatabaseHelper = DomainsDatabaseHelper(context)

            // Get a full domains database cursor.
            val domainsCursor = domainsDatabaseHelper.completeCursorOrderedByDomain

            // Move to the first record.
            domainsCursor.moveToFirst()

            // Get the domain column indexes.
            val domainNameColumnIndex = domainsCursor.getColumnIndexOrThrow(DOMAIN_NAME)
            val domainJavaScriptColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_JAVASCRIPT)
            val domainCookiesColumnIndex = domainsCursor.getColumnIndexOrThrow(COOKIES)
            val domainDomStorageColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_DOM_STORAGE)
            val domainEasyListColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_EASYLIST)
            val domainEasyPrivacyColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY)
            val domainFanboysAnnoyanceListColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_ANNOYANCE_LIST)
            val domainFanboysSocialBlockingListColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)
            val domainUltraListColumnIndex = domainsCursor.getColumnIndexOrThrow(ULTRALIST)
            val domainUltraPrivacyColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY)
            val domainBlockAllThirdPartyRequestsColumnIndex = domainsCursor.getColumnIndexOrThrow(BLOCK_ALL_THIRD_PARTY_REQUESTS)
            val domainUserAgentColumnIndex = domainsCursor.getColumnIndexOrThrow(USER_AGENT)
            val domainFontSizeColumnIndex = domainsCursor.getColumnIndexOrThrow(FONT_SIZE)
            val domainSwipeToRefreshColumnIndex = domainsCursor.getColumnIndexOrThrow(SWIPE_TO_REFRESH)
            val domainWebViewThemeColumnIndex = domainsCursor.getColumnIndexOrThrow(WEBVIEW_THEME)
            val domainWideViewportColumnIndex = domainsCursor.getColumnIndexOrThrow(WIDE_VIEWPORT)
            val domainDisplayImagesColumnIndex = domainsCursor.getColumnIndexOrThrow(DISPLAY_IMAGES)
            val domainPinnedSslCertificateColumnIndex = domainsCursor.getColumnIndexOrThrow(PINNED_SSL_CERTIFICATE)
            val domainSslIssuedToCommonNameColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_COMMON_NAME)
            val domainSslIssuedToOrganizationColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_ORGANIZATION)
            val domainSslIssuedToOrganizationalUnitColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT)
            val domainSslIssuedByCommonNameColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_COMMON_NAME)
            val domainSslIssuedByOrganizationColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_ORGANIZATION)
            val domainSslIssuedByOrganizationalUnitColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT)
            val domainSslStartDateColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_START_DATE)
            val domainSslEndDateColumnIndex = domainsCursor.getColumnIndexOrThrow(SSL_END_DATE)
            val domainPinnedIpAddressesColumnIndex = domainsCursor.getColumnIndexOrThrow(PINNED_IP_ADDRESSES)
            val domainIpAddressesColumnIndex = domainsCursor.getColumnIndexOrThrow(IP_ADDRESSES)

            // Copy the data from the domains cursor into the export database.
            for (i in 0 until domainsCursor.count) {
                // Create a domain content values.
                val domainContentValues = ContentValues()

                // Populate the domain content values.
                domainContentValues.put(DOMAIN_NAME, domainsCursor.getString(domainNameColumnIndex))
                domainContentValues.put(ENABLE_JAVASCRIPT, domainsCursor.getInt(domainJavaScriptColumnIndex))
                domainContentValues.put(COOKIES, domainsCursor.getInt(domainCookiesColumnIndex))
                domainContentValues.put(ENABLE_DOM_STORAGE, domainsCursor.getInt(domainDomStorageColumnIndex))
                domainContentValues.put(ENABLE_EASYLIST, domainsCursor.getInt(domainEasyListColumnIndex))
                domainContentValues.put(ENABLE_EASYPRIVACY, domainsCursor.getInt(domainEasyPrivacyColumnIndex))
                domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, domainsCursor.getInt(domainFanboysAnnoyanceListColumnIndex))
                domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, domainsCursor.getInt(domainFanboysSocialBlockingListColumnIndex))
                domainContentValues.put(ULTRALIST, domainsCursor.getInt(domainUltraListColumnIndex))
                domainContentValues.put(ENABLE_ULTRAPRIVACY, domainsCursor.getInt(domainUltraPrivacyColumnIndex))
                domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, domainsCursor.getInt(domainBlockAllThirdPartyRequestsColumnIndex))
                domainContentValues.put(USER_AGENT, domainsCursor.getString(domainUserAgentColumnIndex))
                domainContentValues.put(FONT_SIZE, domainsCursor.getInt(domainFontSizeColumnIndex))
                domainContentValues.put(SWIPE_TO_REFRESH, domainsCursor.getInt(domainSwipeToRefreshColumnIndex))
                domainContentValues.put(WEBVIEW_THEME, domainsCursor.getInt(domainWebViewThemeColumnIndex))
                domainContentValues.put(WIDE_VIEWPORT, domainsCursor.getInt(domainWideViewportColumnIndex))
                domainContentValues.put(DISPLAY_IMAGES, domainsCursor.getInt(domainDisplayImagesColumnIndex))
                domainContentValues.put(PINNED_SSL_CERTIFICATE, domainsCursor.getInt(domainPinnedSslCertificateColumnIndex))
                domainContentValues.put(SSL_ISSUED_TO_COMMON_NAME, domainsCursor.getString(domainSslIssuedToCommonNameColumnIndex))
                domainContentValues.put(SSL_ISSUED_TO_ORGANIZATION, domainsCursor.getString(domainSslIssuedToOrganizationColumnIndex))
                domainContentValues.put(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT, domainsCursor.getString(domainSslIssuedToOrganizationalUnitColumnIndex))
                domainContentValues.put(SSL_ISSUED_BY_COMMON_NAME, domainsCursor.getString(domainSslIssuedByCommonNameColumnIndex))
                domainContentValues.put(SSL_ISSUED_BY_ORGANIZATION, domainsCursor.getString(domainSslIssuedByOrganizationColumnIndex))
                domainContentValues.put(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT, domainsCursor.getString(domainSslIssuedByOrganizationalUnitColumnIndex))
                domainContentValues.put(SSL_START_DATE, domainsCursor.getLong(domainSslStartDateColumnIndex))
                domainContentValues.put(SSL_END_DATE, domainsCursor.getLong(domainSslEndDateColumnIndex))
                domainContentValues.put(PINNED_IP_ADDRESSES, domainsCursor.getInt(domainPinnedIpAddressesColumnIndex))
                domainContentValues.put(IP_ADDRESSES, domainsCursor.getString(domainIpAddressesColumnIndex))

                // Insert the content values into the temporary export database.
                temporaryExportDatabase.insert(DOMAINS_TABLE, null, domainContentValues)

                // Advance to the next record.
                domainsCursor.moveToNext()
            }

            // Close the domains cursor and database.
            domainsCursor.close()
            domainsDatabaseHelper.close()


            // Prepare the preferences table SQL creation string.
            val createPreferencesTable = "CREATE TABLE $PREFERENCES_TABLE (" +
                    "$ID INTEGER PRIMARY KEY, " +
                    "$JAVASCRIPT BOOLEAN, " +
                    "$COOKIES BOOLEAN, " +
                    "$DOM_STORAGE BOOLEAN, " +
                    "$PREFERENCES_USER_AGENT TEXT, " +
                    "$CUSTOM_USER_AGENT TEXT, " +
                    "$INCOGNITO_MODE BOOLEAN, " +
                    "$ALLOW_SCREENSHOTS BOOLEAN, " +
                    "$EASYLIST BOOLEAN, " +
                    "$EASYPRIVACY BOOLEAN, " +
                    "$FANBOYS_ANNOYANCE_LIST BOOLEAN, " +
                    "$FANBOYS_SOCIAL_BLOCKING_LIST BOOLEAN, " +
                    "$ULTRALIST BOOLEAN, " +
                    "$ULTRAPRIVACY BOOLEAN, " +
                    "$PREFERENCES_BLOCK_ALL_THIRD_PARTY_REQUESTS BOOLEAN, " +
                    "$TRACKING_QUERIES BOOLEAN, " +
                    "$AMP_REDIRECTS BOOLEAN, " +
                    "$SEARCH TEXT, " +
                    "$SEARCH_CUSTOM_URL TEXT, " +
                    "$PROXY TEXT, " +
                    "$PROXY_CUSTOM_URL TEXT, " +
                    "$FULL_SCREEN_BROWSING_MODE BOOLEAN, " +
                    "$HIDE_APP_BAR BOOLEAN, " +
                    "$DISPLAY_UNDER_CUTOUTS BOOLEAN, " +
                    "$CLEAR_EVERYTHING BOOLEAN, " +
                    "$CLEAR_COOKIES BOOLEAN, " +
                    "$CLEAR_DOM_STORAGE BOOLEAN, " +
                    "$CLEAR_LOGCAT BOOLEAN, " +
                    "$CLEAR_CACHE BOOLEAN, " +
                    "$HOMEPAGE TEXT, " +
                    "$PREFERENCES_FONT_SIZE TEXT, " +
                    "$OPEN_INTENTS_IN_NEW_TAB BOOLEAN, " +
                    "$PREFERENCES_SWIPE_TO_REFRESH BOOLEAN, " +
                    "$DOWNLOAD_PROVIDER TEXT, " +
                    "$SCROLL_APP_BAR BOOLEAN, " +
                    "$BOTTOM_APP_BAR BOOLEAN, " +
                    "$DISPLAY_ADDITIONAL_APP_BAR_ICONS BOOLEAN, " +
                    "$SORT_BOOKMARKS_ALPHABETICALLY BOOLEAN, " +
                    "$APP_THEME TEXT, " +
                    "$WEBVIEW_THEME TEXT, " +
                    "$WIDE_VIEWPORT BOOLEAN, " +
                    "$DISPLAY_WEBPAGE_IMAGES BOOLEAN)"

            // Create the temporary export database preferences table.
            temporaryExportDatabase.execSQL(createPreferencesTable)

            // Get a handle for the shared preference.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Create a preferences content values.
            val preferencesContentValues = ContentValues()

            // Populate the preferences content values.
            preferencesContentValues.put(JAVASCRIPT, sharedPreferences.getBoolean(JAVASCRIPT, false))
            preferencesContentValues.put(COOKIES, sharedPreferences.getBoolean(COOKIES, false))
            preferencesContentValues.put(DOM_STORAGE, sharedPreferences.getBoolean(DOM_STORAGE, false))
            preferencesContentValues.put(PREFERENCES_USER_AGENT, sharedPreferences.getString(PREFERENCES_USER_AGENT, context.getString(R.string.user_agent_default_value)))
            preferencesContentValues.put(CUSTOM_USER_AGENT, sharedPreferences.getString(CUSTOM_USER_AGENT, context.getString(R.string.custom_user_agent_default_value)))
            preferencesContentValues.put(INCOGNITO_MODE, sharedPreferences.getBoolean(INCOGNITO_MODE, false))
            preferencesContentValues.put(ALLOW_SCREENSHOTS, sharedPreferences.getBoolean(ALLOW_SCREENSHOTS, false))
            preferencesContentValues.put(EASYLIST, sharedPreferences.getBoolean(EASYLIST, true))
            preferencesContentValues.put(EASYPRIVACY, sharedPreferences.getBoolean(EASYPRIVACY, true))
            preferencesContentValues.put(FANBOYS_ANNOYANCE_LIST, sharedPreferences.getBoolean(FANBOYS_ANNOYANCE_LIST, true))
            preferencesContentValues.put(FANBOYS_SOCIAL_BLOCKING_LIST, sharedPreferences.getBoolean(FANBOYS_SOCIAL_BLOCKING_LIST, true))
            preferencesContentValues.put(ULTRALIST, sharedPreferences.getBoolean(ULTRALIST, true))
            preferencesContentValues.put(ULTRAPRIVACY, sharedPreferences.getBoolean(ULTRAPRIVACY, true))
            preferencesContentValues.put(PREFERENCES_BLOCK_ALL_THIRD_PARTY_REQUESTS, sharedPreferences.getBoolean(PREFERENCES_BLOCK_ALL_THIRD_PARTY_REQUESTS, false))
            preferencesContentValues.put(TRACKING_QUERIES, sharedPreferences.getBoolean(TRACKING_QUERIES, true))
            preferencesContentValues.put(AMP_REDIRECTS, sharedPreferences.getBoolean(AMP_REDIRECTS, true))
            preferencesContentValues.put(SEARCH, sharedPreferences.getString(SEARCH, context.getString(R.string.search_default_value)))
            preferencesContentValues.put(SEARCH_CUSTOM_URL, sharedPreferences.getString(SEARCH_CUSTOM_URL, context.getString(R.string.search_custom_url_default_value)))
            preferencesContentValues.put(PROXY, sharedPreferences.getString(PROXY, context.getString(R.string.proxy_default_value)))
            preferencesContentValues.put(PROXY_CUSTOM_URL, sharedPreferences.getString(PROXY_CUSTOM_URL, context.getString(R.string.proxy_custom_url_default_value)))
            preferencesContentValues.put(FULL_SCREEN_BROWSING_MODE, sharedPreferences.getBoolean(FULL_SCREEN_BROWSING_MODE, false))
            preferencesContentValues.put(HIDE_APP_BAR, sharedPreferences.getBoolean(HIDE_APP_BAR, true))
            preferencesContentValues.put(DISPLAY_UNDER_CUTOUTS, sharedPreferences.getBoolean(DISPLAY_UNDER_CUTOUTS, false))
            preferencesContentValues.put(CLEAR_EVERYTHING, sharedPreferences.getBoolean(CLEAR_EVERYTHING, true))
            preferencesContentValues.put(CLEAR_COOKIES, sharedPreferences.getBoolean(CLEAR_COOKIES, true))
            preferencesContentValues.put(CLEAR_DOM_STORAGE, sharedPreferences.getBoolean(CLEAR_DOM_STORAGE, true))
            preferencesContentValues.put(CLEAR_LOGCAT, sharedPreferences.getBoolean(CLEAR_LOGCAT, true))
            preferencesContentValues.put(CLEAR_CACHE, sharedPreferences.getBoolean(CLEAR_CACHE, true))
            preferencesContentValues.put(HOMEPAGE, sharedPreferences.getString(HOMEPAGE, context.getString(R.string.homepage_default_value)))
            preferencesContentValues.put(PREFERENCES_FONT_SIZE, sharedPreferences.getString(PREFERENCES_FONT_SIZE, context.getString(R.string.font_size_default_value)))
            preferencesContentValues.put(OPEN_INTENTS_IN_NEW_TAB, sharedPreferences.getBoolean(OPEN_INTENTS_IN_NEW_TAB, true))
            preferencesContentValues.put(PREFERENCES_SWIPE_TO_REFRESH, sharedPreferences.getBoolean(PREFERENCES_SWIPE_TO_REFRESH, true))
            preferencesContentValues.put(DOWNLOAD_PROVIDER, sharedPreferences.getString(DOWNLOAD_PROVIDER, context.getString(R.string.download_provider_default_value)))
            preferencesContentValues.put(SCROLL_APP_BAR, sharedPreferences.getBoolean(SCROLL_APP_BAR, true))
            preferencesContentValues.put(BOTTOM_APP_BAR, sharedPreferences.getBoolean(BOTTOM_APP_BAR, false))
            preferencesContentValues.put(DISPLAY_ADDITIONAL_APP_BAR_ICONS, sharedPreferences.getBoolean(DISPLAY_ADDITIONAL_APP_BAR_ICONS, false))
            preferencesContentValues.put(SORT_BOOKMARKS_ALPHABETICALLY, sharedPreferences.getBoolean(SORT_BOOKMARKS_ALPHABETICALLY, false))
            preferencesContentValues.put(APP_THEME, sharedPreferences.getString(APP_THEME, context.getString(R.string.app_theme_default_value)))
            preferencesContentValues.put(WEBVIEW_THEME, sharedPreferences.getString(WEBVIEW_THEME, context.getString(R.string.webview_theme_default_value)))
            preferencesContentValues.put(WIDE_VIEWPORT, sharedPreferences.getBoolean(WIDE_VIEWPORT, true))
            preferencesContentValues.put(DISPLAY_WEBPAGE_IMAGES, sharedPreferences.getBoolean(DISPLAY_WEBPAGE_IMAGES, true))

            // Insert the preferences content values into the temporary export database.
            temporaryExportDatabase.insert(PREFERENCES_TABLE, null, preferencesContentValues)

            // Close the temporary export database.
            temporaryExportDatabase.close()


            // The file may be copied directly in Kotlin using `File.copyTo`.  <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/java.io.-file/copy-to.html>
            // It can be copied in Android using `Files.copy` once the minimum API >= 26.
            // <https://developer.android.com/reference/java/nio/file/Files#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...)>
            // However, the file cannot be acquired from the content URI until the minimum API >= 29.  <https://developer.android.com/reference/kotlin/android/content/ContentResolver#openfile>

            // Create the temporary export file input stream.
            val temporaryExportFileInputStream = FileInputStream(temporaryExportFile)

            // Create a byte array.
            val transferByteArray = ByteArray(1024)

            // Create an integer to track the number of bytes read.
            var bytesRead: Int

            // Copy the temporary export file to the export file output stream.
            while (temporaryExportFileInputStream.read(transferByteArray).also { bytesRead = it } > 0) {
                exportFileOutputStream.write(transferByteArray, 0, bytesRead)
            }

            // Flush the export file output stream.
            exportFileOutputStream.flush()

            // Close the file streams.
            temporaryExportFileInputStream.close()
            exportFileOutputStream.close()

            // Delete the temporary export file database, journal, and other related auxiliary files.
            SQLiteDatabase.deleteDatabase(temporaryExportFile)

            // Return the export successful string.
            EXPORT_SUCCESSFUL
        } catch (exception: Exception) {
            // Return the export error.
            exception.toString()
        }
    }

    // This method is used to convert the old domain settings switches to spinners.
    private fun convertFromSwitchToSpinner(systemDefault: Boolean, currentDatabaseInteger: Int): Int {
        // Return the new spinner integer.
        return if ((!systemDefault && (currentDatabaseInteger == 0)) ||
            (systemDefault && (currentDatabaseInteger == 1)))  // The system default is currently selected.
            SYSTEM_DEFAULT
        else if (currentDatabaseInteger == 0)  // The switch is currently disabled and that is not the system default.
            DISABLED
        else  // The switch is currently enabled and that is not the system default.
            ENABLED
    }

    private fun generateFolderId(database: SQLiteDatabase): Long {
        // Get the current time in epoch format.
        val possibleFolderId = Date().time

        // Get a cursor with any folders that already have this folder ID.
        val existingFolderCursor = database.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $FOLDER_ID = $possibleFolderId", null)

        // Check if the folder ID is unique.
        val folderIdIsUnique = (existingFolderCursor.count == 0)

        // Close the cursor.
        existingFolderCursor.close()

        // Either return the folder ID or test a new one.
        return if (folderIdIsUnique)
            possibleFolderId
        else
            generateFolderId(database)
    }
}
