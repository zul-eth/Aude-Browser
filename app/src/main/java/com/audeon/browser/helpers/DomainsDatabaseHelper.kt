/*
 * Copyright 2017-2024 Soren Stoutner <soren@stoutner.com>.
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
 * along with Privacy Browser Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.audeon.browser.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import androidx.preference.PreferenceManager

import com.audeon.browser.R

// Define the class constants.
private const val SCHEMA_VERSION = 17

// Define the public database constants.
const val DOMAINS_DATABASE = "domains.db"
const val DOMAINS_TABLE = "domains"

// Define the public spinner constants.
const val SYSTEM_DEFAULT = 0
const val ENABLED = 1
const val DISABLED = 2
const val LIGHT_THEME = 1
const val DARK_THEME = 2

// Define the public schema constants.
const val BLOCK_ALL_THIRD_PARTY_REQUESTS = "blockallthirdpartyrequests"
const val COOKIES = "cookies"
const val DISPLAY_IMAGES = "displayimages"
const val DOMAIN_NAME = "domainname"
const val ENABLE_DOM_STORAGE = "enabledomstorage"
const val ENABLE_EASYLIST = "enableeasylist"
const val ENABLE_EASYPRIVACY = "enableeasyprivacy"
const val ENABLE_FANBOYS_ANNOYANCE_LIST = "enablefanboysannoyancelist"
const val ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST = "enablefanboyssocialblockinglist"
const val ENABLE_JAVASCRIPT = "enablejavascript"
const val ENABLE_ULTRAPRIVACY = "enableultraprivacy"
const val FONT_SIZE = "fontsize"
const val ID = "_id"
const val IP_ADDRESSES = "ip_addresses"
const val PINNED_IP_ADDRESSES = "pinned_ip_addresses"
const val PINNED_SSL_CERTIFICATE = "pinnedsslcertificate"
const val SSL_END_DATE = "sslenddate"
const val SSL_START_DATE = "sslstartdate"
const val SSL_ISSUED_BY_COMMON_NAME = "sslissuedbycommonname"
const val SSL_ISSUED_BY_ORGANIZATION = "sslissuedbyorganization"
const val SSL_ISSUED_BY_ORGANIZATIONAL_UNIT = "sslissuedbyorganizationalunit"
const val SSL_ISSUED_TO_COMMON_NAME = "sslissuedtocommonname"
const val SSL_ISSUED_TO_ORGANIZATION = "sslissuedtoorganization"
const val SSL_ISSUED_TO_ORGANIZATIONAL_UNIT = "sslissuedtoorganizationalunit"
const val SWIPE_TO_REFRESH = "swipetorefresh"
const val ULTRALIST = "ultralist"
const val USER_AGENT = "useragent"
const val WEBVIEW_THEME = "webview_theme"
const val WIDE_VIEWPORT = "wide_viewport"

// Define the public table creation constant.
const val CREATE_DOMAINS_TABLE = "CREATE TABLE $DOMAINS_TABLE (" +
        "$ID INTEGER PRIMARY KEY, " +
        "$DOMAIN_NAME TEXT, " +
        "$ENABLE_JAVASCRIPT INTEGER, " +
        "$COOKIES INTEGER, " +
        "$ENABLE_DOM_STORAGE INTEGER, " +
        "$USER_AGENT TEXT, " +
        "$ENABLE_EASYLIST INTEGER, " +
        "$ENABLE_EASYPRIVACY INTEGER, " +
        "$ENABLE_FANBOYS_ANNOYANCE_LIST INTEGER, " +
        "$ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST INTEGER, " +
        "$ULTRALIST INTEGER, " +
        "$ENABLE_ULTRAPRIVACY INTEGER, " +
        "$BLOCK_ALL_THIRD_PARTY_REQUESTS INTEGER, " +
        "$FONT_SIZE INTEGER, " +
        "$SWIPE_TO_REFRESH INTEGER, " +
        "$WEBVIEW_THEME INTEGER, " +
        "$WIDE_VIEWPORT INTEGER, " +
        "$DISPLAY_IMAGES INTEGER, " +
        "$PINNED_SSL_CERTIFICATE BOOLEAN, " +
        "$SSL_ISSUED_TO_COMMON_NAME TEXT, " +
        "$SSL_ISSUED_TO_ORGANIZATION TEXT, " +
        "$SSL_ISSUED_TO_ORGANIZATIONAL_UNIT TEXT, " +
        "$SSL_ISSUED_BY_COMMON_NAME TEXT, " +
        "$SSL_ISSUED_BY_ORGANIZATION TEXT, " +
        "$SSL_ISSUED_BY_ORGANIZATIONAL_UNIT TEXT, " +
        "$SSL_START_DATE INTEGER, " +
        "$SSL_END_DATE INTEGER, " +
        "$PINNED_IP_ADDRESSES BOOLEAN, " +
        "$IP_ADDRESSES TEXT)"

class DomainsDatabaseHelper(private val appContext: Context) : SQLiteOpenHelper(appContext, DOMAINS_DATABASE, null, SCHEMA_VERSION) {
    override fun onCreate(domainsDatabase: SQLiteDatabase) {
        // Create the domains table.
        domainsDatabase.execSQL(CREATE_DOMAINS_TABLE)
    }

    override fun onUpgrade(domainsDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Get a handle for the shared preference.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)

        // Upgrade from schema version 1, first used in Privacy Browser 2.0, to schema version 2, first used in Privacy Browser 2.3.
        if (oldVersion < 2) {
            // Add the display images column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $DISPLAY_IMAGES INTEGER")
        }

        // Upgrade from schema version 2, first used in Privacy Browser 2.3, to schema version 3, first used in Privacy Browser 2.5.
        if (oldVersion < 3) {
            //  Add the SSL certificate columns.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $PINNED_SSL_CERTIFICATE BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_TO_COMMON_NAME TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_TO_ORGANIZATION TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_TO_ORGANIZATIONAL_UNIT TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_BY_COMMON_NAME TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_BY_ORGANIZATION TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_BY_ORGANIZATIONAL_UNIT TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_START_DATE INTEGER")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_END_DATE INTEGER")
        }

        // Upgrade from schema version 3, first used in Privacy Browser 2.5, to schema version 4, first used in Privacy Browser 2.6.
        if (oldVersion < 4) {
            // Add the night mode column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN nightmode INTEGER")
        }

        // Upgrade from schema version 4, first used in Privacy Browser 2.6, to schema version 5, first used in Privacy Browser 2.9.
        if (oldVersion < 5) {
            // Add the block lists columns.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_EASYLIST BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_EASYPRIVACY BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_FANBOYS_ANNOYANCE_LIST BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST BOOLEAN")

            // Get the default filter list settings.
            val easyListEnabled = sharedPreferences.getBoolean(appContext.getString(R.string.easylist_key), true)
            val easyPrivacyEnabled = sharedPreferences.getBoolean(appContext.getString(R.string.easyprivacy_key), true)
            val fanboyAnnoyanceListEnabled = sharedPreferences.getBoolean(appContext.getString(R.string.fanboys_annoyance_list_key), true)
            val fanboySocialBlockingListEnabled = sharedPreferences.getBoolean(appContext.getString(R.string.fanboys_social_blocking_list_key), true)

            // Set EasyList for existing rows according to the current system-wide default.
            // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
            // <https://developer.android.com/reference/android/database/sqlite/package-summary>
            if (easyListEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYLIST = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYLIST = 0")
            }

            // Set EasyPrivacy for existing rows according to the current system-wide default.
            if (easyPrivacyEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYPRIVACY = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYPRIVACY = 0")
            }

            // Set Fanboy's Annoyance List for existing rows according to the current system-wide default.
            if (fanboyAnnoyanceListEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_ANNOYANCE_LIST = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_ANNOYANCE_LIST = 0")
            }

            // Set Fanboy's Social Blocking List for existing rows according to the current system-wide default.
            if (fanboySocialBlockingListEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST = 0")
            }
        }

        // Upgrade from schema version 5, first used in Privacy Browser 2.9, to schema version 6, first used in Privacy Browser 2.11.
        if (oldVersion < 6) {
            // Add the swipe to refresh column.  This defaults to `0`, which is `System default`, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SWIPE_TO_REFRESH INTEGER")
        }

        // Upgrade from schema version 6, first used in Privacy Browser 2.11, to schema version 7, first used in Privacy Browser 2.12.
        if (oldVersion < 7) {
            // Add the block all third-party requests column.  This defaults to `0`, which is off, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $BLOCK_ALL_THIRD_PARTY_REQUESTS BOOLEAN")
        }

        // Upgrade from schema version 7, first used in Privacy Browser 2.12, to schema version 8, first used in Privacy Browser 2.12.
        // For some reason (lack of planning or attention to detail), the 2.12 update included two schema version jumps.
        if (oldVersion < 8) {
            // Add the UltraPrivacy column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_ULTRAPRIVACY BOOLEAN")

            // Enable it for all existing rows.
            domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_ULTRAPRIVACY = 1")
        }

        // Upgrade from schema version 8, first used in Privacy Browser 2.12, to schema version 9, first used in Privacy Browser 2.16.
        if (oldVersion < 9) {
            // Add the pinned IP addresses columns.  These default to `0` and `""`, so a separate step isn't needed to populate the columns.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $PINNED_IP_ADDRESSES BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $IP_ADDRESSES TEXT")
        }

        // Upgrade from schema version 9, first used in Privacy Browser 2.16, to schema version 10, first used in Privacy Browser 3.1.
        if (oldVersion < 10) {
            // Add the wide viewport column.  This defaults to `0`, which is `System default`, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $WIDE_VIEWPORT INTEGER")
        }

        // Upgrade from schema version 10, first used in Privacy Browser 3.1, to schema version 11, first used in Privacy Browser 3.2.
        if (oldVersion < 11) {
            // Add the UltraList column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ULTRALIST BOOLEAN")

            // Enable it for all existing rows.
            domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ULTRALIST = 1")
        }

        // Upgrade from schema version 11, first used in Privacy Browser 3.2, to schema version 12, first used in Privacy Browser 3.5.
        if (oldVersion < 12) {
            // Add the WebView theme column.  This defaults to `0`, which is `System default`, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $WEBVIEW_THEME INTEGER")

            // `night_mode` was removed.
            // SQLite amazingly only added a command to drop a column in version 3.35.0.  <https://www.sqlite.org/changes.html>
            // It will be a while before that is supported in Android.  <https://developer.android.com/reference/android/database/sqlite/package-summary>
            // Although a new table could be created and all the data copied to it, I think I will just leave the old night mode column.  It will be wiped out the next time an import is run.
        }

        // Upgrade from schema version 12, first used in Privacy Browser 3.5, to schema version 13, first used in Privacy Browser 3.8.
        if (oldVersion < 13) {
            // Add the cookies column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $COOKIES BOOLEAN")

            // Copy the data from the old column to the new one.
            domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $COOKIES = enablefirstpartycookies")
        }

        // Upgrade from schema version 13, first used in Privacy Browser 3.8, to schema version 14, first used in Privacy Browser 3.11.
        // This upgrade used to add the X-Requested-With header, but that was removed in schema version 15.

        // Upgrade from schema version 14, first used in Privacy Browser 3.11, to schema version 15, first used in Privacy Browser 3.12.
        // This upgrade removed `x_requested_with_header`.
        // SQLite amazingly only added a command to drop a column in version 3.35.0.  <https://www.sqlite.org/changes.html>
        // It will be a while before that is supported in Android.  <https://developer.android.com/reference/android/database/sqlite/package-summary>
        // Although a new table could be created and all the data copied to it, I think I will just leave the X-Requested-With column.  It will be wiped out the next time an import is run.

        // Upgrade from schema version 15, first used in Privacy Browser 3.12, to schema version 16, first used in Privacy Browser 3.15.
        if (oldVersion < 16) {
            // Get the current switch default values.
            val javaScriptDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.javascript_key), false)
            val cookiesDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.cookies_key), false)
            val domStorageDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.dom_storage_key), false)
            val easyListDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.easylist_key), true)
            val easyPrivacyDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.easyprivacy_key), true)
            val fanboysAnnoyanceListDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.fanboys_annoyance_list_key), true)
            val fanboysSocialBlockingListDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.fanboys_social_blocking_list), true)
            val ultraListDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.ultralist_key), true)
            val ultraPrivacyDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.ultraprivacy_key), true)
            val blockAllThirdPartyRequestsDefaultValue = sharedPreferences.getBoolean(appContext.getString(R.string.block_all_third_party_requests_key), false)

            // Get a domains cursor.
            val domainsCursor = domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE", null)

            // Get the domains column indexes.
            val javaScriptColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_JAVASCRIPT)
            val cookiesColumnIndex = domainsCursor.getColumnIndexOrThrow(COOKIES)
            val domStorageColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_DOM_STORAGE)
            val easyListColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_EASYLIST)
            val easyPrivacyColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY)
            val fanboysAnnoyanceListColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_ANNOYANCE_LIST)
            val fanboysSocialBlockingListColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)
            val ultraListColumnIndex = domainsCursor.getColumnIndexOrThrow(ULTRALIST)
            val ultraPrivacyColumnIndex = domainsCursor.getColumnIndexOrThrow(ENABLE_ULTRAPRIVACY)
            val blockAllThirdPartyRequestsColumnIndex = domainsCursor.getColumnIndexOrThrow(BLOCK_ALL_THIRD_PARTY_REQUESTS)

            // Convert the domain from the switch booleans to the spinner integers.
            for (i in 0 until domainsCursor.count) {
                // Move to the current record.
                domainsCursor.moveToPosition(i)

                // Get the domain current values.
                val javaScriptDomainCurrentValue = domainsCursor.getInt(javaScriptColumnIndex)
                val cookiesDomainCurrentValue = domainsCursor.getInt(cookiesColumnIndex)
                val domStorageDomainCurrentValue = domainsCursor.getInt(domStorageColumnIndex)
                val easyListDomainCurrentValue = domainsCursor.getInt(easyListColumnIndex)
                val easyPrivacyDomainCurrentValue = domainsCursor.getInt(easyPrivacyColumnIndex)
                val fanboysAnnoyanceListCurrentValue = domainsCursor.getInt(fanboysAnnoyanceListColumnIndex)
                val fanboysSocialBlockingListCurrentValue = domainsCursor.getInt(fanboysSocialBlockingListColumnIndex)
                val ultraListCurrentValue = domainsCursor.getInt(ultraListColumnIndex)
                val ultraPrivacyCurrentValue = domainsCursor.getInt(ultraPrivacyColumnIndex)
                val blockAllThirdPartyRequestsCurrentValue = domainsCursor.getInt(blockAllThirdPartyRequestsColumnIndex)

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
                val currentDatabaseId = domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(ID))

                // Update the row for the specified database ID.
                domainsDatabase.update(DOMAINS_TABLE, domainContentValues, "$ID = $currentDatabaseId", null)
            }

            // Upgrade from schema version 16, first used in Privacy Browser 3.15, to schema version 17, first used in Privacy Browser 3.18.
            // This upgrade removed `enableformdata`.
            // SQLite amazingly only added a command to drop a column in version 3.35.0.  <https://www.sqlite.org/changes.html>
            // That will not be supported in Android until the minimum API >= 34.  <https://developer.android.com/reference/android/database/sqlite/package-summary>
            // Although a new table could be created and all the data copied to it, I think I will just leave the `enableformdata` column.  It will be wiped out the next time an import is run.

            // Close the cursor.
            domainsCursor.close()
        }
    }

    val completeCursorOrderedByDomain: Cursor
        get() {
            // Get a readable database handle.
            val domainsDatabase = this.readableDatabase

            // Return everything in the domains table ordered by the domain name.  The cursor can't be closed because it is needed in the calling activity.
            return domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE ORDER BY $DOMAIN_NAME ASC", null)
        }

    val domainNameCursorOrderedByDomain: Cursor
        get() {
            // Get a readable database handle.
            val domainsDatabase = this.readableDatabase

            // Return the database id and the domain name in the domains table ordered by the domain name.  The cursor can't be closed because it is needed in the calling activity.
            return domainsDatabase.rawQuery("SELECT $ID, $DOMAIN_NAME FROM $DOMAINS_TABLE ORDER BY $DOMAIN_NAME ASC", null)
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

    fun getDomainNameCursorOrderedByDomainExcept(databaseId: Int): Cursor {
        // Get a readable database handle.
        val domainsDatabase = this.readableDatabase

        // Return a cursor with the database IDs and domain names except for the specified ID ordered by domain name.  The cursor can't be closed because it is needed in the calling activity.
        return domainsDatabase.rawQuery("SELECT $ID, $DOMAIN_NAME FROM $DOMAINS_TABLE WHERE $ID IS NOT $databaseId ORDER BY $DOMAIN_NAME ASC", null)
    }

    fun getCursorForId(databaseId: Int): Cursor {
        // Get a readable database handle.
        val domainsDatabase = this.readableDatabase

        // Return a cursor for the specified database ID.  The cursor can't be closed because it is needed in the calling activity.
        return domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE WHERE $ID = $databaseId", null)
    }

    fun getCursorForDomainName(domainName: String): Cursor {
        // Get a readable database handle.
        val domainsDatabase = this.readableDatabase

        // SQL escape the domain name.
        val sqlEscapedDomainName = DatabaseUtils.sqlEscapeString(domainName)

        // Return a cursor for the requested domain name.  The cursor can't be closed because it is needed in the calling activity.
        return domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE WHERE $DOMAIN_NAME = $sqlEscapedDomainName", null)
    }

    fun addDomain(contentValues: ContentValues) {
        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Add the new domain.
        domainsDatabase.insert(DOMAINS_TABLE, null, contentValues)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun addDomain(domainName: String): Int {
        // Add the domain with default settings.
        return addDomain(domainName, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, appContext.getString(R.string.system_default_user_agent), SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT,
                         SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT, SYSTEM_DEFAULT)
    }

    fun addDomain(domainName: String, javaScriptInt: Int, cookiesInt: Int, domStorageInt: Int, userAgentName: String, easyListInt: Int, easyPrivacyInt: Int, fanboysAnnoyanceListInt: Int,
                  fanboysSocialBlockingListInt: Int, ultraListInt: Int, ultraPrivacyInt: Int, blockAllThirdPartyRequestsInt: Int, fontSizeInt: Int, swipeToRefreshInt: Int, webViewThemeInt: Int,
                  wideViewportInt: Int, displayImagesInt: Int): Int {
        // Instantiate a content values.
        val domainContentValues = ContentValues()

        // Create entries for the database fields.  The ID is created automatically.  The pinned SSL certificate information is not created unless added by the user.
        domainContentValues.put(DOMAIN_NAME, domainName)
        domainContentValues.put(ENABLE_JAVASCRIPT, javaScriptInt)
        domainContentValues.put(COOKIES, cookiesInt)
        domainContentValues.put(ENABLE_DOM_STORAGE, domStorageInt)
        domainContentValues.put(USER_AGENT, userAgentName)
        domainContentValues.put(ENABLE_EASYLIST, easyListInt)
        domainContentValues.put(ENABLE_EASYPRIVACY, easyPrivacyInt)
        domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, fanboysAnnoyanceListInt)
        domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, fanboysSocialBlockingListInt)
        domainContentValues.put(ULTRALIST, ultraListInt)
        domainContentValues.put(ENABLE_ULTRAPRIVACY, ultraPrivacyInt)
        domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, blockAllThirdPartyRequestsInt)
        domainContentValues.put(FONT_SIZE, fontSizeInt)
        domainContentValues.put(SWIPE_TO_REFRESH, swipeToRefreshInt)
        domainContentValues.put(WEBVIEW_THEME, webViewThemeInt)
        domainContentValues.put(WIDE_VIEWPORT, wideViewportInt)
        domainContentValues.put(DISPLAY_IMAGES, displayImagesInt)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Insert a new row and store the resulting database ID.
        val newDomainDatabaseId = domainsDatabase.insert(DOMAINS_TABLE, null, domainContentValues).toInt()

        // Close the database handle.
        domainsDatabase.close()

        // Return the new domain database ID.
        return newDomainDatabaseId
    }

    fun updateDomain(databaseId: Int, domainName: String, javaScript: Int, cookies: Int, domStorage: Int, userAgent: String, easyList: Int, easyPrivacy: Int, fanboysAnnoyance: Int,
                     fanboysSocialBlocking: Int, ultraList: Int, ultraPrivacy: Int, blockAllThirdPartyRequests: Int, fontSize: Int, swipeToRefresh: Int, webViewTheme: Int, wideViewport: Int, displayImages: Int,
                     pinnedSslCertificate: Boolean, pinnedIpAddresses: Boolean) {

        // Instantiate a content values.
        val domainContentValues = ContentValues()

        // Add entries for each field in the database.
        domainContentValues.put(DOMAIN_NAME, domainName)
        domainContentValues.put(ENABLE_JAVASCRIPT, javaScript)
        domainContentValues.put(COOKIES, cookies)
        domainContentValues.put(ENABLE_DOM_STORAGE, domStorage)
        domainContentValues.put(USER_AGENT, userAgent)
        domainContentValues.put(ENABLE_EASYLIST, easyList)
        domainContentValues.put(ENABLE_EASYPRIVACY, easyPrivacy)
        domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, fanboysAnnoyance)
        domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, fanboysSocialBlocking)
        domainContentValues.put(ULTRALIST, ultraList)
        domainContentValues.put(ENABLE_ULTRAPRIVACY, ultraPrivacy)
        domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, blockAllThirdPartyRequests)
        domainContentValues.put(FONT_SIZE, fontSize)
        domainContentValues.put(SWIPE_TO_REFRESH, swipeToRefresh)
        domainContentValues.put(WEBVIEW_THEME, webViewTheme)
        domainContentValues.put(WIDE_VIEWPORT, wideViewport)
        domainContentValues.put(DISPLAY_IMAGES, displayImages)
        domainContentValues.put(PINNED_SSL_CERTIFICATE, pinnedSslCertificate)
        domainContentValues.put(PINNED_IP_ADDRESSES, pinnedIpAddresses)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Update the row for the specified database ID.
        domainsDatabase.update(DOMAINS_TABLE, domainContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun updatePinnedSslCertificate(databaseId: Int, sslIssuedToCommonName: String, sslIssuedToOrganization: String, sslIssuedToOrganizationalUnit: String, sslIssuedByCommonName: String,
                                   sslIssuedByOrganization: String, sslIssuedByOrganizationalUnit: String, sslStartDate: Long, sslEndDate: Long) {
        // Instantiate a content values.
        val pinnedSslCertificateContentValues = ContentValues()

        // Add entries for each field in the certificate.
        pinnedSslCertificateContentValues.put(SSL_ISSUED_TO_COMMON_NAME, sslIssuedToCommonName)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_TO_ORGANIZATION, sslIssuedToOrganization)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT, sslIssuedToOrganizationalUnit)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_BY_COMMON_NAME, sslIssuedByCommonName)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_BY_ORGANIZATION, sslIssuedByOrganization)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT, sslIssuedByOrganizationalUnit)
        pinnedSslCertificateContentValues.put(SSL_START_DATE, sslStartDate)
        pinnedSslCertificateContentValues.put(SSL_END_DATE, sslEndDate)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Update the row for the specified database ID.
        domainsDatabase.update(DOMAINS_TABLE, pinnedSslCertificateContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun updatePinnedIpAddresses(databaseId: Int, ipAddresses: String) {
        // Instantiate a content values.
        val pinnedIpAddressesContentValues = ContentValues()

        // Add the IP addresses to the content values.
        pinnedIpAddressesContentValues.put(IP_ADDRESSES, ipAddresses)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Update the row for the database ID.
        domainsDatabase.update(DOMAINS_TABLE, pinnedIpAddressesContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun deleteAllDomains() : Int {
        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Delete the row for the specified database ID.
        val rowsDeleted = domainsDatabase.delete(DOMAINS_TABLE, "", null)

        // Close the database handle.
        domainsDatabase.close()

        // Return the delete status.
        return rowsDeleted
    }

    fun deleteDomain(databaseId: Int) : Int {
        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Delete the row for the specified database ID.
        val rowsDeleted = domainsDatabase.delete(DOMAINS_TABLE, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()

        // Return the delete status.
        return rowsDeleted
    }
}
