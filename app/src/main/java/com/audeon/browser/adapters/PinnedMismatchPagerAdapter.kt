/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2021-2023, 2025 Soren Stoutner <soren@stoutner.com>
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

package com.audeon.browser.adapters

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.core.net.toUri
import androidx.viewpager.widget.PagerAdapter

import com.audeon.browser.R
import com.audeon.browser.activities.MainWebViewActivity
import com.audeon.browser.views.NestedScrollWebView

import java.text.DateFormat
import java.util.Date

// This adapter uses a PagerAdapter instead of a FragmentPagerAdapter because dialogs fragments really don't like having a nested FragmentPagerAdapter inside of them.
class PinnedMismatchPagerAdapter(private val context: Context, private val layoutInflater: LayoutInflater, private val webViewFragmentId: Long) : PagerAdapter() {
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        // Check to see if the view and the object are the same.
        return view === `object`
    }

    // Get the number of tabs.
    override fun getCount(): Int {
        // There are two tabs.
        return 2
    }

    // Get the name of each tab.  Tab numbers start at 0.
    override fun getPageTitle(tabNumber: Int): CharSequence {
        return when (tabNumber) {
            0 -> context.getString(R.string.current)
            1 -> context.getString(R.string.pinned)
            else -> ""
        }
    }

    // Setup each tab.
    override fun instantiateItem(container: ViewGroup, tabNumber: Int): Any {
        // Get the current position of this WebView fragment.
        val webViewPosition = MainWebViewActivity.webViewStateAdapter!!.getPositionForId(webViewFragmentId)

        // Get the WebView tab fragment.
        val webViewTabFragment = MainWebViewActivity.webViewStateAdapter!!.getPageFragment(webViewPosition)

        // Get the WebView fragment view.
        val webViewFragmentView = webViewTabFragment.requireView()

        // Get a handle for the current WebView.
        val nestedScrollWebView = webViewFragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)!!

        // Inflate the scroll view for this tab.
        val tabLayout = layoutInflater.inflate(R.layout.pinned_mismatch_tab_linearlayout, container, false) as ViewGroup

        // Get handles for the views.
        val domainNameTextView = tabLayout.findViewById<TextView>(R.id.domain_name)
        val ipAddressesTextView = tabLayout.findViewById<TextView>(R.id.ip_addresses)
        val issuedToCNameTextView = tabLayout.findViewById<TextView>(R.id.issued_to_cname)
        val issuedToONameTextView = tabLayout.findViewById<TextView>(R.id.issued_to_oname)
        val issuedToUNameTextView = tabLayout.findViewById<TextView>(R.id.issued_to_uname)
        val issuedByCNameTextView = tabLayout.findViewById<TextView>(R.id.issued_by_cname)
        val issuedByONameTextView = tabLayout.findViewById<TextView>(R.id.issued_by_oname)
        val issuedByUNameTextView = tabLayout.findViewById<TextView>(R.id.issued_by_uname)
        val startDateTextView = tabLayout.findViewById<TextView>(R.id.start_date)
        val endDateTextView = tabLayout.findViewById<TextView>(R.id.end_date)

        // Setup the labels.
        val domainNameLabel = context.getString(R.string.domain_label)
        val ipAddressesLabel = context.getString(R.string.ip_addresses)
        val cNameLabel = context.getString(R.string.common_name)
        val oNameLabel = context.getString(R.string.organization)
        val uNameLabel = context.getString(R.string.organizational_unit)
        val startDateLabel = context.getString(R.string.start_date)
        val endDateLabel = context.getString(R.string.end_date)

        // Convert the URL to a URI.
        val currentUri = nestedScrollWebView.url!!.toUri()

        // Get the current host from the URI.
        val domainName = currentUri.host

        // Get the current website SSL certificate.
        val sslCertificate = nestedScrollWebView.certificate

        // Initialize the SSL certificate variables.
        var currentSslIssuedToCName = ""
        var currentSslIssuedToOName = ""
        var currentSslIssuedToUName = ""
        var currentSslIssuedByCName = ""
        var currentSslIssuedByOName = ""
        var currentSslIssuedByUName = ""
        var currentSslStartDate: Date? = null
        var currentSslEndDate: Date? = null

        // Extract the individual pieces of information from the current website SSL certificate if it is not null.
        if (sslCertificate != null) {
            currentSslIssuedToCName = sslCertificate.issuedTo.cName
            currentSslIssuedToOName = sslCertificate.issuedTo.oName
            currentSslIssuedToUName = sslCertificate.issuedTo.uName
            currentSslIssuedByCName = sslCertificate.issuedBy.cName
            currentSslIssuedByOName = sslCertificate.issuedBy.oName
            currentSslIssuedByUName = sslCertificate.issuedBy.uName
            currentSslStartDate = sslCertificate.validNotBeforeDate
            currentSslEndDate = sslCertificate.validNotAfterDate
        }

        // Get the pinned SSL certificate pair.
        val pinnedSslCertificatePair = nestedScrollWebView.getPinnedSslCertificate()

        // Extract the arrays from the array list.
        val pinnedSslCertificateStringArray = pinnedSslCertificatePair.first
        val pinnedSslCertificateDateArray = pinnedSslCertificatePair.second

        // Setup the domain name spannable string builder.
        val domainNameStringBuilder = SpannableStringBuilder(domainNameLabel + domainName)

        // Initialize the spannable string builders.
        val ipAddressesStringBuilder: SpannableStringBuilder
        val issuedToCNameStringBuilder: SpannableStringBuilder
        val issuedToONameStringBuilder: SpannableStringBuilder
        val issuedToUNameStringBuilder: SpannableStringBuilder
        val issuedByCNameStringBuilder: SpannableStringBuilder
        val issuedByONameStringBuilder: SpannableStringBuilder
        val issuedByUNameStringBuilder: SpannableStringBuilder
        val startDateStringBuilder: SpannableStringBuilder
        val endDateStringBuilder: SpannableStringBuilder

        // Setup the spannable string builders for each tab.
        if (tabNumber == 0) {  // Setup the current settings tab.
            // Create the string builders.
            ipAddressesStringBuilder = SpannableStringBuilder(ipAddressesLabel + nestedScrollWebView.currentIpAddresses)
            issuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + currentSslIssuedToCName)
            issuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + currentSslIssuedToOName)
            issuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + currentSslIssuedToUName)
            issuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + currentSslIssuedByCName)
            issuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + currentSslIssuedByOName)
            issuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + currentSslIssuedByUName)

            // Set the dates if they aren't null.  Formatting a null date causes a crash.
            startDateStringBuilder = if (currentSslStartDate == null) {
                SpannableStringBuilder(startDateLabel)
            } else {
                SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(currentSslStartDate))
            }

            endDateStringBuilder = if (currentSslEndDate == null) {
                SpannableStringBuilder(endDateLabel)
            } else {
                SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(currentSslEndDate))
            }
        } else {  // Setup the pinned settings tab.
            // Create the string builders.
            ipAddressesStringBuilder = SpannableStringBuilder(ipAddressesLabel + nestedScrollWebView.pinnedIpAddresses)
            issuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + pinnedSslCertificateStringArray[0])
            issuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + pinnedSslCertificateStringArray[1])
            issuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + pinnedSslCertificateStringArray[2])
            issuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + pinnedSslCertificateStringArray[3])
            issuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + pinnedSslCertificateStringArray[4])
            issuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + pinnedSslCertificateStringArray[5])
            startDateStringBuilder = SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(pinnedSslCertificateDateArray[0]))
            endDateStringBuilder = SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(pinnedSslCertificateDateArray[1]))
        }

        // Create the color spans.
        val blueColorSpan = ForegroundColorSpan(context.getColor(R.color.alt_blue_text))
        val redColorSpan = ForegroundColorSpan(context.getColor(R.color.red_text))

        // Set the domain name to be blue.
        domainNameStringBuilder.setSpan(blueColorSpan, domainNameLabel.length, domainNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Color coordinate the IP addresses if they are pinned.
        if (nestedScrollWebView.pinnedIpAddresses != "") {
            if (nestedScrollWebView.currentIpAddresses == nestedScrollWebView.pinnedIpAddresses) {
                ipAddressesStringBuilder.setSpan(blueColorSpan, ipAddressesLabel.length, ipAddressesStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                ipAddressesStringBuilder.setSpan(redColorSpan, ipAddressesLabel.length, ipAddressesStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }

        // Color coordinate the SSL certificate fields if they are pinned.
        if (nestedScrollWebView.hasPinnedSslCertificate()) {
            if (currentSslIssuedToCName == pinnedSslCertificateStringArray[0]) {
                issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslIssuedToOName == pinnedSslCertificateStringArray[1]) {
                issuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                issuedToONameStringBuilder.setSpan(redColorSpan, oNameLabel.length, issuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslIssuedToUName == pinnedSslCertificateStringArray[2]) {
                issuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                issuedToUNameStringBuilder.setSpan(redColorSpan, uNameLabel.length, issuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslIssuedByCName == pinnedSslCertificateStringArray[3]) {
                issuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                issuedByCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslIssuedByOName == pinnedSslCertificateStringArray[4]) {
                issuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                issuedByONameStringBuilder.setSpan(redColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslIssuedByUName == pinnedSslCertificateStringArray[5]) {
                issuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                issuedByUNameStringBuilder.setSpan(redColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslStartDate != null && currentSslStartDate == pinnedSslCertificateDateArray[0]) {
                startDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (currentSslEndDate != null && currentSslEndDate == pinnedSslCertificateDateArray[1]) {
                endDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {
                endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }

        // Display the strings.
        domainNameTextView.text = domainNameStringBuilder
        ipAddressesTextView.text = ipAddressesStringBuilder
        issuedToCNameTextView.text = issuedToCNameStringBuilder
        issuedToONameTextView.text = issuedToONameStringBuilder
        issuedToUNameTextView.text = issuedToUNameStringBuilder
        issuedByCNameTextView.text = issuedByCNameStringBuilder
        issuedByONameTextView.text = issuedByONameStringBuilder
        issuedByUNameTextView.text = issuedByUNameStringBuilder
        startDateTextView.text = startDateStringBuilder
        endDateTextView.text = endDateStringBuilder

        // Add the tab layout to the container.  This needs to be manually done for pager adapters.
        container.addView(tabLayout)

        // Return the tab layout.
        return tabLayout
    }
}
