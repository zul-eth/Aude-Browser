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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.WindowManager
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.audeon.browser.R
import com.audeon.browser.activities.MainWebViewActivity
import com.audeon.browser.views.NestedScrollWebView

import java.io.ByteArrayOutputStream

import java.text.DateFormat
import java.util.Calendar
import java.util.Date

// Define the private class constants.
private const val DOMAIN = "A"
private const val END_DATE = "B"
private const val FAVORITE_ICON_BYTE_ARRAY = "C"
private const val HAS_SSL_CERTIFICATE = "D"
private const val IP_ADDRESSES = "E"
private const val ISSUED_BY_CNAME = "F"
private const val ISSUED_BY_ONAME = "G"
private const val ISSUED_BY_UNAME = "H"
private const val ISSUED_TO_CNAME = "I"
private const val ISSUED_TO_ONAME = "J"
private const val ISSUED_TO_UNAME = "K"
private const val START_DATE = "L"
private const val WEBVIEW_FRAGMENT_ID = "M"
private const val URL = "N"

class ViewSslCertificateDialog : DialogFragment() {
    companion object {
        fun displayDialog(webViewFragmentId: Long, favoriteIconBitmap: Bitmap): ViewSslCertificateDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Create a favorite icon byte array output stream.
            val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the bitmap to a PNG and place it in the byte array output stream.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

            // Convert the favorite icon byte array output stream to a byte array.
            val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

            // Store the arguments in the bundle.
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)
            argumentsBundle.putByteArray(FAVORITE_ICON_BYTE_ARRAY, favoriteIconByteArray)

            // Create a new instance of the view SSL certificate dialog.
            val viewSslCertificateDialog = ViewSslCertificateDialog()

            // Add the bundle to the new dialog.
            viewSslCertificateDialog.arguments = argumentsBundle

            // Return the new dialog.
            return viewSslCertificateDialog
        }
    }

    // Define the class variables.
    private var hasSslCertificate: Boolean = false

    // Declare the class variables.
    private lateinit var domainString: String
    private lateinit var endDate: Date
    private lateinit var ipAddresses: String
    private lateinit var issuedByCName: String
    private lateinit var issuedByOName: String
    private lateinit var issuedByUName: String
    private lateinit var issuedToCName: String
    private lateinit var issuedToOName: String
    private lateinit var issuedToUName: String
    private lateinit var nestedScrollWebView: NestedScrollWebView
    private lateinit var startDate: Date
    private lateinit var urlString: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use a builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.audeonbrowserAlertDialog)

        // Populate the class variables.
        if (savedInstanceState == null) {  // The dialog is starting for the first time.
            // Get the current position of this WebView fragment.
            val webViewPosition = MainWebViewActivity.webViewStateAdapter!!.getPositionForId(requireArguments().getLong(WEBVIEW_FRAGMENT_ID))

            // Get the WebView tab fragment.
            val webViewTabFragment = MainWebViewActivity.webViewStateAdapter!!.getPageFragment(webViewPosition)

            // Get the fragment view.
            val fragmentView = webViewTabFragment.requireView()

            // Get a handle for the current nested scroll WebView.
            nestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview)

            // Get the SSL certificate.
            val sslCertificate = nestedScrollWebView.certificate

            // Store the status of the SSL certificate.
            hasSslCertificate = sslCertificate != null

            // Store the URL string.
            urlString = nestedScrollWebView.currentUrl

            // Populate the certificate class variables if the webpage has an SSL certificate.
            if (hasSslCertificate) {
                // Convert the URL to a URI.
                val uri = nestedScrollWebView.currentUrl.toUri()

                // Extract the domain name from the URI.
                domainString = uri.host!!

                // Get the ip addresses from the nested scroll WebView.
                ipAddresses = nestedScrollWebView.currentIpAddresses

                // Get the strings from the SSL certificate.
                issuedToCName = sslCertificate!!.issuedTo.cName
                issuedToOName = sslCertificate.issuedTo.oName
                issuedToUName = sslCertificate.issuedTo.uName
                issuedByCName = sslCertificate.issuedBy.cName
                issuedByOName = sslCertificate.issuedBy.oName
                issuedByUName = sslCertificate.issuedBy.uName
                startDate = sslCertificate.validNotBeforeDate
                endDate = sslCertificate.validNotAfterDate
            }
        } else {  // The dialog has been restarted.
            // Get the data from the saved instance state.
            hasSslCertificate = savedInstanceState.getBoolean(HAS_SSL_CERTIFICATE)
            urlString = savedInstanceState.getString(URL)!!

            // Populate the certificate class variables if the webpage has an SSL certificate.
            if (hasSslCertificate) {
                // Populate the certificate class variables from the saved instance state.
                domainString = savedInstanceState.getString(DOMAIN)!!
                ipAddresses = savedInstanceState.getString(IP_ADDRESSES)!!
                issuedToCName = savedInstanceState.getString(ISSUED_TO_CNAME)!!
                issuedToOName = savedInstanceState.getString(ISSUED_TO_ONAME)!!
                issuedToUName = savedInstanceState.getString(ISSUED_TO_UNAME)!!
                issuedByCName = savedInstanceState.getString(ISSUED_BY_CNAME)!!
                issuedByOName = savedInstanceState.getString(ISSUED_BY_ONAME)!!
                issuedByUName = savedInstanceState.getString(ISSUED_BY_UNAME)!!
                startDate = Date(savedInstanceState.getLong(START_DATE))
                endDate = Date(savedInstanceState.getLong(END_DATE))
            }
        }

        // Get the favorite icon byte array from the arguments.
        val favoriteIconByteArray = requireArguments().getByteArray(FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Create a drawable version of the favorite icon.
        val favoriteIconDrawable = favoriteIconBitmap.toDrawable(resources)

        // Set the icon.
        dialogBuilder.setIcon(favoriteIconDrawable)

        // Set the close button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.close, null)

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Check to see if the website is encrypted.
        if (hasSslCertificate) {  // The website is encrypted.
            // Set the title.
            dialogBuilder.setTitle(R.string.ssl_certificate)

            // Set the layout.
            dialogBuilder.setView(R.layout.view_ssl_certificate_dialog)

            // Create an alert dialog from the builder.
            val alertDialog = dialogBuilder.create()

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                // Disable screenshots.
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // The alert dialog must be shown before items in the layout can be modified.
            alertDialog.show()

            // Get handles for the text views.
            val domainTextView = alertDialog.findViewById<TextView>(R.id.domain)!!
            val ipAddressesTextView = alertDialog.findViewById<TextView>(R.id.ip_addresses)!!
            val issuedToCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_cname)!!
            val issuedToONameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_oname)!!
            val issuedToUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_uname)!!
            val issuedByCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_cname)!!
            val issuedByONameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_oname)!!
            val issuedByUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_uname)!!
            val startDateTextView = alertDialog.findViewById<TextView>(R.id.start_date)!!
            val endDateTextView = alertDialog.findViewById<TextView>(R.id.end_date)!!

            // Setup the labels.
            val domainLabel = getString(R.string.domain_label)
            val ipAddressesLabel = getString(R.string.ip_addresses)
            val cNameLabel = getString(R.string.common_name)
            val oNameLabel = getString(R.string.organization)
            val uNameLabel = getString(R.string.organizational_unit)
            val startDateLabel = getString(R.string.start_date)
            val endDateLabel = getString(R.string.end_date)

            // Create spannable string builders for each text view that needs multiple colors of text.
            val domainStringBuilder = SpannableStringBuilder(domainLabel + domainString)
            val ipAddressesStringBuilder = SpannableStringBuilder(ipAddressesLabel + ipAddresses)
            val issuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedToCName)
            val issuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedToOName)
            val issuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedToUName)
            val issuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedByCName)
            val issuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedByOName)
            val issuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedByUName)
            val startDateStringBuilder = SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(startDate))
            val endDateStringBuilder = SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(endDate))

            // Define the color spans.
            val blueColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.alt_blue_text))
            val redColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.red_text))

            // Format the domain string and issued to CName colors.
            if (domainString == issuedToCName) {  // The domain and issued to CName match.
                // Set the strings to be blue.
                domainStringBuilder.setSpan(blueColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else if (issuedToCName.startsWith("*.")) {  // The issued to CName begins with a wildcard.
                // Remove the initial `*.`.
                val baseCertificateDomain = issuedToCName.substring(2)

                // Setup a copy of the domain string to test subdomains.
                var domainStringSubdomain = domainString

                // Define a domain names match variable.
                var domainNamesMatch = false

                // Check all the subdomains against the base certificate domain.
                while (!domainNamesMatch && domainStringSubdomain.contains(".")) {  // Stop checking if we know that the domain names match or if we run out of subdomains.
                    // Test the subdomain against the base certificate domain.
                    if (domainStringSubdomain == baseCertificateDomain) {
                        domainNamesMatch = true
                    }

                    // Strip out the lowest subdomain.
                    domainStringSubdomain = domainStringSubdomain.substring(domainStringSubdomain.indexOf(".") + 1)
                }

                // Format the domain and issued to CName.
                if (domainNamesMatch) {  // The domain is a subdomain of the wildcard certificate.
                    // Set the strings to be blue.
                    domainStringBuilder.setSpan(blueColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                } else {  // The domain is not a subdomain of the wildcard certificate.
                    // Set the string to be red.
                    domainStringBuilder.setSpan(redColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
            } else {  // The strings do not match and issued to CName does not begin with a wildcard.
                // Set the strings to be red.
                domainStringBuilder.setSpan(redColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Set the IP addresses, issued to, and issued by spans to display the certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            ipAddressesStringBuilder.setSpan(blueColorSpan, ipAddressesLabel.length, ipAddressesStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Get the current date.
            val currentDate = Calendar.getInstance().time

            //  Format the start date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (startDate.after(currentDate)) {  // The certificate start date is in the future.
                startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {  // The certificate start date is in the past.
                startDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Format the end date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (endDate.before(currentDate)) {  // The certificate end date is in the past.
                endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {  // The certificate end date is in the future.
                endDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Display the strings.
            domainTextView.text = domainStringBuilder
            ipAddressesTextView.text = ipAddressesStringBuilder
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
        } else {  // The website is not encrypted.
            // Populate the dialog according to the URL type.
            if (urlString.startsWith("content://")) {  // A content URL is loaded.
                // Set the title.
                dialogBuilder.setTitle(R.string.content_url)

                // Set the message.
                dialogBuilder.setMessage(R.string.content_url_message)
            } else {  // The website is unencrypted.
                // Set the title.
                dialogBuilder.setTitle(R.string.unencrypted_website)

                // Set the layout.
                dialogBuilder.setView(R.layout.unencrypted_website_dialog)
            }

            // Create an alert dialog from the builder.
            val alertDialog = dialogBuilder.create()

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                // Disable screenshots.
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Return the alert dialog.
            return alertDialog
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Save the common class variables.
        savedInstanceState.putBoolean(HAS_SSL_CERTIFICATE, hasSslCertificate)
        savedInstanceState.putString(URL, urlString)

        // Save the SSL certificate strings if they exist.
        if (hasSslCertificate) {
            savedInstanceState.putString(DOMAIN, domainString)
            savedInstanceState.putString(IP_ADDRESSES, ipAddresses)
            savedInstanceState.putString(ISSUED_TO_CNAME, issuedToCName)
            savedInstanceState.putString(ISSUED_TO_ONAME, issuedToOName)
            savedInstanceState.putString(ISSUED_TO_UNAME, issuedToUName)
            savedInstanceState.putString(ISSUED_BY_CNAME, issuedByCName)
            savedInstanceState.putString(ISSUED_BY_ONAME, issuedByOName)
            savedInstanceState.putString(ISSUED_BY_UNAME, issuedByUName)
            savedInstanceState.putLong(START_DATE, startDate.time)
            savedInstanceState.putLong(END_DATE, endDate.time)
        }
    }
}
