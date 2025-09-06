/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2015-2025 Soren Stoutner <soren@stoutner.com>
 *
 * Download cookie code contributed 2017 Hendrik Knackstedt.  Copyright assigned to Soren Stoutner <soren@stoutner.com>.
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

package com.audeon.browser.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintManager
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.ContextMenu
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.GravityCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

import com.audeon.browser.R
import com.audeon.browser.adapters.WebViewStateAdapter
import com.audeon.browser.coroutines.GetHostIpAddressesCoroutine
import com.audeon.browser.coroutines.PopulateFilterListsCoroutine
import com.audeon.browser.coroutines.PrepareSaveDialogCoroutine
import com.audeon.browser.coroutines.SaveUrlCoroutine
import com.audeon.browser.coroutines.SaveWebpageImageCoroutine
import com.audeon.browser.dataclasses.PendingDialogDataClass
import com.audeon.browser.dialogs.CreateBookmarkDialog
import com.audeon.browser.dialogs.CreateBookmarkFolderDialog
import com.audeon.browser.dialogs.CreateHomeScreenShortcutDialog
import com.audeon.browser.dialogs.FontSizeDialog
import com.audeon.browser.dialogs.HttpAuthenticationDialog
import com.audeon.browser.dialogs.OpenDialog
import com.audeon.browser.dialogs.PinnedMismatchDialog
import com.audeon.browser.dialogs.ProxyNotInstalledDialog
import com.audeon.browser.dialogs.SaveDialog
import com.audeon.browser.dialogs.SslCertificateErrorDialog
import com.audeon.browser.dialogs.UrlHistoryDialog
import com.audeon.browser.dialogs.ViewSslCertificateDialog
import com.audeon.browser.dialogs.WaitingForProxyDialog
import com.audeon.browser.fragments.WebViewTabFragment
import com.audeon.browser.helpers.BOOKMARK_NAME
import com.audeon.browser.helpers.BOOKMARK_URL
import com.audeon.browser.helpers.COOKIES
import com.audeon.browser.helpers.DARK_THEME
import com.audeon.browser.helpers.DISABLED
import com.audeon.browser.helpers.DISPLAY_IMAGES
import com.audeon.browser.helpers.DOMAIN_NAME
import com.audeon.browser.helpers.ENABLE_DOM_STORAGE
import com.audeon.browser.helpers.ENABLE_EASYLIST
import com.audeon.browser.helpers.ENABLE_EASYPRIVACY
import com.audeon.browser.helpers.ENABLE_FANBOYS_ANNOYANCE_LIST
import com.audeon.browser.helpers.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST
import com.audeon.browser.helpers.ENABLE_JAVASCRIPT
import com.audeon.browser.helpers.ENABLE_ULTRAPRIVACY
import com.audeon.browser.helpers.ENABLED
import com.audeon.browser.helpers.FAVORITE_ICON
import com.audeon.browser.helpers.FOLDER_ID
import com.audeon.browser.helpers.FONT_SIZE
import com.audeon.browser.helpers.ID
import com.audeon.browser.helpers.IP_ADDRESSES
import com.audeon.browser.helpers.IS_FOLDER
import com.audeon.browser.helpers.LIGHT_THEME
import com.audeon.browser.helpers.PINNED_IP_ADDRESSES
import com.audeon.browser.helpers.PINNED_SSL_CERTIFICATE
import com.audeon.browser.helpers.REQUEST_ALLOWED
import com.audeon.browser.helpers.REQUEST_BLOCKED
import com.audeon.browser.helpers.REQUEST_DEFAULT
import com.audeon.browser.helpers.REQUEST_THIRD_PARTY
import com.audeon.browser.helpers.SSL_ISSUED_BY_COMMON_NAME
import com.audeon.browser.helpers.SSL_ISSUED_BY_ORGANIZATION
import com.audeon.browser.helpers.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT
import com.audeon.browser.helpers.SSL_ISSUED_TO_COMMON_NAME
import com.audeon.browser.helpers.SSL_ISSUED_TO_ORGANIZATION
import com.audeon.browser.helpers.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT
import com.audeon.browser.helpers.SWIPE_TO_REFRESH
import com.audeon.browser.helpers.SYSTEM_DEFAULT
import com.audeon.browser.helpers.WEBVIEW_THEME
import com.audeon.browser.helpers.WIDE_VIEWPORT
import com.audeon.browser.helpers.BookmarksDatabaseHelper
import com.audeon.browser.helpers.CheckFilterListHelper
import com.audeon.browser.helpers.DomainsDatabaseHelper
import com.audeon.browser.helpers.ProxyHelper
import com.audeon.browser.helpers.SanitizeUrlHelper
import com.audeon.browser.helpers.UrlHelper
import com.audeon.browser.views.BLOCKED_REQUESTS
import com.audeon.browser.views.EASYLIST
import com.audeon.browser.views.EASYPRIVACY
import com.audeon.browser.views.FANBOYS_ANNOYANCE_LIST
import com.audeon.browser.views.FANBOYS_SOCIAL_BLOCKING_LIST
import com.audeon.browser.views.THIRD_PARTY_REQUESTS
import com.audeon.browser.views.ULTRAPRIVACY
import com.audeon.browser.views.NestedScrollWebView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

import java.text.NumberFormat

import java.util.ArrayList
import java.util.Date
import java.util.concurrent.Executors
import kotlin.system.exitProcess

// import profil
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.audeon.browser.managers.ProfileManager
import com.audeon.browser.helpers.ProcessRelauncher

// Define the public constants
const val CURRENT_URL = "current_url"
const val DOMAINS_SYSTEM_DEFAULT_USER_AGENT = 0
const val DOMAINS_WEBVIEW_DEFAULT_USER_AGENT = 2
const val DOMAINS_CUSTOM_USER_AGENT = 12
const val SETTINGS_WEBVIEW_DEFAULT_USER_AGENT = 1
const val SETTINGS_CUSTOM_USER_AGENT = 11
const val UNRECOGNIZED_USER_AGENT = -1

// Define the private class constants.
private const val BOOKMARKS_DRAWER_PINNED = "bookmarks_drawer_pinned"
private const val PROXY_MODE = "proxy_mode"
private const val SAVED_NESTED_SCROLL_WEBVIEW_STATE_ARRAY_LIST = "saved_nested_scroll_webview_state_array_list"
private const val SAVED_STATE_ARRAY_LIST = "saved_state_array_list"
private const val SAVED_TAB_POSITION = "saved_tab_position"
private const val TEMPORARY_MHT_FILE = "temporary_mht_file"

class MainWebViewActivity : AppCompatActivity(), CreateBookmarkDialog.CreateBookmarkListener, CreateBookmarkFolderDialog.CreateBookmarkFolderListener, FontSizeDialog.UpdateFontSizeListener,
    NavigationView.OnNavigationItemSelectedListener, OpenDialog.OpenListener, PinnedMismatchDialog.PinnedMismatchListener, PopulateFilterListsCoroutine.PopulateFilterListsListener, SaveDialog.SaveListener,
    UrlHistoryDialog.NavigateHistoryListener, WebViewTabFragment.NewTabListener {

    companion object {
        // Define the public static variables.
        var currentBookmarksFolderId = 0L
        val executorService = Executors.newFixedThreadPool(4)!!
        var orbotStatus = "unknown"
        val pendingDialogsArrayList = ArrayList<PendingDialogDataClass>()
        var proxyMode = ProxyHelper.NONE
        var restartFromBookmarksActivity = false
        var webViewStateAdapter: WebViewStateAdapter? = null

        // Declare the public static variables.
        lateinit var appBarLayout: AppBarLayout
        lateinit var defaultFavoriteIconBitmap : Bitmap
    }

    // Declare the class variables.
    private lateinit var appBar: ActionBar
    private lateinit var checkFilterListHelper: CheckFilterListHelper
    private lateinit var bookmarksCursorAdapter: CursorAdapter
    private lateinit var bookmarksDrawerPinnedImageView: ImageView
    private lateinit var bookmarksFrameLayout: FrameLayout
    private lateinit var bookmarksHeaderLinearLayout: LinearLayout
    private lateinit var bookmarksListView: ListView
    private lateinit var bookmarksTitleTextView: TextView
    private lateinit var browserFrameLayout: FrameLayout
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var cookieManager: CookieManager
    private lateinit var defaultFontSizeString: String
    private lateinit var defaultUserAgentName: String
    private lateinit var defaultWebViewTheme: String
    private lateinit var domainsSettingsSet: MutableSet<String>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var easyList: ArrayList<List<Array<String>>>
    private lateinit var easyPrivacy: ArrayList<List<Array<String>>>
    private lateinit var fanboysAnnoyanceList: ArrayList<List<Array<String>>>
    private lateinit var fanboysSocialList: ArrayList<List<Array<String>>>
    private lateinit var fileChooserCallback: ValueCallback<Array<Uri>>
    private lateinit var finalGrayColorSpan: ForegroundColorSpan
    private lateinit var findOnPageCountTextView: TextView
    private lateinit var findOnPageEditText: EditText
    private lateinit var findOnPageLinearLayout: LinearLayout
    private lateinit var fullScreenVideoFrameLayout: FrameLayout
    private lateinit var initialGrayColorSpan: ForegroundColorSpan
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var navigationBackMenuItem: MenuItem
    private lateinit var navigationForwardMenuItem: MenuItem
    private lateinit var navigationHistoryMenuItem: MenuItem
    private lateinit var navigationRequestsMenuItem: MenuItem
    private lateinit var navigationScrollToBottomMenuItem: MenuItem
    private lateinit var navigationView: NavigationView
    private lateinit var oldFullScreenVideoFrameLayout: FrameLayout
    private lateinit var optionsAddOrEditDomainMenuItem: MenuItem
    private lateinit var optionsBlockAllThirdPartyRequestsMenuItem: MenuItem
    private lateinit var optionsClearCookiesMenuItem: MenuItem
    private lateinit var optionsClearDataMenuItem: MenuItem
    private lateinit var optionsClearDomStorageMenuItem: MenuItem
    private lateinit var optionsCookiesMenuItem: MenuItem
    private lateinit var optionsDarkWebViewMenuItem: MenuItem
    private lateinit var optionsDisplayImagesMenuItem: MenuItem
    private lateinit var optionsDomStorageMenuItem: MenuItem
    private lateinit var optionsEasyListMenuItem: MenuItem
    private lateinit var optionsEasyPrivacyMenuItem: MenuItem
    private lateinit var optionsFanboysAnnoyanceListMenuItem: MenuItem
    private lateinit var optionsFanboysSocialBlockingListMenuItem: MenuItem
    private lateinit var optionsFilterListsMenuItem: MenuItem
    private lateinit var optionsFontSizeMenuItem: MenuItem
    private lateinit var optionsPrivacyMenuItem: MenuItem
    private lateinit var optionsProxyCustomMenuItem: MenuItem
    private lateinit var optionsProxyI2pMenuItem: MenuItem
    private lateinit var optionsProxyMenuItem: MenuItem
    private lateinit var optionsProxyNoneMenuItem: MenuItem
    private lateinit var optionsProxyTorMenuItem: MenuItem
    private lateinit var optionsRefreshMenuItem: MenuItem
    private lateinit var optionsSwipeToRefreshMenuItem: MenuItem
    private lateinit var optionsUltraListMenuItem: MenuItem
    private lateinit var optionsUltraPrivacyMenuItem: MenuItem
    private lateinit var optionsUserAgentChromeOnAndroidMenuItem: MenuItem
    private lateinit var optionsUserAgentChromeOnWindowsMenuItem: MenuItem
    private lateinit var optionsUserAgentChromiumOnLinuxMenuItem: MenuItem
    private lateinit var optionsUserAgentCustomMenuItem: MenuItem
    private lateinit var optionsUserAgentEdgeOnWindowsMenuItem: MenuItem
    private lateinit var optionsUserAgentFirefoxOnAndroidMenuItem: MenuItem
    private lateinit var optionsUserAgentFirefoxOnLinuxMenuItem: MenuItem
    private lateinit var optionsUserAgentFirefoxOnWindowsMenuItem: MenuItem
    private lateinit var optionsUserAgentMenuItem: MenuItem
    private lateinit var optionsUserAgentaudeonbrowserMenuItem: MenuItem
    private lateinit var optionsUserAgentSafariOnIosMenuItem: MenuItem
    private lateinit var optionsUserAgentSafariOnMacosMenuItem: MenuItem
    private lateinit var optionsUserAgentWebViewDefaultMenuItem: MenuItem
    private lateinit var optionsViewSourceMenuItem: MenuItem
    private lateinit var optionsWideViewportMenuItem: MenuItem
    private lateinit var proxyHelper: ProxyHelper
    private lateinit var redColorSpan: ForegroundColorSpan
    private lateinit var saveUrlString: String
    private lateinit var searchURL: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var tabsLinearLayout: LinearLayout
    private lateinit var toolbar: Toolbar
    private lateinit var webViewDefaultUserAgent: String
    private lateinit var webViewThemeEntryValuesStringArray: Array<String>
    private lateinit var webViewViewPager2: ViewPager2
    private lateinit var windowInsetsController: WindowInsetsController
    private lateinit var ultraList: ArrayList<List<Array<String>>>
    private lateinit var urlEditText: EditText
    private lateinit var urlRelativeLayout: RelativeLayout
    private lateinit var userAgentDataArray: Array<String>
    private lateinit var userAgentNamesArray: Array<String>
    private lateinit var userAgentDataArrayAdapter: ArrayAdapter<CharSequence>
    private lateinit var userAgentNamesArrayAdapter: ArrayAdapter<CharSequence>

    // Define the class variables.
    private var appBarHeight = 0
    private var bookmarksCursor: Cursor? = null
    private var bookmarksDatabaseHelper: BookmarksDatabaseHelper? = null
    private var bookmarksDrawerPinned = false
    private var bottomAppBar = false
    private var closeNavigationDrawer = false
    private var currentWebView: NestedScrollWebView? = null
    private var defaultBlockAllThirdPartyRequests = false
    private var defaultCookies = false
    private var defaultDisplayWebpageImages = true
    private var defaultDomStorage = false
    private var defaultEasyList = true
    private var defaultEasyPrivacy = true
    private var defaultFanboysAnnoyanceList = true
    private var defaultFanboysSocialBlockingList = true
    private var defaultProgressViewEndOffset = 0
    private var defaultProgressViewStartOffset = 0
    private var defaultJavaScript = false
    private var defaultSwipeToRefresh = true
    private var defaultUltraList = true
    private var defaultUltraPrivacy = true
    private var defaultWideViewport = true
    private var displayAdditionalAppBarIcons = false
    private var displayUnderCutouts = false
    private var displayingFullScreenVideo = false
    private var displayingInitialTab = true
    private var domainsDatabaseHelper: DomainsDatabaseHelper? = null
    private var downloadWithExternalApp = false
    private var fullScreenBrowsingModeEnabled = false
    private var hideAppBar = true
    private var inFullScreenBrowsingMode = false
    private var incognitoModeEnabled = false
    private var loadingNewIntent = false
    private var navigationDrawerFirstView = true
    private var objectAnimator = ObjectAnimator()
    private var optionsMenu: Menu? = null
    private var orbotStatusBroadcastReceiver: BroadcastReceiver? = null
    private var reapplyAppSettingsOnRestart = false
    private var reapplyDomainSettingsOnRestart = false
    private var sanitizeAmpRedirects = false
    private var sanitizeTrackingQueries = false
    private var savedProxyMode: String? = null
    private var savedNestedScrollWebViewStateArrayList: ArrayList<Bundle>? = null
    private var savedStateArrayList: ArrayList<Bundle>? = null
    private var savedTabPosition = 0
    private var scrollAppBar = false
    private var sortBookmarksAlphabetically = false
    private var ultraPrivacy: ArrayList<List<Array<String>>>? = null
    private var waitingForProxy = false

    // Define the browse file upload activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val browseFileUploadActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
        // Pass the file to the WebView.
        fileChooserCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(activityResult.resultCode, activityResult.data))
    }

    // Define the save URL activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveUrlActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { fileUri ->
        // Only save the URL if the file URI is not null, which happens if the user exited the file picker by pressing back.
        if (fileUri != null) {
            // Instantiate the save URL coroutine.
            val saveUrlCoroutine = SaveUrlCoroutine()

            // Save the URL.
            saveUrlCoroutine.save(this, this, saveUrlString, fileUri, currentWebView!!.settings.userAgentString, currentWebView!!.acceptCookies)
        }

        // Reset the save URL string.
        saveUrlString = ""
    }

    // Define the save webpage archive activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveWebpageArchiveActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("multipart/related")) { fileUri ->
        // Only save the webpage archive if the file URI is not null, which happens if the user exited the file picker by pressing back.
        if (fileUri != null) {
            // Get a cursor from the content resolver.
            val contentResolverCursor = contentResolver.query(fileUri, null, null, null)!!

            // Move to the fist row.
            contentResolverCursor.moveToFirst()

            // Get the file name from the cursor.
            val fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

            // Close the cursor.
            contentResolverCursor.close()

            // Use a coroutine to save the file.
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Create the file on the IO thread.
                    withContext(Dispatchers.IO) {
                        // Create a temporary MHT file.
                        val temporaryMhtFile = File.createTempFile(TEMPORARY_MHT_FILE, ".mht", cacheDir)

                        // The WebView must be accessed from the main thread.
                        withContext(Dispatchers.Main) {
                            currentWebView!!.saveWebArchive(temporaryMhtFile.toString(), false) { callbackValue ->
                                if (callbackValue != null) {  // The temporary MHT file was saved successfully.
                                    try {
                                        // Create a temporary MHT file input stream.
                                        val temporaryMhtFileInputStream = FileInputStream(temporaryMhtFile)

                                        // Get an output stream for the save webpage file path.
                                        val mhtOutputStream = contentResolver.openOutputStream(fileUri)!!

                                        // Create a transfer byte array.
                                        val transferByteArray = ByteArray(1024)

                                        // Create an integer to track the number of bytes read.
                                        var bytesRead: Int

                                        // Copy the temporary MHT file input stream to the MHT output stream.
                                        while (temporaryMhtFileInputStream.read(transferByteArray).also { bytesRead = it } > 0)
                                            mhtOutputStream.write(transferByteArray, 0, bytesRead)

                                        // Close the streams.
                                        mhtOutputStream.close()
                                        temporaryMhtFileInputStream.close()

                                        // Display a snackbar.
                                        Snackbar.make(currentWebView!!, getString(R.string.saved, fileNameString), Snackbar.LENGTH_SHORT).show()
                                    } catch (exception: Exception) {
                                        // Display snackbar with the exception.
                                        Snackbar.make(currentWebView!!, getString(R.string.error_saving_file, fileNameString, exception), Snackbar.LENGTH_INDEFINITE).show()
                                    } finally {
                                        // Delete the temporary MHT file.
                                        temporaryMhtFile.delete()
                                    }
                                } else {  // There was an unspecified error while saving the temporary MHT file.
                                    // Display a snackbar.
                                    Snackbar.make(currentWebView!!, getString(R.string.error_saving_file, fileNameString, getString(R.string.unknown_error)), Snackbar.LENGTH_INDEFINITE).show()
                                }
                            }
                        }
                    }
                } catch (ioException: IOException) {
                    // Display a snackbar with the IO exception.
                    Snackbar.make(currentWebView!!, getString(R.string.error_saving_file, fileNameString, ioException), Snackbar.LENGTH_INDEFINITE).show()
                }
            }
        }
    }

    // Define the save webpage image activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveWebpageImageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { fileUri ->
        // Only save the webpage image if the file URI is not null, which happens if the user exited the file picker by pressing back.
        if (fileUri != null) {
            // Instantiate the save webpage image coroutine.
            val saveWebpageImageCoroutine = SaveWebpageImageCoroutine()

            // Save the webpage image.
            saveWebpageImageCoroutine.save(this, fileUri, currentWebView!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Initialize the default preference values the first time the program is run.  `false` keeps this command from resetting any current preferences back to default.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // Get a handle for the shared preferences.
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the preferences.
        val appTheme = sharedPreferences.getString(getString(R.string.app_theme_key), getString(R.string.app_theme_default_value))
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)
        displayAdditionalAppBarIcons = sharedPreferences.getBoolean(getString(R.string.display_additional_app_bar_icons_key), false)
        displayUnderCutouts = sharedPreferences.getBoolean(getString(R.string.display_under_cutouts_key), false)

        // Set the display under cutouts mode.  This must be done here as it doesn't appear to work correctly if handled after the app is fully initialized.
        if ((Build.VERSION.SDK_INT < 35) && displayUnderCutouts) {
            if (Build.VERSION.SDK_INT >= 30)
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else if (Build.VERSION.SDK_INT >= 28)
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Get the entry values string arrays.
        val appThemeEntryValuesStringArray = resources.getStringArray(R.array.app_theme_entry_values)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the app theme according to the preference.  A switch statement cannot be used because the theme entry values string array is not a compile time constant.
        if (appTheme == appThemeEntryValuesStringArray[1]) {  // The light theme is selected.
            // Apply the light theme.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if (appTheme == appThemeEntryValuesStringArray[2]) {  // The dark theme is selected.
            // Apply the dark theme.
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {  // The system default theme is selected.
            if (Build.VERSION.SDK_INT >= 28) {  // The system default theme is supported.
                // Follow the system default theme.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {  // The system default theme is not supported.
                // Follow the battery saver mode.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }

        // Do not continue if the app theme is different than the OS theme.  The app always initially starts in the OS theme.
        // If the user has specified the opposite theme should be used, the app will restart in that mode after the above `setDefaultNightMode()` code processes.  However, the restart is delayed.
        // If the filter list coroutine starts below it will continue to run during the restart, which leads to indeterminate behavior, with the system often not knowing how many tabs exist.
        // See https://redmine.stoutner.com/issues/952.
        if ((appTheme == appThemeEntryValuesStringArray[0]) ||  // The system default theme is used.
            ((appTheme == appThemeEntryValuesStringArray[1]) && (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)) ||  // The app is running in day theme as desired.
            ((appTheme == appThemeEntryValuesStringArray[2]) && (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES))) {  // The app is running in night theme as desired.

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Check to see if the activity has been restarted.
            if (savedInstanceState != null) {
                // Store the saved instance state variables.  The deprecated `getParcelableArrayList` can be upgraded once the minimum API >= 33.
                bookmarksDrawerPinned = savedInstanceState.getBoolean(BOOKMARKS_DRAWER_PINNED)
                @Suppress("DEPRECATION")
                savedNestedScrollWebViewStateArrayList = savedInstanceState.getParcelableArrayList(SAVED_NESTED_SCROLL_WEBVIEW_STATE_ARRAY_LIST)
                savedProxyMode = savedInstanceState.getString(PROXY_MODE)
                @Suppress("DEPRECATION")
                savedStateArrayList = savedInstanceState.getParcelableArrayList(SAVED_STATE_ARRAY_LIST)
                savedTabPosition = savedInstanceState.getInt(SAVED_TAB_POSITION)
            }

            // Enable the drawing of the entire webpage.  This makes it possible to save a website image.  This must be done before anything else happens with the WebView.
            WebView.enableSlowWholeDocumentDraw()

            // Set the content view according to the position of the app bar.
            if (bottomAppBar)
                setContentView(R.layout.main_framelayout_bottom_appbar)
            else
                setContentView(R.layout.main_framelayout_top_appbar)

            // Get handles for the views.
            browserFrameLayout = findViewById(R.id.browser_framelayout)
            drawerLayout = findViewById(R.id.drawerlayout)
            coordinatorLayout = findViewById(R.id.coordinatorlayout)
            appBarLayout = findViewById(R.id.appbar_layout)
            toolbar = findViewById(R.id.toolbar)
            findOnPageLinearLayout = findViewById(R.id.find_on_page_linearlayout)
            findOnPageEditText = findViewById(R.id.find_on_page_edittext)
            findOnPageCountTextView = findViewById(R.id.find_on_page_count_textview)
            tabsLinearLayout = findViewById(R.id.tabs_linearlayout)
            tabLayout = findViewById(R.id.tablayout)
            swipeRefreshLayout = findViewById(R.id.swiperefreshlayout)
            webViewViewPager2 = findViewById(R.id.webview_viewpager2)
            navigationView = findViewById(R.id.navigationview)
            bookmarksFrameLayout = findViewById(R.id.bookmarks_framelayout)
            bookmarksHeaderLinearLayout = findViewById(R.id.bookmarks_header_linearlayout)
            bookmarksListView = findViewById(R.id.bookmarks_drawer_listview)
            bookmarksTitleTextView = findViewById(R.id.bookmarks_title_textview)
            bookmarksDrawerPinnedImageView = findViewById(R.id.bookmarks_drawer_pinned_imageview)
            oldFullScreenVideoFrameLayout = findViewById(R.id.old_full_screen_video_framelayout)
            fullScreenVideoFrameLayout = findViewById(R.id.full_screen_video_framelayout)

            // Get a handle for the window inset controller.
            if (Build.VERSION.SDK_INT >= 30)
                windowInsetsController = browserFrameLayout.windowInsetsController!!

            // Set the layout to fit the system windows according to the API.
            if (Build.VERSION.SDK_INT >= 35) {
                // Set the browser frame layout to fit system windows.
                browserFrameLayout.fitsSystemWindows = true
            } else {
                // Set the layouts to fit system windows.
                coordinatorLayout.fitsSystemWindows = true
                bookmarksFrameLayout.fitsSystemWindows = true
            }

            // Get a handle for the navigation menu.
            val navigationMenu = navigationView.menu

            // Get handles for the navigation menu items.
            navigationBackMenuItem = navigationMenu.findItem(R.id.back)
            navigationForwardMenuItem = navigationMenu.findItem(R.id.forward)
            navigationScrollToBottomMenuItem = navigationMenu.findItem(R.id.scroll_to_bottom)
            navigationHistoryMenuItem = navigationMenu.findItem(R.id.history)
            navigationRequestsMenuItem = navigationMenu.findItem(R.id.requests)

            // Listen for touches on the navigation menu.
            navigationView.setNavigationItemSelectedListener(this)

            // Set the support action bar.
            setSupportActionBar(toolbar)

            // Get a handle for the app bar.
            appBar = supportActionBar!!

            // Set the custom app bar layout, which shows the URL text bar.
            appBar.setCustomView(R.layout.url_app_bar)

            // Display the custom app bar layout.
            appBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM

            // Get handles for the views in the URL app bar.
            urlRelativeLayout = findViewById(R.id.url_relativelayout)
            urlEditText = findViewById(R.id.url_edittext)

            // Store the URL when it is changed.  This enables the restoring of partially-typed URLs when tabs change.
            urlEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
                    // Do nothing.
                }

                override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
                    // Do nothing.
                }

                override fun afterTextChanged(editable: Editable) {
                    // Store the URL
                    currentWebView?.currentUrl = editable.toString()
                }
            })

            // Initially disable the sliding drawers.  They will be enabled once the filter lists are loaded.
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            // Initially hide the user interface so that only the filter list loading screen is shown (if reloading).
            drawerLayout.visibility = View.GONE

            // Initialize the WebView state adapter.
            webViewStateAdapter = WebViewStateAdapter(this, bottomAppBar)

            // Set the WebView pager adapter.
            webViewViewPager2.adapter = webViewStateAdapter

            // Store up to 100 tabs in memory.
            webViewViewPager2.offscreenPageLimit = 100

            // Disable swiping between pages in the view pager.
            webViewViewPager2.isUserInputEnabled = false

            // Get a handle for the cookie manager.
            cookieManager = CookieManager.getInstance()

            // Instantiate the helpers.
            bookmarksDatabaseHelper = BookmarksDatabaseHelper(this)
            domainsDatabaseHelper = DomainsDatabaseHelper(this)
            proxyHelper = ProxyHelper()

            // Update the bookmarks drawer pinned image view.
            updateBookmarksDrawerPinnedImageView()

            // Get the default favorite icon drawable.
            val favoriteIconDrawable = AppCompatResources.getDrawable(this, R.drawable.world)

            // Cast the favorite icon drawable to a bitmap drawable.
            val favoriteIconBitmapDrawable = (favoriteIconDrawable as BitmapDrawable?)!!

            // Store the default favorite icon bitmap.
            defaultFavoriteIconBitmap = favoriteIconBitmapDrawable.bitmap

            // Initialize the app.
            initializeApp()

            // Apply the app settings from the shared preferences.
            applyAppSettings()

            // Control what the system back command does.
            val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Process the different back options.
                    if (drawerLayout.isDrawerVisible(GravityCompat.START)) {  // The navigation drawer is open.
                        // Close the navigation drawer.
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else if (drawerLayout.isDrawerVisible(GravityCompat.END)) {  // The bookmarks drawer is open.
                        // close the bookmarks drawer.
                        drawerLayout.closeDrawer(GravityCompat.END)
                    } else if (displayingFullScreenVideo) {  // A full screen video is shown.
                        // Exit the full screen video.
                        exitFullScreenVideo()
                        // It shouldn't be possible for the currentWebView to be null, but crash logs indicate it sometimes happens.
                    } else if (currentWebView != null && currentWebView!!.canGoBack()) {  // There is at least one item in the current WebView history.
                        // Navigate back one page.
                        navigateHistory(-1)
                    } else {  // Close the current tab.
                        // A view is required because the method is also called by an XML `onClick`.
                        closeTab(null)
                    }
                }
            }

            // Register the on back pressed callback.
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

            // Instantiate the populate filter lists coroutine.
            val populateFilterListsCoroutine = PopulateFilterListsCoroutine(this)

            // Populate the filter lists.
            populateFilterListsCoroutine.populateFilterLists(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        // Run the default commands.
        super.onNewIntent(intent)

        // Close the navigation drawer if it is open.
        if (drawerLayout.isDrawerVisible(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)

        // Close the bookmarks drawer if it is open.
        if (drawerLayout.isDrawerVisible(GravityCompat.END))
            drawerLayout.closeDrawer(GravityCompat.END)

        // Get the information from the intent.
        val intentAction = intent.action
        val intentUriData = intent.data
        val intentStringExtra = intent.getStringExtra(Intent.EXTRA_TEXT)

        // Determine if this is a web search.
        val isWebSearch = (intentAction != null) && (intentAction == Intent.ACTION_WEB_SEARCH)

        // Check to see if the app is being restarted from a saved state.
        if (ultraPrivacy != null) {  // The activity is not being restarted from a saved state.
            // Only process the URI if it contains data or it is a web search.  If the user pressed the desktop icon after the app was already running the URI will be null.
            if ((intentUriData != null) || (intentStringExtra != null) || isWebSearch) {
                // Exit the full screen video if it is displayed.
                if (displayingFullScreenVideo) {
                    // Exit full screen video mode.
                    exitFullScreenVideo()

                    // Reload the current WebView.  Otherwise, it can display entirely black.
                    currentWebView!!.reload()
                }

                // Get the URL.
                val url = if (isWebSearch) {  // The intent is a web search.
                    // Sanitize the search input and convert it to a search.
                    val encodedSearchString = try {
                        URLEncoder.encode(intent.getStringExtra(SearchManager.QUERY), "UTF-8")
                    } catch (exception: UnsupportedEncodingException) {
                        ""
                    }

                    // Add the base search URL.
                    searchURL + encodedSearchString
                } else {  // The intent contains a URL in either the data or an extra.
                    intentUriData?.toString() ?: intentStringExtra
                }

                // Add a new tab if specified in the preferences.
                if (sharedPreferences.getBoolean(getString(R.string.open_intents_in_new_tab_key), true)) {  // Load the URL in a new tab.
                    // Set the loading new intent flag.
                    loadingNewIntent = true

                    // Add a new tab.
                    addNewPage(url!!, adjacent = false, moveToTab = true)
                } else {  // Load the URL in the current tab.
                    // Make it so.
                    loadUrl(currentWebView!!, url!!)
                }
            }
        } else {  // The app has been restarted.
            // Replace the intent that started the app with this one.  This will load the tab after the others have been restored.
            setIntent(intent)
        }
    }

    public override fun onRestart() {
        // Run the default commands.
        super.onRestart()

        // Apply the app settings if returning from the Settings activity.
        if (reapplyAppSettingsOnRestart) {
            // Reset the reapply app settings on restart flag.
            reapplyAppSettingsOnRestart = false

            // Apply the app settings.
            applyAppSettings()
        }

        // Apply the domain settings if returning from the settings or domains activity.
        if (reapplyDomainSettingsOnRestart) {
            // Reset the reapply domain settings on restart flag.
            reapplyDomainSettingsOnRestart = false

            // Update the domains settings set.
            updateDomainsSettingsSet()

            // Reapply the domain settings for each tab.
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the fragment view.
                val fragmentView = webViewTabFragment.view

                // Only reload the WebViews if they exist.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Reset the current domain name so the domain settings will be reapplied.
                    nestedScrollWebView.currentDomainName = ""

                    // Reapply the domain settings if the URL is not null, which happens for empty tabs when returning from settings.
                    if (nestedScrollWebView.url != null)
                        applyDomainSettings(nestedScrollWebView, nestedScrollWebView.url, resetTab = false, reloadWebsite = true, loadUrl = false)
                }
            }
        }

        // Update the bookmarks drawer if returning from the Bookmarks activity.
        if (restartFromBookmarksActivity) {
            // Reset the restart from bookmarks activity flag.
            restartFromBookmarksActivity = false

            // Close the bookmarks drawer.
            drawerLayout.closeDrawer(GravityCompat.END)

            // Reload the bookmarks drawer.
            loadBookmarksFolder()
        }

        // Update the privacy icon.  `true` runs `invalidateOptionsMenu` as the last step.  This can be important if the screen was rotated.
        updatePrivacyIcons(true)
    }

    // `onStart()` runs after `onCreate()` or `onRestart()`.  This is used instead of `onResume()` so the commands aren't called every time the screen is partially hidden.
    public override fun onStart() {
        // Run the default commands.
        super.onStart()

        // Resume any WebViews if the state adapter exists.  If the app is restarting to change the initial app theme it won't have been populated yet.
        if (webViewStateAdapter != null) {
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the fragment view.
                val fragmentView = webViewTabFragment.view

                // Only resume the WebViews if they exist (they won't when the app is first created).
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Resume the nested scroll WebView.
                    nestedScrollWebView.onResume()
                }
            }
        }

        // Resume the nested scroll WebView JavaScript timers.  This is a global command that resumes JavaScript timers on all WebViews.
        if (currentWebView != null)
            currentWebView!!.resumeTimers()

        // Reapply the proxy settings if the system is using a proxy.  This redisplays the appropriate alert dialog.
        if (proxyMode != ProxyHelper.NONE)
            applyProxy(false)

        // Reapply any system UI flags on older APIs.
        if ((Build.VERSION.SDK_INT < 30) && (displayingFullScreenVideo || inFullScreenBrowsingMode)) {  // The system is displaying a website or a video in full screen mode.
            /* Hide the system bars.
             * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
             * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
             * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
             * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
             */

            @Suppress("DEPRECATION")
            browserFrameLayout.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        // Show any pending dialogs.
        for (i in pendingDialogsArrayList.indices) {
            // Get the pending dialog from the array list.
            val (dialogFragment, tag) = pendingDialogsArrayList[i]

            // Show the pending dialog.
            dialogFragment.show(supportFragmentManager, tag)
        }

        // Clear the pending dialogs array list.
        pendingDialogsArrayList.clear()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(outState)

        // Only save the instance state if the WebView state adapter is not null, which will be the case if the app is restarting to change the initial app theme.
        if (webViewStateAdapter != null) {
            // Initialize the saved state array lists.
            savedStateArrayList = ArrayList()
            savedNestedScrollWebViewStateArrayList = ArrayList()

            // Get the URLs from each tab.
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the fragment view.
                val fragmentView = webViewTabFragment.view

                // Save the fragment state if it is not null.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Create the saved state bundle.
                    val savedStateBundle = Bundle()

                    // Get the current states.
                    nestedScrollWebView.saveState(savedStateBundle)
                    val savedNestedScrollWebViewStateBundle = nestedScrollWebView.saveNestedScrollWebViewState()

                    // Store the saved states in the array lists.
                    savedStateArrayList!!.add(savedStateBundle)
                    savedNestedScrollWebViewStateArrayList!!.add(savedNestedScrollWebViewStateBundle)
                }
            }

            // Get the current tab position.
            val currentTabPosition = tabLayout.selectedTabPosition

            // Store the saved states in the bundle.
            outState.putBoolean(BOOKMARKS_DRAWER_PINNED, bookmarksDrawerPinned)
            outState.putString(PROXY_MODE, proxyMode)
            outState.putParcelableArrayList(SAVED_NESTED_SCROLL_WEBVIEW_STATE_ARRAY_LIST, savedNestedScrollWebViewStateArrayList)
            outState.putParcelableArrayList(SAVED_STATE_ARRAY_LIST, savedStateArrayList)
            outState.putInt(SAVED_TAB_POSITION, currentTabPosition)
        }
    }

    // `onStop()` runs after `onPause()`.  It is used instead of `onPause()` so the commands are not called every time the screen is partially hidden.
    public override fun onStop() {
        // Run the default commands.
        super.onStop()

        // Only pause the WebViews if the state adapter is not null, which is the case if the app is restarting to change the initial app theme.
        if (webViewStateAdapter != null) {
            // Pause each web view.
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the fragment view.
                val fragmentView = webViewTabFragment.view

                // Only pause the WebViews if they exist (they won't when the app is first created).
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Pause the nested scroll WebView.
                    nestedScrollWebView.onPause()
                }
            }
        }

        // Pause the WebView JavaScript timers.  This is a global command that pauses JavaScript on all WebViews.
        if (currentWebView != null)
            currentWebView!!.pauseTimers()
    }

    public override fun onDestroy() {
        // Unregister the orbot status broadcast receiver if it exists.
        if (orbotStatusBroadcastReceiver != null) {
            unregisterReceiver(orbotStatusBroadcastReceiver)
        }

        // Close the bookmarks cursor if it exists.
        bookmarksCursor?.close()

        // Close the databases if they exist.
        bookmarksDatabaseHelper?.close()
        domainsDatabaseHelper?.close()

        // Run the default commands.
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Run the default commands.
        super.onConfigurationChanged(newConfig)

        // Reset the navigation drawer first view flag.
        navigationDrawerFirstView = true

        // Get the current page.
        val currentPage = webViewViewPager2.currentItem

        // Toggle the pages if there is more than one so that the view pager will recalculate their size.
        if (currentPage > 0) {
            // Switch to the previous page after 25 milliseconds.
            webViewViewPager2.postDelayed ({ webViewViewPager2.currentItem = (currentPage - 1) }, 25)

            // Switch back to the current page after the view pager has quiesced (which we are deciding should be 25 milliseconds).
            webViewViewPager2.postDelayed ({ webViewViewPager2.currentItem = currentPage }, 25)
        }

        // Scroll to the current tab position after 25 milliseconds.
        tabLayout.postDelayed ({ tabLayout.setScrollPosition(currentPage, 0F, false, false) }, 25)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.  This adds items to the app bar if it is present.
        menuInflater.inflate(R.menu.webview_options_menu, menu)

        // Get handles for the menu items.
        optionsPrivacyMenuItem = menu.findItem(R.id.javascript)
        optionsRefreshMenuItem = menu.findItem(R.id.refresh)
        val optionsBookmarksMenuItem = menu.findItem(R.id.bookmarks)
        optionsCookiesMenuItem = menu.findItem(R.id.cookies)
        optionsDomStorageMenuItem = menu.findItem(R.id.dom_storage)
        optionsClearDataMenuItem = menu.findItem(R.id.clear_data)
        optionsClearCookiesMenuItem = menu.findItem(R.id.clear_cookies)
        optionsClearDomStorageMenuItem = menu.findItem(R.id.clear_dom_storage)
        optionsEasyListMenuItem = menu.findItem(R.id.easylist)
        optionsEasyPrivacyMenuItem = menu.findItem(R.id.easyprivacy)
        optionsFanboysAnnoyanceListMenuItem = menu.findItem(R.id.fanboys_annoyance_list)
        optionsFanboysSocialBlockingListMenuItem = menu.findItem(R.id.fanboys_social_blocking_list)
        optionsFilterListsMenuItem = menu.findItem(R.id.filterlists)
        optionsUltraListMenuItem = menu.findItem(R.id.ultralist)
        optionsUltraPrivacyMenuItem = menu.findItem(R.id.ultraprivacy)
        optionsBlockAllThirdPartyRequestsMenuItem = menu.findItem(R.id.block_all_third_party_requests)
        optionsProxyMenuItem = menu.findItem(R.id.proxy)
        optionsProxyNoneMenuItem = menu.findItem(R.id.proxy_none)
        optionsProxyTorMenuItem = menu.findItem(R.id.proxy_tor)
        optionsProxyI2pMenuItem = menu.findItem(R.id.proxy_i2p)
        optionsProxyCustomMenuItem = menu.findItem(R.id.proxy_custom)
        optionsUserAgentMenuItem = menu.findItem(R.id.user_agent)
        optionsUserAgentaudeonbrowserMenuItem = menu.findItem(R.id.user_agent_audeon_browser)
        optionsUserAgentWebViewDefaultMenuItem = menu.findItem(R.id.user_agent_webview_default)
        optionsUserAgentFirefoxOnAndroidMenuItem = menu.findItem(R.id.user_agent_firefox_on_android)
        optionsUserAgentChromeOnAndroidMenuItem = menu.findItem(R.id.user_agent_chrome_on_android)
        optionsUserAgentSafariOnIosMenuItem = menu.findItem(R.id.user_agent_safari_on_ios)
        optionsUserAgentFirefoxOnLinuxMenuItem = menu.findItem(R.id.user_agent_firefox_on_linux)
        optionsUserAgentChromiumOnLinuxMenuItem = menu.findItem(R.id.user_agent_chromium_on_linux)
        optionsUserAgentFirefoxOnWindowsMenuItem = menu.findItem(R.id.user_agent_firefox_on_windows)
        optionsUserAgentChromeOnWindowsMenuItem = menu.findItem(R.id.user_agent_chrome_on_windows)
        optionsUserAgentEdgeOnWindowsMenuItem = menu.findItem(R.id.user_agent_edge_on_windows)
        optionsUserAgentSafariOnMacosMenuItem = menu.findItem(R.id.user_agent_safari_on_macos)
        optionsUserAgentCustomMenuItem = menu.findItem(R.id.user_agent_custom)
        optionsSwipeToRefreshMenuItem = menu.findItem(R.id.swipe_to_refresh)
        optionsWideViewportMenuItem = menu.findItem(R.id.wide_viewport)
        optionsDisplayImagesMenuItem = menu.findItem(R.id.display_images)
        optionsDarkWebViewMenuItem = menu.findItem(R.id.dark_webview)
        optionsFontSizeMenuItem = menu.findItem(R.id.font_size)
        optionsViewSourceMenuItem = menu.findItem(R.id.view_source)
        optionsAddOrEditDomainMenuItem = menu.findItem(R.id.add_or_edit_domain)

        // Set the initial status of the privacy icons.  `false` does not call `invalidateOptionsMenu` as the last step.
        updatePrivacyIcons(false)

        // Set the status of the additional app bar icons.  Setting the refresh menu item to `SHOW_AS_ACTION_ALWAYS` makes it appear even on small devices like phones.
        if (displayAdditionalAppBarIcons) {  // Display the additional icons.
            optionsRefreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            optionsBookmarksMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            optionsCookiesMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        } else { //Do not display the additional icons.
            optionsRefreshMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            optionsBookmarksMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            optionsCookiesMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        // Replace `Refresh` with `Stop` if a URL is already loading.
        if ((currentWebView != null) && (currentWebView!!.progress != 100)) {
            // Set the title.
            optionsRefreshMenuItem.setTitle(R.string.stop)

            // Set the icon if it is displayed in the app bar.
            if (displayAdditionalAppBarIcons)
                optionsRefreshMenuItem.setIcon(R.drawable.close_blue)
        }

        // Store a handle for the options menu.
        optionsMenu = menu

        // Done.
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Initialize the current user agent string and the font size.
        var currentUserAgent = getString(R.string.user_agent_audeon_browser)
        var fontSize = 100

        // Set items that require the current web view to be populated.  It will be null when the program is first opened, as `onPrepareOptionsMenu()` is called before the first WebView is initialized.
        if (currentWebView != null) {
            // Set the add or edit domain text.
            if (currentWebView!!.domainSettingsApplied)
                optionsAddOrEditDomainMenuItem.setTitle(R.string.edit_domain_settings)
            else
                optionsAddOrEditDomainMenuItem.setTitle(R.string.add_domain_settings)

            // Get the current user agent from the WebView.
            currentUserAgent = currentWebView!!.settings.userAgentString

            // Get the current font size from the the WebView.
            fontSize = currentWebView!!.settings.textZoom

            // Set the status of the menu item checkboxes.
            optionsDomStorageMenuItem.isChecked = currentWebView!!.settings.domStorageEnabled
            optionsEasyListMenuItem.isChecked = currentWebView!!.easyListEnabled
            optionsEasyPrivacyMenuItem.isChecked = currentWebView!!.easyPrivacyEnabled
            optionsFanboysAnnoyanceListMenuItem.isChecked = currentWebView!!.fanboysAnnoyanceListEnabled
            optionsFanboysSocialBlockingListMenuItem.isChecked = currentWebView!!.fanboysSocialBlockingListEnabled
            optionsUltraListMenuItem.isChecked = currentWebView!!.ultraListEnabled
            optionsUltraPrivacyMenuItem.isChecked = currentWebView!!.ultraPrivacyEnabled
            optionsBlockAllThirdPartyRequestsMenuItem.isChecked = currentWebView!!.blockAllThirdPartyRequests
            optionsSwipeToRefreshMenuItem.isChecked = currentWebView!!.swipeToRefresh
            optionsWideViewportMenuItem.isChecked = currentWebView!!.settings.useWideViewPort
            optionsDisplayImagesMenuItem.isChecked = currentWebView!!.settings.loadsImagesAutomatically

            // Set the display names for the filter lists with the number of blocked requests.
            optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + currentWebView!!.getRequestsCount(BLOCKED_REQUESTS)
            optionsEasyListMenuItem.title = currentWebView!!.getRequestsCount(EASYLIST).toString() + " - " + getString(R.string.easylist)
            optionsEasyPrivacyMenuItem.title = currentWebView!!.getRequestsCount(EASYPRIVACY).toString() + " - " + getString(R.string.easyprivacy)
            optionsFanboysAnnoyanceListMenuItem.title = currentWebView!!.getRequestsCount(FANBOYS_ANNOYANCE_LIST).toString() + " - " + getString(R.string.fanboys_annoyance_list)
            optionsFanboysSocialBlockingListMenuItem.title = currentWebView!!.getRequestsCount(FANBOYS_SOCIAL_BLOCKING_LIST).toString() + " - " + getString(R.string.fanboys_social_blocking_list)
            optionsUltraListMenuItem.title = currentWebView!!.getRequestsCount(com.audeon.browser.views.ULTRALIST).toString() + " - " + getString(R.string.ultralist)
            optionsUltraPrivacyMenuItem.title = currentWebView!!.getRequestsCount(ULTRAPRIVACY).toString() + " - " + getString(R.string.ultraprivacy)
            optionsBlockAllThirdPartyRequestsMenuItem.title = currentWebView!!.getRequestsCount(THIRD_PARTY_REQUESTS).toString() + " - " + getString(R.string.block_all_third_party_requests)

            // Enable DOM Storage if JavaScript is enabled.
            optionsDomStorageMenuItem.isEnabled = currentWebView!!.settings.javaScriptEnabled

            // Get the current theme status.
            val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            // Enable dark WebView if night mode is enabled.
            optionsDarkWebViewMenuItem.isEnabled = (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES)

            // Set the checkbox status for dark WebView if algorithmic darkening is supported.
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                optionsDarkWebViewMenuItem.isChecked = WebSettingsCompat.isAlgorithmicDarkeningAllowed(currentWebView!!.settings)

            // Set the view source title according to the current URL.
            if (currentWebView!!.currentUrl.startsWith("view-source:"))
                optionsViewSourceMenuItem.title = getString(R.string.view_rendered_website)
            else
                optionsViewSourceMenuItem.title = getString(R.string.view_source)
        }

        // Set the cookies menu item checked status.
        optionsCookiesMenuItem.isChecked = cookieManager.acceptCookie()

        // Enable Clear Cookies if there are any.
        optionsClearCookiesMenuItem.isEnabled = cookieManager.hasCookies()

        // Get the application's private data directory, which will be something like `/data/user/0/com.audeon.browser.standard`, which links to `/data/data/com.audeon.browser.standard`.
        val privateDataDirectoryString = applicationInfo.dataDir

        // Get the storage directories.
        val localStorageDirectory = File("$privateDataDirectoryString/app_webview/Default/Local Storage/")
        val sessionStorageDirectory = File("$privateDataDirectoryString/app_webview/Default/Session Storage/")
        val indexedDBDirectory = File("$privateDataDirectoryString/app_webview/Default/IndexedDB")

        // Initialize the number of files counters.
        var localStorageDirectoryNumberOfFiles = 0
        var sessionStorageDirectoryNumberOfFiles = 0
        var indexedDBDirectoryNumberOfFiles = 0

        // Get a count of the number of files in the Local Storage directory.  The list can be null, in which case a `0` is returned.
        if (localStorageDirectory.exists())
            localStorageDirectoryNumberOfFiles = (localStorageDirectory.list())?.size ?: 0

        // Get a count of the number of files in the Local Storage directory.  The list can be null, in which case a `0` is returned.
        if (sessionStorageDirectory.exists())
            sessionStorageDirectoryNumberOfFiles = (sessionStorageDirectory.list())?.size ?: 0

        // Get a count of the number of files in the IndexedDB directory.  The list can be null, in which case a `0` is returned.
        if (indexedDBDirectory.exists())
            indexedDBDirectoryNumberOfFiles = (indexedDBDirectory.list())?.size ?: 0

        // Enable Clear DOM Storage if there is any.
        optionsClearDomStorageMenuItem.isEnabled = localStorageDirectoryNumberOfFiles > 0 || sessionStorageDirectoryNumberOfFiles > 0 || indexedDBDirectoryNumberOfFiles > 0

        // Enable Clear Data if any of the submenu items are enabled.
        optionsClearDataMenuItem.isEnabled = (optionsClearCookiesMenuItem.isEnabled || optionsClearDomStorageMenuItem.isEnabled)

        // Disable Fanboy's Social Blocking List menu item if Fanboy's Annoyance List is checked.
        optionsFanboysSocialBlockingListMenuItem.isEnabled = !optionsFanboysAnnoyanceListMenuItem.isChecked

        // Set the proxy title and check the applied proxy.
        when (proxyMode) {
            ProxyHelper.NONE -> {
                // Set the proxy title.
                optionsProxyMenuItem.title = getString(R.string.proxy) + " - " + getString(R.string.proxy_none)

                // Check the proxy None radio button.
                optionsProxyNoneMenuItem.isChecked = true
            }

            ProxyHelper.TOR -> {
                // Set the proxy title.
                optionsProxyMenuItem.title = getString(R.string.proxy) + " - " + getString(R.string.proxy_tor)

                // Check the proxy Tor radio button.
                optionsProxyTorMenuItem.isChecked = true
            }

            ProxyHelper.I2P -> {
                // Set the proxy title.
                optionsProxyMenuItem.title = getString(R.string.proxy) + " - " + getString(R.string.proxy_i2p)

                // Check the proxy I2P radio button.
                optionsProxyI2pMenuItem.isChecked = true
            }

            ProxyHelper.CUSTOM -> {
                // Set the proxy title.
                optionsProxyMenuItem.title = getString(R.string.proxy) + " - " + getString(R.string.proxy_custom)

                // Check the proxy Custom radio button.
                optionsProxyCustomMenuItem.isChecked = true
            }
        }

        // Select the current user agent menu item.
        when (currentUserAgent) {
            resources.getStringArray(R.array.user_agent_data)[0] -> {  // Privacy Browser.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_audeon_browser)

                // Select the Privacy Browser radio box.
                optionsUserAgentaudeonbrowserMenuItem.isChecked = true
            }

            webViewDefaultUserAgent -> {  // WebView Default.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_webview_default)

                // Select the WebView Default radio box.
                optionsUserAgentWebViewDefaultMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[2] -> {  // Firefox on Android.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_firefox_on_android)

                // Select the Firefox on Android radio box.
                optionsUserAgentFirefoxOnAndroidMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[3] -> {  // Chrome on Android.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_chrome_on_android)

                // Select the Chrome on Android radio box.
                optionsUserAgentChromeOnAndroidMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[4] -> {  // Safari on iOS.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_safari_on_ios)

                // Select the Safari on iOS radio box.
                optionsUserAgentSafariOnIosMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[5] -> {  // Firefox on Linux.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_firefox_on_linux)

                // Select the Firefox on Linux radio box.
                optionsUserAgentFirefoxOnLinuxMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[6] -> {  // Chromium on Linux.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_chromium_on_linux)

                // Select the Chromium on Linux radio box.
                optionsUserAgentChromiumOnLinuxMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[7] -> {  // Firefox on Windows.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_firefox_on_windows)

                // Select the Firefox on Windows radio box.
                optionsUserAgentFirefoxOnWindowsMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[8] -> {  // Chrome on Windows.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_chrome_on_windows)

                // Select the Chrome on Windows radio box.
                optionsUserAgentChromeOnWindowsMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[9] -> {  // Edge on Windows.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_edge_on_windows)

                // Select the Edge on Windows radio box.
                optionsUserAgentEdgeOnWindowsMenuItem.isChecked = true
            }

            resources.getStringArray(R.array.user_agent_data)[10] -> {  // Safari on macOS.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_safari_on_macos)

                // Select the Safari on macOS radio box.
                optionsUserAgentSafariOnMacosMenuItem.isChecked = true
            }

            else -> {  // Custom user agent.
                // Update the user agent menu item title.
                optionsUserAgentMenuItem.title = getString(R.string.options_user_agent) + " - " + getString(R.string.user_agent_custom)

                // Select the Custom radio box.
                optionsUserAgentCustomMenuItem.isChecked = true
            }
        }

        // Set the font size title.
        optionsFontSizeMenuItem.title = getString(R.string.font_size) + " - " + fontSize + "%"

        // Run all the other default commands.
        super.onPrepareOptionsMenu(menu)

        // Display the menu.
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Run the commands that correlate to the selected menu item.
        return when (menuItem.itemId) {
            R.id.action_profiles -> {
                showProfilesDialog()
                return true
            }

            R.id.javascript -> {  // JavaScript.
                // Toggle the JavaScript status.
                currentWebView!!.settings.javaScriptEnabled = !currentWebView!!.settings.javaScriptEnabled

                // Update the privacy icon.
                updatePrivacyIcons(true)

                // Display a snackbar.
                if (currentWebView!!.settings.javaScriptEnabled)  // JavaScrip is enabled.
                    Snackbar.make(webViewViewPager2, R.string.javascript_enabled, Snackbar.LENGTH_SHORT).show()
                else if (cookieManager.acceptCookie())  // JavaScript is disabled, but cookies are enabled.
                    Snackbar.make(webViewViewPager2, R.string.javascript_disabled, Snackbar.LENGTH_SHORT).show()
                else  // Privacy mode.
                    Snackbar.make(webViewViewPager2, R.string.privacy_mode, Snackbar.LENGTH_SHORT).show()

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.refresh -> {  // Refresh.
                // Run the command that correlates to the current status of the menu item.
                if (menuItem.title == getString(R.string.refresh))  // The refresh button was pushed.
                    currentWebView!!.reload()
                else  // The stop button was pushed.
                    currentWebView!!.stopLoading()

                // Consume the event.
                true
            }

            R.id.bookmarks -> {  // Bookmarks.
                // Open the bookmarks drawer.
                drawerLayout.openDrawer(GravityCompat.END)

                // Consume the event.
                true
            }

            R.id.cookies -> {  // Cookies.
                // Toggle the cookie status.
                cookieManager.setAcceptCookie(!cookieManager.acceptCookie())

                // Store the cookie status.
                currentWebView!!.acceptCookies = cookieManager.acceptCookie()

                // Update the menu checkbox.
                menuItem.isChecked = cookieManager.acceptCookie()

                // Update the privacy icon.
                updatePrivacyIcons(true)

                // Display a snackbar.
                if (cookieManager.acceptCookie())  // Cookies are enabled.
                    Snackbar.make(webViewViewPager2, R.string.cookies_enabled, Snackbar.LENGTH_SHORT).show()
                else if (currentWebView!!.settings.javaScriptEnabled)  // JavaScript is still enabled.
                    Snackbar.make(webViewViewPager2, R.string.cookies_disabled, Snackbar.LENGTH_SHORT).show()
                else  // Privacy mode.
                    Snackbar.make(webViewViewPager2, R.string.privacy_mode, Snackbar.LENGTH_SHORT).show()

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.dom_storage -> {  // DOM storage.
                // Toggle the DOM storage status.
                currentWebView!!.settings.domStorageEnabled = !currentWebView!!.settings.domStorageEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.settings.domStorageEnabled

                // Update the privacy icon.
                updatePrivacyIcons(true)

                // Display a snackbar.
                if (currentWebView!!.settings.domStorageEnabled)
                    Snackbar.make(webViewViewPager2, R.string.dom_storage_enabled, Snackbar.LENGTH_SHORT).show()
                else
                    Snackbar.make(webViewViewPager2, R.string.dom_storage_disabled, Snackbar.LENGTH_SHORT).show()

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.clear_cookies -> {  // Clear cookies.
                // Create a snackbar.
                Snackbar.make(webViewViewPager2, R.string.cookies_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {}  // Everything will be handled by `onDismissed()` below.
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(snackbar: Snackbar, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {  // The snackbar was dismissed without the undo button being pushed.
                                // Delete the cookies.
                                cookieManager.removeAllCookies(null)
                            }
                        }
                    })
                    .show()

                // Consume the event.
                true
            }

            R.id.clear_dom_storage -> {  // Clear DOM storage.
                // Create a snackbar.
                Snackbar.make(webViewViewPager2, R.string.dom_storage_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {}  // Everything will be handled by `onDismissed()` below.
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(snackbar: Snackbar, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {  // The snackbar was dismissed without the undo button being pushed.
                                // Ask web storage to clear the DOM storage.
                                WebStorage.getInstance().deleteAllData()

                                // Initialize a handler to manually delete the DOM storage files and directories.
                                val deleteDomStorageHandler = Handler(Looper.getMainLooper())

                                // Setup a runnable to manually delete the DOM storage files and directories.
                                val deleteDomStorageRunnable = Runnable {
                                    try {
                                        // Get a handle for the runtime.
                                        val runtime = Runtime.getRuntime()

                                        // Get the application's private data directory, which will be something like `/data/user/0/com.audeon.browser.standard`,
                                        // which links to `/data/data/com.audeon.browser.standard`.
                                        val privateDataDirectoryString = applicationInfo.dataDir

                                        // A string array must be used because the directory contains a space and `Runtime.exec` will otherwise not escape the string correctly.
                                        val deleteLocalStorageProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Local Storage/"))
                                        val deleteSessionStorageProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Session Storage/"))
                                        val deleteWebDataProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Web Data"))
                                        val deleteWebDataJournalProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Web Data-journal"))

                                        // Multiple commands must be used because `Runtime.exec()` does not like `*`.
                                        val deleteIndexProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/IndexedDB")
                                        val deleteWebStorageProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/WebStorage")
                                        val deleteDatabasesProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/databases")
                                        val deleteBlobStorageProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/blob_storage")

                                        // Wait for the processes to finish.
                                        deleteLocalStorageProcess.waitFor()
                                        deleteSessionStorageProcess.waitFor()
                                        deleteWebDataProcess.waitFor()
                                        deleteWebDataJournalProcess.waitFor()
                                        deleteIndexProcess.waitFor()
                                        deleteWebStorageProcess.waitFor()
                                        deleteDatabasesProcess.waitFor()
                                        deleteBlobStorageProcess.waitFor()
                                    } catch (exception: Exception) {
                                        // Do nothing if an error is thrown.
                                    }
                                }

                                // Manually delete the DOM storage files after 200 milliseconds.
                                deleteDomStorageHandler.postDelayed(deleteDomStorageRunnable, 200)
                            }
                        }
                    })
                    .show()

                // Consume the event.
                true
            }

            R.id.easylist -> {  // EasyList.
                // Toggle the EasyList status.
                currentWebView!!.easyListEnabled = !currentWebView!!.easyListEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.easyListEnabled

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.easyprivacy -> {  // EasyPrivacy.
                // Toggle the EasyPrivacy status.
                currentWebView!!.easyPrivacyEnabled = !currentWebView!!.easyPrivacyEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.easyPrivacyEnabled

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.fanboys_annoyance_list -> {  // Fanboy's Annoyance List.
                // Toggle Fanboy's Annoyance List status.
                currentWebView!!.fanboysAnnoyanceListEnabled = !currentWebView!!.fanboysAnnoyanceListEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.fanboysAnnoyanceListEnabled

                // Update the status of Fanboy's Social Blocking List.
                optionsFanboysSocialBlockingListMenuItem.isEnabled = !currentWebView!!.fanboysAnnoyanceListEnabled

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.fanboys_social_blocking_list -> {  // Fanboy's Social Blocking List.
                // Toggle Fanboy's Social Blocking List status.
                currentWebView!!.fanboysSocialBlockingListEnabled = !currentWebView!!.fanboysSocialBlockingListEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.fanboysSocialBlockingListEnabled

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.ultralist -> {  // UltraList.
                // Toggle the UltraList status.
                currentWebView!!.ultraListEnabled = !currentWebView!!.ultraListEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.ultraListEnabled

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.ultraprivacy -> {  // UltraPrivacy.
                // Toggle the UltraPrivacy status.
                currentWebView!!.ultraPrivacyEnabled = !currentWebView!!.ultraPrivacyEnabled

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.ultraPrivacyEnabled

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.block_all_third_party_requests -> {  // Block all third-party requests.
                //Toggle the third-party requests blocker status.
                currentWebView!!.blockAllThirdPartyRequests = !currentWebView!!.blockAllThirdPartyRequests

                // Update the menu checkbox.
                menuItem.isChecked = currentWebView!!.blockAllThirdPartyRequests

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.proxy_none -> {  // Proxy - None.
                // Update the proxy mode.
                proxyMode = ProxyHelper.NONE

                // Apply the proxy mode.
                applyProxy(true)

                // Consume the event.
                true
            }

            R.id.proxy_tor -> {  // Proxy - Tor.
                // Update the proxy mode.
                proxyMode = ProxyHelper.TOR

                // Apply the proxy mode.
                applyProxy(true)

                // Consume the event.
                true
            }

            R.id.proxy_i2p -> {  // Proxy - I2P.
                // Update the proxy mode.
                proxyMode = ProxyHelper.I2P

                // Apply the proxy mode.
                applyProxy(true)

                // Consume the event.
                true
            }

            R.id.proxy_custom -> {  // Proxy - Custom.
                // Update the proxy mode.
                proxyMode = ProxyHelper.CUSTOM

                // Apply the proxy mode.
                applyProxy(true)

                // Consume the event.
                true
            }

            R.id.user_agent_audeon_browser -> {  // User Agent - Privacy Browser.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[0]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_webview_default -> {  // User Agent - WebView Default.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = ""

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_firefox_on_android -> {  // User Agent - Firefox on Android.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[2]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_chrome_on_android -> {  // User Agent - Chrome on Android.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[3]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_safari_on_ios -> {  // User Agent - Safari on iOS.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[4]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_firefox_on_linux -> {  // User Agent - Firefox on Linux.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[5]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_chromium_on_linux -> {  // User Agent - Chromium on Linux.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[6]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_firefox_on_windows -> {  // User Agent - Firefox on Windows.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[7]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_chrome_on_windows -> {  // User Agent - Chrome on Windows.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[8]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_edge_on_windows -> {  // User Agent - Edge on Windows.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[9]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_safari_on_macos -> {  // User Agent - Safari on macOS.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = resources.getStringArray(R.array.user_agent_data)[10]

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.user_agent_custom -> {  // User Agent - Custom.
                // Update the user agent.
                currentWebView!!.settings.userAgentString = sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value))

                // Reload the current WebView.
                currentWebView!!.reload()

                // Consume the event.
                true
            }

            R.id.font_size -> {  // Font size.
                // Instantiate the font size dialog.
                val fontSizeDialogFragment: DialogFragment = FontSizeDialog.displayDialog(currentWebView!!.settings.textZoom)

                // Show the font size dialog.
                fontSizeDialogFragment.show(supportFragmentManager, getString(R.string.font_size))

                // Consume the event.
                true
            }

            R.id.swipe_to_refresh -> {  // Swipe to refresh.
                // Toggle the stored status of swipe to refresh.
                currentWebView!!.swipeToRefresh = !currentWebView!!.swipeToRefresh

                // Update the swipe refresh layout.
                if (currentWebView!!.swipeToRefresh)  // Swipe to refresh is enabled.  Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                    swipeRefreshLayout.isEnabled = currentWebView!!.scrollY == 0
                else  // Swipe to refresh is disabled.
                    swipeRefreshLayout.isEnabled = false

                // Consume the event.
                true
            }

            R.id.wide_viewport -> {  // Wide viewport.
                // Toggle the viewport.
                currentWebView!!.settings.useWideViewPort = !currentWebView!!.settings.useWideViewPort

                // Consume the event.
                true
            }

            R.id.display_images -> {  // Display images.
                // Toggle the displaying of images.
                if (currentWebView!!.settings.loadsImagesAutomatically) {  // Images are currently loaded automatically.
                    // Disable loading of images.
                    currentWebView!!.settings.loadsImagesAutomatically = false

                    // Reload the website to remove existing images.
                    currentWebView!!.reload()
                } else {  // Images are not currently loaded automatically.
                    // Enable loading of images.  Missing images will be loaded without the need for a reload.
                    currentWebView!!.settings.loadsImagesAutomatically = true
                }

                // Consume the event.
                true
            }

            R.id.dark_webview -> {  // Dark WebView.
                // Toggle dark WebView if supported.
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(currentWebView!!.settings, !WebSettingsCompat.isAlgorithmicDarkeningAllowed(currentWebView!!.settings)
                )

                // Consume the event.
                true
            }

            R.id.find_on_page -> {  // Find on page.
                // Set the minimum height of the find on page linear layout to match the toolbar.
                findOnPageLinearLayout.minimumHeight = toolbar.height

                // Hide the toolbar.
                toolbar.visibility = View.GONE

                // Show the find on page linear layout.
                findOnPageLinearLayout.visibility = View.VISIBLE

                // Display the keyboard.  The app must wait 200 ms before running the command to work around a bug in Android.
                // http://stackoverflow.com/questions/5520085/android-show-softkeyboard-with-showsoftinput-is-not-working
                findOnPageEditText.postDelayed({
                    // Set the focus on the find on page edit text.
                    findOnPageEditText.requestFocus()

                    // Display the keyboard.  `0` sets no input flags.
                    inputMethodManager.showSoftInput(findOnPageEditText, 0)
                }, 200)

                // Consume the event.
                true
            }

            R.id.print -> {  // Print.
                // Get a print manager instance.
                val printManager = (getSystemService(PRINT_SERVICE) as PrintManager)

                // Create a print document adapter from the current WebView.
                val printDocumentAdapter = currentWebView!!.createPrintDocumentAdapter(getString(R.string.print))

                // Print the document.
                printManager.print(getString(R.string.audeon_browser_webpage), printDocumentAdapter, null)

                // Consume the event.
                true
            }

            R.id.save_url -> {  // Save URL.
                // Check the download preference.
                if (downloadWithExternalApp)  // Download with an external app.
                    saveWithExternalApp(currentWebView!!.currentUrl)
                else  // Handle the download inside of Privacy Browser.  The dialog will be displayed once the file size and the content disposition have been acquired.
                    PrepareSaveDialogCoroutine.prepareSaveDialog(this, supportFragmentManager, currentWebView!!.currentUrl, currentWebView!!.settings.userAgentString, currentWebView!!.acceptCookies)

                // Consume the event.
                true
            }

            R.id.save_archive -> {
                // Open the file picker with a default file name built from the website title.
                saveWebpageArchiveActivityResultLauncher.launch(currentWebView!!.title + ".mht")

                // Consume the event.
                true
            }

            R.id.save_image -> {  // Save image.
                // Open the file picker with a default file name built from the current domain name.
                saveWebpageImageActivityResultLauncher.launch(currentWebView!!.currentDomainName + ".png")

                // Consume the event.
                true
            }

            R.id.add_to_homescreen -> {  // Add to homescreen.
                // Instantiate the create home screen shortcut dialog.
                val createHomeScreenShortcutDialogFragment: DialogFragment = CreateHomeScreenShortcutDialog.createDialog(currentWebView!!.title!!, currentWebView!!.url!!, currentWebView!!.getFavoriteIcon())

                // Show the create home screen shortcut dialog.
                createHomeScreenShortcutDialogFragment.show(supportFragmentManager, getString(R.string.create_shortcut))

                // Consume the event.
                true
            }

            R.id.view_source -> {  // View source.
                // Open a new tab according to the current URL.
                if (currentWebView!!.currentUrl.startsWith("view-source:")) {  // The source is currently viewed.
                    // Open the rendered website in a new tab.
                    addNewPage(currentWebView!!.currentUrl.substring(12, currentWebView!!.currentUrl.length), true, moveToTab = true)
                } else {  // The rendered website is currently viewed.
                    // Open the source in a new tab.
                    addNewPage("view-source:${currentWebView!!.currentUrl}", adjacent = true, moveToTab = true)
                }

                // Consume the event.
                true
            }

            R.id.view_headers -> {  // View headers.
                // Create an intent to launch the view headers activity.
                val viewHeadersIntent = Intent(this, ViewHeadersActivity::class.java)

                // Add the variables to the intent.
                viewHeadersIntent.putExtra(CURRENT_URL, currentWebView!!.url)
                viewHeadersIntent.putExtra(USER_AGENT, currentWebView!!.settings.userAgentString)

                // Make it so.
                startActivity(viewHeadersIntent)

                // Consume the event.
                true
            }

            R.id.share_message -> {  // Share a message.
                // Prepare the share string.
                val shareString = currentWebView!!.title + " – " + currentWebView!!.url

                // Create the share intent.
                val shareMessageIntent = Intent(Intent.ACTION_SEND)

                // Add the share string to the intent.
                shareMessageIntent.putExtra(Intent.EXTRA_TEXT, shareString)

                // Set the MIME type.
                shareMessageIntent.type = "text/plain"

                // Set the intent to open in a new task.
                shareMessageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Make it so.
                startActivity(Intent.createChooser(shareMessageIntent, getString(R.string.share_message)))

                // Consume the event.
                true
            }

            R.id.share_url -> {  // Share URL.
                // Create the share intent.
                val shareUrlIntent = Intent(Intent.ACTION_SEND)

                // Add the URL to the intent.
                shareUrlIntent.putExtra(Intent.EXTRA_TEXT, currentWebView!!.url)

                // Add the title to the intent.
                shareUrlIntent.putExtra(Intent.EXTRA_SUBJECT, currentWebView!!.title)

                // Set the MIME type.
                shareUrlIntent.type = "text/plain"

                // Set the intent to open in a new task.
                shareUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                //Make it so.
                startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.share_url)))

                // Consume the event.
                true
            }

            R.id.open_with_app -> {  // Open with app.
                // Open the URL with an outside app.
                openWithApp(currentWebView!!.url!!)

                // Consume the event.
                true
            }

            R.id.open_with_browser -> {  // Open with browser.
                // Open the URL with an outside browser.
                openWithBrowser(currentWebView!!.url!!)

                // Consume the event.
                true
            }

            R.id.add_or_edit_domain -> {  // Add or edit domain.
                // Reapply the domain settings on returning to the main WebView activity.
                reapplyDomainSettingsOnRestart = true

                // Check if domain settings are currently applied.
                if (currentWebView!!.domainSettingsApplied) {  // Edit the current domain settings.
                    // Create an intent to launch the domains activity.
                    val domainsIntent = Intent(this, DomainsActivity::class.java)

                    // Add the extra information to the intent.
                    domainsIntent.putExtra(LOAD_DOMAIN, currentWebView!!.domainSettingsDatabaseId)
                    domainsIntent.putExtra(CLOSE_ON_BACK, true)
                    domainsIntent.putExtra(CURRENT_IP_ADDRESSES, currentWebView!!.currentIpAddresses)

                    // Get the current certificate.
                    val sslCertificate = currentWebView!!.certificate

                    // Check to see if the SSL certificate is populated.
                    if (sslCertificate != null) {
                        // Extract the certificate to strings.
                        val issuedToCName = sslCertificate.issuedTo.cName
                        val issuedToOName = sslCertificate.issuedTo.oName
                        val issuedToUName = sslCertificate.issuedTo.uName
                        val issuedByCName = sslCertificate.issuedBy.cName
                        val issuedByOName = sslCertificate.issuedBy.oName
                        val issuedByUName = sslCertificate.issuedBy.uName
                        val startDateLong = sslCertificate.validNotBeforeDate.time
                        val endDateLong = sslCertificate.validNotAfterDate.time

                        // Add the certificate to the intent.
                        domainsIntent.putExtra(SSL_ISSUED_TO_CNAME, issuedToCName)
                        domainsIntent.putExtra(SSL_ISSUED_TO_ONAME, issuedToOName)
                        domainsIntent.putExtra(SSL_ISSUED_TO_UNAME, issuedToUName)
                        domainsIntent.putExtra(SSL_ISSUED_BY_CNAME, issuedByCName)
                        domainsIntent.putExtra(SSL_ISSUED_BY_ONAME, issuedByOName)
                        domainsIntent.putExtra(SSL_ISSUED_BY_UNAME, issuedByUName)
                        domainsIntent.putExtra(SSL_START_DATE, startDateLong)
                        domainsIntent.putExtra(SSL_END_DATE, endDateLong)
                    }

                    // Make it so.
                    startActivity(domainsIntent)
                } else {  // Add a new domain.
                    // Get the current URI.
                    val currentUri = currentWebView!!.url!!.toUri()

                    // Get the current domain from the URI.  Use an empty string if it is null.
                    val currentDomain = currentUri.host?: ""

                    // Get the current settings status.
                    val javaScriptInt = calculateSettingsInt(currentWebView!!.settings.javaScriptEnabled, sharedPreferences.getBoolean(getString(R.string.javascript_key), false))
                    val cookiesInt = calculateSettingsInt(currentWebView!!.acceptCookies, sharedPreferences.getBoolean(getString(R.string.cookies_key), false))
                    val domStorageInt = calculateSettingsInt(currentWebView!!.settings.domStorageEnabled, sharedPreferences.getBoolean(getString(R.string.dom_storage_key), false))
                    val easyListInt = calculateSettingsInt(currentWebView!!.easyListEnabled, sharedPreferences.getBoolean(getString(R.string.easylist_key), true))
                    val easyPrivacyInt = calculateSettingsInt(currentWebView!!.easyPrivacyEnabled, sharedPreferences.getBoolean(getString(R.string.easyprivacy_key), true))
                    val fanboysAnnoyanceListInt = calculateSettingsInt(currentWebView!!.fanboysAnnoyanceListEnabled, sharedPreferences.getBoolean(getString(R.string.fanboys_annoyance_list_key), true))
                    val fanboysSocialBlockingListInt = calculateSettingsInt(currentWebView!!.fanboysSocialBlockingListEnabled, sharedPreferences.getBoolean(getString(R.string.fanboys_social_blocking_list_key), true))
                    val ultraListInt = calculateSettingsInt(currentWebView!!.ultraListEnabled, sharedPreferences.getBoolean(getString(R.string.ultralist_key), true))
                    val ultraPrivacyInt = calculateSettingsInt(currentWebView!!.ultraPrivacyEnabled, sharedPreferences.getBoolean(getString(R.string.ultraprivacy_key), true))
                    val blockAllThirdPartyRequestsInt = calculateSettingsInt(currentWebView!!.blockAllThirdPartyRequests, sharedPreferences.getBoolean(getString(R.string.block_all_third_party_requests_key), true))
                    val swipeToRefreshInt = calculateSettingsInt(currentWebView!!.swipeToRefresh, sharedPreferences.getBoolean(getString(R.string.swipe_to_refresh_key), true))
                    val wideViewportInt = calculateSettingsInt(currentWebView!!.settings.useWideViewPort, sharedPreferences.getBoolean(getString(R.string.wide_viewport_key), true))
                    val displayImagesInt = calculateSettingsInt(currentWebView!!.settings.loadsImagesAutomatically, sharedPreferences.getBoolean(getString(R.string.display_webpage_images_key), true))

                    // Get the current user agent string.
                    val currentUserAgentString = currentWebView!!.settings.userAgentString

                    // Get the user agent string array position.
                    val userAgentStringArrayPosition = userAgentDataArrayAdapter.getPosition(currentUserAgentString)

                    // Set the user agent name.
                    val userAgentName = if ((userAgentStringArrayPosition >= 0) && (defaultUserAgentName == userAgentNamesArray[userAgentStringArrayPosition])) {  // The system default user agent is in use.
                        getString(R.string.system_default_user_agent)
                    } else {  // An on-the-fly user agent is being used (or the WebView default user agent is applied).
                        when (userAgentStringArrayPosition) {
                            UNRECOGNIZED_USER_AGENT -> { // The user agent is unrecognized.
                                if (currentUserAgentString == webViewDefaultUserAgent) {  // The WebView default user agent is being used.
                                    if (defaultUserAgentName == getString(R.string.webview_default)) {  // The WebView default user agent is the system default.
                                        // Set the user agent name to be the system default.
                                        getString(R.string.system_default_user_agent)
                                    } else {  // The WebView default user agent is set as an on-the-fly setting.
                                        // Set the default user agent name.
                                        getString(R.string.webview_default)
                                    }
                                } else {  // A custom user agent is being used.
                                    if (defaultUserAgentName == getString(R.string.custom_user_agent_non_translatable)) {  // The system custom user agent is in use.
                                        // Set the user agent name to be the system default.
                                        getString(R.string.system_default_user_agent)
                                    } else {  // An on-the-fly custom user agent is in use.
                                        // Store the user agent as currently applied.
                                        currentUserAgentString
                                    }
                                }
                            }

                            else ->  // Store the standard user agent name.
                                userAgentNamesArray[userAgentStringArrayPosition]
                        }
                    }

                    // Get the current text zoom integer.
                    val textZoomInt = currentWebView!!.settings.textZoom

                    // Set the font size integer.
                    val fontSizeInt = if (textZoomInt == defaultFontSizeString.toInt())  // The current system default is used, which is encoded as a zoom of `0`.
                        SYSTEM_DEFAULT
                    else  // A custom font size is used.
                        textZoomInt

                    // Get the current WebView dark theme status.
                    val webViewDarkThemeCurrentlyEnabled = if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))  // Algorithmic darkening is supported.
                        WebSettingsCompat.isAlgorithmicDarkeningAllowed(currentWebView!!.settings)
                    else  // Algorithmic darkening is not supported.
                        false

                    // Get the default WebView dark theme setting.
                    val defaultWebViewDarkThemeEnabled = if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {  // Algorithmic darkening is supported.
                        when (defaultWebViewTheme) {
                            webViewThemeEntryValuesStringArray[1] ->  // The dark theme is disabled by default.
                                false

                            webViewThemeEntryValuesStringArray[2] ->  // The dark theme is enabled by default.
                                true

                            else -> {  // The system default theme is selected.
                                // Get the current app theme status.
                                val currentAppThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                                // Check if the current app theme is dark.
                                currentAppThemeStatus == Configuration.UI_MODE_NIGHT_YES
                            }
                        }
                    } else {  // Algorithmic darkening is not supported.
                        false
                    }

                    // Set the WebView theme int.
                    val webViewThemeInt = if (webViewDarkThemeCurrentlyEnabled == defaultWebViewDarkThemeEnabled)  // The current WebView theme matches the default.
                        SYSTEM_DEFAULT
                    else if (webViewDarkThemeCurrentlyEnabled)  // The current WebView theme is dark and that is not the default.
                        DARK_THEME
                    else  // The current WebView theme is light and that is not the default.
                        LIGHT_THEME

                    // Create the domain and store the database ID.
                    val newDomainDatabaseId = domainsDatabaseHelper!!.addDomain(currentDomain, javaScriptInt, cookiesInt, domStorageInt, userAgentName, easyListInt, easyPrivacyInt,
                                                                                fanboysAnnoyanceListInt, fanboysSocialBlockingListInt, ultraListInt, ultraPrivacyInt, blockAllThirdPartyRequestsInt, fontSizeInt,
                                                                                swipeToRefreshInt, webViewThemeInt, wideViewportInt, displayImagesInt)

                    // Create an intent to launch the domains activity.
                    val domainsIntent = Intent(this, DomainsActivity::class.java)

                    // Add the extra information to the intent.
                    domainsIntent.putExtra(LOAD_DOMAIN, newDomainDatabaseId)
                    domainsIntent.putExtra(CLOSE_ON_BACK, true)
                    domainsIntent.putExtra(CURRENT_IP_ADDRESSES, currentWebView!!.currentIpAddresses)

                    // Get the current certificate.
                    val sslCertificate = currentWebView!!.certificate

                    // Check to see if the SSL certificate is populated.
                    if (sslCertificate != null) {
                        // Extract the certificate to strings.
                        val issuedToCName = sslCertificate.issuedTo.cName
                        val issuedToOName = sslCertificate.issuedTo.oName
                        val issuedToUName = sslCertificate.issuedTo.uName
                        val issuedByCName = sslCertificate.issuedBy.cName
                        val issuedByOName = sslCertificate.issuedBy.oName
                        val issuedByUName = sslCertificate.issuedBy.uName
                        val startDateLong = sslCertificate.validNotBeforeDate.time
                        val endDateLong = sslCertificate.validNotAfterDate.time

                        // Add the certificate to the intent.
                        domainsIntent.putExtra(SSL_ISSUED_TO_CNAME, issuedToCName)
                        domainsIntent.putExtra(SSL_ISSUED_TO_ONAME, issuedToOName)
                        domainsIntent.putExtra(SSL_ISSUED_TO_UNAME, issuedToUName)
                        domainsIntent.putExtra(SSL_ISSUED_BY_CNAME, issuedByCName)
                        domainsIntent.putExtra(SSL_ISSUED_BY_ONAME, issuedByOName)
                        domainsIntent.putExtra(SSL_ISSUED_BY_UNAME, issuedByUName)
                        domainsIntent.putExtra(SSL_START_DATE, startDateLong)
                        domainsIntent.putExtra(SSL_END_DATE, endDateLong)
                    }

                    // Make it so.
                    startActivity(domainsIntent)
                }

                // Consume the event.
                true
            }

            else -> {  // There is no match with the options menu.  Pass the event up to the parent method.
                // Don't consume the event.
                super.onOptionsItemSelected(menuItem)
            }
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        // Run the commands that correspond to the selected menu item.
        when (menuItem.itemId) {
            R.id.clear_and_exit -> {  // Clear and exit.
                // Clear and exit Privacy Browser.
                clearAndExit()
            }

            R.id.home -> {  // Home.
                // Load the homepage.
                loadUrl(currentWebView!!, sharedPreferences.getString(getString(R.string.homepage_key), getString(R.string.homepage_default_value))!!)
            }

            R.id.back -> {  // Back.
                // Check if the WebView can go back.
                if (currentWebView!!.canGoBack()) {
                    // Navigate back one page.
                    navigateHistory(-1)
                }
            }

            R.id.forward -> {  // Forward.
                // Check if the WebView can go forward.
                if (currentWebView!!.canGoForward()) {
                    // Navigate forward one page.
                    navigateHistory(+1)
                }
            }

            R.id.scroll_to_bottom -> {  // Scroll to Bottom.
                // Check if the WebView is scrolled to the top.
                if (currentWebView!!.scrollY == 0) {  // The WebView is at the top; scroll to the bottom.  Using a large Y number is more efficient than trying to calculate the exact WebView length.
                    currentWebView!!.scrollTo(0, 1_000_000_000)
                } else {  // The WebView is not at the top; scroll to the top.
                    currentWebView!!.scrollTo(0, 0)
                }
            }

            R.id.history -> {  // History.
                // Instantiate the URL history dialog.
                val urlHistoryDialogFragment: DialogFragment = UrlHistoryDialog.loadBackForwardList(currentWebView!!.webViewFragmentId)

                // Show the URL history dialog.
                urlHistoryDialogFragment.show(supportFragmentManager, getString(R.string.history))
            }

            R.id.open -> {  // Open.
                // Instantiate the open file dialog.
                val openDialogFragment: DialogFragment = OpenDialog()

                // Show the open file dialog.
                openDialogFragment.show(supportFragmentManager, getString(R.string.open))
            }

            R.id.requests -> {  // Requests.
                // Populate the resource requests.
                RequestsActivity.resourceRequests = currentWebView!!.getResourceRequests()

                // Create an intent to launch the Requests activity.
                val requestsIntent = Intent(this, RequestsActivity::class.java)

                // Add the block third-party requests status to the intent.
                requestsIntent.putExtra(BLOCK_ALL_THIRD_PARTY_REQUESTS, currentWebView!!.blockAllThirdPartyRequests)

                // Make it so.
                startActivity(requestsIntent)
            }

            R.id.downloads -> {  // Downloads.
                // Try the default system download manager.
                try {
                    // Launch the default system Download Manager.
                    val defaultDownloadManagerIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)

                    // Launch as a new task so that the download manager and Privacy Browser show as separate windows in the recent tasks list.
                    defaultDownloadManagerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    // Make it so.
                    startActivity(defaultDownloadManagerIntent)
                } catch (defaultDownloadManagerException: Exception) {  // The system download manager is not available.
                    // Try a generic file manager.
                    try {
                        // Create a generic file manager intent.
                        val genericFileManagerIntent = Intent(Intent.ACTION_VIEW)

                        // Open the download directory.
                        genericFileManagerIntent.setDataAndType(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString().toUri(), DocumentsContract.Document.MIME_TYPE_DIR)

                        // Launch as a new task so that the file manager and Privacy Browser show as separate windows in the recent tasks list.
                        genericFileManagerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        // Make it so.
                        startActivity(genericFileManagerIntent)
                    } catch (genericFileManagerException: Exception) {  // A generic file manager is not available.
                        // Try an alternate file manager.
                        try {
                            // Create an alternate file manager intent.
                            val alternateFileManagerIntent = Intent(Intent.ACTION_VIEW)

                            // Open the download directory.
                            alternateFileManagerIntent.setDataAndType(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString().toUri(), "resource/folder")

                            // Launch as a new task so that the file manager and Privacy Browser show as separate windows in the recent tasks list.
                            alternateFileManagerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                            // Open the alternate file manager.
                            startActivity(alternateFileManagerIntent)
                        } catch (alternateFileManagerException: Exception) {
                            // Display a snackbar.
                            Snackbar.make(currentWebView!!, R.string.no_file_manager_detected, Snackbar.LENGTH_INDEFINITE).show()
                        }
                    }
                }
            }

            R.id.domains -> {  // Domains.
                // Set the flag to reapply the domain settings on restart when returning from Domain Settings.
                reapplyDomainSettingsOnRestart = true

                // Create a domains activity intent.
                val domainsIntent = Intent(this, DomainsActivity::class.java)

                // Add the extra information to the intent.
                domainsIntent.putExtra(CURRENT_IP_ADDRESSES, currentWebView!!.currentIpAddresses)

                // Get the current certificate.
                val sslCertificate = currentWebView!!.certificate

                // Check to see if the SSL certificate is populated.
                if (sslCertificate != null) {
                    // Extract the certificate to strings.
                    val issuedToCName = sslCertificate.issuedTo.cName
                    val issuedToOName = sslCertificate.issuedTo.oName
                    val issuedToUName = sslCertificate.issuedTo.uName
                    val issuedByCName = sslCertificate.issuedBy.cName
                    val issuedByOName = sslCertificate.issuedBy.oName
                    val issuedByUName = sslCertificate.issuedBy.uName
                    val startDateLong = sslCertificate.validNotBeforeDate.time
                    val endDateLong = sslCertificate.validNotAfterDate.time

                    // Add the certificate to the intent.
                    domainsIntent.putExtra(SSL_ISSUED_TO_CNAME, issuedToCName)
                    domainsIntent.putExtra(SSL_ISSUED_TO_ONAME, issuedToOName)
                    domainsIntent.putExtra(SSL_ISSUED_TO_UNAME, issuedToUName)
                    domainsIntent.putExtra(SSL_ISSUED_BY_CNAME, issuedByCName)
                    domainsIntent.putExtra(SSL_ISSUED_BY_ONAME, issuedByOName)
                    domainsIntent.putExtra(SSL_ISSUED_BY_UNAME, issuedByUName)
                    domainsIntent.putExtra(SSL_START_DATE, startDateLong)
                    domainsIntent.putExtra(SSL_END_DATE, endDateLong)
                }

                // Make it so.
                startActivity(domainsIntent)
            }

            R.id.settings -> {  // Settings.
                // Set the reapply on restart flags.
                reapplyAppSettingsOnRestart = true
                reapplyDomainSettingsOnRestart = true

                // Create a settings intent.
                val settingsIntent = Intent(this, SettingsActivity::class.java)

                // Make it so.
                startActivity(settingsIntent)
            }

            R.id.import_export -> { // Import/Export.
                // Create an intent to launch the import/export activity.
                val importExportIntent = Intent(this, ImportExportActivity::class.java)

                // Make it so.
                startActivity(importExportIntent)
            }

            R.id.logcat -> {  // Logcat.
                // Create an intent to launch the logcat activity.
                val logcatIntent = Intent(this, LogcatActivity::class.java)

                // Make it so.
                startActivity(logcatIntent)
            }

            R.id.webview_devtools -> {  // WebView DevTools.
                // Create a WebView DevTools intent.
                val webViewDevToolsIntent = Intent("com.android.webview.SHOW_DEV_UI")

                // Launch as a new task so that the WebView DevTools and Privacy Browser show as a separate windows in the recent tasks list.
                webViewDevToolsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                // Make it so.
                startActivity(webViewDevToolsIntent)
            }

            R.id.about -> {  // About
                // Create an intent to launch the about activity.
                val aboutIntent = Intent(this, AboutActivity::class.java)

                // Create a string array for the filter list versions.
                val filterListVersions = arrayOf(easyList[0][0][0], easyPrivacy[0][0][0], fanboysAnnoyanceList[0][0][0], fanboysSocialList[0][0][0], ultraList[0][0][0], ultraPrivacy!![0][0][0])

                // Add the filter list versions to the intent.
                aboutIntent.putExtra(FILTERLIST_VERSIONS, filterListVersions)

                // Make it so.
                startActivity(aboutIntent)
            }
        }

        // Close the navigation drawer.
        drawerLayout.closeDrawer(GravityCompat.START)

        // Return true.
        return true
    }

    override fun onCreateContextMenu(contextMenu: ContextMenu, view: View, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        // Get the hit test result.
        val hitTestResult = currentWebView!!.hitTestResult

        // Define the URL strings.
        val imageUrl: String?
        val linkUrl: String?

        // Get a handle for the clipboard manager.
        val clipboardManager = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)

        // Process the link according to the type.
        when (hitTestResult.type) {
            // `SRC_ANCHOR_TYPE` is a link.
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                // Get the target URL.
                linkUrl = hitTestResult.extra!!

                // Set the target URL as the context menu title.
                contextMenu.setHeaderTitle(linkUrl)

                // Get a new message from the WebView handler.
                val hrefMessage = currentWebView!!.handler.obtainMessage()

                // Request the focus node href.
                currentWebView!!.requestFocusNodeHref(hrefMessage)

                // Get the link text from the href message.
                val linkText = hrefMessage.data.getString("title")

                // Add an open in new tab entry.
                contextMenu.add(R.string.open_in_new_tab).setOnMenuItemClickListener {
                    // Load the link URL in a new tab and move to it.
                    addNewPage(linkUrl, adjacent = true, moveToTab = true)

                    // Consume the event.
                    true
                }

                // Add an open in background entry.
                contextMenu.add(R.string.open_in_background).setOnMenuItemClickListener {
                    // Load the link URL in a new tab but do not move to it.
                    addNewPage(linkUrl, adjacent = true, moveToTab = false)

                    // Consume the event.
                    true
                }

                // Add an open with app entry.
                contextMenu.add(R.string.open_with_app).setOnMenuItemClickListener {
                    // Open the URL with another app.
                    openWithApp(linkUrl)

                    // Consume the event.
                    true
                }

                // Add an open with browser entry.
                contextMenu.add(R.string.open_with_browser).setOnMenuItemClickListener {
                    // Open the URL with another browser.
                    openWithBrowser(linkUrl)

                    // Consume the event.
                    true
                }

                // Add a copy URL entry.
                contextMenu.add(R.string.copy_url).setOnMenuItemClickListener {
                    // Save the link URL in a clip data.
                    val srcAnchorTypeClipData = ClipData.newPlainText(getString(R.string.url), linkUrl)

                    // Set the clip data as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(srcAnchorTypeClipData)

                    // Consume the event.
                    true
                }

                // Add a Save URL entry.
                contextMenu.add(R.string.save_url).setOnMenuItemClickListener {
                    // Check the download preference.
                    if (downloadWithExternalApp)  // Download with an external app.
                        saveWithExternalApp(linkUrl)
                    else  // Handle the download inside of Privacy Browser.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, supportFragmentManager, linkUrl, currentWebView!!.settings.userAgentString, currentWebView!!.acceptCookies)

                    // Consume the event.
                    true
                }

                // Add a Share URL entry.
                contextMenu.add(R.string.share_url).setOnMenuItemClickListener {
                    // Create the share intent.
                    val shareUrlIntent = Intent(Intent.ACTION_SEND)

                    // Add the URL to the intent.
                    shareUrlIntent.putExtra(Intent.EXTRA_TEXT, linkUrl)

                    // Set the MIME type.
                    shareUrlIntent.type = "text/plain"

                    // Set the intent to open in a new task.
                    shareUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    //Make it so.
                    startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.share_url)))

                    // Consume the event.
                    true
                }

                // Add a Copy Text entry if the link text is not null.
                if (linkText != null) {
                    contextMenu.add(R.string.copy_text).setOnMenuItemClickListener {
                        // Save the link URL in a clip data.
                        val srcAnchorTypeTextClipData = ClipData.newPlainText(getString(R.string.copy_text), linkText)

                        // Set the clip data as the clipboard's primary clip.
                        clipboardManager.setPrimaryClip(srcAnchorTypeTextClipData)

                        // Consume the event.
                        true
                    }
                }

                // Add an empty cancel entry, which by default closes the context menu.
                contextMenu.add(R.string.cancel)
            }

            // `IMAGE_TYPE` is an image.
            WebView.HitTestResult.IMAGE_TYPE -> {
                // Get the image URL.
                imageUrl = hitTestResult.extra!!

                // Set the context menu title.
                if (imageUrl.startsWith("data:"))  // The image data is contained in within the URL, making it exceedingly long.  Truncate the image URL before making it the title.
                    contextMenu.setHeaderTitle(imageUrl.substring(0, 100))
                else  // The image URL does not contain the full image data.  Set the image URL as the title of the context menu.
                    contextMenu.setHeaderTitle(imageUrl)

                // Add an open in new tab entry.
                contextMenu.add(R.string.open_image_in_new_tab).setOnMenuItemClickListener {
                    // Load the image in a new tab.
                    addNewPage(imageUrl, adjacent = true, moveToTab = true)

                    // Consume the event.
                    true
                }

                // Add an open with app entry.
                contextMenu.add(R.string.open_with_app).setOnMenuItemClickListener {
                    // Open the image URL with an external app.
                    openWithApp(imageUrl)

                    // Consume the event.
                    true
                }

                // Add an open with browser entry.
                contextMenu.add(R.string.open_with_browser).setOnMenuItemClickListener {
                    // Open the image URL with an external browser.
                    openWithBrowser(imageUrl)

                    // Consume the event.
                    true
                }

                // Add a view image entry.
                contextMenu.add(R.string.view_image).setOnMenuItemClickListener {
                    // Load the image in the current tab.
                    loadUrl(currentWebView!!, imageUrl)

                    // Consume the event.
                    true
                }

                // Add a save image entry.
                contextMenu.add(R.string.save_image).setOnMenuItemClickListener {
                    // Check the download preference.
                    if (downloadWithExternalApp) {  // Download with an external app.
                        saveWithExternalApp(imageUrl)
                    } else {  // Handle the download inside of Privacy Browser.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, supportFragmentManager, imageUrl, currentWebView!!.settings.userAgentString, currentWebView!!.acceptCookies)
                    }

                    // Consume the event.
                    true
                }

                // Add a copy URL entry.
                contextMenu.add(R.string.copy_url).setOnMenuItemClickListener {
                    // Save the image URL in a clip data.
                    val imageTypeClipData = ClipData.newPlainText(getString(R.string.url), imageUrl)

                    // Set the clip data as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(imageTypeClipData)

                    // Consume the event.
                    true
                }

                // Add a Share URL entry.
                contextMenu.add(R.string.share_url).setOnMenuItemClickListener {
                    // Create the share intent.
                    val shareUrlIntent = Intent(Intent.ACTION_SEND)

                    // Add the URL to the intent.
                    shareUrlIntent.putExtra(Intent.EXTRA_TEXT, imageUrl)

                    // Set the MIME type.
                    shareUrlIntent.type = "text/plain"

                    // Set the intent to open in a new task.
                    shareUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    //Make it so.
                    startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.share_url)))

                    // Consume the event.
                    true
                }

                // Add an empty cancel entry, which by default closes the context menu.
                contextMenu.add(R.string.cancel)
            }

            // `SRC_IMAGE_ANCHOR_TYPE` is an image that is also a link.
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                // Get the image URL.
                imageUrl = hitTestResult.extra!!

                // Instantiate a handler.
                val handler = Handler(Looper.getMainLooper())

                // Get a handle for the handler message.
                val message = handler.obtainMessage()

                // Request the image details from the last touched node be returned in the message.
                currentWebView!!.requestFocusNodeHref(message)

                // Get the link URL from the message data.
                linkUrl = message.data.getString("url")!!

                // Set the link URL as the title of the context menu.
                contextMenu.setHeaderTitle(linkUrl)

                // Add an open in new tab entry.
                contextMenu.add(R.string.open_in_new_tab).setOnMenuItemClickListener {
                    // Load the link URL in a new tab and move to it.
                    addNewPage(linkUrl, adjacent = true, moveToTab = true)

                    // Consume the event.
                    true
                }

                // Add an open in background entry.
                contextMenu.add(R.string.open_in_background).setOnMenuItemClickListener {
                    // Lod the link URL in a new tab but do not move to it.
                    addNewPage(linkUrl, adjacent = true, moveToTab = false)

                    // Consume the event.
                    true
                }

                // Add an open image in new tab entry.
                contextMenu.add(R.string.open_image_in_new_tab).setOnMenuItemClickListener {
                    // Load the image in a new tab and move to it.
                    addNewPage(imageUrl, adjacent = true, moveToTab = true)

                    // Consume the event.
                    true
                }

                // Add an open with app entry.
                contextMenu.add(R.string.open_with_app).setOnMenuItemClickListener {
                    // Open the link URL with an external app.
                    openWithApp(linkUrl)

                    // Consume the event.
                    true
                }

                // Add an open with browser entry.
                contextMenu.add(R.string.open_with_browser).setOnMenuItemClickListener {
                    // Open the link URL with an external browser.
                    openWithBrowser(linkUrl)

                    // Consume the event.
                    true
                }

                // Add a view image entry.
                contextMenu.add(R.string.view_image).setOnMenuItemClickListener {
                    // View the image in the current tab.
                    loadUrl(currentWebView!!, imageUrl)

                    // Consume the event.
                    true
                }

                // Add a Save Image entry.
                contextMenu.add(R.string.save_image).setOnMenuItemClickListener {
                    // Check the download preference.
                    if (downloadWithExternalApp)  // Download with an external app.
                        saveWithExternalApp(imageUrl)
                    else  // Handle the download inside of Privacy Browser.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, supportFragmentManager, imageUrl, currentWebView!!.settings.userAgentString, currentWebView!!.acceptCookies)

                    // Consume the event.
                    true
                }

                // Add a Share Image entry.
                contextMenu.add(R.string.share_image).setOnMenuItemClickListener {
                    // Create the share intent.
                    val shareUrlIntent = Intent(Intent.ACTION_SEND)

                    // Add the URL to the intent.
                    shareUrlIntent.putExtra(Intent.EXTRA_TEXT, imageUrl)

                    // Set the MIME type.
                    shareUrlIntent.type = "text/plain"

                    // Set the intent to open in a new task.
                    shareUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    //Make it so.
                    startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.share_url)))

                    // Consume the event.
                    true
                }

                // Add a copy URL entry.
                contextMenu.add(R.string.copy_url).setOnMenuItemClickListener {
                    // Save the link URL in a clip data.
                    val srcImageAnchorTypeClipData = ClipData.newPlainText(getString(R.string.url), linkUrl)

                    // Set the clip data as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(srcImageAnchorTypeClipData)

                    // Consume the event.
                    true
                }

                // Add a save URL entry.
                contextMenu.add(R.string.save_url).setOnMenuItemClickListener {
                    // Check the download preference.
                    if (downloadWithExternalApp)  // Download with an external app.
                        saveWithExternalApp(linkUrl)
                    else  // Handle the download inside of Privacy Browser.  The dialog will be displayed once the file size and the content disposition have been acquired.
                        PrepareSaveDialogCoroutine.prepareSaveDialog(this, supportFragmentManager, linkUrl, currentWebView!!.settings.userAgentString, currentWebView!!.acceptCookies)

                    // Consume the event.
                    true
                }

                // Add a Share URL entry.
                contextMenu.add(R.string.share_url).setOnMenuItemClickListener {
                    // Create the share intent.
                    val shareUrlIntent = Intent(Intent.ACTION_SEND)

                    // Add the URL to the intent.
                    shareUrlIntent.putExtra(Intent.EXTRA_TEXT, linkUrl)

                    // Set the MIME type.
                    shareUrlIntent.type = "text/plain"

                    // Set the intent to open in a new task.
                    shareUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    //Make it so.
                    startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.share_url)))

                    // Consume the event.
                    true
                }

                // Add an empty cancel entry, which by default closes the context menu.
                contextMenu.add(R.string.cancel)
            }

            WebView.HitTestResult.EMAIL_TYPE -> {
                // Get the target URL.
                linkUrl = hitTestResult.extra

                // Set the target URL as the title of the context menu.
                contextMenu.setHeaderTitle(linkUrl)

                // Get a new message from the WebView handler.
                val hrefMessage = currentWebView!!.handler.obtainMessage()

                // Request the focus node href.
                currentWebView!!.requestFocusNodeHref(hrefMessage)

                // Get the link text from the href message.
                val linkText = hrefMessage.data.getString("title")

                // Add a write email entry.
                contextMenu.add(R.string.write_email).setOnMenuItemClickListener {
                    // Use `ACTION_SENDTO` instead of `ACTION_SEND` so that only email programs are launched.
                    val emailIntent = Intent(Intent.ACTION_SENDTO)

                    // Parse the url and set it as the data for the intent.
                    emailIntent.data = "mailto:$linkUrl".toUri()

                    // `FLAG_ACTIVITY_NEW_TASK` opens the email program in a new task instead as part of Privacy Browser.
                    emailIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    try {
                        // Make it so.
                        startActivity(emailIntent)
                    } catch (exception: ActivityNotFoundException) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView!!, getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }

                    // Consume the event.
                    true
                }

                // Add a copy email address entry.
                contextMenu.add(R.string.copy_email_address).setOnMenuItemClickListener {
                    // Save the email address in a clip data.
                    val srcEmailTypeClipData = ClipData.newPlainText(getString(R.string.email_address), linkUrl)

                    // Set the clip data as the clipboard's primary clip.
                    clipboardManager.setPrimaryClip(srcEmailTypeClipData)

                    // Consume the event.
                    true
                }

                // Add a Copy Text entry if the link text is not null.
                if (linkText != null) {
                    contextMenu.add(R.string.copy_text).setOnMenuItemClickListener {
                        // Save the link URL in a clip data.
                        val srcEmailTypeTextClipData = ClipData.newPlainText(getString(R.string.copy_text), linkText)

                        // Set the clip data as the clipboard's primary clip.
                        clipboardManager.setPrimaryClip(srcEmailTypeTextClipData)

                        // Consume the event.
                        true
                    }
                }

                // Add an empty cancel entry, which by default closes the context menu.
                contextMenu.add(R.string.cancel)
            }
        }
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun addTab(@Suppress("UNUSED_PARAMETER")view: View?) {
        // Add a new tab with a blank URL.
        addNewPage(urlString = "", adjacent = true, moveToTab = true)
    }

    private fun addNewPage(urlString: String, adjacent: Boolean, moveToTab: Boolean) {
        // Clear the focus from the URL edit text, so that it will be populated with the information from the new tab.
        urlEditText.clearFocus()

        // Add the new tab after the tab layout has quiesced.
        // Otherwise, there can be problems when restoring a large number of tabs and processing a new intent at the same time.  <https://redmine.stoutner.com/issues/1136>
        tabLayout.post {
            // Get the new tab position.
            val newTabPosition = if (adjacent)  // The new tab position is immediately to the right of the current tab position.
                tabLayout.selectedTabPosition + 1
            else  // The new tab position is at the end.  The tab positions are 0 indexed, so the new page number will match the current count.
                tabLayout.tabCount

            // Add the new WebView page.
            webViewStateAdapter!!.addPage(newTabPosition, urlString)

            // Add the new tab.
            addNewTab(newTabPosition, moveToTab)
        }
    }

    private fun addNewTab(newTabPosition: Int, moveToTab: Boolean) {
        // Check to see if the new page is ready.
        if (webViewStateAdapter!!.itemCount >= tabLayout.tabCount) {  // The new page is ready.
            // Create a new tab.
            val newTab = tabLayout.newTab()

            // Set a custom view on the new tab.
            newTab.setCustomView(R.layout.tab_custom_view)

            // Add the new tab.
            tabLayout.addTab(newTab, newTabPosition, moveToTab)

            // Select the new tab if it is the first one.  For some odd reason, Android doesn't select the first tab if it is the only one, which causes problems with the new tab position logic above.
            if (newTabPosition == 0)
                tabLayout.selectTab(newTab)

            // Scroll to the new tab position if moving to the new tab.
            if (moveToTab)
                tabLayout.post {
                    tabLayout.setScrollPosition(newTabPosition, 0F, false, false)
                }

            // Show the app bar if it is at the bottom of the screen and the new tab is taking focus.
            if (bottomAppBar && moveToTab && appBarLayout.translationY != 0f) {
                // Animate the bottom app bar onto the screen.
                objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", 0f)

                // Make it so.
                objectAnimator.start()
            }
        } else {  // The new page is not ready.
            // Create a new tab handler.
            val newTabHandler = Handler(Looper.getMainLooper())

            // Create a new tab runnable.
            val newTabRunnable = Runnable {
                // Create the new tab.
                addNewTab(newTabPosition, moveToTab)
            }

            // Try adding the new tab again after 50 milliseconds.
            newTabHandler.postDelayed(newTabRunnable, 50)
        }
    }

    private fun applyAppSettings() {
        // Store the default preferences used in `applyDomainSettings()`.  These are done here so that expensive preference requests are not done each time a domain is loaded.
        defaultJavaScript = sharedPreferences.getBoolean(getString(R.string.javascript_key), false)
        defaultCookies = sharedPreferences.getBoolean(getString(R.string.cookies_key), false)
        defaultDomStorage = sharedPreferences.getBoolean(getString(R.string.dom_storage_key), false)
        defaultEasyList = sharedPreferences.getBoolean(getString(R.string.easylist_key), true)
        defaultEasyPrivacy = sharedPreferences.getBoolean(getString(R.string.easyprivacy_key), true)
        defaultFanboysAnnoyanceList = sharedPreferences.getBoolean(getString(R.string.fanboys_annoyance_list_key), true)
        defaultFanboysSocialBlockingList = sharedPreferences.getBoolean(getString(R.string.fanboys_social_blocking_list_key), true)
        defaultUltraList = sharedPreferences.getBoolean(getString(R.string.ultralist_key), true)
        defaultUltraPrivacy = sharedPreferences.getBoolean(getString(R.string.ultraprivacy_key), true)
        defaultBlockAllThirdPartyRequests = sharedPreferences.getBoolean(getString(R.string.block_all_third_party_requests_key), false)
        defaultFontSizeString = sharedPreferences.getString(getString(R.string.font_size_key), getString(R.string.font_size_default_value))!!
        defaultUserAgentName = sharedPreferences.getString(getString(R.string.user_agent_key), getString(R.string.user_agent_default_value))!!
        defaultSwipeToRefresh = sharedPreferences.getBoolean(getString(R.string.swipe_to_refresh_key), true)
        defaultWebViewTheme = sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value))!!
        defaultWideViewport = sharedPreferences.getBoolean(getString(R.string.wide_viewport_key), true)
        defaultDisplayWebpageImages = sharedPreferences.getBoolean(getString(R.string.display_webpage_images_key), true)

        // Get the string arrays.  These are done here so that expensive resource requests are not made each time a domain is loaded.
        webViewThemeEntryValuesStringArray = resources.getStringArray(R.array.webview_theme_entry_values)
        userAgentDataArray = resources.getStringArray(R.array.user_agent_data)
        userAgentNamesArray = resources.getStringArray(R.array.user_agent_names)
        val downloadProviderEntryValuesStringArray = resources.getStringArray(R.array.download_provider_entry_values)

        // Get the user agent array adapters.  These are done here so that expensive resource requests are not made each time a domain is loaded.
        userAgentDataArrayAdapter = ArrayAdapter.createFromResource(this, R.array.user_agent_data, R.layout.spinner_item)
        userAgentNamesArrayAdapter = ArrayAdapter.createFromResource(this, R.array.user_agent_names, R.layout.spinner_item)

        // Store the values from the shared preferences in variables.
        incognitoModeEnabled = sharedPreferences.getBoolean(getString(R.string.incognito_mode_key), false)
        sanitizeTrackingQueries = sharedPreferences.getBoolean(getString(R.string.tracking_queries_key), true)
        sanitizeAmpRedirects = sharedPreferences.getBoolean(getString(R.string.amp_redirects_key), true)
        proxyMode = sharedPreferences.getString(getString(R.string.proxy_key), getString(R.string.proxy_default_value))!!
        fullScreenBrowsingModeEnabled = sharedPreferences.getBoolean(getString(R.string.full_screen_browsing_mode_key), false)
        hideAppBar = sharedPreferences.getBoolean(getString(R.string.hide_app_bar_key), true)
        displayUnderCutouts = sharedPreferences.getBoolean(getString(R.string.display_under_cutouts_key), false)
        val downloadProvider = sharedPreferences.getString(getString(R.string.download_provider_key), getString(R.string.download_provider_default_value))!!
        scrollAppBar = sharedPreferences.getBoolean(getString(R.string.scroll_app_bar_key), false)
        sortBookmarksAlphabetically = sharedPreferences.getBoolean(getString(R.string.sort_bookmarks_alphabetically_key), false)

        // Determine if downloading should be handled by an external app.
        downloadWithExternalApp = (downloadProvider == downloadProviderEntryValuesStringArray[2])

        // Apply the saved proxy mode if the app has been restarted.
        if (savedProxyMode != null) {
            // Apply the saved proxy mode.
            proxyMode = savedProxyMode!!

            // Reset the saved proxy mode.
            savedProxyMode = null
        }

        // Get the search string.
        val searchString = sharedPreferences.getString(getString(R.string.search_key), getString(R.string.search_default_value))!!

        // Set the search string, using the custom search URL if specified.
        searchURL = if (searchString == getString(R.string.custom_url_item))
            sharedPreferences.getString(getString(R.string.search_custom_url_key), getString(R.string.search_custom_url_default_value))!!
        else
            searchString

        // Apply the proxy.
        applyProxy(false)

        // Adjust the layout and scrolling parameters according to the position of the app bar.
        if (bottomAppBar) {  // The app bar is on the bottom.
            // Adjust the UI.
            if (scrollAppBar || (inFullScreenBrowsingMode && hideAppBar)) {  // The app bar scrolls or full screen browsing mode is engaged with the app bar hidden.
                // Reset the WebView padding to fill the available space.
                swipeRefreshLayout.setPadding(0, 0, 0, 0)
            } else {  // The app bar doesn't scroll or full screen browsing mode is not engaged with the app bar hidden.
                // Move the WebView above the app bar layout.
                swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight)

                // Show the app bar if it is scrolled off the screen.
                if (appBarLayout.translationY != 0f) {
                    // Animate the bottom app bar onto the screen.
                    objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", 0f)

                    // Make it so.
                    objectAnimator.start()
                }
            }
        } else {  // The app bar is on the top.
            // Get the current layout parameters.  Using coordinator layout parameters allows the `setBehavior()` command and using app bar layout parameters allows the `setScrollFlags()` command.
            val swipeRefreshLayoutParams = swipeRefreshLayout.layoutParams as CoordinatorLayout.LayoutParams
            val toolbarLayoutParams = toolbar.layoutParams as AppBarLayout.LayoutParams
            val findOnPageLayoutParams = findOnPageLinearLayout.layoutParams as AppBarLayout.LayoutParams
            val tabsLayoutParams = tabsLinearLayout.layoutParams as AppBarLayout.LayoutParams

            // Add the scrolling behavior to the layout parameters.
            if (scrollAppBar) {
                // Enable scrolling of the app bar.
                swipeRefreshLayoutParams.behavior = AppBarLayout.ScrollingViewBehavior()
                toolbarLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                findOnPageLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                tabsLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            } else {
                // Disable scrolling of the app bar.
                swipeRefreshLayoutParams.behavior = null
                toolbarLayoutParams.scrollFlags = 0
                findOnPageLayoutParams.scrollFlags = 0
                tabsLayoutParams.scrollFlags = 0

                // Expand the app bar if it is currently collapsed.
                appBarLayout.setExpanded(true)
            }

            // Set the app bar scrolling for each WebView.
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the fragment view.
                val fragmentView = webViewTabFragment.view

                // Only modify the WebViews if they exist.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Set the app bar scrolling.
                    nestedScrollWebView.isNestedScrollingEnabled = scrollAppBar
                }
            }
        }

        // Force an exit of full screen browsing mode if it is now disabled in settings.
        if (!fullScreenBrowsingModeEnabled)
            inFullScreenBrowsingMode = false

        // Update the full screen browsing mode settings.
        if (inFullScreenBrowsingMode) {  // Privacy Browser is currently in full screen browsing mode.
            // Update the visibility of the app bar, which might have changed in the settings.
            if (hideAppBar) {
                // Hide the tab linear layout.
                tabsLinearLayout.visibility = View.GONE

                // Hide the app bar.
                appBar.hide()
            } else {
                // Show the tab linear layout.
                tabsLinearLayout.visibility = View.VISIBLE

                // Show the app bar.
                appBar.show()
            }

            // Re-enforce the fullscreen flags if the API < 30.
            if (Build.VERSION.SDK_INT < 30) {
                /* Hide the system bars.
                 * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                 * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                 * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                 */

                @Suppress("DEPRECATION")
                window.addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        } else {  // Privacy Browser is not in full screen browsing mode.
            // Show the tab linear layout.
            tabsLinearLayout.visibility = View.VISIBLE

            // Show the app bar.
            appBar.show()

            // Remove the fullscreen flags if the API < 30.
            if (Build.VERSION.SDK_INT < 30) {
                /* Show the system bars.
                 * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                 * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                 * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                 */

                @Suppress("DEPRECATION")
                window.clearFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }

        // Load the bookmarks folder.
        loadBookmarksFolder()
    }
    
    private fun showProfilesDialog() {
        if (Build.VERSION.SDK_INT < 28) {
            Toast.makeText(this, getString(R.string.profiles_min_api), Toast.LENGTH_LONG).show()
            return
        }

        val mgr = ProfileManager.get(this)
        val active = mgr.getActiveProfileIdOrDefault()
        val profiles = mgr.list().sortedByDescending { it.lastUsedAt }.map { it.id }
        val entries = profiles.map { id ->
            if (id == active) "$id${getString(R.string.profiles_active_suffix)}" else id
        }.toMutableList()
        entries.add(getString(R.string.profiles_create_new))

        val items = entries.toTypedArray()

        AlertDialog.Builder(this)
           .setTitle(getString(R.string.profiles_title))
           .setItems(items) { dialog, idx ->
               dialog.dismiss()
                if (idx == items.lastIndex) {
                    promptCreateProfile()
                } else {
                    val chosenId = profiles[idx]
                    if (chosenId == active) return@setItems
                    if (mgr.switchTo(chosenId)) {
                        Toast.makeText(this, getString(R.string.profiles_switching, chosenId), Toast.LENGTH_SHORT).show()
                        ProcessRelauncher.relaunch(this)
                    }
                }
           }
           .show()
    }

    private fun promptCreateProfile() {
        val input = EditText(this).apply {
           hint = getString(R.string.profiles_new_prompt)
        }

        AlertDialog.Builder(this)
           .setTitle(getString(R.string.profiles_create_new))
           .setView(input)
           .setPositiveButton(android.R.string.ok) { d, _ ->
               val mgr = ProfileManager.get(this)
               val raw = input.text.toString().trim()
               val slug = raw.lowercase()
                   .replace(Regex("[^a-z0-9_-]+"), "-")
                   .trim('-')
                   .ifEmpty { "baru" }

               var id = if (slug.startsWith("profil-")) slug else "profil-$slug"
               if (id.length > 32) id = id.substring(0, 32).trimEnd('-','_')
               
               when {
                   !ProfileManager.validateId(id) ->
                       Toast.makeText(this, getString(R.string.profiles_invalid_id), Toast.LENGTH_LONG).show()
                   mgr.list().any { it.id == id } ->
                       Toast.makeText(this, getString(R.string.profiles_exists), Toast.LENGTH_LONG).show()
                   else -> {
                       if (mgr.create(id)) {
                           Toast.makeText(this, getString(R.string.profiles_created), Toast.LENGTH_SHORT).show()
                           if (mgr.switchTo(id)) {
                               Toast.makeText(this, getString(R.string.profiles_switching, id), Toast.LENGTH_SHORT).show()
                               ProcessRelauncher.relaunch(this)
                           }
                       }
                   }
               }
               d.dismiss()
           }
           .setNegativeButton(android.R.string.cancel, null)
           .show()
    }


    // `reloadWebsite` is used if returning from the Domains activity.  Otherwise JavaScript might not function correctly if it is newly enabled.
    @SuppressLint("SetJavaScriptEnabled")
    private fun applyDomainSettings(nestedScrollWebView: NestedScrollWebView, url: String?, resetTab: Boolean, reloadWebsite: Boolean, loadUrl: Boolean) {
        // Store the current URL.
        nestedScrollWebView.currentUrl = url!!

        // Parse the URL into a URI.
        val uri = url.toUri()

        // Extract the domain from the URI.
        var newHostName = uri.host

        // Strings don't like to be null.
        if (newHostName == null)
            newHostName = ""

        // Apply the domain settings if a new domain is being loaded or if the new domain is blank.  This allows the user to set temporary settings for JavaScript, cookies, DOM storage, etc.
        if ((nestedScrollWebView.currentDomainName != newHostName) || (newHostName == "")) {  // A new domain is being loaded.
            // Set the new host name as the current domain name.
            nestedScrollWebView.currentDomainName = newHostName

            // Reset the ignoring of pinned domain information.
            nestedScrollWebView.ignorePinnedDomainInformation = false

            // Clear any pinned SSL certificate or IP addresses.
            nestedScrollWebView.clearPinnedSslCertificate()
            nestedScrollWebView.pinnedIpAddresses = ""

            // Reset the tab if specified.
            if (resetTab) {
                // Initialize the favorite icon.
                nestedScrollWebView.resetFavoriteIcon()

                // Get the current page position.
                val currentPagePosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

                // Get the corresponding tab.
                val tab = tabLayout.getTabAt(currentPagePosition)

                // Update the tab if it isn't null, which sometimes happens when restarting from the background.
                if (tab != null) {
                    // Get the tab custom view.
                    val tabCustomView = tab.customView!!

                    // Get the tab views.
                    val tabFavoriteIconImageView = tabCustomView.findViewById<ImageView>(R.id.favorite_icon_imageview)
                    val tabTitleTextView = tabCustomView.findViewById<TextView>(R.id.title_textview)

                    // Store the current values in case they need to be restored.
                    nestedScrollWebView.previousFavoriteIconDrawable = tabFavoriteIconImageView.drawable
                    nestedScrollWebView.previousWebpageTitle = tabTitleTextView.text.toString()

                    // Set the default favorite icon as the favorite icon for this tab.
                    tabFavoriteIconImageView.setImageBitmap(nestedScrollWebView.getFavoriteIcon().scale(128, 128))

                    // Set the loading title text.
                    tabTitleTextView.setText(R.string.loading)
                }
            }

            // Initialize the domain name in database variable.
            var domainNameInDatabase: String? = null

            // Check the hostname against the domain settings set.
            if (domainsSettingsSet.contains(newHostName)) {  // The hostname is contained in the domain settings set.
                // Record the domain name in the database.
                domainNameInDatabase = newHostName

                // Set the domain settings applied tracker to true.
                nestedScrollWebView.domainSettingsApplied = true
            } else {  // The hostname is not contained in the domain settings set.
                // Set the domain settings applied tracker to false.
                nestedScrollWebView.domainSettingsApplied = false
            }

            // Check all the subdomains of the host name against wildcard domains in the domain cursor.
            while (!nestedScrollWebView.domainSettingsApplied && newHostName!!.contains(".")) {  // Stop checking if domain settings are already applied or there are no more `.` in the hostname.
                if (domainsSettingsSet.contains("*.$newHostName")) {  // Check the host name prepended by `*.`.
                    // Set the domain settings applied tracker to true.
                    nestedScrollWebView.domainSettingsApplied = true

                    // Store the applied domain names as it appears in the database.
                    domainNameInDatabase = "*.$newHostName"
                }

                // Strip out the lowest subdomain of of the host name.
                newHostName = newHostName.substring(newHostName.indexOf(".") + 1)
            }

            // Apply either the domain settings or the default settings.
            if (nestedScrollWebView.domainSettingsApplied) {  // The url has custom domain settings.
                // Get a cursor for the current host.
                val currentDomainSettingsCursor = domainsDatabaseHelper!!.getCursorForDomainName(domainNameInDatabase!!)

                // Move to the first position.
                currentDomainSettingsCursor.moveToFirst()

                // Get the settings from the cursor.
                nestedScrollWebView.domainSettingsDatabaseId = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ID))
                val javaScriptInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_JAVASCRIPT))
                val cookiesInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(COOKIES))
                val domStorageInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_DOM_STORAGE))
                val easyListInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_EASYLIST))
                val easyPrivacyInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_EASYPRIVACY))
                val fanboysAnnoyanceListInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_ANNOYANCE_LIST))
                val fanboysSocialBlockingListInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST))
                val ultraListInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(com.audeon.browser.helpers.ULTRALIST))
                val ultraPrivacyInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(ENABLE_ULTRAPRIVACY))
                val blockAllThirdPartyRequestsInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(com.audeon.browser.helpers.BLOCK_ALL_THIRD_PARTY_REQUESTS))
                val userAgentName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(com.audeon.browser.helpers.USER_AGENT))
                val fontSize = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(FONT_SIZE))
                val swipeToRefreshInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(SWIPE_TO_REFRESH))
                val webViewThemeInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(WEBVIEW_THEME))
                val wideViewportInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(WIDE_VIEWPORT))
                val displayWebpageImagesInt = currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(DISPLAY_IMAGES))
                val pinnedSslCertificate = (currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(PINNED_SSL_CERTIFICATE)) == 1)
                val pinnedSslIssuedToCName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_COMMON_NAME))
                val pinnedSslIssuedToOName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_ORGANIZATION))
                val pinnedSslIssuedToUName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT))
                val pinnedSslIssuedByCName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_COMMON_NAME))
                val pinnedSslIssuedByOName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_ORGANIZATION))
                val pinnedSslIssuedByUName = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT))
                val pinnedSslStartDate = Date(currentDomainSettingsCursor.getLong(currentDomainSettingsCursor.getColumnIndexOrThrow(com.audeon.browser.helpers.SSL_START_DATE)))
                val pinnedSslEndDate = Date(currentDomainSettingsCursor.getLong(currentDomainSettingsCursor.getColumnIndexOrThrow(com.audeon.browser.helpers.SSL_END_DATE)))
                val pinnedIpAddresses = (currentDomainSettingsCursor.getInt(currentDomainSettingsCursor.getColumnIndexOrThrow(PINNED_IP_ADDRESSES)) == 1)
                val pinnedHostIpAddresses = currentDomainSettingsCursor.getString(currentDomainSettingsCursor.getColumnIndexOrThrow(IP_ADDRESSES))

                // Close the current host domain settings cursor.
                currentDomainSettingsCursor.close()

                // Set the JavaScript status.
                when (javaScriptInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.settings.javaScriptEnabled = defaultJavaScript
                    ENABLED -> nestedScrollWebView.settings.javaScriptEnabled = true
                    DISABLED -> nestedScrollWebView.settings.javaScriptEnabled = false
                }

                // Store the cookies status.
                when (cookiesInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.acceptCookies = defaultCookies
                    ENABLED -> nestedScrollWebView.acceptCookies = true
                    DISABLED -> nestedScrollWebView.acceptCookies = false
                }

                // Apply the cookies status.
                cookieManager.setAcceptCookie(nestedScrollWebView.acceptCookies)

                // Set the DOM storage status.
                when (domStorageInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.settings.domStorageEnabled = defaultDomStorage
                    ENABLED -> nestedScrollWebView.settings.domStorageEnabled = true
                    DISABLED -> nestedScrollWebView.settings.domStorageEnabled = false
                }

                // Set the EasyList status.
                when (easyListInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.easyListEnabled = defaultEasyList
                    ENABLED -> nestedScrollWebView.easyListEnabled = true
                    DISABLED -> nestedScrollWebView.easyListEnabled = false
                }

                // Set the EasyPrivacy status.
                when (easyPrivacyInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.easyPrivacyEnabled = defaultEasyPrivacy
                    ENABLED -> nestedScrollWebView.easyPrivacyEnabled = true
                    DISABLED -> nestedScrollWebView.easyPrivacyEnabled = false
                }

                // Set the Fanboy's Annoyance List status.
                when (fanboysAnnoyanceListInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.fanboysAnnoyanceListEnabled = defaultFanboysAnnoyanceList
                    ENABLED -> nestedScrollWebView.fanboysAnnoyanceListEnabled = true
                    DISABLED -> nestedScrollWebView.fanboysAnnoyanceListEnabled = false
                }

                // Set the Fanboy's Social Blocking List status.
                when (fanboysSocialBlockingListInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.fanboysSocialBlockingListEnabled = defaultFanboysSocialBlockingList
                    ENABLED -> nestedScrollWebView.fanboysSocialBlockingListEnabled = true
                    DISABLED -> nestedScrollWebView.fanboysSocialBlockingListEnabled = false
                }

                // Set the UltraList status.
                when (ultraListInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.ultraListEnabled = defaultUltraList
                    ENABLED -> nestedScrollWebView.ultraListEnabled = true
                    DISABLED -> nestedScrollWebView.ultraListEnabled = false
                }

                // Set the UltraPrivacy status.
                when (ultraPrivacyInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.ultraPrivacyEnabled = defaultUltraPrivacy
                    ENABLED -> nestedScrollWebView.ultraPrivacyEnabled = true
                    DISABLED -> nestedScrollWebView.ultraPrivacyEnabled = false
                }

                // Set the block all third-party requests status.
                when (blockAllThirdPartyRequestsInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.blockAllThirdPartyRequests = defaultBlockAllThirdPartyRequests
                    ENABLED -> nestedScrollWebView.blockAllThirdPartyRequests = true
                    DISABLED -> nestedScrollWebView.blockAllThirdPartyRequests = false
                }

                // Set the user agent.
                if (userAgentName == getString(R.string.system_default_user_agent)) {  // Use the system default user agent.
                    // Set the user agent according to the system default.
                    when (val defaultUserAgentArrayPosition = userAgentNamesArrayAdapter.getPosition(defaultUserAgentName)) {
                        UNRECOGNIZED_USER_AGENT ->  // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                            nestedScrollWebView.settings.userAgentString = defaultUserAgentName

                        SETTINGS_WEBVIEW_DEFAULT_USER_AGENT ->  // Set the user agent to `""`, which uses the default value.
                            nestedScrollWebView.settings.userAgentString = ""

                        SETTINGS_CUSTOM_USER_AGENT ->  // Set the default custom user agent.
                            nestedScrollWebView.settings.userAgentString = sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value))

                        else ->  // Get the user agent string from the user agent data array
                            nestedScrollWebView.settings.userAgentString = userAgentDataArray[defaultUserAgentArrayPosition]
                    }
                } else {  // Set the user agent according to the stored name.
                    // Set the user agent.
                    when (val userAgentArrayPosition = userAgentNamesArrayAdapter.getPosition(userAgentName)) {
                        // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                        UNRECOGNIZED_USER_AGENT ->
                            nestedScrollWebView.settings.userAgentString = userAgentName

                        // Set the user agent to `""`, which uses the default value.
                        SETTINGS_WEBVIEW_DEFAULT_USER_AGENT ->
                            nestedScrollWebView.settings.userAgentString = ""

                        // Get the user agent string from the user agent data array.
                        else ->
                            nestedScrollWebView.settings.userAgentString = userAgentDataArray[userAgentArrayPosition]
                    }
                }

                // Apply the font size.
                try {  // Try the specified font size to see if it is valid.
                    if (fontSize == 0) {  // Apply the default font size.
                        // Set the font size from the value in the app settings.
                        nestedScrollWebView.settings.textZoom = defaultFontSizeString.toInt()
                    } else {  // Apply the font size from domain settings.
                        nestedScrollWebView.settings.textZoom = fontSize
                    }
                } catch (exception: Exception) {  // The specified font size is invalid
                    // Set the font size to be 100%
                    nestedScrollWebView.settings.textZoom = 100
                }

                // Set swipe to refresh.
                when (swipeToRefreshInt) {
                    SYSTEM_DEFAULT -> {
                        // Store the swipe to refresh status in the nested scroll WebView.
                        nestedScrollWebView.swipeToRefresh = defaultSwipeToRefresh

                        // Update the swipe refresh layout.
                        if (defaultSwipeToRefresh) {  // Swipe to refresh is enabled.
                            // Update the status of the swipe refresh layout if the current WebView is not null (crash reports indicate that in some unexpected way it sometimes is null).
                            if (currentWebView != null) {
                                // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                                swipeRefreshLayout.isEnabled = (currentWebView!!.scrollY == 0)
                            }
                        } else {  // Swipe to refresh is disabled.
                            // Disable the swipe refresh layout.
                            swipeRefreshLayout.isEnabled = false
                        }
                    }

                    ENABLED -> {
                        // Store the swipe to refresh status in the nested scroll WebView.
                        nestedScrollWebView.swipeToRefresh = true

                        // Update the status of the swipe refresh layout if the current WebView is not null (crash reports indicate that in some unexpected way it sometimes is null).
                        if (currentWebView != null) {
                            // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                            swipeRefreshLayout.isEnabled = (currentWebView!!.scrollY == 0)
                        }
                    }

                    DISABLED -> {
                        // Store the swipe to refresh status in the nested scroll WebView.
                        nestedScrollWebView.swipeToRefresh = false

                        // Disable swipe to refresh.
                        swipeRefreshLayout.isEnabled = false
                    }
                }

                // Set the WebView theme if algorithmic darkening is supported.
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    // Set the WebView theme.
                    when (webViewThemeInt) {
                        SYSTEM_DEFAULT ->  // Set the WebView theme.
                            when (defaultWebViewTheme) {
                                webViewThemeEntryValuesStringArray[1] ->  // The light theme is selected.  Turn off algorithmic darkening.
                                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, false)

                                webViewThemeEntryValuesStringArray[2] ->  // The dark theme is selected.  Turn on algorithmic darkening.
                                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, true)

                                else -> {  // The system default theme is selected.
                                    // Get the current system theme status.
                                    val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                                    // Set the algorithmic darkening according to the current system theme status.
                                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, currentThemeStatus == Configuration.UI_MODE_NIGHT_YES)
                                }
                            }

                        LIGHT_THEME ->  // Turn off algorithmic darkening.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, false)

                        DARK_THEME ->  // Turn on algorithmic darkening.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, true)
                    }
                }

                // Set the wide viewport status.
                when (wideViewportInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.settings.useWideViewPort = defaultWideViewport
                    ENABLED -> nestedScrollWebView.settings.useWideViewPort = true
                    DISABLED -> nestedScrollWebView.settings.useWideViewPort = false
                }

                // Set the display webpage images status.
                when (displayWebpageImagesInt) {
                    SYSTEM_DEFAULT -> nestedScrollWebView.settings.loadsImagesAutomatically = defaultDisplayWebpageImages
                    ENABLED -> nestedScrollWebView.settings.loadsImagesAutomatically = true
                    DISABLED -> nestedScrollWebView.settings.loadsImagesAutomatically = false
                }

                // If there is a pinned SSL certificate, store it in the WebView.
                if (pinnedSslCertificate)
                    nestedScrollWebView.setPinnedSslCertificate(pinnedSslIssuedToCName, pinnedSslIssuedToOName, pinnedSslIssuedToUName, pinnedSslIssuedByCName, pinnedSslIssuedByOName, pinnedSslIssuedByUName,
                        pinnedSslStartDate, pinnedSslEndDate)

                // If there is a pinned IP address, store it in the WebView.
                if (pinnedIpAddresses)
                    nestedScrollWebView.pinnedIpAddresses = pinnedHostIpAddresses

                // Set a background on the URL relative layout to indicate that custom domain settings are being used.
                urlRelativeLayout.background = AppCompatResources.getDrawable(this, R.drawable.domain_settings_url_background)
            } else {  // The new URL does not have custom domain settings.  Load the defaults.
                // Store the values from the shared preferences.
                nestedScrollWebView.settings.javaScriptEnabled = defaultJavaScript
                nestedScrollWebView.acceptCookies = defaultCookies
                nestedScrollWebView.settings.domStorageEnabled = defaultDomStorage
                nestedScrollWebView.easyListEnabled = defaultEasyList
                nestedScrollWebView.easyPrivacyEnabled = defaultEasyPrivacy
                nestedScrollWebView.fanboysAnnoyanceListEnabled = defaultFanboysAnnoyanceList
                nestedScrollWebView.fanboysSocialBlockingListEnabled = defaultFanboysSocialBlockingList
                nestedScrollWebView.ultraListEnabled = defaultUltraList
                nestedScrollWebView.ultraPrivacyEnabled = defaultUltraPrivacy
                nestedScrollWebView.blockAllThirdPartyRequests = defaultBlockAllThirdPartyRequests

                // Apply the default cookie setting.
                cookieManager.setAcceptCookie(nestedScrollWebView.acceptCookies)

                // Apply the default font size setting.
                try {
                    // Try to set the font size from the value in the app settings.
                    nestedScrollWebView.settings.textZoom = defaultFontSizeString.toInt()
                } catch (exception: Exception) {
                    // If the app settings value is invalid, set the font size to 100%.
                    nestedScrollWebView.settings.textZoom = 100
                }

                // Store the swipe to refresh status in the nested scroll WebView.
                nestedScrollWebView.swipeToRefresh = defaultSwipeToRefresh

                // Update the swipe refresh layout.
                if (defaultSwipeToRefresh) {  // Swipe to refresh is enabled.
                    // Update the status of the swipe refresh layout if the current WebView is not null (crash reports indicate that in some unexpected way it sometimes is null).
                    if (currentWebView != null) {
                        // Only enable the swipe refresh layout if the WebView is scrolled to the top.  It is updated every time the scroll changes.
                        swipeRefreshLayout.isEnabled = currentWebView!!.scrollY == 0
                    }
                } else {  // Swipe to refresh is disabled.
                    // Disable the swipe refresh layout.
                    swipeRefreshLayout.isEnabled = false
                }

                // Reset the domain settings database ID.
                nestedScrollWebView.domainSettingsDatabaseId = -1

                // Set the user agent.
                when (val userAgentArrayPosition = userAgentNamesArrayAdapter.getPosition(defaultUserAgentName)) {
                    UNRECOGNIZED_USER_AGENT ->  // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                        nestedScrollWebView.settings.userAgentString = defaultUserAgentName

                    SETTINGS_WEBVIEW_DEFAULT_USER_AGENT ->  // Set the user agent to `""`, which uses the default value.
                        nestedScrollWebView.settings.userAgentString = ""

                    SETTINGS_CUSTOM_USER_AGENT ->  // Set the default custom user agent.
                        nestedScrollWebView.settings.userAgentString = sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value))

                    else ->  // Get the user agent string from the user agent data array
                        nestedScrollWebView.settings.userAgentString = userAgentDataArray[userAgentArrayPosition]
                }

                // Set the WebView theme if algorithmic darkening is supported.
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    // Set the WebView theme.
                    when (defaultWebViewTheme) {
                        webViewThemeEntryValuesStringArray[1] ->  // The light theme is selected.  Turn off algorithmic darkening.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, false)

                        webViewThemeEntryValuesStringArray[2] ->  // The dark theme is selected.  Turn on algorithmic darkening.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, true)

                        else -> {  // The system default theme is selected.  Get the current system theme status.
                            // Get the current theme status.
                            val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                            // Set the algorithmic darkening according to the current system theme status.
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, currentThemeStatus == Configuration.UI_MODE_NIGHT_YES)
                        }
                    }
                }

                // Set the viewport.
                nestedScrollWebView.settings.useWideViewPort = defaultWideViewport

                // Set the loading of webpage images.
                nestedScrollWebView.settings.loadsImagesAutomatically = defaultDisplayWebpageImages

                // Set a transparent background on the URL relative layout.
                urlRelativeLayout.background = AppCompatResources.getDrawable(this, R.color.transparent)
            }

            // Update the privacy icons.
            updatePrivacyIcons(true)
        }

        // Reload the website if returning from the Domains activity.
        if (reloadWebsite)
            nestedScrollWebView.reload()

        // Disable the wide viewport if the source is being viewed.
        if (url.startsWith("view-source:"))
            nestedScrollWebView.settings.useWideViewPort = false

        // Load the URL if directed.  This makes sure that the domain settings are properly loaded before the URL.  By using `loadUrl()`, instead of `loadUrlFromBase()`, the Referer header will never be sent.
        if (loadUrl)
            nestedScrollWebView.loadUrl(url)
    }

    private fun applyProxy(reloadWebViews: Boolean) {
        // Set the proxy according to the mode.
        proxyHelper.setProxy(applicationContext, appBarLayout, proxyMode)

        // Reset the waiting for proxy tracker.
        waitingForProxy = false

        // Set the proxy.
        when (proxyMode) {
            ProxyHelper.NONE -> {
                // Initialize a color background typed value.
                val colorBackgroundTypedValue = TypedValue()

                // Get the color background from the theme.
                theme.resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true)

                // Get the color background int from the typed value.
                val colorBackgroundInt = colorBackgroundTypedValue.data

                // Set the default app bar and status bar backgrounds.
                appBarLayout.setBackgroundColor(colorBackgroundInt)

                // Set the background color if the API < 35 (Android 15).  https://redmine.stoutner.com/issues/1169
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 35)
                    window.statusBarColor = colorBackgroundInt
            }

            ProxyHelper.TOR -> {
                // Set the app bar and status bar backgrounds to indicate proxying is enabled.
                appBarLayout.setBackgroundResource(R.color.blue_background)

                // Set the background color if the API < 35 (Android 15).  https://redmine.stoutner.com/issues/1169
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 35)
                    window.statusBarColor = getColor(R.color.blue_background)

                // Check to see if Orbot is installed.
                try {
                    // Get the package manager.
                    val packageManager = packageManager

                    // Check to see if Orbot is in the list.  This will throw an error and drop to the catch section if it isn't installed.
                    packageManager.getPackageInfo("org.torproject.android", 0)

                    // Check to see if the proxy is ready.
                    if (orbotStatus != ProxyHelper.ORBOT_STATUS_ON) {  // Orbot is not ready.
                        // Set the waiting for proxy status.
                        waitingForProxy = true

                        // Show the waiting for proxy dialog if it isn't already displayed.
                        if (supportFragmentManager.findFragmentByTag(getString(R.string.waiting_for_proxy_dialog)) == null) {
                            // Get a handle for the waiting for proxy alert dialog.
                            val waitingForProxyDialogFragment = WaitingForProxyDialog()

                            // Try to show the dialog.  Sometimes the window is not yet active if returning from Settings.
                            try {
                                // Show the waiting for proxy alert dialog.
                                waitingForProxyDialogFragment.show(supportFragmentManager, getString(R.string.waiting_for_proxy_dialog))
                            } catch (waitingForTorException: Exception) {
                                // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                                pendingDialogsArrayList.add(PendingDialogDataClass(waitingForProxyDialogFragment, getString(R.string.waiting_for_proxy_dialog)))
                            }
                        }
                    }
                } catch (exception: PackageManager.NameNotFoundException) {  // Orbot is not installed.
                    // Show the Orbot not installed dialog if it is not already displayed.
                    if (supportFragmentManager.findFragmentByTag(getString(R.string.proxy_not_installed_dialog)) == null) {
                        // Get a handle for the Orbot not installed alert dialog.
                        val orbotNotInstalledDialogFragment = ProxyNotInstalledDialog.displayDialog(proxyMode)

                        // Try to show the dialog.  Sometimes the window is not yet active if returning from Settings.
                        try {
                            // Display the Orbot not installed alert dialog.
                            orbotNotInstalledDialogFragment.show(supportFragmentManager, getString(R.string.proxy_not_installed_dialog))
                        } catch (orbotNotInstalledException: Exception) {
                            // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                            pendingDialogsArrayList.add(PendingDialogDataClass(orbotNotInstalledDialogFragment, getString(R.string.proxy_not_installed_dialog)))
                        }
                    }
                }
            }

            ProxyHelper.I2P -> {
                // Set the app bar and status bar backgrounds to indicate proxying is enabled.
                appBarLayout.setBackgroundResource(R.color.blue_background)

                // Set the background color if the API < 35 (Android 15).  https://redmine.stoutner.com/issues/1169
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 35)
                    window.statusBarColor = getColor(R.color.blue_background)

                // Check to see if I2P is installed.
                try {
                    // Check to see if the F-Droid flavor is installed.  This will throw an error and drop to the catch section if it isn't installed.
                    packageManager.getPackageInfo("net.i2p.android.router", 0)
                } catch (fdroidException: PackageManager.NameNotFoundException) {  // The F-Droid flavor is not installed.
                    try {
                        // Check to see if the Google Play flavor is installed.  This will throw an error and drop to the catch section if it isn't installed.
                        packageManager.getPackageInfo("net.i2p.android", 0)
                    } catch (googlePlayException: PackageManager.NameNotFoundException) {  // The Google Play flavor is not installed.
                        // Sow the I2P not installed dialog if it is not already displayed.
                        if (supportFragmentManager.findFragmentByTag(getString(R.string.proxy_not_installed_dialog)) == null) {
                            // Get a handle for the waiting for proxy alert dialog.
                            val i2pNotInstalledDialogFragment = ProxyNotInstalledDialog.displayDialog(proxyMode)

                            // Try to show the dialog.  Sometimes the window is not yet active if returning from Settings.
                            try {
                                // Display the I2P not installed alert dialog.
                                i2pNotInstalledDialogFragment.show(supportFragmentManager, getString(R.string.proxy_not_installed_dialog))
                            } catch (i2pNotInstalledException: Exception) {
                                // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                                pendingDialogsArrayList.add(PendingDialogDataClass(i2pNotInstalledDialogFragment, getString(R.string.proxy_not_installed_dialog)))
                            }
                        }
                    }
                }
            }

            ProxyHelper.CUSTOM -> {
                // Set the app bar and status bar backgrounds to indicate proxying is enabled.
                appBarLayout.setBackgroundResource(R.color.blue_background)

                // Set the background color if the API < 35 (Android 15).  https://redmine.stoutner.com/issues/1169
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 35)
                    window.statusBarColor = getColor(R.color.blue_background)
            }
        }

        // Reload the WebViews if requested and not waiting for the proxy.
        if (reloadWebViews && !waitingForProxy) {
            // Reload the WebViews.
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the fragment view.
                val fragmentView = webViewTabFragment.view

                // Only reload the WebViews if they exist.
                if (fragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Reload the WebView.
                    nestedScrollWebView.reload()
                }
            }
        }
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun bookmarksBack(@Suppress("UNUSED_PARAMETER")view: View?) {
        if (currentBookmarksFolderId == HOME_FOLDER_ID) {  // The home folder is displayed.
            // close the bookmarks drawer.
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {  // A subfolder is displayed.
            // Set the former parent folder as the current folder.
            currentBookmarksFolderId = bookmarksDatabaseHelper!!.getParentFolderId(currentBookmarksFolderId)

            // Load the new folder.
            loadBookmarksFolder()
        }
    }

    private fun calculateSettingsInt(settingCurrentlyEnabled: Boolean, settingEnabledByDefault: Boolean): Int {
        return if (settingCurrentlyEnabled == settingEnabledByDefault)  // The current system default is used.
            SYSTEM_DEFAULT
        else if (settingCurrentlyEnabled)  // The setting is enabled, which is different from the system default.
            ENABLED
        else  // The setting is disabled, which is different from the system default.
            DISABLED
    }

    private fun clearAndExit() {
        // Close the bookmarks cursor if it exists.
        bookmarksCursor?.close()

        // Close the databases helpers if they exist.
        bookmarksDatabaseHelper?.close()
        domainsDatabaseHelper?.close()

        // Get the status of the clear everything preference.
        val clearEverything = sharedPreferences.getBoolean(getString(R.string.clear_everything_key), true)

        // Get a handle for the runtime.
        val runtime = Runtime.getRuntime()

        // Get the application's private data directory, which will be something like `/data/user/0/com.audeon.browser.standard`,
        // which links to `/data/data/com.audeon.browser.standard`.
        val privateDataDirectoryString = applicationInfo.dataDir

        // Clear cookies.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_cookies_key), true)) {
            // Ass the cookie manager to delete all the cookies.
            cookieManager.removeAllCookies(null)

            // Ask the cookie manager to flush the cookie database.
            cookieManager.flush()

            // Manually delete the cookies database, as the cookie manager sometimes will not flush its changes to disk before system exit is run.
            try {
                // Two commands must be used because `Runtime.exec()` does not like `*`.
                val deleteCookiesProcess = runtime.exec("rm -f $privateDataDirectoryString/app_webview/Cookies")
                val deleteCookiesJournalProcess = runtime.exec("rm -f $privateDataDirectoryString/app_webview/Cookies-journal")

                // Wait until the processes have finished.
                deleteCookiesProcess.waitFor()
                deleteCookiesJournalProcess.waitFor()
            } catch (exception: Exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Clear DOM storage.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_dom_storage_key), true)) {
            // Ask web storage to clear the DOM storage.
            WebStorage.getInstance().deleteAllData()

            // Manually delete the DOM storage files and directories, as web storage sometimes will not flush its changes to disk before system exit is run.
            try {
                // A string array must be used because the directory contains a space and `Runtime.exec` will otherwise not escape the string correctly.
                val deleteLocalStorageProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Local Storage/"))
                val deleteSessionStorageProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Session Storage/"))
                val deleteWebDataProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Web Data"))
                val deleteWebDataJournalProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Web Data-journal"))

                // Multiple commands must be used because `Runtime.exec()` does not like `*`.
                val deleteIndexProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/IndexedDB")
                val deleteWebStorageProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/WebStorage")
                val deleteDatabasesProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/databases")
                val deleteBlobStorageProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview/Default/blob_storage")

                // Wait until the processes have finished.
                deleteLocalStorageProcess.waitFor()
                deleteSessionStorageProcess.waitFor()
                deleteWebDataProcess.waitFor()
                deleteWebDataJournalProcess.waitFor()
                deleteIndexProcess.waitFor()
                deleteWebStorageProcess.waitFor()
                deleteDatabasesProcess.waitFor()
                deleteBlobStorageProcess.waitFor()
            } catch (exception: Exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Clear the logcat.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_logcat_key), true)) {
            try {
                // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                val process = Runtime.getRuntime().exec("logcat -b all -c")

                // Wait for the process to finish.
                process.waitFor()
            } catch (exception: IOException) {
                // Do nothing.
            } catch (exception: InterruptedException) {
                // Do nothing.
            }
        }

        // Clear the cache.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_cache_key), true)) {
            // Clear the cache from each WebView.
            for (i in 0 until webViewStateAdapter!!.itemCount) {
                // Get the WebView tab fragment.
                val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                // Get the WebView fragment view.
                val webViewFragmentView = webViewTabFragment.view

                // Only clear the cache if the WebView exists.
                if (webViewFragmentView != null) {
                    // Get the nested scroll WebView from the tab fragment.
                    val nestedScrollWebView = webViewFragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                    // Clear the cache for this WebView.
                    nestedScrollWebView.clearCache(true)
                }
            }

            // Manually delete the cache directories.
            try {
                // Delete the main cache directory.
                val deleteCacheProcess = runtime.exec("rm -rf $privateDataDirectoryString/cache")

                // Delete the secondary `Service Worker` cache directory.
                // A string array must be used because the directory contains a space and `Runtime.exec` will otherwise not escape the string correctly.
                val deleteServiceWorkerProcess = runtime.exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Service Worker/"))

                // Wait until the processes have finished.
                deleteCacheProcess.waitFor()
                deleteServiceWorkerProcess.waitFor()
            } catch (exception: Exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Wipe out each WebView.
        for (i in 0 until webViewStateAdapter!!.itemCount) {
            // Get the WebView tab fragment.
            val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

            // Get the WebView frame layout.
            val webViewFrameLayout = webViewTabFragment.view as FrameLayout?

            // Only wipe out the WebView if it exists.
            if (webViewFrameLayout != null) {
                // Get the nested scroll WebView from the tab fragment.
                val nestedScrollWebView = webViewFrameLayout.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                // Clear SSL certificate preferences for this WebView.
                nestedScrollWebView.clearSslPreferences()

                // Clear the back/forward history for this WebView.
                nestedScrollWebView.clearHistory()

                // Remove all the views from the frame layout.
                webViewFrameLayout.removeAllViews()

                // Destroy the internal state of the WebView.
                nestedScrollWebView.destroy()
            }
        }

        // Manually delete the `app_webview` folder, which contains the cookies, DOM storage, form data, and `Service Worker` cache.
        // See `https://code.google.com/p/android/issues/detail?id=233826&thanks=233826&ts=1486670530`.
        if (clearEverything) {
            try {
                // Delete the folder.
                val deleteAppWebviewProcess = runtime.exec("rm -rf $privateDataDirectoryString/app_webview")

                // Wait until the process has finished.
                deleteAppWebviewProcess.waitFor()
            } catch (exception: Exception) {
                // Do nothing if an error is thrown.
            }
        }

        // Close Privacy Browser.  `finishAndRemoveTask` also removes Privacy Browser from the recent app list.
        finishAndRemoveTask()

        // Remove the terminated program from RAM.  The status code is `0`.
        exitProcess(0)
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun closeFindOnPage(@Suppress("UNUSED_PARAMETER")view: View?) {
        // Delete the contents of the find on page edit text.
        findOnPageEditText.text = null

        // Clear the highlighted phrases if the WebView is not null.
        currentWebView?.clearMatches()

        // Hide the find on page linear layout.
        findOnPageLinearLayout.visibility = View.GONE

        // Show the toolbar.
        toolbar.visibility = View.VISIBLE

        // Hide the keyboard.
        inputMethodManager.hideSoftInputFromWindow(toolbar.windowToken, 0)
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun closeTab(@Suppress("UNUSED_PARAMETER")view: View?) {
        // Run the command according to the number of tabs.
        if (tabLayout.tabCount > 1) {  // There is more than one tab open.
            // Get the current tab number.
            val currentTabNumber = tabLayout.selectedTabPosition

            // Delete the current tab.
            tabLayout.removeTabAt(currentTabNumber)

            // Delete the current page.  If the selected page number did not change during the delete (because the newly selected tab has has same number as the previously deleted tab), it will return true,
            // meaning that the current WebView must be reset.  Otherwise it will happen automatically as the selected tab number changes.
            if (webViewStateAdapter!!.deletePage(currentTabNumber, webViewViewPager2))
                setCurrentWebView(currentTabNumber)
        } else {  // There is only one tab open.
            clearAndExit()
        }
    }

    override fun createBookmark(dialogFragment: DialogFragment) {
        // Get the dialog.
        val dialog = dialogFragment.dialog!!

        // Get the views from the dialog fragment.
        val webpageFavoriteIconRadioButton = dialog.findViewById<RadioButton>(R.id.webpage_favorite_icon_radiobutton)
        val webpageFavoriteIconImageView = dialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)
        val customIconImageView = dialog.findViewById<ImageView>(R.id.custom_icon_imageview)
        val bookmarkNameEditText = dialog.findViewById<EditText>(R.id.bookmark_name_edittext)
        val bookmarkUrlEditText = dialog.findViewById<EditText>(R.id.bookmark_url_edittext)

        // Extract the strings from the edit texts.
        val bookmarkNameString = bookmarkNameEditText.text.toString()
        val bookmarkUrlString = bookmarkUrlEditText.text.toString()

        // Get the selected favorite icon drawable.
        val favoriteIconDrawable = if (webpageFavoriteIconRadioButton.isChecked)  // Use the webpage favorite icon.
            webpageFavoriteIconImageView.drawable
        else  // Use the custom icon.
            customIconImageView.drawable

        // Cast the favorite icon bitmap to a bitmap drawable
        val favoriteIconBitmapDrawable = favoriteIconDrawable as BitmapDrawable

        // Convert the favorite icon bitmap drawable to a bitmap.
        val favoriteIconBitmap = favoriteIconBitmapDrawable.bitmap

        // Create a favorite icon byte array output stream.
        val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

        // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

        // Convert the favorite icon byte array stream to a byte array.
        val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

        // Display the new bookmark below the current items in the (0 indexed) list.
        val newBookmarkDisplayOrder = bookmarksListView.count

        // Create the bookmark.
        bookmarksDatabaseHelper!!.createBookmark(bookmarkNameString, bookmarkUrlString, currentBookmarksFolderId, newBookmarkDisplayOrder, favoriteIconByteArray)

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = if (sortBookmarksAlphabetically)
            bookmarksDatabaseHelper!!.getBookmarksSortedAlphabetically(currentBookmarksFolderId)
        else
            bookmarksDatabaseHelper!!.getBookmarksByDisplayOrder(currentBookmarksFolderId)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)

        // Scroll to the new bookmark.
        bookmarksListView.setSelection(newBookmarkDisplayOrder)
    }

    override fun createBookmarkFolder(dialogFragment: DialogFragment) {
        // Get the dialog.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views in the dialog fragment.
        val defaultFolderIconRadioButton = dialog.findViewById<RadioButton>(R.id.default_folder_icon_radiobutton)
        val defaultFolderIconImageView = dialog.findViewById<ImageView>(R.id.default_folder_icon_imageview)
        val webpageFavoriteIconRadioButton = dialog.findViewById<RadioButton>(R.id.webpage_favorite_icon_radiobutton)
        val webpageFavoriteIconImageView = dialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)
        val customIconImageView = dialog.findViewById<ImageView>(R.id.custom_icon_imageview)
        val folderNameEditText = dialog.findViewById<EditText>(R.id.folder_name_edittext)

        // Get new folder name string.
        val folderNameString = folderNameEditText.text.toString()

        // Set the folder icon bitmap according to the dialog.
        val folderIconDrawable = if (defaultFolderIconRadioButton.isChecked)  // Use the default folder icon.
            defaultFolderIconImageView.drawable
        else if (webpageFavoriteIconRadioButton.isChecked)  // Use the webpage favorite icon.
            webpageFavoriteIconImageView.drawable
        else  // Use the custom icon.
            customIconImageView.drawable

        // Cast the folder icon bitmap to a bitmap drawable.
        val folderIconBitmapDrawable = folderIconDrawable as BitmapDrawable

        // Convert the folder icon bitmap drawable to a bitmap.
        val folderIconBitmap = folderIconBitmapDrawable.bitmap

        // Create a folder icon byte array output stream.
        val folderIconByteArrayOutputStream = ByteArrayOutputStream()

        // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, folderIconByteArrayOutputStream)

        // Convert the folder icon byte array stream to a byte array.
        val folderIconByteArray = folderIconByteArrayOutputStream.toByteArray()

        // Move all the bookmarks down one in the display order.
        for (i in 0 until bookmarksListView.count) {
            // Get the bookmark database id.
            val databaseId = bookmarksListView.getItemIdAtPosition(i).toInt()

            // Move the bookmark down one slot.
            bookmarksDatabaseHelper!!.updateDisplayOrder(databaseId, displayOrder = i + 1)
        }

        // Create the folder, which will be placed at the top of the list view.
        bookmarksDatabaseHelper!!.createFolder(folderNameString, currentBookmarksFolderId, displayOrder = 0, folderIconByteArray)

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = if (sortBookmarksAlphabetically)
            bookmarksDatabaseHelper!!.getBookmarksSortedAlphabetically(currentBookmarksFolderId)
        else
            bookmarksDatabaseHelper!!.getBookmarksByDisplayOrder(currentBookmarksFolderId)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)

        // Scroll to the new folder.
        bookmarksListView.setSelection(0)
    }

    private fun exitFullScreenVideo() {
        // Unset the full screen video flag.
        displayingFullScreenVideo = false

        // Hide the full screen video according to the API and display under cutouts status.
        if ((Build.VERSION.SDK_INT < 35) && displayUnderCutouts) {  // The device is running API < 35 and display under cutouts is enabled.
            // Re-enable the screen timeout.
            oldFullScreenVideoFrameLayout.keepScreenOn = false

            // Remove all the views from the full screen video frame layout.
            oldFullScreenVideoFrameLayout.removeAllViews()

            // Hide the full screen video frame layout.
            oldFullScreenVideoFrameLayout.visibility = View.GONE
        } else {  // The device is running API >= 35 or display under cutouts is disabled.
            // Re-enable the screen timeout.
            fullScreenVideoFrameLayout.keepScreenOn = false

            // Remove all the views from the full screen video frame layout.
            fullScreenVideoFrameLayout.removeAllViews()

            // Hide the full screen video frame layout.
            fullScreenVideoFrameLayout.visibility = View.GONE
        }

        // Enable the sliding drawers.
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        // Show the browser view according to the API and display under cutouts status.
        if ((Build.VERSION.SDK_INT < 35) && displayUnderCutouts) {  // The device is running API < 35 and display under cutouts is enabled.
            // Display the app bar if it is not supposed to be hidden, the `||` ensures that `!hideAppBar` is only evaluated in `inFullScreenBrowsingMode == true`.
            if (!inFullScreenBrowsingMode || !hideAppBar) {
                // Show the tab linear layout.
                tabsLinearLayout.visibility = View.VISIBLE

                // Show the app bar.
                appBar.show()
            }

            // Display the swipe refresh layout (which includes the WebView).
            swipeRefreshLayout.visibility = View.VISIBLE

            // Enable fits system windows if not in full screen browsing mode.
            if (!inFullScreenBrowsingMode) {
                coordinatorLayout.fitsSystemWindows = true
                bookmarksFrameLayout.fitsSystemWindows = true
            }
        } else {  // The device is running API >= 35 or display under cutouts is disabled.
            browserFrameLayout.visibility = View.VISIBLE
        }

        // Apply the appropriate full screen mode flags.
        if (inFullScreenBrowsingMode) {  // Privacy Browser is currently in full screen browsing mode.
            // Hide the app bar if specified.
            if (hideAppBar) {
                // Hide the tab linear layout.
                tabsLinearLayout.visibility = View.GONE

                // Hide the app bar.
                appBar.hide()
            }

            if (Build.VERSION.SDK_INT < 30) {
                /* Hide the system bars.
                 * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                 * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                 * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                 */

                @Suppress("DEPRECATION")
                window.addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        } else {  // Switch to normal viewing mode.
            // Show the system bars according to the API.
            if (Build.VERSION.SDK_INT >= 30) {
                // Show the system bars.
                windowInsetsController.show(WindowInsets.Type.systemBars())
            } else {
                /* Show the system bars.
                 * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                 * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                 * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                 * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                 */

                @Suppress("DEPRECATION")
                window.clearFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun findNextOnPage(@Suppress("UNUSED_PARAMETER")view: View?) {
        // Go to the next highlighted phrase on the page. `true` goes forwards instead of backwards.
        currentWebView!!.findNext(true)
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun findPreviousOnPage(@Suppress("UNUSED_PARAMETER")view: View?) {
        // Go to the previous highlighted phrase on the page.  `false` goes backwards instead of forwards.
        currentWebView!!.findNext(false)
    }

    override fun finishedPopulatingFilterLists(combinedFilterLists: ArrayList<ArrayList<List<Array<String>>>>) {
        // Store the filter lists.
        easyList = combinedFilterLists[0]
        easyPrivacy = combinedFilterLists[1]
        fanboysAnnoyanceList = combinedFilterLists[2]
        fanboysSocialList = combinedFilterLists[3]
        ultraList = combinedFilterLists[4]
        ultraPrivacy = combinedFilterLists[5]

        // Check to see if the activity has been restarted with a saved state.
        if ((savedStateArrayList == null) || (savedStateArrayList!!.size == 0)) {  // The activity has not been restarted or it was restarted on start to change the theme.
            // Add the first tab.
            addNewPage(urlString = "", adjacent = false, moveToTab = false)
        } else {  // The activity has been restarted with a saved state.
            // Restore each tab.
            for (i in savedStateArrayList!!.indices) {
                // Add a new tab.
                tabLayout.addTab(tabLayout.newTab())

                // Get the new tab.
                val newTab = tabLayout.getTabAt(i)!!

                // Set a custom view on the new tab.
                newTab.setCustomView(R.layout.tab_custom_view)

                // Add the new page.
                webViewStateAdapter!!.restorePage(savedStateArrayList!![i], savedNestedScrollWebViewStateArrayList!![i])
            }

            // Reset the saved state variables.
            savedStateArrayList = null
            savedNestedScrollWebViewStateArrayList = null

            // Get the intent that started the app.
            val intent = intent

            // Reset the intent.  This prevents a duplicate tab from being created on restart.
            setIntent(Intent())

            // Get the information from the intent.
            val intentAction = intent.action
            val intentUriData = intent.data
            val intentStringExtra = intent.getStringExtra(Intent.EXTRA_TEXT)

            // Determine if this is a web search.
            val isWebSearch = (intentAction != null) && (intentAction == Intent.ACTION_WEB_SEARCH)

            // Only process the URI if it contains data or it is a web search.  If the user pressed the desktop icon after the app was already running the URI will be null.
            if ((intentUriData != null) || (intentStringExtra != null) || isWebSearch) {  // A new tab is being loaded.
                // Get the URL string.
                val urlString = if (isWebSearch) {  // The intent is a web search.
                    // Sanitize the search input.
                    val encodedSearchString: String = try {
                        URLEncoder.encode(intent.getStringExtra(SearchManager.QUERY), "UTF-8")
                    } catch (exception: UnsupportedEncodingException) {
                        ""
                    }

                    // Add the base search URL.
                    searchURL + encodedSearchString
                } else { // The intent contains a URL formatted as a URI or a URL in the string extra.
                    // Get the URL string.
                    intentUriData?.toString() ?: intentStringExtra!!
                }

                // Add a new tab if specified in the preferences.
                if (sharedPreferences.getBoolean(getString(R.string.open_intents_in_new_tab_key), true)) {  // Load the URL in a new tab.
                    // Set the loading new intent flag.
                    loadingNewIntent = true

                    // Add a new tab.
                    addNewPage(urlString, adjacent = false, moveToTab = true)
                } else {  // Load the URL in the current tab.
                    // Make it so.
                    loadUrl(currentWebView!!, urlString)
                }
            } else {  // A new tab is not being loaded.
                // Restore the selected tab position.
                if (savedTabPosition == 0) {  // The first tab is selected.
                    // Set the first page as the current WebView.
                    setCurrentWebView(0)
                } else {  // The first tab is not selected.
                    // Select the tab when the layout has finished populating.
                    tabLayout.post {
                        // Get a handle for the tab.
                        val tab = tabLayout.getTabAt(savedTabPosition)!!

                        // Select the tab.
                        tab.select()
                    }
                }
            }
        }
    }

    // Remove the warning that `OnTouchListener()` needs to override `performClick()`, as the only purpose of setting the `OnTouchListener()` is to make it do nothing.
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeApp() {
        // Get a handle for the input method.
        inputMethodManager = (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)

        // Initialize the color spans for highlighting the URLs.
        initialGrayColorSpan = ForegroundColorSpan(getColor(R.color.gray_500))
        finalGrayColorSpan = ForegroundColorSpan(getColor(R.color.gray_500))
        redColorSpan = ForegroundColorSpan(getColor(R.color.red_text))

        // Remove the formatting from the URL edit text when the user is editing the text.
        urlEditText.onFocusChangeListener = View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {  // The user is editing the URL text box.
                // Remove the syntax highlighting.
                urlEditText.text.removeSpan(redColorSpan)
                urlEditText.text.removeSpan(initialGrayColorSpan)
                urlEditText.text.removeSpan(finalGrayColorSpan)
            } else {  // The user has stopped editing the URL text box.
                // Move to the beginning of the string.
                urlEditText.setSelection(0)

                // Reapply the syntax highlighting.
                UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan)
            }
        }

        // Set the go button on the keyboard to load the URL in url text box.
        urlEditText.setOnKeyListener { _: View?, keyCode: Int, keyEvent: KeyEvent ->
            // If the event is a key-down event on the `enter` button, load the URL.
            if ((keyEvent.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {  // The enter key was pressed.
                // Load the URL.
                loadUrlFromTextBox()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // Some other key was pressed.
                // Do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Create an Orbot status broadcast receiver.
        orbotStatusBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Get the content of the status message.
                orbotStatus = intent.getStringExtra("org.torproject.android.intent.extra.STATUS")!!

                // If Privacy Browser is waiting on the proxy, load the website now that Orbot is connected.
                if ((orbotStatus == ProxyHelper.ORBOT_STATUS_ON) && waitingForProxy) {
                    // Reset the waiting for proxy status.
                    waitingForProxy = false

                    // Get a list of the current fragments.
                    val fragmentList = supportFragmentManager.fragments

                    // Check each fragment to see if it is a waiting for proxy dialog.  Sometimes more than one is displayed.
                    for (i in fragmentList.indices) {
                        // Get the fragment tag.
                        val fragmentTag = fragmentList[i].tag

                        // Check to see if it is the waiting for proxy dialog.
                        if (fragmentTag != null && fragmentTag == getString(R.string.waiting_for_proxy_dialog)) {
                            // Dismiss the waiting for proxy dialog.
                            (fragmentList[i] as DialogFragment).dismiss()
                        }
                    }

                    // Reload existing URLs and load any URLs that are waiting for the proxy.
                    for (i in 0 until webViewStateAdapter!!.itemCount) {
                        // Get the WebView tab fragment.
                        val webViewTabFragment = webViewStateAdapter!!.getPageFragment(i)

                        // Get the fragment view.
                        val fragmentView = webViewTabFragment.view

                        // Only process the WebViews if they exist.
                        if (fragmentView != null) {
                            // Get the nested scroll WebView from the tab fragment.
                            val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

                            // Get the waiting for proxy URL string.
                            val waitingForProxyUrlString = nestedScrollWebView.waitingForProxyUrlString

                            // Load the pending URL if it exists.
                            if (waitingForProxyUrlString.isNotEmpty()) {  // A URL is waiting to be loaded.
                                // Load the URL.
                                loadUrl(nestedScrollWebView, waitingForProxyUrlString)

                                // Reset the waiting for proxy URL string.
                                nestedScrollWebView.waitingForProxyUrlString = ""
                            } else {  // No URL is waiting to be loaded.
                                // Reload the existing URL.
                                nestedScrollWebView.reload()
                            }
                        }
                    }
                }
            }
        }

        // Register the Orbot status broadcast receiver.  `ContextCompat` must be used until the minimum API >= 34.
        ContextCompat.registerReceiver(this, orbotStatusBroadcastReceiver, IntentFilter("org.torproject.android.intent.action.STATUS"), ContextCompat.RECEIVER_EXPORTED)

        // Get handles for views that need to be modified.
        val bookmarksHeaderLinearLayout = findViewById<LinearLayout>(R.id.bookmarks_header_linearlayout)
        val launchBookmarksActivityFab = findViewById<FloatingActionButton>(R.id.launch_bookmarks_activity_fab)
        val createBookmarkFolderFab = findViewById<FloatingActionButton>(R.id.create_bookmark_folder_fab)
        val createBookmarkFab = findViewById<FloatingActionButton>(R.id.create_bookmark_fab)

        // Handle tab selections.
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Close the find on page bar if it is open.
                closeFindOnPage(null)

                // Update the view pager when it has quiesced.  Otherwise, if a page launched by a new intent on restart has not yet been created, the view pager will not be updated to match the tab layout.
                webViewViewPager2.post {
                    // Select the same page in the view pager.
                    webViewViewPager2.currentItem = tab.position

                    // Set the current WebView after the tab layout has quiesced (otherwise, sometimes the wong WebView might be used).  See <https://redmine.stoutner.com/issues/1136>
                    tabLayout.post {
                        setCurrentWebView(tab.position)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Only display the view SSL certificate dialog if the current WebView is not null.
                // This can happen if the tab is programmatically reselected while the app is being restarted and is not yet populated.
                if (currentWebView != null) {
                    // Instantiate the View SSL Certificate dialog.
                    val viewSslCertificateDialogFragment: DialogFragment = ViewSslCertificateDialog.displayDialog(currentWebView!!.webViewFragmentId, currentWebView!!.getFavoriteIcon())

                    // Display the View SSL Certificate dialog.
                    viewSslCertificateDialogFragment.show(supportFragmentManager, getString(R.string.view_ssl_certificate))
                }
            }
        })

        // Set a touch listener on the bookmarks header linear layout so that touches don't pass through to the button underneath.
        bookmarksHeaderLinearLayout.setOnTouchListener { _: View?, _: MotionEvent? -> true }

        // Set the launch bookmarks activity floating action button to launch the bookmarks activity.
        launchBookmarksActivityFab.setOnClickListener {
            // Get a copy of the favorite icon bitmap.
            val currentFavoriteIconBitmap = currentWebView!!.getFavoriteIcon()

            // Create a favorite icon byte array output stream.
            val currentFavoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            currentFavoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, currentFavoriteIconByteArrayOutputStream)

            // Convert the favorite icon byte array stream to a byte array.
            val currentFavoriteIconByteArray = currentFavoriteIconByteArrayOutputStream.toByteArray()

            // Create an intent to launch the bookmarks activity.
            val bookmarksIntent = Intent(applicationContext, BookmarksActivity::class.java)

            // Add the extra information to the intent.
            bookmarksIntent.putExtra(CURRENT_FOLDER_ID, currentBookmarksFolderId)
            bookmarksIntent.putExtra(CURRENT_TITLE, currentWebView!!.title)
            bookmarksIntent.putExtra(CURRENT_URL, currentWebView!!.url)
            bookmarksIntent.putExtra(CURRENT_FAVORITE_ICON_BYTE_ARRAY, currentFavoriteIconByteArray)

            // Make it so.
            startActivity(bookmarksIntent)
        }

        // Set the create new bookmark folder floating action button to display an alert dialog.
        createBookmarkFolderFab.setOnClickListener {
            // Create a create bookmark folder dialog.
            val createBookmarkFolderDialog: DialogFragment = CreateBookmarkFolderDialog.createBookmarkFolder(currentWebView!!.getFavoriteIcon())

            // Show the create bookmark folder dialog.
            createBookmarkFolderDialog.show(supportFragmentManager, getString(R.string.create_folder))
        }

        // Set the create new bookmark floating action button to display an alert dialog.
        createBookmarkFab.setOnClickListener {
            // Instantiate the create bookmark dialog.
            val createBookmarkDialog: DialogFragment = CreateBookmarkDialog.createBookmark(currentWebView!!.url!!, currentWebView!!.title!!, currentWebView!!.getFavoriteIcon())

            // Display the create bookmark dialog.
            createBookmarkDialog.show(supportFragmentManager, getString(R.string.create_bookmark))
        }

        // Search for the string on the page whenever a character changes in the find on page edit text.
        findOnPageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable) {
                // Search for the text in the WebView if it is not null.  Sometimes on resume after a period of non-use the WebView will be null.
                currentWebView?.findAllAsync(editable.toString())
            }
        })

        // Set the `check mark` button for the find on page edit text keyboard to close the soft keyboard.
        findOnPageEditText.setOnKeyListener { _: View?, keyCode: Int, keyEvent: KeyEvent ->
            if ((keyEvent.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {  // The `enter` key was pressed.
                // Hide the soft keyboard.
                inputMethodManager.hideSoftInputFromWindow(currentWebView!!.windowToken, 0)

                // Consume the event.
                return@setOnKeyListener true
            } else {  // A different key was pressed.
                // Do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Implement swipe to refresh.
        swipeRefreshLayout.setOnRefreshListener {
            // Reload the website.
            currentWebView!!.reload()
        }

        // Store the default progress view offsets.
        defaultProgressViewStartOffset = swipeRefreshLayout.progressViewStartOffset
        defaultProgressViewEndOffset = swipeRefreshLayout.progressViewEndOffset

        // Set the refresh color scheme according to the theme.
        swipeRefreshLayout.setColorSchemeResources(R.color.blue_text)

        // Initialize a color background typed value.
        val colorBackgroundTypedValue = TypedValue()

        // Get the color background from the theme.
        theme.resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true)

        // Get the color background int from the typed value.
        val colorBackgroundInt = colorBackgroundTypedValue.data

        // Set the swipe refresh background color.
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorBackgroundInt)

        // Set the drawer titles, which identify the drawer layouts in accessibility mode.
        drawerLayout.setDrawerTitle(GravityCompat.START, getString(R.string.navigation_drawer))
        drawerLayout.setDrawerTitle(GravityCompat.END, getString(R.string.bookmarks))

        // Handle clicks on bookmarks.
        bookmarksListView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
            // Convert the id from long to int to match the format of the bookmarks database.
            val databaseId = id.toInt()

            // Get the bookmark cursor for this ID.
            val bookmarkCursor = bookmarksDatabaseHelper!!.getBookmark(databaseId)

            // Move the bookmark cursor to the first row.
            bookmarkCursor.moveToFirst()

            // Act upon the bookmark according to the type.
            if (bookmarkCursor.getInt(bookmarkCursor.getColumnIndexOrThrow(IS_FOLDER)) == 1) {  // The selected bookmark is a folder.
                // Store the folder ID.
                currentBookmarksFolderId = bookmarkCursor.getLong(bookmarkCursor.getColumnIndexOrThrow(FOLDER_ID))

                // Load the new folder.
                loadBookmarksFolder()
            } else {  // The selected bookmark is not a folder.
                // Load the bookmark URL.
                loadUrl(currentWebView!!, bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BOOKMARK_URL)))

                // Close the bookmarks drawer if it is not pinned.
                if (!bookmarksDrawerPinned)
                    drawerLayout.closeDrawer(GravityCompat.END)
            }

            // Close the cursor.
            bookmarkCursor.close()
        }

        // Handle long-presses on bookmarks.
        bookmarksListView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
            // Convert the database ID from `long` to `int`.
            val databaseId = id.toInt()

            // Run the commands associated with the type.
            if (bookmarksDatabaseHelper!!.isFolder(databaseId)) {  // The bookmark is a folder.
                // Get the folder ID.
                val folderId = bookmarksDatabaseHelper!!.getFolderId(databaseId)

                // Get a cursor of all the bookmarks in the folder.
                val bookmarksCursor = bookmarksDatabaseHelper!!.getFolderBookmarks(folderId)

                // Move to the first entry in the cursor.
                bookmarksCursor.moveToFirst()

                // Open each bookmark
                for (i in 0 until bookmarksCursor.count) {
                    // Load the bookmark in a new tab, moving to the tab for the first bookmark if the drawer is not pinned.
                    addNewPage(bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BOOKMARK_URL)), adjacent = false, moveToTab = !bookmarksDrawerPinned && (i == 0))

                    // Move to the next bookmark.
                    bookmarksCursor.moveToNext()
                }

                // Close the cursor.
                bookmarksCursor.close()
            } else {  // The bookmark is not a folder.
                // Get the bookmark cursor for this ID.
                val bookmarkCursor = bookmarksDatabaseHelper!!.getBookmark(databaseId)

                // Move the bookmark cursor to the first row.
                bookmarkCursor.moveToFirst()

                // Load the bookmark in a new tab and move to the tab if the drawer is not pinned.
                addNewPage(bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BOOKMARK_URL)), adjacent = true, moveToTab = !bookmarksDrawerPinned)

                // Close the cursor.
                bookmarkCursor.close()
            }

            // Close the bookmarks drawer if it is not pinned.
            if (!bookmarksDrawerPinned)
                drawerLayout.closeDrawer(GravityCompat.END)

            // Consume the event.
            true
        }

        // The drawer listener is used to update the navigation menu.
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                // Close the navigation drawer if requested.  <https://redmine.stoutner.com/issues/1267>
                if (closeNavigationDrawer) {
                    // Reset the close navigation drawer flag.
                    closeNavigationDrawer = false

                    // Close the navigation drawer.
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
            }

            override fun onDrawerClosed(drawerView: View) {}

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_SETTLING || newState == DrawerLayout.STATE_DRAGGING) {  // A drawer is opening or closing.
                    // Adjust the scroll position of the navigation drawer.
                    if (bottomAppBar && navigationDrawerFirstView) {  // The bottom app bar is in use.
                        // Reset the navigation drawer first view flag.
                        navigationDrawerFirstView = false

                        // Get a handle for the navigation recycler view.
                        val navigationRecyclerView = navigationView.getChildAt(0) as RecyclerView

                        // Get the navigation linear layout manager.
                        val navigationLinearLayoutManager = navigationRecyclerView.layoutManager as LinearLayoutManager

                        // Scroll the navigation drawer to the bottom.
                        navigationLinearLayoutManager.scrollToPositionWithOffset(14, 0)
                    } else if (Build.VERSION.SDK_INT < 35 && navigationDrawerFirstView) {  // The top app bar is in use and the API < 35 (which causes the drawer to scroll down for some reason).
                        // Reset the navigation drawer first view flag.
                        navigationDrawerFirstView = false

                        // Get a handle for the navigation recycler view.
                        val navigationRecyclerView = navigationView.getChildAt(0) as RecyclerView

                        // Get the navigation linear layout manager.
                        val navigationLinearLayoutManager = navigationRecyclerView.layoutManager as LinearLayoutManager

                        // Scroll the navigation drawer to the top.  <
                        navigationLinearLayoutManager.scrollToPositionWithOffset(0, 0)
                    }

                    // Update the navigation menu items if the WebView is not null.
                    if (currentWebView != null) {
                        // Set the enabled status of the menu items.
                        navigationBackMenuItem.isEnabled = currentWebView!!.canGoBack()
                        navigationForwardMenuItem.isEnabled = currentWebView!!.canGoForward()
                        navigationScrollToBottomMenuItem.isEnabled = (currentWebView!!.canScrollVertically(-1) || currentWebView!!.canScrollVertically(1))
                        navigationHistoryMenuItem.isEnabled = currentWebView!!.canGoBack() || currentWebView!!.canGoForward()

                        // Update the scroll menu item.
                        if (currentWebView!!.scrollY == 0) {  // The WebView is scrolled to the top.
                            // Set the title.
                            navigationScrollToBottomMenuItem.title = getString(R.string.scroll_to_bottom)

                            // Set the icon.
                            navigationScrollToBottomMenuItem.icon = AppCompatResources.getDrawable(applicationContext, R.drawable.move_to_bottom_enabled)
                        } else {  // The WebView is not scrolled to the top.
                            // Set the title.
                            navigationScrollToBottomMenuItem.title = getString(R.string.scroll_to_top)

                            // Set the icon.
                            navigationScrollToBottomMenuItem.icon = AppCompatResources.getDrawable(applicationContext, R.drawable.move_to_top_enabled)
                        }

                        // Display the number of blocked requests.
                        navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + currentWebView!!.getRequestsCount(BLOCKED_REQUESTS)

                        // Hide the keyboard (if displayed).
                        inputMethodManager.hideSoftInputFromWindow(currentWebView!!.windowToken, 0)
                    }

                    // Clear the focus from from the URL text box.  This removes any text selection markers and context menus, which otherwise draw above the open drawers.
                    urlEditText.clearFocus()

                    // Clear the focus from from the WebView if it is not null, which can happen if a user opens a drawer while the browser is being resumed.
                    // Clearing the focus from the WebView removes any text selection markers and context menus, which otherwise draw above the open drawers.
                    currentWebView?.clearFocus()
                }
            }
        })

        // Inflate a bare WebView to get the default user agent.  It is not used to render content on the screen.
        @SuppressLint("InflateParams") val webViewLayout = layoutInflater.inflate(R.layout.bare_webview, null, false)

        // Get a handle for the WebView.
        val bareWebView = webViewLayout.findViewById<WebView>(R.id.bare_webview)

        // Store the default user agent.
        webViewDefaultUserAgent = bareWebView.settings.userAgentString

        // Destroy the bare WebView.
        bareWebView.destroy()

        // Update the domains settings set.
        updateDomainsSettingsSet()

        // Instantiate the check filter list helper.
        checkFilterListHelper = CheckFilterListHelper()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initializeWebView(nestedScrollWebView: NestedScrollWebView, pagePosition: Int, progressBar: ProgressBar, urlString: String, restoringState: Boolean) {
        // Fix the bookmarks drawer top padding on API <= 29.
        if (Build.VERSION.SDK_INT <= 29) {
            // Set the top padding according to the app bar location.
            if (bottomAppBar)
                bookmarksListView.setPadding(bookmarksListView.paddingLeft, swipeRefreshLayout.top, bookmarksListView.paddingRight, bookmarksListView.paddingBottom)
            else
                bookmarksHeaderLinearLayout.setPadding(bookmarksHeaderLinearLayout.paddingLeft, appBarLayout.top, bookmarksHeaderLinearLayout.paddingRight, bookmarksHeaderLinearLayout.paddingBottom)
        }

        // Get the WebView theme.
        val webViewTheme = sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value))

        // Get the WebView theme entry values string array.
        val webViewThemeEntryValuesStringArray = resources.getStringArray(R.array.webview_theme_entry_values)

        // Set the WebView theme if algorithmic darkening is supported.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            // Set the WebView them.  A switch statement cannot be used because the WebView theme entry values string array is not a compile time constant.
            if (webViewTheme == webViewThemeEntryValuesStringArray[1]) {  // The light theme is selected.
                // Turn off algorithmic darkening.
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, false)

                // Make the WebView visible. The WebView was created invisible in `webview_framelayout` to prevent a white background splash in night mode.
                // If the system is currently in night mode, showing the WebView will be handled in `onProgressChanged()`.
                nestedScrollWebView.visibility = View.VISIBLE
            } else if (webViewTheme == webViewThemeEntryValuesStringArray[2]) {  // The dark theme is selected.
                // Turn on algorithmic darkening.
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, true)
            } else {  // The system default theme is selected.
                // Get the current theme status.
                val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

                // Set the algorithmic darkening according to the current system theme status.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {  // The system is in day mode.
                    // Turn off algorithmic darkening.
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, false)

                    // Make the WebView visible. The WebView was created invisible in `webview_framelayout` to prevent a white background splash in night mode.
                    // If the system is currently in night mode, showing the WebView will be handled in `onProgressChanged()`.
                    nestedScrollWebView.visibility = View.VISIBLE
                } else {  // The system is in night mode.
                    // Turn on algorithmic darkening.
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(nestedScrollWebView.settings, true)
                }
            }
        }

        // Disable using the web cache.
        nestedScrollWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Set the app bar scrolling.
        nestedScrollWebView.isNestedScrollingEnabled = scrollAppBar

        // Allow pinch to zoom.
        nestedScrollWebView.settings.builtInZoomControls = true

        // Hide zoom controls.
        nestedScrollWebView.settings.displayZoomControls = false

        // Don't allow mixed content (HTTP and HTTPS) on the same website.
        nestedScrollWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        // Set the WebView to load in overview mode (zoomed out to the maximum width).
        nestedScrollWebView.settings.loadWithOverviewMode = true

        // Explicitly disable geolocation.
        nestedScrollWebView.settings.setGeolocationEnabled(false)

        // Allow loading of file:// URLs.  This is necessary for opening MHT web archives, which are copied into a temporary cache location.
        nestedScrollWebView.settings.allowFileAccess = true

        // Create a double-tap gesture detector to toggle full-screen mode.
        val doubleTapGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            // Override `onDoubleTap()`.  All other events are handled using the default settings.
            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                return if (fullScreenBrowsingModeEnabled) {  // Only process the double-tap if full screen browsing mode is enabled.
                    // Toggle the full screen browsing mode tracker.
                    inFullScreenBrowsingMode = !inFullScreenBrowsingMode

                    // Toggle the full screen browsing mode.
                    if (inFullScreenBrowsingMode) {  // Switch to full screen mode.
                        // Hide the app bar if specified.
                        if (hideAppBar) {  // App bar hiding is enabled.
                            // Close the find on page bar if it is visible.
                            closeFindOnPage(null)

                            // Hide the tab linear layout.
                            tabsLinearLayout.visibility = View.GONE

                            // Hide the app bar.
                            appBar.hide()

                            // Set layout and scrolling parameters according to the position of the app bar.
                            if (bottomAppBar) {  // The app bar is at the bottom.
                                // Reset the WebView padding to fill the available space.
                                swipeRefreshLayout.setPadding(0, 0, 0, 0)
                            } else {  // The app bar is at the top.
                                // Check to see if the app bar is normally scrolled.
                                if (scrollAppBar) {  // The app bar is scrolled when it is displayed.
                                    // Get the swipe refresh layout parameters.
                                    val swipeRefreshLayoutParams = swipeRefreshLayout.layoutParams as CoordinatorLayout.LayoutParams

                                    // Remove the off-screen scrolling layout.
                                    swipeRefreshLayoutParams.behavior = null
                                } else {  // The app bar is not scrolled when it is displayed.
                                    // Remove the padding from the top of the swipe refresh layout.
                                    swipeRefreshLayout.setPadding(0, 0, 0, 0)

                                    // The swipe refresh circle must be moved above the now removed status bar location.
                                    swipeRefreshLayout.setProgressViewOffset(false, -200, defaultProgressViewEndOffset)
                                }
                            }
                        } else {  // App bar hiding is not enabled.
                            // Adjust the UI for the bottom app bar.
                            if (bottomAppBar) {
                                // Adjust the UI according to the scrolling of the app bar.
                                if (scrollAppBar) {
                                    // Reset the WebView padding to fill the available space.
                                    swipeRefreshLayout.setPadding(0, 0, 0, 0)
                                } else {
                                    // Move the WebView above the app bar layout.
                                    swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight)
                                }
                            }
                        }

                        // Hide the system bars.
                        if (Build.VERSION.SDK_INT >= 30) {
                            // Set the system bars to display transiently when swiped.
                            windowInsetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                            // Hide the system bars.
                            windowInsetsController.hide(WindowInsets.Type.systemBars())
                        } else {
                            /* Hide the system bars.
                             * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                             * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                             * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                             * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                             */

                            @Suppress("DEPRECATION")
                            window.addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                        }

                        // Disable fits system windows according to the configuration.
                        if (displayUnderCutouts) {  // Display under cutouts.
                            // Disable fits system windows according to the API.
                            if (Build.VERSION.SDK_INT >= 35) {  // The device is running API >= 35.
                                // Disable fits system windows.
                                browserFrameLayout.fitsSystemWindows = false

                                // Manually update the padding, which isn't done on API >= 35
                                browserFrameLayout.setPadding(0, 0, 0, 0)
                            } else {  // The device is running API < 35.
                                // Disable fits system windows.
                                coordinatorLayout.fitsSystemWindows = false
                                bookmarksFrameLayout.fitsSystemWindows = false

                                // Remove any padding on the bookmarks frame layout.
                                bookmarksFrameLayout.setPadding(0, 0, 0, 0)
                            }
                        } else if (Build.VERSION.SDK_INT < 35) {  // The device is running API < 35 and display under cutouts is disabled.
                            // Disable fits system windows to display under the navigation bar.
                            coordinatorLayout.fitsSystemWindows = false
                            bookmarksFrameLayout.fitsSystemWindows = false

                            // Remove any padding on the bookmarks frame layout.
                            bookmarksFrameLayout.setPadding(0, 0, 0, 0)
                        }
                    } else {  // Switch to normal viewing mode.
                        // Show the app bar if it was hidden.
                        if (hideAppBar) {
                            // Show the tab linear layout.
                            tabsLinearLayout.visibility = View.VISIBLE

                            // Show the app bar.
                            appBar.show()
                        }

                        // Set layout and scrolling parameters according to the position of the app bar.
                        if (bottomAppBar) {  // The app bar is at the bottom.
                            // Adjust the UI.
                            if (scrollAppBar) {
                                // Reset the WebView padding to fill the available space.
                                swipeRefreshLayout.setPadding(0, 0, 0, 0)
                            } else {
                                // Move the WebView above the app bar layout.
                                swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight)
                            }
                        } else {  // The app bar is at the top.
                            // Check to see if the app bar is normally scrolled.
                            if (scrollAppBar) {  // The app bar is scrolled when it is displayed.
                                // Get the swipe refresh layout parameters.
                                val swipeRefreshLayoutParams = swipeRefreshLayout.layoutParams as CoordinatorLayout.LayoutParams

                                // Add the off-screen scrolling layout.
                                swipeRefreshLayoutParams.behavior = AppBarLayout.ScrollingViewBehavior()
                            } else {  // The app bar is not scrolled when it is displayed.
                                // The swipe refresh layout must be manually moved below the app bar layout.
                                swipeRefreshLayout.setPadding(0, appBarHeight, 0, 0)

                                // The swipe to refresh circle doesn't always hide itself completely unless it is moved up 10 pixels.
                                swipeRefreshLayout.setProgressViewOffset(false, defaultProgressViewStartOffset - 10 + appBarHeight, defaultProgressViewEndOffset + appBarHeight)
                            }
                        }

                        // Enable fits system windows if display under cutouts is enabled.
                        if (displayUnderCutouts) {
                            // Enable fits system windows according to the API.
                            if (Build.VERSION.SDK_INT >= 35) {
                                browserFrameLayout.fitsSystemWindows = true
                            } else {
                                coordinatorLayout.fitsSystemWindows = true
                                bookmarksFrameLayout.fitsSystemWindows = true
                            }
                        } else if (Build.VERSION.SDK_INT < 35) {  // The device is running API < 35 and display under cutouts is disabled.
                            // Enable fits system windows.
                            coordinatorLayout.fitsSystemWindows = true
                            bookmarksFrameLayout.fitsSystemWindows = true
                        }

                        // Show the system bars according to the API.
                        if (Build.VERSION.SDK_INT >= 30) {
                            // Show the system bars.
                            windowInsetsController.show(WindowInsets.Type.systemBars())
                        } else {
                            /* Show the system bars.
                             * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                             * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                             * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                             * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                             */

                            @Suppress("DEPRECATION")
                            window.clearFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                        }
                    }

                    // Consume the double-tap.
                    true
                } else { // Do not consume the double-tap because full screen browsing mode is disabled.
                    // Return false.
                    false
                }
            }

            override fun onFling(motionEvent1: MotionEvent?, motionEvent2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                // Scroll the bottom app bar if enabled.
                if (bottomAppBar && scrollAppBar && !objectAnimator.isRunning && (motionEvent1 != null)) {
                    // Calculate the Y change.
                    val motionY = motionEvent2.y - motionEvent1.y

                    // Scroll the app bar if the change is greater than 50 pixels.
                    if (motionY > 50) {
                        // Animate the bottom app bar onto the screen.
                        objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", 0f)
                    } else if (motionY < -50) {
                        // Animate the bottom app bar off the screen.
                        objectAnimator = ObjectAnimator.ofFloat(appBarLayout, "translationY", appBarLayout.height.toFloat())
                    }

                    // Make it so.
                    objectAnimator.start()
                }

                // Do not consume the event.
                return false
            }
        })

        // Pass all touch events on the WebView through the double-tap gesture detector.
        nestedScrollWebView.setOnTouchListener { view: View, motionEvent: MotionEvent? ->
            // Call `performClick()` on the view, which is required for accessibility.
            view.performClick()

            // Check for double-taps.
            doubleTapGestureDetector.onTouchEvent(motionEvent!!)
        }

        // Register the WebView for a context menu.  This is used to see link targets and download images.
        registerForContextMenu(nestedScrollWebView)

        // Allow the downloading of files.
        nestedScrollWebView.setDownloadListener { downloadUrlString: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long ->
            // Use the specified download provider.
            if (downloadWithExternalApp) {  // Download with an external app.
                // Download with an external app.
                saveWithExternalApp(downloadUrlString)
            } else {  // Download with Privacy Browser or Android's download manager.
                // Process the content length if it contains data.
                val formattedFileSizeString = if (contentLength > 0) {  // The content length is greater than 0.
                    // Format the content length as a string.
                    NumberFormat.getInstance().format(contentLength) + " " + getString(R.string.bytes)
                } else {  // The content length is not greater than 0.
                    // Set the formatted file size string to be `unknown size`.
                    getString(R.string.unknown_size)
                }

                // Get the file name from the content disposition.
                val fileNameString = UrlHelper.getFileName(this, contentDisposition, mimetype, downloadUrlString)

                // Instantiate the save dialog according.
                val saveDialogFragment = SaveDialog.saveUrl(downloadUrlString, fileNameString, formattedFileSizeString, userAgent, nestedScrollWebView.acceptCookies)

                // Try to show the dialog.  The download listener continues to function even when the WebView is paused.  Attempting to display a dialog in that state leads to a crash.
                try {
                    // Show the save dialog.
                    saveDialogFragment.show(supportFragmentManager, getString(R.string.save_dialog))
                } catch (exception: Exception) {  // The dialog could not be shown.
                    // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                    pendingDialogsArrayList.add(PendingDialogDataClass(saveDialogFragment, getString(R.string.save_dialog)))
                }
            }

            // Get the current page position.
            val currentPagePosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

            // Get the corresponding tab.
            val tab = tabLayout.getTabAt(currentPagePosition)!!

            // Get the tab custom view.
            val tabCustomView = tab.customView!!

            // Get the tab views.
            val tabFavoriteIconImageView = tabCustomView.findViewById<ImageView>(R.id.favorite_icon_imageview)
            val tabTitleTextView = tabCustomView.findViewById<TextView>(R.id.title_textview)

            // Restore the previous webpage favorite icon and title if the title is currently set to `Loading...`.
            if (tabTitleTextView.text.toString() == getString(R.string.loading)) {
                // Restore the previous webpage title text.
                tabTitleTextView.text = nestedScrollWebView.previousWebpageTitle

                // Restore the previous webpage favorite icon if it is not null.
                if (nestedScrollWebView.previousFavoriteIconDrawable != null)
                    tabFavoriteIconImageView.setImageDrawable(nestedScrollWebView.previousFavoriteIconDrawable)
            }
        }

        // Update the find on page count.
        nestedScrollWebView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting && (numberOfMatches == 0)) {  // There are no matches.
                // Set the find on page count text view to be `0/0`.
                findOnPageCountTextView.setText(R.string.zero_of_zero)
            } else if (isDoneCounting) {  // There are matches.
                // The active match ordinal is zero-based.
                val activeMatch = activeMatchOrdinal + 1

                // Build the match string.
                val matchString = "$activeMatch/$numberOfMatches"

                // Update the find on page count text view.
                findOnPageCountTextView.text = matchString
            }
        }

        // Process scroll changes.
        nestedScrollWebView.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
            // Set the swipe to refresh status.
            if (nestedScrollWebView.swipeToRefresh)  // Only enable swipe to refresh if the WebView is scrolled to the top.
                swipeRefreshLayout.isEnabled = nestedScrollWebView.scrollY == 0
            else  // Disable swipe to refresh.
                swipeRefreshLayout.isEnabled = false
        }

        // Set the web chrome client.
        nestedScrollWebView.webChromeClient = object : WebChromeClient() {
            // Update the progress bar when a page is loading.
            override fun onProgressChanged(view: WebView, progress: Int) {
                // Update the progress bar.
                progressBar.progress = progress

                // Set the visibility of the progress bar.
                if (progress < 100) {
                    // Show the progress bar.
                    progressBar.visibility = View.VISIBLE
                } else {
                    // Hide the progress bar.
                    progressBar.visibility = View.GONE

                    //Stop the swipe to refresh indicator if it is running
                    swipeRefreshLayout.isRefreshing = false

                    // Make the current WebView visible.  If this is a new tab, the current WebView would have been created invisible in `webview_framelayout` to prevent a white background splash in night mode.
                    nestedScrollWebView.visibility = View.VISIBLE
                }
            }

            // Set the favorite icon when it changes.
            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                // Only update the favorite icon if the website has finished loading and the new favorite icon height is greater than the current favorite icon height.
                // This prevents low resolution icons from replacing high resolution one.
                // The check for the visibility of the progress bar can possibly be removed once https://redmine.stoutner.com/issues/747 is fixed.
                if ((progressBar.isGone) && (icon.height > nestedScrollWebView.getFavoriteIconHeight())) {
                    // Store the new favorite icon.
                    nestedScrollWebView.setFavoriteIcon(icon)

                    // Get the current page position.
                    val currentPosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

                    // Get the current tab.
                    val tab = tabLayout.getTabAt(currentPosition)

                    // Check to see if the tab has been populated.
                    if (tab != null) {
                        // Get the custom view from the tab.
                        val tabView = tab.customView

                        // Check to see if the custom tab view has been populated.
                        if (tabView != null) {
                            // Get the favorite icon image view from the tab.
                            val tabFavoriteIconImageView = tabView.findViewById<ImageView>(R.id.favorite_icon_imageview)

                            // Display the favorite icon in the tab.
                            tabFavoriteIconImageView.setImageBitmap(icon.scale(128, 128))
                        }
                    }
                }
            }

            // Save a copy of the title when it changes.
            override fun onReceivedTitle(view: WebView, title: String) {
                // Get the current page position.
                val currentPosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

                // Get the current tab.
                val tab = tabLayout.getTabAt(currentPosition)

                // Only populate the title text view if the tab has been fully created.
                if (tab != null) {
                    // Get the custom view from the tab.
                    val tabView = tab.customView

                    // Only populate the title text view if the tab view has been fully populated.
                    if (tabView != null) {
                        // Get the title text view from the tab.
                        val tabTitleTextView = tabView.findViewById<TextView>(R.id.title_textview)

                        // Set the title according to the URL.
                        if (title == "about:blank") {
                            // Set the title to indicate a new tab.
                            tabTitleTextView.setText(R.string.new_tab)
                        } else {
                            // Set the title as the tab text.
                            tabTitleTextView.text = title
                        }
                    }
                }
            }

            // Enter full screen video.
            override fun onShowCustomView(video: View, callback: CustomViewCallback) {
                // Set the full screen video flag.
                displayingFullScreenVideo = true

                // Hide the keyboard.
                inputMethodManager.hideSoftInputFromWindow(nestedScrollWebView.windowToken, 0)

                // Hide the browser view according to the API.
                if ((Build.VERSION.SDK_INT < 35) && displayUnderCutouts) {  // The device is running API < 35 and display under cutouts is enabled.
                    // Hide the tab linear layout.
                    tabsLinearLayout.visibility = View.GONE

                    // Hide the app bar.
                    appBar.hide()

                    // Hide the swipe refresh layout (which includes the WebView).
                    swipeRefreshLayout.visibility = View.GONE

                    // Disable fits system windows.
                    coordinatorLayout.fitsSystemWindows = false
                    bookmarksFrameLayout.fitsSystemWindows = false

                    // Remove any padding from the bookmark frame layout
                    bookmarksFrameLayout.setPadding(0, 0, 0, 0)
                } else {  // The device is running API >= 35 or display under cutouts is disabled.
                    // Hide the browser view.
                    browserFrameLayout.visibility = View.GONE
                }

                // Hide the system bars according to the API.
                if (Build.VERSION.SDK_INT >= 30) {
                    // Set the system bars to show transiently when swiped.
                    windowInsetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                    // Hide the system bars.
                    windowInsetsController.hide(WindowInsets.Type.systemBars())
                } else {
                    /* Hide the system bars.
                     * SYSTEM_UI_FLAG_FULLSCREEN hides the status bar at the top of the screen.
                     * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the root frame layout fill the area that is normally reserved for the status bar.
                     * SYSTEM_UI_FLAG_HIDE_NAVIGATION hides the navigation bar on the bottom or right of the screen.
                     * SYSTEM_UI_FLAG_IMMERSIVE_STICKY makes the status and navigation bars translucent and automatically re-hides them after they are shown.
                     */

                    @Suppress("DEPRECATION")
                    window.addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                }

                // Disable the sliding drawers.
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

                if ((Build.VERSION.SDK_INT < 35) && displayUnderCutouts) {
                    // Add the video view to the full screen video frame layout.
                    oldFullScreenVideoFrameLayout.addView(video)

                    // Show the full screen video frame layout.
                    oldFullScreenVideoFrameLayout.visibility = View.VISIBLE

                    // Disable the screen timeout while the video is playing.  YouTube does this automatically, but not all other videos do.
                    oldFullScreenVideoFrameLayout.keepScreenOn = true
                } else {
                    // Add the video view to the full screen video frame layout.
                    fullScreenVideoFrameLayout.addView(video)

                    // Show the full screen video frame layout.
                    fullScreenVideoFrameLayout.visibility = View.VISIBLE

                    // Disable the screen timeout while the video is playing.  YouTube does this automatically, but not all other videos do.
                    fullScreenVideoFrameLayout.keepScreenOn = true
                }
            }

            // Exit full screen video.
            override fun onHideCustomView() {
                // Exit the full screen video.
                exitFullScreenVideo()
            }

            // Upload files.
            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                // Store the file path callback.
                fileChooserCallback = filePathCallback

                // Create a file chooser intent.
                val fileChooserIntent = Intent(Intent.ACTION_GET_CONTENT)

                // Request an openable file.
                fileChooserIntent.addCategory(Intent.CATEGORY_OPENABLE)

                // Set the file type to everything.  The file chooser params cannot be used to create the intent because it only selects the first specified file type.
                // See <https://redmine.stoutner.com/issues/1197>.
                fileChooserIntent.type = "*/*"

                // Launch the file chooser intent.
                browseFileUploadActivityResultLauncher.launch(fileChooserIntent)

                // Handle the event.
                return true
            }
        }
        nestedScrollWebView.webViewClient = object : WebViewClient() {
            // `shouldOverrideUrlLoading` makes this WebView the default handler for URLs inside the app, so that links are not kicked out to other apps.
            override fun shouldOverrideUrlLoading(view: WebView, webResourceRequest: WebResourceRequest): Boolean {
                // Get the URL from the web resource request.
                var requestUrlString = webResourceRequest.url.toString()

                // Sanitize the url.
                requestUrlString = sanitizeUrl(requestUrlString)

                // Handle the URL according to the type.
                return if (requestUrlString.startsWith("http")) {  // Load the URL in Privacy Browser.
                    // Load the URL.  By using `loadUrl()`, instead of `loadUrlFromBase()`, the Referer header will never be sent.
                    loadUrl(nestedScrollWebView, requestUrlString)

                    // Returning true indicates that Privacy Browser is manually handling the loading of the URL.
                    // Custom headers cannot be added if false is returned and the WebView handles the loading of the URL.
                    true
                } else if (requestUrlString.startsWith("mailto:")) {  // Load the email address in an external email program.
                    // Use `ACTION_SENDTO` instead of `ACTION_SEND` so that only email programs are launched.
                    val emailIntent = Intent(Intent.ACTION_SENDTO)

                    // Parse the url and set it as the data for the intent.
                    emailIntent.data = requestUrlString.toUri()

                    // Open the email program in a new task instead of as part of Privacy Browser.
                    emailIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    try {
                        // Make it so.
                        startActivity(emailIntent)
                    } catch (exception: ActivityNotFoundException) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView!!, getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }

                    // Returning true indicates Privacy Browser is handling the URL by creating an intent.
                    true
                } else if (requestUrlString.startsWith("tel:")) {  // Load the phone number in the dialer.
                    // Create a dial intent.
                    val dialIntent = Intent(Intent.ACTION_DIAL)

                    // Add the phone number to the intent.
                    dialIntent.data = requestUrlString.toUri()

                    // Open the dialer in a new task instead of as part of Privacy Browser.
                    dialIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    try {
                        // Make it so.
                        startActivity(dialIntent)
                    } catch (exception: ActivityNotFoundException) {
                        // Display a snackbar.
                        Snackbar.make(currentWebView!!, getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }

                    // Returning true indicates Privacy Browser is handling the URL by creating an intent.
                    true
                } else {  // Load a system chooser to select an app that can handle the URL.
                    // Create a generic intent to open an app.
                    val genericIntent = Intent(Intent.ACTION_VIEW)

                    // Add the URL to the intent.
                    genericIntent.data = requestUrlString.toUri()

                    // List all apps that can handle the URL instead of just opening the first one.
                    genericIntent.addCategory(Intent.CATEGORY_BROWSABLE)

                    // Open the app in a new task instead of as part of Privacy Browser.
                    genericIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    try {
                        // Make it so.
                        startActivity(genericIntent)
                    } catch (exception: ActivityNotFoundException) {
                        // Display a snackbar.
                        Snackbar.make(nestedScrollWebView, getString(R.string.unrecognized_url, requestUrlString), Snackbar.LENGTH_SHORT).show()
                    }

                    // Returning true indicates Privacy Browser is handling the URL by creating an intent.
                    true
                }
            }

            // Check requests against the block lists.
            override fun shouldInterceptRequest(view: WebView, webResourceRequest: WebResourceRequest): WebResourceResponse? {
                // Get the URL.
                val requestUrlString = webResourceRequest.url.toString()

                // Check to see if the resource request is for the main URL.
                if (requestUrlString == nestedScrollWebView.currentUrl) {
                    // `return null` loads the resource request, which should never be blocked if it is the main URL.
                    return null
                }

                // Wait until the filter lists have been populated.  When Privacy Browser is being resumed after having the process killed in the background it will try to load the URLs immediately.
                while (ultraPrivacy == null) {
                    try {
                        // Check to see if the filter lists have been populated after 100 ms.
                        Thread.sleep(100)
                    } catch (exception: InterruptedException) {
                        // Do nothing.
                    }
                }

                // Create an empty web resource response to be used if the resource request is blocked.
                val emptyWebResourceResponse = WebResourceResponse("text/plain", "utf8", ByteArrayInputStream("".toByteArray()))

                // Initialize the variables.
                var allowListResultStringArray: Array<String>? = null
                var isThirdPartyRequest = false

                // Get the current URL.  `.getUrl()` throws an error because operations on the WebView cannot be made from this thread.
                var currentBaseDomain = nestedScrollWebView.currentDomainName

                // Store a copy of the current domain for use in later requests.
                val currentDomain = currentBaseDomain

                // Get the request host name.
                var requestBaseDomain = webResourceRequest.url.host

                // Only check for third-party requests if the current base domain is not empty and the request domain is not null.
                if (currentBaseDomain.isNotEmpty() && (requestBaseDomain != null)) {
                    // Determine the current base domain.
                    while (currentBaseDomain.indexOf(".", currentBaseDomain.indexOf(".") + 1) > 0) {  // There is at least one subdomain.
                        // Remove the first subdomain.
                        currentBaseDomain = currentBaseDomain.substring(currentBaseDomain.indexOf(".") + 1)
                    }

                    // Determine the request base domain.
                    while (requestBaseDomain!!.indexOf(".", requestBaseDomain.indexOf(".") + 1) > 0) {  // There is at least one subdomain.
                        // Remove the first subdomain.
                        requestBaseDomain = requestBaseDomain.substring(requestBaseDomain.indexOf(".") + 1)
                    }

                    // Update the third party request tracker.
                    isThirdPartyRequest = currentBaseDomain != requestBaseDomain
                }

                // Get the current WebView page position.
                val webViewPagePosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

                // Determine if the WebView is currently displayed.
                val webViewDisplayed = (webViewPagePosition == tabLayout.selectedTabPosition)

                // Block third-party requests if enabled.
                if (isThirdPartyRequest && nestedScrollWebView.blockAllThirdPartyRequests) {
                    // Add the result to the resource requests.
                    nestedScrollWebView.addResourceRequest(arrayOf(REQUEST_THIRD_PARTY, requestUrlString))

                    // Increment the blocked requests counters.
                    nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                    nestedScrollWebView.incrementRequestsCount(THIRD_PARTY_REQUESTS)

                    // Update the titles of the filter lists menu items if the WebView is currently displayed.
                    if (webViewDisplayed) {
                        // Updating the UI must be run from the UI thread.
                        runOnUiThread {
                            // Update the menu item titles.
                            navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                            // Update the options menu if it has been populated.
                            if (optionsMenu != null) {
                                optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                optionsBlockAllThirdPartyRequestsMenuItem.title =
                                    nestedScrollWebView.getRequestsCount(THIRD_PARTY_REQUESTS).toString() + " - " + getString(R.string.block_all_third_party_requests)
                            }
                        }
                    }

                    // The resource request was blocked.  Return an empty web resource response.
                    return emptyWebResourceResponse
                }

                // Check UltraList if it is enabled.
                if (nestedScrollWebView.ultraListEnabled) {
                    // Check the URL against UltraList.
                    val ultraListResults = checkFilterListHelper.checkFilterList(currentDomain, requestUrlString, isThirdPartyRequest, ultraList)

                    // Process the UltraList results.
                    if (ultraListResults[0] == REQUEST_BLOCKED) {  // The resource request matched UltraList's block list.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(arrayOf(ultraListResults[0], ultraListResults[1], ultraListResults[2], ultraListResults[3], ultraListResults[4], ultraListResults[5]))

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                        nestedScrollWebView.incrementRequestsCount(com.audeon.browser.views.ULTRALIST)

                        // Update the titles of the filter lists menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            runOnUiThread {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                    optionsUltraListMenuItem.title = nestedScrollWebView.getRequestsCount(com.audeon.browser.views.ULTRALIST).toString() + " - " + getString(R.string.ultralist)
                                }
                            }
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse
                    } else if (ultraListResults[0] == REQUEST_ALLOWED) {  // The resource request matched UltraList's allow list.
                        // Add an allow list entry to the resource requests array.
                        nestedScrollWebView.addResourceRequest(arrayOf(ultraListResults[0], ultraListResults[1], ultraListResults[2], ultraListResults[3], ultraListResults[4], ultraListResults[5]))

                        // The resource request has been allowed by UltraList.  `return null` loads the requested resource.
                        return null
                    }
                }

                // Check UltraPrivacy if it is enabled.
                if (nestedScrollWebView.ultraPrivacyEnabled) {
                    // Check the URL against UltraPrivacy.
                    val ultraPrivacyResults = checkFilterListHelper.checkFilterList(currentDomain, requestUrlString, isThirdPartyRequest, ultraPrivacy!!)

                    // Process the UltraPrivacy results.
                    if (ultraPrivacyResults[0] == REQUEST_BLOCKED) {  // The resource request matched UltraPrivacy's block list.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(arrayOf(ultraPrivacyResults[0], ultraPrivacyResults[1], ultraPrivacyResults[2], ultraPrivacyResults[3], ultraPrivacyResults[4],
                            ultraPrivacyResults[5]))

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                        nestedScrollWebView.incrementRequestsCount(ULTRAPRIVACY)

                        // Update the titles of the filter lists menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            runOnUiThread {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                    optionsUltraPrivacyMenuItem.title = nestedScrollWebView.getRequestsCount(ULTRAPRIVACY).toString() + " - " + getString(R.string.ultraprivacy)
                                }
                            }
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse
                    } else if (ultraPrivacyResults[0] == REQUEST_ALLOWED) {  // The resource request matched UltraPrivacy's allow list.
                        // Add an allow list entry to the resource requests array.
                        nestedScrollWebView.addResourceRequest(arrayOf(ultraPrivacyResults[0], ultraPrivacyResults[1], ultraPrivacyResults[2], ultraPrivacyResults[3], ultraPrivacyResults[4],
                            ultraPrivacyResults[5]))

                        // The resource request has been allowed by UltraPrivacy.  `return null` loads the requested resource.
                        return null
                    }
                }

                // Check EasyList if it is enabled.
                if (nestedScrollWebView.easyListEnabled) {
                    // Check the URL against EasyList.
                    val easyListResults = checkFilterListHelper.checkFilterList(currentDomain, requestUrlString, isThirdPartyRequest, easyList)

                    // Process the EasyList results.
                    if (easyListResults[0] == REQUEST_BLOCKED) {  // The resource request matched EasyList's block list.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(arrayOf(easyListResults[0], easyListResults[1], easyListResults[2], easyListResults[3], easyListResults[4], easyListResults[5]))

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                        nestedScrollWebView.incrementRequestsCount(EASYLIST)

                        // Update the titles of the filter lists menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            runOnUiThread {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                    optionsEasyListMenuItem.title = nestedScrollWebView.getRequestsCount(EASYLIST).toString() + " - " + getString(R.string.easylist)
                                }
                            }
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse
                    } else if (easyListResults[0] == REQUEST_ALLOWED) {  // The resource request matched EasyList's allow list.
                        // Update the allow list result string array tracker.
                        allowListResultStringArray = arrayOf(easyListResults[0], easyListResults[1], easyListResults[2], easyListResults[3], easyListResults[4], easyListResults[5])
                    }
                }

                // Check EasyPrivacy if it is enabled.
                if (nestedScrollWebView.easyPrivacyEnabled) {
                    // Check the URL against EasyPrivacy.
                    val easyPrivacyResults = checkFilterListHelper.checkFilterList(currentDomain, requestUrlString, isThirdPartyRequest, easyPrivacy)

                    // Process the EasyPrivacy results.
                    if (easyPrivacyResults[0] == REQUEST_BLOCKED) {  // The resource request matched EasyPrivacy's block list.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(arrayOf(easyPrivacyResults[0], easyPrivacyResults[1], easyPrivacyResults[2], easyPrivacyResults[3], easyPrivacyResults[4], easyPrivacyResults[5]))

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                        nestedScrollWebView.incrementRequestsCount(EASYPRIVACY)

                        // Update the titles of the filter lists menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            runOnUiThread {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                    optionsEasyPrivacyMenuItem.title = nestedScrollWebView.getRequestsCount(EASYPRIVACY).toString() + " - " + getString(R.string.easyprivacy)
                                }
                            }
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse
                    } else if (easyPrivacyResults[0] == REQUEST_ALLOWED) {  // The resource request matched EasyPrivacy's allow list.
                        // Update the allow list result string array tracker.
                        allowListResultStringArray = arrayOf(easyPrivacyResults[0], easyPrivacyResults[1], easyPrivacyResults[2], easyPrivacyResults[3], easyPrivacyResults[4], easyPrivacyResults[5])
                    }
                }

                // Check Fanboy’s Annoyance List if it is enabled.
                if (nestedScrollWebView.fanboysAnnoyanceListEnabled) {
                    // Check the URL against Fanboy's Annoyance List.
                    val fanboysAnnoyanceListResults = checkFilterListHelper.checkFilterList(currentDomain, requestUrlString, isThirdPartyRequest, fanboysAnnoyanceList)

                    // Process the Fanboy's Annoyance List results.
                    if (fanboysAnnoyanceListResults[0] == REQUEST_BLOCKED) {  // The resource request matched Fanboy's Annoyance List's block list.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(arrayOf(fanboysAnnoyanceListResults[0], fanboysAnnoyanceListResults[1], fanboysAnnoyanceListResults[2], fanboysAnnoyanceListResults[3],
                            fanboysAnnoyanceListResults[4], fanboysAnnoyanceListResults[5]))

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                        nestedScrollWebView.incrementRequestsCount(FANBOYS_ANNOYANCE_LIST)

                        // Update the titles of the filter lists menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            runOnUiThread {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                    optionsFanboysAnnoyanceListMenuItem.title = nestedScrollWebView.getRequestsCount(FANBOYS_ANNOYANCE_LIST).toString() + " - " + getString(R.string.fanboys_annoyance_list)
                                }
                            }
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse
                    } else if (fanboysAnnoyanceListResults[0] == REQUEST_ALLOWED) {  // The resource request matched Fanboy's Annoyance List's allow list.
                        // Update the allow list result string array tracker.
                        allowListResultStringArray = arrayOf(fanboysAnnoyanceListResults[0], fanboysAnnoyanceListResults[1], fanboysAnnoyanceListResults[2], fanboysAnnoyanceListResults[3],
                            fanboysAnnoyanceListResults[4], fanboysAnnoyanceListResults[5])
                    }
                } else if (nestedScrollWebView.fanboysSocialBlockingListEnabled) {  // Only check Fanboy’s Social Blocking List if Fanboy’s Annoyance List is disabled.
                    // Check the URL against Fanboy's Annoyance List.
                    val fanboysSocialListResults = checkFilterListHelper.checkFilterList(currentDomain, requestUrlString, isThirdPartyRequest, fanboysSocialList)

                    // Process the Fanboy's Social Blocking List results.
                    if (fanboysSocialListResults[0] == REQUEST_BLOCKED) {  // The resource request matched Fanboy's Social Blocking List's block list.
                        // Add the result to the resource requests.
                        nestedScrollWebView.addResourceRequest(arrayOf(fanboysSocialListResults[0], fanboysSocialListResults[1], fanboysSocialListResults[2], fanboysSocialListResults[3],
                            fanboysSocialListResults[4], fanboysSocialListResults[5]))

                        // Increment the blocked requests counters.
                        nestedScrollWebView.incrementRequestsCount(BLOCKED_REQUESTS)
                        nestedScrollWebView.incrementRequestsCount(FANBOYS_SOCIAL_BLOCKING_LIST)

                        // Update the titles of the filter lists menu items if the WebView is currently displayed.
                        if (webViewDisplayed) {
                            // Updating the UI must be run from the UI thread.
                            runOnUiThread {
                                // Update the menu item titles.
                                navigationRequestsMenuItem.title = getString(R.string.requests) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)

                                // Update the options menu if it has been populated.
                                if (optionsMenu != null) {
                                    optionsFilterListsMenuItem.title = getString(R.string.filterlists) + " - " + nestedScrollWebView.getRequestsCount(BLOCKED_REQUESTS)
                                    optionsFanboysSocialBlockingListMenuItem.title =
                                        nestedScrollWebView.getRequestsCount(FANBOYS_SOCIAL_BLOCKING_LIST).toString() + " - " + getString(R.string.fanboys_social_blocking_list)
                                }
                            }
                        }

                        // The resource request was blocked.  Return an empty web resource response.
                        return emptyWebResourceResponse
                    } else if (fanboysSocialListResults[0] == REQUEST_ALLOWED) {  // The resource request matched Fanboy's Social Blocking List's allow list.
                        // Update the allow list result string array tracker.
                        allowListResultStringArray = arrayOf(fanboysSocialListResults[0], fanboysSocialListResults[1], fanboysSocialListResults[2], fanboysSocialListResults[3], fanboysSocialListResults[4],
                            fanboysSocialListResults[5])
                    }
                }

                // Add the request to the log because it hasn't been processed by any of the previous checks.
                if (allowListResultStringArray != null) {  // The request was processed by an allow list.
                    nestedScrollWebView.addResourceRequest(allowListResultStringArray)
                } else {  // The request didn't match any filter list entry.  Log it as a default request.
                    nestedScrollWebView.addResourceRequest(arrayOf(REQUEST_DEFAULT, requestUrlString))
                }

                // The resource request has not been blocked.  `return null` loads the requested resource.
                return null
            }

            // Handle HTTP authentication requests.
            override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
                // Store the handler.
                nestedScrollWebView.httpAuthHandler = handler

                // Instantiate an HTTP authentication dialog.
                val httpAuthenticationDialogFragment = HttpAuthenticationDialog.displayDialog(host, realm, nestedScrollWebView.webViewFragmentId)

                // Try to show the dialog.  WebView can receive an HTTP authentication request even after the app has been paused.  Attempting to display a dialog in that state leads to a crash.
                try {
                    // Show the HTTP authentication dialog.
                    httpAuthenticationDialogFragment.show(supportFragmentManager, getString(R.string.http_authentication))
                } catch (exception: Exception) {  // The dialog could not be shown.
                    // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                    pendingDialogsArrayList.add(PendingDialogDataClass(httpAuthenticationDialogFragment, getString(R.string.http_authentication)))
                }
            }

            override fun onPageStarted(webView: WebView, url: String, favicon: Bitmap?) {
                // Get the app bar layout height.  This can't be done in `applyAppSettings()` because the app bar is not yet populated there.
                // This should only be populated if it is greater than 0 because otherwise it will be reset to 0 if the app bar is hidden in full screen browsing mode.
                if (appBarLayout.height > 0)
                    appBarHeight = appBarLayout.height

                // Set the padding and layout settings according to the position of the app bar.
                if (bottomAppBar) {  // The app bar is on the bottom.
                    // Adjust the UI.
                    if (scrollAppBar || (inFullScreenBrowsingMode && hideAppBar)) {  // The app bar scrolls or full screen browsing mode is engaged with the app bar hidden.
                        // Reset the WebView padding to fill the available space.
                        swipeRefreshLayout.setPadding(0, 0, 0, 0)
                    } else {  // The app bar doesn't scroll or full screen browsing mode is not engaged with the app bar hidden.
                        // Move the WebView above the app bar layout.
                        swipeRefreshLayout.setPadding(0, 0, 0, appBarHeight)
                    }
                } else {  // The app bar is on the top.
                    // Set the top padding of the swipe refresh layout according to the app bar scrolling preference.  This can't be done in `appAppSettings()` because the app bar is not yet populated there.
                    if (scrollAppBar || (inFullScreenBrowsingMode && hideAppBar)) {  // The app bar scrolls or full screen browsing mode is engaged with the app bar hidden.
                        // No padding is needed because it will automatically be placed below the app bar layout due to the scrolling layout behavior.
                        swipeRefreshLayout.setPadding(0, 0, 0, 0)

                        // The swipe to refresh circle doesn't always hide itself completely unless it is moved up 10 pixels.
                        swipeRefreshLayout.setProgressViewOffset(false, defaultProgressViewStartOffset - 10, defaultProgressViewEndOffset)
                    } else {  // The app bar doesn't scroll or full screen browsing mode is not engaged with the app bar hidden.
                        // The swipe refresh layout must be manually moved below the app bar layout.
                        swipeRefreshLayout.setPadding(0, appBarHeight, 0, 0)

                        // The swipe to refresh circle doesn't always hide itself completely unless it is moved up 10 pixels.
                        swipeRefreshLayout.setProgressViewOffset(false, defaultProgressViewStartOffset - 10 + appBarHeight, defaultProgressViewEndOffset + appBarHeight)
                    }
                }

                // Reset the list of resource requests.
                nestedScrollWebView.clearResourceRequests()

                // Reset the requests counters.
                nestedScrollWebView.resetRequestsCounters()

                // Get the current page position.
                val currentPagePosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

                // Update the URL text bar if the page is currently selected and the URL edit text is not currently being edited.
                if ((tabLayout.selectedTabPosition == currentPagePosition) && !urlEditText.hasFocus()) {
                    // Display the formatted URL text.  The nested scroll WebView current URL preserves any initial `view-source:`, and opposed to the method URL variable.
                    urlEditText.setText(nestedScrollWebView.currentUrl)

                    // Highlight the URL syntax.
                    UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan)

                    // Hide the keyboard.
                    inputMethodManager.hideSoftInputFromWindow(nestedScrollWebView.windowToken, 0)
                }

                // Reset the list of host IP addresses.
                nestedScrollWebView.currentIpAddresses = ""

                // Get a URI for the current URL.
                val currentUri = url.toUri()

                // Get the current domain name.
                val currentDomainName = currentUri.host

                // Get the IP addresses for the current domain.
                if (!currentDomainName.isNullOrEmpty())
                    GetHostIpAddressesCoroutine.checkPinnedMismatch(currentDomainName, nestedScrollWebView, supportFragmentManager, getString(R.string.pinned_mismatch))

                // Replace Refresh with Stop if the options menu has been created and the WebView is currently displayed.  (The first WebView typically begins loading before the menu items are instantiated.)
                if ((optionsMenu != null) && (webView == currentWebView)) {
                    // Set the title.
                    optionsRefreshMenuItem.setTitle(R.string.stop)

                    // Set the icon if it is displayed in the AppBar.
                    if (displayAdditionalAppBarIcons)
                        optionsRefreshMenuItem.setIcon(R.drawable.close_blue)
                }
            }

            override fun onPageFinished(webView: WebView, url: String) {
                // Flush any cookies to persistent storage.  The cookie manager has become very lazy about flushing cookies in recent versions.
                if (nestedScrollWebView.acceptCookies)
                    cookieManager.flush()

                // Update the Refresh menu item if the options menu has been created and the WebView is currently displayed.
                if (optionsMenu != null && (webView == currentWebView)) {
                    // Reset the Refresh title.
                    optionsRefreshMenuItem.setTitle(R.string.refresh)

                    // Reset the icon if it is displayed in the app bar.
                    if (displayAdditionalAppBarIcons)
                        optionsRefreshMenuItem.setIcon(R.drawable.refresh_enabled)
                }

                // Clear the cache.  `true` includes disk files.
                nestedScrollWebView.clearCache(true)

                // Get the application's private data directory, which will be something like `/data/user/0/com.audeon.browser.standard`,
                // which links to `/data/data/com.audeon.browser.standard`.
                val privateDataDirectoryString = applicationInfo.dataDir

                // Clear the history, and logcat if Incognito Mode is enabled.
                if (incognitoModeEnabled) {
                    // Clear the back/forward history.
                    nestedScrollWebView.clearHistory()

                    // Manually delete cache folders.
                    try {
                        // Delete the main cache directory.
                        Runtime.getRuntime().exec("rm -rf $privateDataDirectoryString/cache")
                    } catch (exception: IOException) {
                        // Do nothing if an error is thrown.
                    }

                    // Clear the logcat.
                    try {
                        // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                        Runtime.getRuntime().exec("logcat -b all -c")
                    } catch (exception: IOException) {
                        // Do nothing.
                    }
                }

                // Clear the `Service Worker` directory.
                try {
                    // A string array must be used because the directory contains a space and `Runtime.exec` will not escape the string correctly otherwise.
                    Runtime.getRuntime().exec(arrayOf("rm", "-rf", "$privateDataDirectoryString/app_webview/Default/Service Worker/"))
                } catch (exception: IOException) {
                    // Do nothing.
                }

                // Get the current page position.
                val currentPagePosition = webViewStateAdapter!!.getPositionForId(nestedScrollWebView.webViewFragmentId)

                // Get the current URL from the nested scroll WebView.  This is more accurate than using the URL passed into the method, which is sometimes not the final one.
                val currentUrl = nestedScrollWebView.url

                // Get the current tab.
                val tab = tabLayout.getTabAt(currentPagePosition)

                // Update the URL text bar if the page is currently selected and the user is not currently typing in the URL edit text.
                // Crash records show that, in some crazy way, it is possible for the current URL to be blank at this point.
                // Probably some sort of race condition when Privacy Browser is being resumed.
                if ((tabLayout.selectedTabPosition == currentPagePosition) && !urlEditText.hasFocus() && (currentUrl != null)) {
                    // Check to see if the URL is `about:blank`.
                    if (currentUrl == "about:blank") {  // The WebView is blank.
                        // Display the hint in the URL edit text.
                        urlEditText.text = null

                        // Request focus for the URL text box.
                        urlEditText.requestFocus()

                        // Display the keyboard.
                        inputMethodManager.showSoftInput(urlEditText, 0)

                        // Apply the domain settings.  This clears any settings from the previous domain.
                        applyDomainSettings(nestedScrollWebView, "", resetTab = true, reloadWebsite = false, loadUrl = false)

                        // Only populate the title text view if the tab has been fully created.
                        if (tab != null) {
                            // Get the custom view from the tab.
                            val tabView = tab.customView!!

                            // Get the title text view from the tab.
                            val tabTitleTextView = tabView.findViewById<TextView>(R.id.title_textview)

                            // Set the title as the tab text.
                            tabTitleTextView.setText(R.string.new_tab)
                        }
                    } else {  // The WebView has loaded a webpage.
                        // Update the URL edit text if it is not currently being edited.
                        if (!urlEditText.hasFocus()) {
                            // Sanitize the current URL.  This removes unwanted URL elements that were added by redirects, so that they won't be included if the URL is shared.
                            val sanitizedUrl = sanitizeUrl(currentUrl)

                            // Display the final URL.  Getting the URL from the WebView instead of using the one provided by `onPageFinished()` makes websites like YouTube function correctly.
                            urlEditText.setText(sanitizedUrl)

                            // Highlight the URL syntax.
                            UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan)
                        }

                        // Only populate the title text view if the tab has been fully created.
                        if (tab != null) {
                            // Get the custom view from the tab.
                            val tabView = tab.customView!!

                            // Get the title text view from the tab.
                            val tabTitleTextView = tabView.findViewById<TextView>(R.id.title_textview)

                            // Set the title as the tab text.  Sometimes `onReceivedTitle()` is not called, especially when navigating history.
                            tabTitleTextView.text = nestedScrollWebView.title
                        }
                    }
                }
            }

            // Handle SSL Certificate errors.  Suppress the lint warning that ignoring the error might be dangerous.
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                // Get the current website SSL certificate.
                val currentWebsiteSslCertificate = error.certificate

                // Extract the individual pieces of information from the current website SSL certificate.
                val currentWebsiteIssuedToCName = currentWebsiteSslCertificate.issuedTo.cName
                val currentWebsiteIssuedToOName = currentWebsiteSslCertificate.issuedTo.oName
                val currentWebsiteIssuedToUName = currentWebsiteSslCertificate.issuedTo.uName
                val currentWebsiteIssuedByCName = currentWebsiteSslCertificate.issuedBy.cName
                val currentWebsiteIssuedByOName = currentWebsiteSslCertificate.issuedBy.oName
                val currentWebsiteIssuedByUName = currentWebsiteSslCertificate.issuedBy.uName
                val currentWebsiteSslStartDate = currentWebsiteSslCertificate.validNotBeforeDate
                val currentWebsiteSslEndDate = currentWebsiteSslCertificate.validNotAfterDate

                // Get the pinned SSL certificate.
                val (pinnedSslCertificateStringArray, pinnedSslCertificateDateArray) = nestedScrollWebView.getPinnedSslCertificate()

                // Proceed to the website if the current SSL website certificate matches the pinned domain certificate.
                if (nestedScrollWebView.hasPinnedSslCertificate() &&
                    (currentWebsiteIssuedToCName == pinnedSslCertificateStringArray[0]) &&
                    (currentWebsiteIssuedToOName == pinnedSslCertificateStringArray[1]) &&
                    (currentWebsiteIssuedToUName == pinnedSslCertificateStringArray[2]) &&
                    (currentWebsiteIssuedByCName == pinnedSslCertificateStringArray[3]) &&
                    (currentWebsiteIssuedByOName == pinnedSslCertificateStringArray[4]) &&
                    (currentWebsiteIssuedByUName == pinnedSslCertificateStringArray[5]) &&
                    (currentWebsiteSslStartDate == pinnedSslCertificateDateArray[0]) &&
                    (currentWebsiteSslEndDate == pinnedSslCertificateDateArray[1])) {

                    // An SSL certificate is pinned and matches the current domain certificate.  Proceed to the website without displaying an error.
                    handler.proceed()
                } else {  // Either there isn't a pinned SSL certificate or it doesn't match the current website certificate.
                    // Store the SSL error handler.
                    nestedScrollWebView.sslErrorHandler = handler

                    // Instantiate an SSL certificate error alert dialog.
                    val sslCertificateErrorDialogFragment = SslCertificateErrorDialog.displayDialog(error, nestedScrollWebView.webViewFragmentId)

                    // Try to show the dialog.  The SSL error handler continues to function even when the app has been stopped.  Attempting to display a dialog in that state leads to a crash.
                    try {
                        // Show the SSL certificate error dialog.
                        sslCertificateErrorDialogFragment.show(supportFragmentManager, getString(R.string.ssl_certificate_error))
                    } catch (exception: Exception) {
                        // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                        pendingDialogsArrayList.add(PendingDialogDataClass(sslCertificateErrorDialogFragment, getString(R.string.ssl_certificate_error)))
                    }
                }
            }
        }

        // Check to see if the state is being restored.
        if (restoringState) {  // The state is being restored.
            // Resume the nested scroll WebView JavaScript timers.
            nestedScrollWebView.resumeTimers()
        } else if (pagePosition == 0) {  // The first page is being loaded.
            // Set this nested scroll WebView as the current WebView.
            currentWebView = nestedScrollWebView

            // Get the intent that started the app.
            val launchingIntent = intent

            // Reset the intent.  This prevents a duplicate tab from being created on restart.
            intent = Intent()

            // Get the information from the intent.
            val launchingIntentAction = launchingIntent.action
            val launchingIntentUriData = launchingIntent.data
            val launchingIntentStringExtra = launchingIntent.getStringExtra(Intent.EXTRA_TEXT)

            // Parse the launching intent URL.  Suppress the suggestions of using elvis expressions as they make the logic very difficult to follow.
            @Suppress("IfThenToElvis") val urlToLoadString = if ((launchingIntentAction != null) && (launchingIntentAction == Intent.ACTION_WEB_SEARCH)) {  // The intent contains a search string.
                // Sanitize the search input and convert it to a search.
                val encodedSearchString = try {
                    URLEncoder.encode(launchingIntent.getStringExtra(SearchManager.QUERY), "UTF-8")
                } catch (exception: UnsupportedEncodingException) {
                    ""
                }

                // Add the search URL to the encodedSearchString
                searchURL + encodedSearchString
            } else if (launchingIntentUriData != null) {  // The launching intent contains a URL formatted as a URI.
                // Get the URL from the URI.
                launchingIntentUriData.toString()
            } else if (launchingIntentStringExtra != null) {  // The launching intent contains text that might be a URL.
                // Get the URL from the string extra.
                launchingIntentStringExtra
            } else if (urlString != "") {  // The activity has been restarted.
                // Load the saved URL.
                urlString
            } else {  // The is no saved URL and there is no URL in the intent.
                // Load the homepage.
                sharedPreferences.getString("homepage", getString(R.string.homepage_default_value))
            }

            // Load the website if not waiting for the proxy.
            if (waitingForProxy) {  // Store the URL to be loaded in the Nested Scroll WebView.
                nestedScrollWebView.waitingForProxyUrlString = urlToLoadString!!
            } else {  // Load the URL.
                loadUrl(nestedScrollWebView, urlToLoadString!!)
            }
        } else {  // This is not the first tab.
            // Load the URL.
            loadUrl(nestedScrollWebView, urlString)

            // Set the focus and display the keyboard if the URL is blank.
            if (urlString == "") {
                // Request focus for the URL text box.
                urlEditText.requestFocus()

                // Display the keyboard once the tab layout has settled.
                tabLayout.post {
                    inputMethodManager.showSoftInput(urlEditText, 0)
                }
            }
        }
    }

    private fun loadBookmarksFolder() {
        // Update the bookmarks cursor with the contents of the bookmarks database for the current folder.
        bookmarksCursor = if (sortBookmarksAlphabetically)
            bookmarksDatabaseHelper!!.getBookmarksSortedAlphabetically(currentBookmarksFolderId)
        else
            bookmarksDatabaseHelper!!.getBookmarksByDisplayOrder(currentBookmarksFolderId)

        // Populate the bookmarks cursor adapter.
        bookmarksCursorAdapter = object : CursorAdapter(this, bookmarksCursor, false) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                // Inflate the individual item layout.
                return layoutInflater.inflate(R.layout.bookmarks_drawer_item_linearlayout, parent, false)
            }

            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get handles for the views.
                val bookmarkFavoriteIcon = view.findViewById<ImageView>(R.id.bookmark_favorite_icon)
                val bookmarkNameTextView = view.findViewById<TextView>(R.id.bookmark_name)

                // Get the favorite icon byte array from the cursor.
                val favoriteIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(FAVORITE_ICON))

                // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
                val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

                // Display the bitmap in the bookmark favorite icon.
                bookmarkFavoriteIcon.setImageBitmap(favoriteIconBitmap)

                // Display the bookmark name from the cursor in the bookmark name text view.
                bookmarkNameTextView.text = cursor.getString(cursor.getColumnIndexOrThrow(BOOKMARK_NAME))

                // Make the font bold for folders.
                if (cursor.getInt(cursor.getColumnIndexOrThrow(IS_FOLDER)) == 1)
                    bookmarkNameTextView.typeface = Typeface.DEFAULT_BOLD
                else  // Reset the font to default for normal bookmarks.
                    bookmarkNameTextView.typeface = Typeface.DEFAULT
            }
        }

        // Populate the list view with the adapter.
        bookmarksListView.adapter = bookmarksCursorAdapter

        // Set the bookmarks drawer title.
        if (currentBookmarksFolderId == HOME_FOLDER_ID)  // The current bookmarks folder is the home folder.
            bookmarksTitleTextView.setText(R.string.bookmarks)
        else
            bookmarksTitleTextView.text = bookmarksDatabaseHelper!!.getFolderName(currentBookmarksFolderId)
    }

    private fun loadUrl(nestedScrollWebView: NestedScrollWebView, url: String) {
        // Sanitize the URL.
        val urlString = sanitizeUrl(url)

        // Apply the domain settings and load the URL.
        applyDomainSettings(nestedScrollWebView, urlString, resetTab = true, reloadWebsite = false, loadUrl = true)
    }

    private fun loadUrlFromTextBox() {
        // Get the text from URL text box and convert it to a string.  trim() removes white spaces from the beginning and end of the string.
        var unformattedUrlString = urlEditText.text.toString().trim()

        // Create the formatted URL string.
        var urlString = ""

        // Check to see if the unformatted URL string is a valid URL.  Otherwise, convert it into a search.
        if (unformattedUrlString.startsWith("content://") || unformattedUrlString.startsWith("view-source:")) {  // This is a content or source URL.
            // Load the entire content URL.
            urlString = unformattedUrlString
        } else if (Patterns.WEB_URL.matcher(unformattedUrlString).matches() || unformattedUrlString.startsWith("http://") || unformattedUrlString.startsWith("https://") ||
            unformattedUrlString.startsWith("file://")) {  // This is a standard URL.

            // Add `https://` at the beginning if there is no protocol.  Otherwise the app will segfault.
            if (!unformattedUrlString.startsWith("http") && !unformattedUrlString.startsWith("file://"))
                unformattedUrlString = "https://$unformattedUrlString"

            // Initialize the unformatted URL.
            var unformattedUrl: URL? = null

            // Convert the unformatted URL string to a URL.
            try {
                unformattedUrl = URL(unformattedUrlString)
            } catch (exception: MalformedURLException) {
                exception.printStackTrace()
            }

            // Get the components of the URL.
            val scheme = unformattedUrl?.protocol
            val authority = unformattedUrl?.authority
            val path = unformattedUrl?.path
            val query = unformattedUrl?.query
            val fragment = unformattedUrl?.ref

            // Create a URI.
            val uri = Uri.Builder()

            // Build the URI from the components of the URL.
            uri.scheme(scheme).authority(authority).path(path).query(query).fragment(fragment)

            // Decode the URI as a UTF-8 string in.
            try {
                urlString = URLDecoder.decode(uri.build().toString(), "UTF-8")
            } catch (exception: UnsupportedEncodingException) {
                // Do nothing.  The formatted URL string will remain blank.
            }
        } else if (unformattedUrlString.isNotEmpty()) {  // This is not a URL, but rather a search string.
            // Sanitize the search input.
            val encodedSearchString = try {
                URLEncoder.encode(unformattedUrlString, "UTF-8")
            } catch (exception: UnsupportedEncodingException) {
                ""
            }

            // Add the base search URL.
            urlString = searchURL + encodedSearchString
        }

        // Clear the focus from the URL edit text.  Otherwise, proximate typing in the box will retain the colorized formatting instead of being reset during refocus.
        urlEditText.clearFocus()

        // Make it so.
        loadUrl(currentWebView!!, urlString)
    }

    override fun navigateHistory(steps: Int) {
        // Get the current web back forward list.
        val webBackForwardList = currentWebView!!.copyBackForwardList()

        // Calculate the target index.
        val targetIndex = webBackForwardList.currentIndex + steps

        // Get the previous entry data.
        val previousUrl = webBackForwardList.getItemAtIndex(targetIndex).url
        val previousFavoriteIcon = webBackForwardList.getItemAtIndex(targetIndex).favicon

        // Apply the domain settings.
        applyDomainSettings(currentWebView!!, previousUrl, resetTab = false, reloadWebsite = false, loadUrl = false)

        // Get the current tab.
        val tab = tabLayout.getTabAt(tabLayout.selectedTabPosition)!!

        // Get the custom view from the tab.
        val tabView = tab.customView!!

        // Get the favorite icon image view from the tab.
        val tabFavoriteIconImageView = tabView.findViewById<ImageView>(R.id.favorite_icon_imageview)

        // Store the previous favorite icon.
        if (previousFavoriteIcon == null)
            currentWebView!!.setFavoriteIcon(defaultFavoriteIconBitmap)
        else
            currentWebView!!.setFavoriteIcon(previousFavoriteIcon)

        // Display the previous favorite icon in the tab.
        tabFavoriteIconImageView.setImageBitmap(currentWebView!!.getFavoriteIcon().scale(128, 128))

        // Load the history entry.
        currentWebView!!.goBackOrForward(steps)

        // Create a handler to update the URL edit box.
        val urlEditTextUpdateHandler = Handler(Looper.getMainLooper())

        // Create a runnable to update the URL edit box.
        val urlEditTextUpdateRunnable = Runnable {
            // Update the URL edit text.
            urlEditText.setText(currentWebView!!.url)

            // Disable the wide viewport if the source is being viewed.
            if (currentWebView!!.url!!.startsWith("view-source:"))
                currentWebView!!.settings.useWideViewPort = false
        }

        // Update the URL edit text after 50 milliseconds, so that the WebView has enough time to navigate to the new URL.
        urlEditTextUpdateHandler.postDelayed(urlEditTextUpdateRunnable, 50)
    }

    override fun openFile(dialogFragment: DialogFragment) {
        // Get the dialog.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views.
        val fileNameEditText = dialog.findViewById<EditText>(R.id.file_name_edittext)
        val mhtCheckBox = dialog.findViewById<CheckBox>(R.id.mht_checkbox)

        // Get the file path string.
        val openFilePath = fileNameEditText.text.toString()

        // Apply the domain settings.  This resets the favorite icon and removes any domain settings.
        applyDomainSettings(currentWebView!!, openFilePath, resetTab = true, reloadWebsite = false, loadUrl = false)

        // Open the file according to the type.
        if (mhtCheckBox.isChecked) {  // Force opening of an MHT file.
            try {
                // Get the MHT file input stream.
                val mhtFileInputStream = contentResolver.openInputStream(openFilePath.toUri())

                // Create a temporary MHT file.
                val temporaryMhtFile = File.createTempFile(TEMPORARY_MHT_FILE, ".mht", cacheDir)

                // Get a file output stream for the temporary MHT file.
                val temporaryMhtFileOutputStream = FileOutputStream(temporaryMhtFile)

                // Create a transfer byte array.
                val transferByteArray = ByteArray(1024)

                // Create an integer to track the number of bytes read.
                var bytesRead: Int

                // Copy the temporary MHT file input stream to the MHT output stream.
                while (mhtFileInputStream!!.read(transferByteArray).also { bytesRead = it } > 0)
                    temporaryMhtFileOutputStream.write(transferByteArray, 0, bytesRead)

                // Flush the temporary MHT file output stream.
                temporaryMhtFileOutputStream.flush()

                // Close the streams.
                temporaryMhtFileOutputStream.close()
                mhtFileInputStream.close()

                // Load the temporary MHT file.
                currentWebView!!.loadUrl(temporaryMhtFile.toString())
            } catch (exception: Exception) {
                // Display a snackbar.
                Snackbar.make(currentWebView!!, getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
            }
        } else {  // Let the WebView handle opening of the file.
            // Open the file.
            currentWebView!!.loadUrl(openFilePath)
        }
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun openNavigationDrawer(@Suppress("UNUSED_PARAMETER")view: View) {
        // Open the navigation drawer.
        drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun openWithApp(url: String) {
        // Create an open with app intent with `ACTION_VIEW`.
        val openWithAppIntent = Intent(Intent.ACTION_VIEW)

        // Set the URI but not the MIME type.  This should open all available apps.
        openWithAppIntent.data = url.toUri()

        // Flag the intent to open in a new task.
        openWithAppIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Try the intent.
        try {
            // Show the chooser.
            startActivity(openWithAppIntent)
        } catch (exception: ActivityNotFoundException) {  // There are no apps available to open the URL.
            // Show a snackbar with the error.
            Snackbar.make(currentWebView!!, getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    private fun openWithBrowser(url: String) {

        // Create an open with browser intent with `ACTION_VIEW`.
        val openWithBrowserIntent = Intent(Intent.ACTION_VIEW)

        // Set the URI and the MIME type.  `"text/html"` should load browser options.
        openWithBrowserIntent.setDataAndType(url.toUri(), "text/html")

        // Flag the intent to open in a new task.
        openWithBrowserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Try the intent.
        try {
            // Show the chooser.
            startActivity(openWithBrowserIntent)
        } catch (exception: ActivityNotFoundException) {  // There are no browsers available to open the URL.
            // Show a snackbar with the error.
            Snackbar.make(currentWebView!!, getString(R.string.error, exception), Snackbar.LENGTH_INDEFINITE).show()
        }
    }

    override fun pinnedErrorGoBack() {
        // Navigate back one page.
        navigateHistory(-1)
    }

    private fun sanitizeUrl(urlString: String): String {
        // Initialize a sanitized URL string.
        var sanitizedUrlString = urlString

        // Sanitize tracking queries.
        if (sanitizeTrackingQueries)
            sanitizedUrlString = SanitizeUrlHelper.sanitizeTrackingQueries(sanitizedUrlString)

        // Sanitize AMP redirects.
        if (sanitizeAmpRedirects)
            sanitizedUrlString = SanitizeUrlHelper.sanitizeAmpRedirects(sanitizedUrlString)

        // Return the sanitized URL string.
        return sanitizedUrlString
    }

    override fun saveWithAndroidDownloadManager(dialogFragment: DialogFragment) {
        // Get the dialog.
        val dialog = dialogFragment.dialog!!

        // Get handles for the dialog views.
        val dialogUrlEditText = dialog.findViewById<EditText>(R.id.url_edittext)
        val downloadDirectoryRadioGroup = dialog.findViewById<RadioGroup>(R.id.download_directory_radiogroup)
        val dialogFileNameEditText = dialog.findViewById<EditText>(R.id.file_name_edittext)

        // Get the string from the edit texts, which may have been modified by the user.
        val saveUrlString = dialogUrlEditText.text.toString()
        val fileNameString = dialogFileNameEditText.text.toString()

        // Get a handle for the system download service.
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        // Parse the URL.
        val downloadRequest = DownloadManager.Request(saveUrlString.toUri())

        // Pass cookies to download manager if cookies are enabled.  This is required to download files from websites that require a login.
        // Code contributed 2017 Hendrik Knackstedt.  Copyright assigned to Soren Stoutner <soren@stoutner.com>.
        if (cookieManager.acceptCookie()) {
            // Get the cookies for the URL.
            val cookiesString = cookieManager.getCookie(saveUrlString)

            // Add the cookies to the download request.  In the HTTP request header, cookies are named `Cookie`.
            downloadRequest.addRequestHeader("Cookie", cookiesString)
        }

        // Get the download directory.
        val downloadDirectory = when (downloadDirectoryRadioGroup.checkedRadioButtonId) {
            R.id.downloads_radiobutton -> Environment.DIRECTORY_DOWNLOADS
            R.id.documents_radiobutton -> Environment.DIRECTORY_DOCUMENTS
            R.id.pictures_radiobutton -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_MUSIC
        }

        // Set the download destination.
        downloadRequest.setDestinationInExternalPublicDir(downloadDirectory, fileNameString)

        // Allow media scanner to index the download if it is a media file.  This is automatic for API >= 29.
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT <= 28)
            downloadRequest.allowScanningByMediaScanner()

        // Add the URL as the description for the download.
        downloadRequest.setDescription(saveUrlString)

        // Show the download notification after the download is completed.
        downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // Initiate the download.
        downloadManager.enqueue(downloadRequest)
    }

    private fun saveWithExternalApp(url: String) {
        // Create a download intent.  Not specifying the action type will display the maximum number of options.
        val downloadIntent = Intent()

        // Set the URI and the mime type.
        downloadIntent.setDataAndType(url.toUri(), "text/html")

        // Flag the intent to open in a new task.
        downloadIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Show the chooser.
        startActivity(Intent.createChooser(downloadIntent, getString(R.string.download_with_external_app)))
    }

    override fun saveWithaudeonbrowser(originalUrlString: String, fileNameString: String, dialogFragment: DialogFragment) {
        // Store the URL.  This will be used in the save URL activity result launcher.
        saveUrlString = if (originalUrlString.startsWith("data:")) {
            // Save the original URL.
            originalUrlString
        } else {
            // Get the dialog.
            val dialog = dialogFragment.dialog!!

            // Get a handle for the dialog URL edit text.
            val dialogUrlEditText = dialog.findViewById<EditText>(R.id.url_edittext)

            // Get the URL from the edit text, which may have been modified by the user.
            dialogUrlEditText.text.toString()
        }

        // Open the file picker.
        saveUrlActivityResultLauncher.launch(fileNameString)
    }

    private fun setCurrentWebView(pageNumber: Int) {
        // Stop the swipe to refresh indicator if it is running
        swipeRefreshLayout.isRefreshing = false

        // Open the navigation drawer if the bottom app bar is enabled and this is the first tab.  <https://redmine.stoutner.com/issues/1267>
        if (displayingInitialTab && bottomAppBar) {
            // Open the navigation drawer.
            drawerLayout.openDrawer(GravityCompat.START)

            // Set the close navigation drawer flag.
            closeNavigationDrawer = true
        }

        // Set the displaying initial tab flag to be false.
        displayingInitialTab = false

        // Try to set the current WebView.  This will fail if the WebView has not yet been populated.
        try {
            // Get the WebView tab fragment.
            val webViewTabFragment = webViewStateAdapter!!.getPageFragment(pageNumber)

            // Get the fragment view.
            val webViewFragmentView = webViewTabFragment.view

            // Store the current WebView.
            currentWebView = webViewFragmentView!!.findViewById(R.id.nestedscroll_webview)

            // Update the status of swipe to refresh.
            if (currentWebView!!.swipeToRefresh) {  // Swipe to refresh is enabled.
                // Enable the swipe refresh layout if the WebView is scrolled all the way to the top.  It is updated every time the scroll changes.
                swipeRefreshLayout.isEnabled = (currentWebView!!.scrollY == 0)
            } else {  // Swipe to refresh is disabled.
                // Disable the swipe refresh layout.
                swipeRefreshLayout.isEnabled = false
            }

            // Set the cookie status.
            cookieManager.setAcceptCookie(currentWebView!!.acceptCookies)

            // Update the privacy icons.  `true` redraws the icons in the app bar.
            updatePrivacyIcons(true)

            // Get the current URL.
            val urlString = currentWebView!!.currentUrl

            // Update the URL edit text if not loading a new intent.  Otherwise, this will be handled by `onPageStarted()` (if called) and `onPageFinished()`.
            if (!loadingNewIntent) {  // A new intent is not being loaded.
                if (urlString.isBlank()) {  // The WebView is blank.
                    // Display the hint in the URL edit text.
                    urlEditText.text = null

                    // Request focus for the URL text box.
                    urlEditText.requestFocus()

                    // Display the keyboard.
                    inputMethodManager.showSoftInput(urlEditText, 0)
                } else {  // The WebView has a loaded URL.
                    // Clear the focus from the URL text box.
                    urlEditText.clearFocus()

                    // Hide the soft keyboard.
                    inputMethodManager.hideSoftInputFromWindow(currentWebView!!.windowToken, 0)

                    // Display the current URL in the URL text box.
                    urlEditText.setText(urlString)

                    // Highlight the URL syntax.
                    UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan)
                }
            } else {  // A new intent is being loaded.
                // Reset the loading new intent flag.
                loadingNewIntent = false
            }

            // Set the background to indicate the domain settings status.
            if (currentWebView!!.domainSettingsApplied) {
                // Set a background on the URL relative layout to indicate that custom domain settings are being used.
                urlRelativeLayout.background = AppCompatResources.getDrawable(this, R.drawable.domain_settings_url_background)
            } else {
                // Remove any background on the URL relative layout.
                urlRelativeLayout.background = AppCompatResources.getDrawable(this, R.color.transparent)
            }
        }  catch (exception: Exception) {  //  Try again in 10 milliseconds if the WebView has not yet been populated.
            // Create a handler to set the current WebView.
            val setCurrentWebViewHandler = Handler(Looper.getMainLooper())

            // Create a runnable to set the current WebView.
            val setCurrentWebWebRunnable = Runnable {
                // Set the current WebView.
                setCurrentWebView(pageNumber)
            }

            // Try setting the current WebView again after 10 milliseconds.
            setCurrentWebViewHandler.postDelayed(setCurrentWebWebRunnable, 10)
        }
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun toggleBookmarksDrawerPinned(@Suppress("UNUSED_PARAMETER")view: View?) {
        // Toggle the bookmarks drawer pinned tracker.
        bookmarksDrawerPinned = !bookmarksDrawerPinned

        // Update the bookmarks drawer pinned image view.
        updateBookmarksDrawerPinnedImageView()
    }

    private fun updateBookmarksDrawerPinnedImageView() {
        // Set the current icon.
        if (bookmarksDrawerPinned)
            bookmarksDrawerPinnedImageView.setImageResource(R.drawable.pin_selected)
        else
            bookmarksDrawerPinnedImageView.setImageResource(R.drawable.pin)
    }

    private fun updateDomainsSettingsSet() {
        // Reset the domains settings set.
        domainsSettingsSet = HashSet()

        // Get a domains cursor.
        val domainsCursor = domainsDatabaseHelper!!.domainNameCursorOrderedByDomain

        // Get the current count of domains.
        val domainsCount = domainsCursor.count

        // Get the domain name column index.
        val domainNameColumnIndex = domainsCursor.getColumnIndexOrThrow(DOMAIN_NAME)

        // Populate the domain settings set.
        for (i in 0 until domainsCount) {
            // Move the domains cursor to the current row.
            domainsCursor.moveToPosition(i)

            // Store the domain name in the domain settings set.
            domainsSettingsSet.add(domainsCursor.getString(domainNameColumnIndex))
        }

        // Close the domains cursor.
        domainsCursor.close()
    }

    override fun updateFontSize(dialogFragment: DialogFragment) {
        // Get the dialog.
        val dialog = dialogFragment.dialog!!

        // Get a handle for the font size edit text.
        val fontSizeEditText = dialog.findViewById<EditText>(R.id.font_size_edittext)

        // Initialize the new font size variable with the current font size.
        var newFontSize = currentWebView!!.settings.textZoom

        // Get the font size from the edit text.
        try {
            newFontSize = fontSizeEditText.text.toString().toInt()
        } catch (exception: Exception) {
            // If the edit text does not contain a valid font size do nothing.
        }

        // Apply the new font size.
        currentWebView!!.settings.textZoom = newFontSize
    }

    private fun updatePrivacyIcons(runInvalidateOptionsMenu: Boolean) {
        // Only update the privacy icons if the options menu and the current WebView have already been populated.
        if ((optionsMenu != null) && (currentWebView != null)) {
            // Update the privacy icon.
            if (currentWebView!!.settings.javaScriptEnabled)  // JavaScript is enabled.
                optionsPrivacyMenuItem.setIcon(R.drawable.javascript_enabled)
            else if (currentWebView!!.acceptCookies)  // JavaScript is disabled but cookies are enabled.
                optionsPrivacyMenuItem.setIcon(R.drawable.warning)
            else  // All the dangerous features are disabled.
                optionsPrivacyMenuItem.setIcon(R.drawable.privacy_mode)

            // Update the cookies icon.
            if (currentWebView!!.acceptCookies)
                optionsCookiesMenuItem.setIcon(R.drawable.cookies_enabled)
            else
                optionsCookiesMenuItem.setIcon(R.drawable.cookies_disabled)

            // Update the refresh icon.
            if (optionsRefreshMenuItem.title == getString(R.string.refresh))  // The refresh icon is displayed.
                optionsRefreshMenuItem.setIcon(R.drawable.refresh_enabled)
            else  // The stop icon is displayed.
                optionsRefreshMenuItem.setIcon(R.drawable.close_blue)

            // `invalidateOptionsMenu()` calls `onPrepareOptionsMenu()` and redraws the icons in the app bar.
            if (runInvalidateOptionsMenu)
                invalidateOptionsMenu()
        }
    }
}
