/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2017-2024 Soren Stoutner <soren@stoutner.com>
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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.google.android.material.snackbar.Snackbar

import com.audeon.browser.R
import com.audeon.browser.dialogs.AVAILABLE_CIPHERS
import com.audeon.browser.dialogs.SSL_CERTIFICATE
import com.audeon.browser.dialogs.AboutViewHeadersDialog
import com.audeon.browser.dialogs.ViewHeadersDetailDialog
import com.audeon.browser.dialogs.UntrustedSslCertificateDialog
import com.audeon.browser.dialogs.UntrustedSslCertificateDialog.UntrustedSslCertificateListener
import com.audeon.browser.helpers.ProxyHelper
import com.audeon.browser.helpers.UrlHelper
import com.audeon.browser.viewmodelfactories.ViewHeadersFactory
import com.audeon.browser.viewmodels.HeadersViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.lang.Exception
import java.nio.charset.StandardCharsets

// Define the public constants.
const val USER_AGENT = "user_agent"

class ViewHeadersActivity: AppCompatActivity(), UntrustedSslCertificateListener {
    // Declare the class variables.
    private lateinit var appliedCipherString: String
    private lateinit var availableCiphersString: String
    private lateinit var headersViewModel: HeadersViewModel
    private lateinit var initialGrayColorSpan: ForegroundColorSpan
    private lateinit var finalGrayColorSpan: ForegroundColorSpan
    private lateinit var redColorSpan: ForegroundColorSpan
    private lateinit var sslCertificateString: String

    // Declare the class views.
    private lateinit var urlEditText: EditText
    private lateinit var sslInformationTitleTextView: TextView
    private lateinit var sslInformationTextView: TextView
    private lateinit var sslButtonsConstraintLayout: ConstraintLayout
    private lateinit var requestHeadersTitleTextView: TextView
    private lateinit var requestHeadersTextView: TextView
    private lateinit var responseMessageTitleTextView: TextView
    private lateinit var responseMessageTextView: TextView
    private lateinit var responseHeadersTitleTextView: TextView
    private lateinit var responseHeadersTextView: TextView
    private lateinit var responseBodyTitleTextView: TextView
    private lateinit var responseBodyTextView: TextView

    // Define the save text activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveTextActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { fileUri ->
        // Only save the file if the URI is not null, which happens if the user exited the file picker by pressing back.
        if (fileUri != null) {
            // Get a cursor from the content resolver.
            val contentResolverCursor = contentResolver.query(fileUri, null, null, null)!!

            // Move to the first row.
            contentResolverCursor.moveToFirst()

            // Get the file name from the cursor.
            val fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

            // Close the cursor.
            contentResolverCursor.close()

            try {
                // Get the about version string.
                val headersString = getHeadersString()

                // Open an output stream.
                val outputStream = contentResolver.openOutputStream(fileUri)!!

                // Save the headers using a coroutine with Dispatchers.IO.
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        // Write the headers string to the output stream.
                        outputStream.write(headersString.toByteArray(StandardCharsets.UTF_8))

                        // Close the output stream.
                        outputStream.close()
                    }
                }

                // Display a snackbar with the saved logcat information.
                Snackbar.make(urlEditText, getString(R.string.saved, fileNameString), Snackbar.LENGTH_SHORT).show()
            } catch (exception: Exception) {
                // Display a snackbar with the error message.
                Snackbar.make(urlEditText, getString(R.string.error_saving_file, fileNameString, exception.toString()), Snackbar.LENGTH_INDEFINITE).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots)
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Get the launching intent
        val intent = intent

        // Get the information from the intent.
        val currentUrl = intent.getStringExtra(CURRENT_URL)!!
        val userAgent = intent.getStringExtra(USER_AGENT)!!

        // Set the content view.
        if (bottomAppBar)
            setContentView(R.layout.view_headers_bottom_appbar)
        else
            setContentView(R.layout.view_headers_top_appbar)

        // Get a handle for the toolbar.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Add the custom layout to the action bar.
        actionBar.setCustomView(R.layout.view_headers_appbar_custom_view)

        // Instruct the action bar to display a custom layout.
        actionBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_HOME_AS_UP

        // Get handles for the views.
        urlEditText = findViewById(R.id.url_edittext)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefreshlayout)
        sslInformationTitleTextView = findViewById(R.id.ssl_information_title_textview)
        sslInformationTextView = findViewById(R.id.ssl_information_textview)
        sslButtonsConstraintLayout = findViewById(R.id.ssl_buttons_constraintlayout)
        requestHeadersTitleTextView = findViewById(R.id.request_headers_title_textview)
        requestHeadersTextView = findViewById(R.id.request_headers_textview)
        responseMessageTitleTextView = findViewById(R.id.response_message_title_textview)
        responseMessageTextView = findViewById(R.id.response_message_textview)
        responseHeadersTitleTextView = findViewById(R.id.response_headers_title_textview)
        responseHeadersTextView = findViewById(R.id.response_headers_textview)
        responseBodyTitleTextView = findViewById(R.id.response_body_title_textview)
        responseBodyTextView = findViewById(R.id.response_body_textview)

        // Initialize the gray foreground color spans for highlighting the URLs.
        initialGrayColorSpan = ForegroundColorSpan(getColor(R.color.gray_500))
        finalGrayColorSpan = ForegroundColorSpan(getColor(R.color.gray_500))
        redColorSpan = ForegroundColorSpan(getColor(R.color.red_text))

        // Get a handle for the input method manager, which is used to hide the keyboard.
        val inputMethodManager = (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)

        // Remove the formatting from the URL when the user is editing the text.
        urlEditText.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {  // The user is editing the URL text box.
                // Get the foreground color spans.
                val foregroundColorSpans: Array<ForegroundColorSpan> = urlEditText.text.getSpans(0, urlEditText.text.length, ForegroundColorSpan::class.java)

                // Remove each foreground color span that highlights the text.
                for (foregroundColorSpan in foregroundColorSpans)
                    urlEditText.text.removeSpan(foregroundColorSpan)
            } else {  // The user has stopped editing the URL text box.
                // Hide the soft keyboard.
                inputMethodManager.hideSoftInputFromWindow(urlEditText.windowToken, 0)

                // Move to the beginning of the string.
                urlEditText.setSelection(0)

                // Store the URL text in the intent, so update layout uses the new text if the app is restarted.
                intent.putExtra(CURRENT_URL, urlEditText.text.toString())

                // Reapply the highlighting.
                UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan)
            }
        }

        // Populate the URL text box.
        urlEditText.setText(currentUrl)

        // Apply the initial text highlighting to the URL.
        UrlHelper.highlightSyntax(urlEditText, initialGrayColorSpan, finalGrayColorSpan, redColorSpan)

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

        // Get the list of locales.
        val localeList = resources.configuration.locales

        // Initialize a string builder to extract the locales from the list.
        val localesStringBuilder = StringBuilder()

        // Initialize a `q` value, which is used by `WebView` to indicate the order of importance of the languages.
        var q = 10

        // Populate the string builder with the contents of the locales list.
        for (i in 0 until localeList.size()) {
            // Append a comma if there is already an item in the string builder.
            if (i > 0) {
                localesStringBuilder.append(",")
            }

            // Get the locale from the list.
            val locale = localeList[i]

            // Add the locale to the string.  `locale` by default displays as `en_US`, but WebView uses the `en-US` format.
            localesStringBuilder.append(locale.language)
            localesStringBuilder.append("-")
            localesStringBuilder.append(locale.country)

            // If not the first locale, append `;q=0.x`, which drops by .1 for each removal from the main locale until q=0.1.
            if (q < 10) {
                localesStringBuilder.append(";q=0.")
                localesStringBuilder.append(q)
            }

            // Decrement `q` if it is greater than 1.
            if (q > 1) {
                q--
            }

            // Add a second entry for the language only portion of the locale.
            localesStringBuilder.append(",")
            localesStringBuilder.append(locale.language)

            // Append `1;q=0.x`, which drops by .1 for each removal form the main locale until q=0.1.
            localesStringBuilder.append(";q=0.")
            localesStringBuilder.append(q)

            // Decrement `q` if it is greater than 1.
            if (q > 1) {
                q--
            }
        }

        // Instantiate the proxy helper.
        val proxyHelper = ProxyHelper()

        // Get the current proxy.
        val proxy = proxyHelper.getCurrentProxy(this)

        // Make the progress bar visible.
        progressBar.visibility = View.VISIBLE

        // Set the progress bar to be indeterminate.
        progressBar.isIndeterminate = true

        // Update the layout.
        updateLayout(currentUrl)

        // Instantiate the view headers factory.
        val viewHeadersFactory: ViewModelProvider.Factory = ViewHeadersFactory(application, currentUrl, userAgent, localesStringBuilder.toString(), proxy, contentResolver, MainWebViewActivity.executorService)

        // Instantiate the headers view model.
        headersViewModel = ViewModelProvider(this, viewHeadersFactory)[HeadersViewModel::class.java]

        // Create a headers observer.
        headersViewModel.observeHeaders().observe(this) { headersStringArray: Array<SpannableStringBuilder> ->
            // Populate the text views.  This can take a long time, and freezes the user interface, if the response body is particularly large.
            sslInformationTextView.text = headersStringArray[0]
            requestHeadersTextView.text = headersStringArray[4]
            responseMessageTextView.text = headersStringArray[5]
            responseHeadersTextView.text = headersStringArray[6]
            responseBodyTextView.text = headersStringArray[7]

            // Populate the dialog strings.
            appliedCipherString = headersStringArray[1].toString()
            availableCiphersString = headersStringArray[2].toString()
            sslCertificateString = headersStringArray[3].toString()

            // Hide the progress bar.
            progressBar.isIndeterminate = false
            progressBar.visibility = View.GONE

            // Stop the swipe to refresh indicator if it is running
            swipeRefreshLayout.isRefreshing = false
        }

        // Create an error observer.
        headersViewModel.observeErrors().observe(this) { errorString: String ->
            // Display an error snackbar if the string is not `""`.
            if (errorString != "") {
                if (errorString.startsWith("javax.net.ssl.SSLHandshakeException")) {
                    // Instantiate the untrusted SSL certificate dialog.
                    val untrustedSslCertificateDialog = UntrustedSslCertificateDialog()

                    // Show the untrusted SSL certificate dialog.
                    untrustedSslCertificateDialog.show(supportFragmentManager, getString(R.string.invalid_certificate))
                } else {
                    // Display a snackbar with the error message.
                    Snackbar.make(swipeRefreshLayout, errorString, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        // Implement swipe to refresh.
        swipeRefreshLayout.setOnRefreshListener {
            // Make the progress bar visible.
            progressBar.visibility = View.VISIBLE

            // Set the progress bar to be indeterminate.
            progressBar.isIndeterminate = true

            // Get the URL.
            val urlString = urlEditText.text.toString()

            // Update the layout.
            updateLayout(urlString)

            // Get the updated headers.
            headersViewModel.updateHeaders(urlString, false)
        }

        // Set the go button on the keyboard to request new headers data.
        urlEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            // Request new headers data if the enter key was pressed.
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Hide the soft keyboard.
                inputMethodManager.hideSoftInputFromWindow(urlEditText.windowToken, 0)

                // Remove the focus from the URL box.
                urlEditText.clearFocus()

                // Make the progress bar visible.
                progressBar.visibility = View.VISIBLE

                // Set the progress bar to be indeterminate.
                progressBar.isIndeterminate = true

                // Get the URL.
                val urlString = urlEditText.text.toString()

                // Update the layout.
                updateLayout(urlString)

                // Get the updated headers.
                headersViewModel.updateHeaders(urlString, false)

                // Consume the key press.
                return@setOnKeyListener true
            } else {
                // Do not consume the key press.
                return@setOnKeyListener false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.
        menuInflater.inflate(R.menu.view_headers_options_menu, menu)

        // Display the menu.
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Run the commands that correlate to the selected menu item.
        when (menuItem.itemId) {
            R.id.copy_headers -> {  // Copy the headers.
                // Get the headers string.
                val headersString = getHeadersString()

                // Get a handle for the clipboard manager.
                val clipboardManager = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)

                // Place the headers string in a clip data.
                val headersClipData = ClipData.newPlainText(getString(R.string.view_headers), headersString)

                // Place the clip data on the clipboard.
                clipboardManager.setPrimaryClip(headersClipData)

                // Display a snackbar if the API <= 32 (Android 12L).  Beginning in Android 13 the OS displays a notification that covers up the snackbar.
                if (Build.VERSION.SDK_INT <= 32)
                    Snackbar.make(urlEditText, R.string.headers_copied, Snackbar.LENGTH_SHORT).show()

                // Consume the event.
                return true
            }

            R.id.share_headers -> {  // Share the headers.
                // Get the headers string.
                val headersString = getHeadersString()

                // Create a share intent.
                val shareIntent = Intent(Intent.ACTION_SEND)

                // Add the headers string to the intent.
                shareIntent.putExtra(Intent.EXTRA_TEXT, headersString)

                // Set the MIME type.
                shareIntent.type = "text/plain"

                // Set the intent to open in a new task.
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Make it so.
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))

                // Consume the event.
                return true
            }

            R.id.save_headers -> {  // Save the headers as a text file.
                // Get the current URL.
                val currentUrlString = urlEditText.text.toString()

                // Get a URI for the current URL.
                val currentUri = Uri.parse(currentUrlString)

                // Get the current domain name.
                val currentDomainName = currentUri.host

                // Open the file picker.
                saveTextActivityResultLauncher.launch(getString(R.string.headers_txt, currentDomainName))

                // Consume the event.
                return true
            }

            R.id.about_view_headers -> {  // Display the about dialog.
                // Instantiate the about dialog fragment.
                val aboutDialogFragment = AboutViewHeadersDialog()

                // Show the about alert dialog.
                aboutDialogFragment.show(supportFragmentManager, getString(R.string.about))

                // Consume the event.
                return true
            }

            else -> {  // The home button was selected.
                // Do not consume the event.  The system will process the home command.
                return super.onOptionsItemSelected(menuItem)
            }
        }
    }

    private fun getHeadersString(): String {
        // Initialize a headers string builder.
        val headersStringBuilder = StringBuilder()

        // Populate the SSL information if it is visible (an HTTPS URL is loaded).
        if (sslInformationTitleTextView.visibility == View.VISIBLE) {
            headersStringBuilder.append(sslInformationTitleTextView.text)
            headersStringBuilder.append("\n")
            headersStringBuilder.append(sslInformationTextView.text)
            headersStringBuilder.append("\n\n")
            headersStringBuilder.append(getString(R.string.available_ciphers))
            headersStringBuilder.append("\n")
            headersStringBuilder.append(availableCiphersString)
            headersStringBuilder.append("\n\n")
            headersStringBuilder.append(getString(R.string.ssl_certificate))
            headersStringBuilder.append("\n")
            headersStringBuilder.append(sslCertificateString)
            headersStringBuilder.append("\n")  // Only a single new line is needed after the certificate as it already ends in one.
        }

        // Populate the request information if it is visible (an HTTP URL is loaded).
        if (requestHeadersTitleTextView.visibility == View.VISIBLE) {
            headersStringBuilder.append(requestHeadersTitleTextView.text)
            headersStringBuilder.append("\n")
            headersStringBuilder.append(requestHeadersTextView.text)
            headersStringBuilder.append("\n\n")
            headersStringBuilder.append(responseMessageTitleTextView.text)
            headersStringBuilder.append("\n")
            headersStringBuilder.append(responseMessageTextView.text)
            headersStringBuilder.append("\n\n")
        }

        // Populate the response information, which is visible for both HTTP and content URLs.
        headersStringBuilder.append(responseHeadersTitleTextView.text)
        headersStringBuilder.append("\n")
        headersStringBuilder.append(responseHeadersTextView.text)
        headersStringBuilder.append("\n\n")
        headersStringBuilder.append(responseBodyTitleTextView.text)
        headersStringBuilder.append("\n")
        headersStringBuilder.append(responseBodyTextView.text)

        // Return the string.
        return headersStringBuilder.toString()
    }

    override fun loadAnyway() {
        // Load the URL anyway.
        headersViewModel.updateHeaders(urlEditText.text.toString(), true)
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun showCertificate(@Suppress("UNUSED_PARAMETER")view: View) {
        // Instantiate an SSL certificate dialog.
        val sslCertificateDialogFragment= ViewHeadersDetailDialog.displayDialog(SSL_CERTIFICATE, sslCertificateString)

        // Show the dialog.
        sslCertificateDialogFragment.show(supportFragmentManager, getString(R.string.ssl_certificate))
    }

    // The view parameter cannot be removed because it is called from the layout onClick.
    fun showCiphers(@Suppress("UNUSED_PARAMETER")view: View) {
        // Instantiate an SSL certificate dialog.
        val ciphersDialogFragment= ViewHeadersDetailDialog.displayDialog(AVAILABLE_CIPHERS, availableCiphersString, appliedCipherString)

        // Show the dialog.
        ciphersDialogFragment.show(supportFragmentManager, getString(R.string.ssl_certificate))
    }

    private fun updateLayout(urlString: String) {
        if (urlString.startsWith("content://")) {  // This is a content URL.
            // Hide the unused views.
            sslInformationTitleTextView.visibility = View.GONE
            sslInformationTextView.visibility = View.GONE
            sslButtonsConstraintLayout.visibility = View.GONE
            requestHeadersTitleTextView.visibility = View.GONE
            requestHeadersTextView.visibility = View.GONE
            responseMessageTitleTextView.visibility = View.GONE
            responseMessageTextView.visibility = View.GONE

            // Change the text of the remaining title text views.
            responseHeadersTitleTextView.setText(R.string.content_metadata)
            responseBodyTitleTextView.setText(R.string.content_data)
        } else {  // This is not a content URL.
            // Set the status if the the SSL information views.
            if (urlString.startsWith("http://")) {  // This is an HTTP URL.
                // Hide the SSL information views.
                sslInformationTitleTextView.visibility = View.GONE
                sslInformationTextView.visibility = View.GONE
                sslButtonsConstraintLayout.visibility = View.GONE
            } else {  // This is not an HTTP URL.
                // Show the SSL information views.
                sslInformationTitleTextView.visibility = View.VISIBLE
                sslInformationTextView.visibility = View.VISIBLE
                sslButtonsConstraintLayout.visibility = View.VISIBLE
            }

            // Show the other views.
            requestHeadersTitleTextView.visibility = View.VISIBLE
            requestHeadersTextView.visibility = View.VISIBLE
            responseMessageTitleTextView.visibility = View.VISIBLE
            responseMessageTextView.visibility = View.VISIBLE

            // Restore the text of the other title text views.
            responseHeadersTitleTextView.setText(R.string.response_headers)
            responseBodyTitleTextView.setText(R.string.response_body)
        }
    }
}
