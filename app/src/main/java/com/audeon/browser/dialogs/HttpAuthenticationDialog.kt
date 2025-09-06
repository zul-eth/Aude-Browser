/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2017-2023 Soren Stoutner <soren@stoutner.com>
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
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.HttpAuthHandler
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.activities.MainWebViewActivity
import com.audeon.browser.views.NestedScrollWebView

// Define the class constants.
private const val HOST = "host"
private const val REALM = "realm"
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class HttpAuthenticationDialog : DialogFragment() {
    companion object {
        fun displayDialog(host: String, realm: String, webViewFragmentId: Long): HttpAuthenticationDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the variables in the bundle.
            argumentsBundle.putString(HOST, host)
            argumentsBundle.putString(REALM, realm)
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the HTTP authentication dialog.
            val httpAuthenticationDialog = HttpAuthenticationDialog()

            // Add the arguments bundle to the dialog.
            httpAuthenticationDialog.arguments = argumentsBundle

            // Return the new dialog.
            return httpAuthenticationDialog
        }
    }

    // Define the class variables.
    private var httpAuthHandler: HttpAuthHandler? = null

    // Declare the class views.
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get a handle for the arguments.
        val arguments = requireArguments()

        // Get the variables from the bundle.
        val httpAuthHost = arguments.getString(HOST)
        val httpAuthRealm = arguments.getString(REALM)
        val webViewFragmentId = arguments.getLong(WEBVIEW_FRAGMENT_ID)

        // Try to populate the alert dialog.
        try {  // Getting the WebView tab fragment will fail if Privacy Browser has been restarted.
            // Get the current position of this WebView fragment.
            val webViewPosition = MainWebViewActivity.webViewStateAdapter!!.getPositionForId(webViewFragmentId)

            // Get the WebView tab fragment.
            val webViewTabFragment = MainWebViewActivity.webViewStateAdapter!!.getPageFragment(webViewPosition)

            // Get the fragment view.
            val fragmentView = webViewTabFragment.requireView()

            // Get a handle for the current WebView.
            val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

            // Get a handle for the HTTP authentication handler.
            httpAuthHandler = nestedScrollWebView.httpAuthHandler

            // Use an alert dialog builder to create the alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

            // Set the icon.
            dialogBuilder.setIcon(R.drawable.lock)

            // Set the title.
            dialogBuilder.setTitle(R.string.http_authentication)

            // Set the view.
            dialogBuilder.setView(R.layout.http_authentication_dialog)

            // Set the close button listener.
            dialogBuilder.setNegativeButton(R.string.close) { _: DialogInterface?, _: Int ->
                if (httpAuthHandler != null) {
                    // Cancel the HTTP authentication request.
                    httpAuthHandler!!.cancel()

                    // Reset the HTTP authentication handler.
                    nestedScrollWebView.resetHttpAuthHandler()
                }
            }

            // Set the proceed button listener.
            dialogBuilder.setPositiveButton(R.string.proceed) { _: DialogInterface?, _: Int ->
                // Send the login information
                login(httpAuthHandler, nestedScrollWebView)
            }

            // Create an alert dialog from the alert dialog builder.
            val alertDialog = dialogBuilder.create()

            // Get the alert dialog window.
            val dialogWindow = alertDialog.window!!

            // Get a handle for the shared preferences.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

            // Get the screenshot preference.
            val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Display the keyboard.
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            // The alert dialog needs to be shown before the contents can be modified.
            alertDialog.show()

            // Get handles for the views.
            val realmTextView = alertDialog.findViewById<TextView>(R.id.http_authentication_realm)!!
            val hostTextView = alertDialog.findViewById<TextView>(R.id.http_authentication_host)!!
            usernameEditText = alertDialog.findViewById(R.id.http_authentication_username)!!
            passwordEditText = alertDialog.findViewById(R.id.http_authentication_password)!!

            // Set the realm text.
            realmTextView.text = httpAuthRealm

            // Initialize the host label and the spannable string builder.
            val hostLabel = getString(R.string.host)
            val hostStringBuilder = SpannableStringBuilder(hostLabel + httpAuthHost)

            // Set the blue color span.
            val blueColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.blue_text))

            // Setup the span to display the host name in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            hostStringBuilder.setSpan(blueColorSpan, hostLabel.length, hostStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Set the host text.
            hostTextView.text = hostStringBuilder

            // Allow the enter key on the keyboard to send the login information from the username edit text.
            usernameEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
                // Check the key code and event.
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {  // The enter key was pressed.
                    // Send the login information.
                    login(httpAuthHandler, nestedScrollWebView)

                    // Manually dismiss the alert dialog.
                    alertDialog.dismiss()

                    // Consume the event.
                    return@setOnKeyListener true
                } else {  // If any other key was pressed, do not consume the event.
                    return@setOnKeyListener false
                }
            }

            // Allow the enter key on the keyboard to send the login information from the password edit text.
            passwordEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
                // Check the key code and event.
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {  // The enter key was pressed.
                    // Send the login information.
                    login(httpAuthHandler, nestedScrollWebView)

                    // Manually dismiss the alert dialog.
                    alertDialog.dismiss()

                    // Consume the event.
                    return@setOnKeyListener true
                } else {  // If any other key was pressed, do not consume the event.
                    return@setOnKeyListener false
                }
            }

            // Return the alert dialog.
            return alertDialog
        } catch (exception: Exception) {  // Privacy Browser was restarted and the HTTP auth handler no longer exists.
            // Dismiss this new instance of the dialog as soon as it is displayed.
            dismiss()

            // Use an alert dialog builder to create an empty alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

            // Return the alert dialog.
            return dialogBuilder.create()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(outState)

        // Cancel the request if the SSL error handler is not null.  This resets the WebView so it is not waiting on a response to the error handler if it is restarted in the background.
        // Otherwise, after restart, the dialog is no longer displayed, but the error handler is still pending and there is no way to cause the dialog to redisplay for that URL in that tab.
        if (httpAuthHandler != null)
            httpAuthHandler!!.cancel()
    }

    private fun login(httpAuthHandler: HttpAuthHandler?, nestedScrollWebView: NestedScrollWebView) {
        if (httpAuthHandler != null) {
            // Send the login information.
            httpAuthHandler.proceed(usernameEditText.text.toString(), passwordEditText.text.toString())

            // Reset the HTTP authentication handler.
            nestedScrollWebView.resetHttpAuthHandler()
        }
    }
}
