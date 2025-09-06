/*
 * Copyright 2019,2021-2023 Soren Stoutner <soren@stoutner.com>.
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
 * along with Privacy Browser Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.audeon.browser.coroutines

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView

import androidx.fragment.app.FragmentManager

import com.audeon.browser.helpers.CheckPinnedMismatchHelper
import com.audeon.browser.views.NestedScrollWebView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.lang.StringBuilder
import java.net.InetAddress
import java.net.UnknownHostException

object GetHostIpAddressesCoroutine {
    fun getAddresses(domainName: String, ipAddressesLabel: String, blueColorSpan: ForegroundColorSpan, ipAddressesTextView: TextView) {
        // Get the IP addresses using a coroutine.
        CoroutineScope(Dispatchers.Main).launch {
            // Get the IP addresses on the IO thread.
            withContext(Dispatchers.IO) {
                // Get an array with the IP addresses for the host.
                try {
                    // Initialize an IP address string builder.
                    val ipAddressesStringBuilder = StringBuilder()

                    // Get an array with all the IP addresses for the domain.
                    val inetAddressesArray = InetAddress.getAllByName(domainName)

                    // Add each IP address to the string builder.
                    for (inetAddress in inetAddressesArray) {
                        // Add a line break to the string builder if this is not the first IP address.
                        if (ipAddressesStringBuilder.isNotEmpty()) {
                            ipAddressesStringBuilder.append("\n")
                        }

                        // Add the IP address to the string builder.
                        ipAddressesStringBuilder.append(inetAddress.hostAddress)
                    }

                    // Create a spannable string builder.
                    val addressesStringBuilder = SpannableStringBuilder(ipAddressesLabel + ipAddressesStringBuilder)

                    // Set the string builder to display the certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
                    addressesStringBuilder.setSpan(blueColorSpan, ipAddressesLabel.length, addressesStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                    // Populate the IP addresses text view on the UI thread.
                    withContext(Dispatchers.Main) {
                        // Populate the IP addresses text view.
                        ipAddressesTextView.text = addressesStringBuilder
                    }
                } catch (exception: UnknownHostException) {
                    // Do nothing.
                }
            }
        }
    }

    fun checkPinnedMismatch(domainName: String, nestedScrollWebView: NestedScrollWebView, supportFragmentManager: FragmentManager, pinnedMismatchString: String) {
        // Get the IP addresses using a coroutine.
        CoroutineScope(Dispatchers.Main).launch {
            // Get the IP addresses on the IO thread.
            withContext(Dispatchers.IO) {
                // Get an array with the IP addresses for the host.
                try {
                    // Initialize an IP address string builder.
                    val ipAddressesStringBuilder = StringBuilder()

                    // Get an array with all the IP addresses for the domain.
                    val inetAddressesArray = InetAddress.getAllByName(domainName)

                    // Add each IP address to the string builder.
                    for (inetAddress in inetAddressesArray) {
                        // Add a line break to the string builder if this is not the first IP address.
                        if (ipAddressesStringBuilder.isNotEmpty()) {
                            ipAddressesStringBuilder.append("\n")
                        }

                        // Add the IP address to the string builder.
                        ipAddressesStringBuilder.append(inetAddress.hostAddress)
                    }

                    // Store the IP addresses.
                    nestedScrollWebView.currentIpAddresses = ipAddressesStringBuilder.toString()

                    // Checked for pinned mismatches if there is pinned information and it is not ignored.  This must be done on the UI thread because checking the pinned mismatch interacts with the WebView.
                    withContext(Dispatchers.Main) {
                        if ((nestedScrollWebView.hasPinnedSslCertificate() || nestedScrollWebView.pinnedIpAddresses.isNotEmpty()) && !nestedScrollWebView.ignorePinnedDomainInformation) {
                            CheckPinnedMismatchHelper.checkPinnedMismatch(nestedScrollWebView, supportFragmentManager, pinnedMismatchString)
                        }
                    }
                } catch (exception: UnknownHostException) {
                    // Do nothing.
                }
            }
        }
    }
}
