/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2023, 2025 Soren Stoutner <soren@stoutner.com>
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
import android.net.http.SslError
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.activities.MainWebViewActivity
import com.audeon.browser.coroutines.GetHostIpAddressesCoroutine
import com.audeon.browser.views.NestedScrollWebView

import java.text.DateFormat

// Define the class constants.
private const val PRIMARY_ERROR_INT = "primary_error_int"
private const val URL_WITH_ERRORS = "url_with_errors"
private const val ISSUED_TO_CNAME = "issued_to_cname"
private const val ISSUED_TO_ONAME = "issued_to_oname"
private const val ISSUED_TO_UNAME = "issued_to_uname"
private const val ISSUED_BY_CNAME = "issued_by_cname"
private const val ISSUED_BY_ONAME = "issued_by_oname"
private const val ISSUED_BY_UNAME = "issued_by_uname"
private const val START_DATE = "start_date"
private const val END_DATE = "end_date"
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class SslCertificateErrorDialog : DialogFragment() {
    companion object {
        fun displayDialog(sslError: SslError, webViewFragmentId: Long): SslCertificateErrorDialog {
            // Get the various components of the SSL error message.
            val primaryErrorInt = sslError.primaryError
            val urlWithErrors = sslError.url
            val sslCertificate = sslError.certificate
            val issuedToCName = sslCertificate.issuedTo.cName
            val issuedToOName = sslCertificate.issuedTo.oName
            val issuedToUName = sslCertificate.issuedTo.uName
            val issuedByCName = sslCertificate.issuedBy.cName
            val issuedByOName = sslCertificate.issuedBy.oName
            val issuedByUName = sslCertificate.issuedBy.uName
            val startDate = sslCertificate.validNotBeforeDate
            val endDate = sslCertificate.validNotAfterDate

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the SSL error message components in the bundle.
            argumentsBundle.putInt(PRIMARY_ERROR_INT, primaryErrorInt)
            argumentsBundle.putString(URL_WITH_ERRORS, urlWithErrors)
            argumentsBundle.putString(ISSUED_TO_CNAME, issuedToCName)
            argumentsBundle.putString(ISSUED_TO_ONAME, issuedToOName)
            argumentsBundle.putString(ISSUED_TO_UNAME, issuedToUName)
            argumentsBundle.putString(ISSUED_BY_CNAME, issuedByCName)
            argumentsBundle.putString(ISSUED_BY_ONAME, issuedByOName)
            argumentsBundle.putString(ISSUED_BY_UNAME, issuedByUName)
            argumentsBundle.putString(START_DATE, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(startDate))
            argumentsBundle.putString(END_DATE, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(endDate))
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the SSL certificate error dialog.
            val thisSslCertificateErrorDialog = SslCertificateErrorDialog()

            // Add the arguments bundle to the new dialog.
            thisSslCertificateErrorDialog.arguments = argumentsBundle

            // Return the new dialog.
            return thisSslCertificateErrorDialog
        }
    }

    // Define the class variables.
    private var sslErrorHandler: SslErrorHandler? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        try {
            // Get the variables from the bundle.
            val primaryErrorInt = requireArguments().getInt(PRIMARY_ERROR_INT)
            val urlWithErrors = requireArguments().getString(URL_WITH_ERRORS)
            val issuedToCName = requireArguments().getString(ISSUED_TO_CNAME)
            val issuedToOName = requireArguments().getString(ISSUED_TO_ONAME)
            val issuedToUName = requireArguments().getString(ISSUED_TO_UNAME)
            val issuedByCName = requireArguments().getString(ISSUED_BY_CNAME)
            val issuedByOName = requireArguments().getString(ISSUED_BY_ONAME)
            val issuedByUName = requireArguments().getString(ISSUED_BY_UNAME)
            val startDate = requireArguments().getString(START_DATE)
            val endDate = requireArguments().getString(END_DATE)
            val webViewFragmentId = requireArguments().getLong(WEBVIEW_FRAGMENT_ID)

            // Get the current position of this WebView fragment.
            val webViewPosition = MainWebViewActivity.webViewStateAdapter!!.getPositionForId(webViewFragmentId)

            // Get the WebView tab fragment.
            val webViewTabFragment = MainWebViewActivity.webViewStateAdapter!!.getPageFragment(webViewPosition)

            // Get the fragment view.
            val fragmentView = webViewTabFragment.requireView()

            // Get a handle for the current WebView.
            val nestedScrollWebView: NestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview)

            // Get a handle for the SSL error handler.
            sslErrorHandler = nestedScrollWebView.sslErrorHandler

            // Use an alert dialog builder to create the alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

            // Set the icon.
            dialogBuilder.setIcon(R.drawable.ssl_certificate)

            // Set the title.
            dialogBuilder.setTitle(R.string.ssl_certificate_error)

            // Set the view.
            dialogBuilder.setView(R.layout.ssl_certificate_error)

            // Set the cancel button listener.
            dialogBuilder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                // Check to make sure the SSL error handler is not null.  This might happen if multiple dialogs are displayed at once.
                if (sslErrorHandler != null) {
                    // Cancel the request.
                    sslErrorHandler!!.cancel()

                    // Reset the SSL error handler.
                    nestedScrollWebView.resetSslErrorHandler()
                }
            }

            // Set the proceed button listener.
            dialogBuilder.setPositiveButton(R.string.proceed) { _: DialogInterface?, _: Int ->
                // Check to make sure the SSL error handler is not null.  This might happen if multiple dialogs are displayed at once.
                if (sslErrorHandler != null) {
                    // Proceed to the website.
                    sslErrorHandler!!.proceed()

                    // Reset the SSL error handler.
                    nestedScrollWebView.resetSslErrorHandler()
                }
            }

            // Create an alert dialog from the builder.
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

            // Get a URI for the URL with errors.
            val uriWithErrors = urlWithErrors?.toUri()

            // The alert dialog must be shown before the contents can be modified.
            alertDialog.show()

            // Get handles for the views.
            val primaryErrorTextView = alertDialog.findViewById<TextView>(R.id.primary_error)!!
            val urlTextView = alertDialog.findViewById<TextView>(R.id.url)!!
            val ipAddressesTextView = alertDialog.findViewById<TextView>(R.id.ip_addresses)!!
            val issuedToCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_cname)!!
            val issuedToONameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_oname)!!
            val issuedToUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_uname)!!
            val issuedByTextView = alertDialog.findViewById<TextView>(R.id.issued_by_textview)!!
            val issuedByCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_cname)!!
            val issuedByONameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_oname)!!
            val issuedByUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_uname)!!
            val validDatesTextView = alertDialog.findViewById<TextView>(R.id.valid_dates_textview)!!
            val startDateTextView = alertDialog.findViewById<TextView>(R.id.start_date)!!
            val endDateTextView = alertDialog.findViewById<TextView>(R.id.end_date)!!

            // Define the color spans.
            val blueColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.alt_blue_text))
            val redColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.red_text))

            // Get the IP Addresses for the URI.
            GetHostIpAddressesCoroutine.getAddresses(uriWithErrors?.host!!, getString(R.string.ip_addresses), blueColorSpan, ipAddressesTextView)

            // Setup the common strings.
            val urlLabel = getString(R.string.url_label)
            val cNameLabel = getString(R.string.common_name)
            val oNameLabel = getString(R.string.organization)
            val uNameLabel = getString(R.string.organizational_unit)
            val startDateLabel = getString(R.string.start_date)
            val endDateLabel = getString(R.string.end_date)

            // Create a spannable string builder for each text view that needs multiple colors of text.
            val urlStringBuilder = SpannableStringBuilder(urlLabel + urlWithErrors)
            val issuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedToCName)
            val issuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedToOName)
            val issuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedToUName)
            val issuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedByCName)
            val issuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedByOName)
            val issuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedByUName)
            val startDateStringBuilder = SpannableStringBuilder(startDateLabel + startDate)
            val endDateStringBuilder = SpannableStringBuilder(endDateLabel + endDate)

            // Setup the spans to display the certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            urlStringBuilder.setSpan(blueColorSpan, urlLabel.length, urlStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            startDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            endDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Define the primary error string.
            var primaryErrorString = ""

            // Highlight the primary error in red and store it in the primary error string.
            when (primaryErrorInt) {
                SslError.SSL_IDMISMATCH -> {
                    // Change the URL span colors to red.
                    urlStringBuilder.setSpan(redColorSpan, urlLabel.length, urlStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                    // Store the primary error string.
                    primaryErrorString = getString(R.string.cn_mismatch)
                }

                SslError.SSL_UNTRUSTED -> {
                    // Change the issued by text view text to red.
                    issuedByTextView.setTextColor(requireContext().getColor(R.color.red_text))

                    // Change the issued by span color to red.
                    issuedByCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedByONameStringBuilder.setSpan(redColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedByUNameStringBuilder.setSpan(redColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                    // Store the primary error string.
                    primaryErrorString = getString(R.string.untrusted)
                }

                SslError.SSL_DATE_INVALID -> {
                    // Change the valid dates text view text to red.
                    validDatesTextView.setTextColor(requireContext().getColor(R.color.red_text))

                    // Change the date span colors to red.
                    startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                    // Store the primary error string.
                    primaryErrorString = getString(R.string.invalid_date)
                }

                SslError.SSL_NOTYETVALID -> {
                    // Change the start date span color to red.
                    startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                    // Store the primary error string.
                    primaryErrorString = getString(R.string.future_certificate)
                }

                SslError.SSL_EXPIRED -> {
                    // Change the end date span color to red.
                    endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                    // Store the primary error string.
                    primaryErrorString = getString(R.string.expired_certificate)
                }

                SslError.SSL_INVALID ->
                    // Store the primary error string.
                    primaryErrorString = getString(R.string.invalid_certificate)
            }

            // Display the strings.
            primaryErrorTextView.text = primaryErrorString
            urlTextView.text = urlStringBuilder
            issuedToCNameTextView.text = issuedToCNameStringBuilder
            issuedToONameTextView.text = issuedToONameStringBuilder
            issuedToUNameTextView.text = issuedToUNameStringBuilder
            issuedByCNameTextView.text = issuedByCNameStringBuilder
            issuedByONameTextView.text = issuedByONameStringBuilder
            issuedByUNameTextView.text = issuedByUNameStringBuilder
            startDateTextView.text = startDateStringBuilder
            endDateTextView.text = endDateStringBuilder

            // Return the alert dialog.
            return alertDialog
        } catch (exception: Exception) {  // The app was restarted while the dialog was displayed.
            // Dismiss this new instance of the dialog as soon as it is displayed.
            dismiss()

            // Use an alert dialog builder to create an empty alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

            // Return the empty alert dialog.
            return dialogBuilder.create()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(outState)

        // Cancel the request if the SSL error handler is not null.  This resets the WebView so it is not waiting on a response to the error handler if it is restarted in the background.
        // Otherwise, after restart, the dialog is no longer displayed, but the error handler is still pending and there is no way to cause the dialog to redisplay for that URL in that tab.
        if (sslErrorHandler != null)
            sslErrorHandler!!.cancel()
    }
}
