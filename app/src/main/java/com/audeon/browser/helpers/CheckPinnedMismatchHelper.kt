/*
 * Copyright 2018-2019,2021-2023 Soren Stoutner <soren@stoutner.com>.
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

package com.audeon.browser.helpers

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

import com.audeon.browser.activities.MainWebViewActivity
import com.audeon.browser.dataclasses.PendingDialogDataClass
import com.audeon.browser.dialogs.PinnedMismatchDialog.Companion.displayDialog
import com.audeon.browser.views.NestedScrollWebView

import java.lang.Exception

import java.util.Date

object CheckPinnedMismatchHelper {
    fun checkPinnedMismatch(nestedScrollWebView: NestedScrollWebView, supportFragmentManager: FragmentManager, pinnedMismatchString: String) {
        // Initialize the current SSL certificate variables.
        var currentWebsiteIssuedToCName = ""
        var currentWebsiteIssuedToOName = ""
        var currentWebsiteIssuedToUName = ""
        var currentWebsiteIssuedByCName = ""
        var currentWebsiteIssuedByOName = ""
        var currentWebsiteIssuedByUName = ""
        var currentWebsiteSslStartDate: Date? = null
        var currentWebsiteSslEndDate: Date? = null

        // Initialize the pinned SSL certificate variables.
        var pinnedSslIssuedToCName = ""
        var pinnedSslIssuedToOName = ""
        var pinnedSslIssuedToUName = ""
        var pinnedSslIssuedByCName = ""
        var pinnedSslIssuedByOName = ""
        var pinnedSslIssuedByUName = ""
        var pinnedSslStartDate: Date? = null
        var pinnedSslEndDate: Date? = null

        // Get the current website SSL certificate.
        val currentWebsiteSslCertificate = nestedScrollWebView.certificate

        // Extract the individual pieces of information from the current website SSL certificate if it is not null.
        if (currentWebsiteSslCertificate != null) {
            currentWebsiteIssuedToCName = currentWebsiteSslCertificate.issuedTo.cName
            currentWebsiteIssuedToOName = currentWebsiteSslCertificate.issuedTo.oName
            currentWebsiteIssuedToUName = currentWebsiteSslCertificate.issuedTo.uName
            currentWebsiteIssuedByCName = currentWebsiteSslCertificate.issuedBy.cName
            currentWebsiteIssuedByOName = currentWebsiteSslCertificate.issuedBy.oName
            currentWebsiteIssuedByUName = currentWebsiteSslCertificate.issuedBy.uName
            currentWebsiteSslStartDate = currentWebsiteSslCertificate.validNotBeforeDate
            currentWebsiteSslEndDate = currentWebsiteSslCertificate.validNotAfterDate
        }

        // Get the pinned SSL certificate information if it exists.
        if (nestedScrollWebView.hasPinnedSslCertificate()) {
            // Get the pinned SSL certificate.
            val pinnedSslCertificatePair = nestedScrollWebView.getPinnedSslCertificate()

            // Extract the arrays from the array list.
            val pinnedSslCertificateStringArray = pinnedSslCertificatePair.first
            val pinnedSslCertificateDateArray = pinnedSslCertificatePair.second

            // Populate the pinned SSL certificate string variables.
            pinnedSslIssuedToCName = pinnedSslCertificateStringArray[0]
            pinnedSslIssuedToOName = pinnedSslCertificateStringArray[1]
            pinnedSslIssuedToUName = pinnedSslCertificateStringArray[2]
            pinnedSslIssuedByCName = pinnedSslCertificateStringArray[3]
            pinnedSslIssuedByOName = pinnedSslCertificateStringArray[4]
            pinnedSslIssuedByUName = pinnedSslCertificateStringArray[5]

            // Populate the pinned SSL certificate date variables.
            pinnedSslStartDate = pinnedSslCertificateDateArray[0]
            pinnedSslEndDate = pinnedSslCertificateDateArray[1]
        }

        // Initialize string variables to store the SSL certificate dates.  Strings are needed to compare the values below, which doesn't work with dates if the first one is null.
        var currentWebsiteSslStartDateString = ""
        var currentWebsiteSslEndDateString = ""
        var pinnedSslStartDateString = ""
        var pinnedSslEndDateString = ""

        // Convert the dates to strings if they are not null.
        if (currentWebsiteSslStartDate != null) {
            currentWebsiteSslStartDateString = currentWebsiteSslStartDate.toString()
        }
        if (currentWebsiteSslEndDate != null) {
            currentWebsiteSslEndDateString = currentWebsiteSslEndDate.toString()
        }
        if (pinnedSslStartDate != null) {
            pinnedSslStartDateString = pinnedSslStartDate.toString()
        }
        if (pinnedSslEndDate != null) {
            pinnedSslEndDateString = pinnedSslEndDate.toString()
        }

        // Check to see if the pinned information matches the current information.
        if (((nestedScrollWebView.pinnedIpAddresses.isNotEmpty()) && (nestedScrollWebView.currentIpAddresses != nestedScrollWebView.pinnedIpAddresses)) ||
             (nestedScrollWebView.hasPinnedSslCertificate() && ((currentWebsiteIssuedToCName != pinnedSslIssuedToCName) ||
             (currentWebsiteIssuedToOName != pinnedSslIssuedToOName) || (currentWebsiteIssuedToUName != pinnedSslIssuedToUName) ||
             (currentWebsiteIssuedByCName != pinnedSslIssuedByCName) || (currentWebsiteIssuedByOName != pinnedSslIssuedByOName) ||
             (currentWebsiteIssuedByUName != pinnedSslIssuedByUName) || (currentWebsiteSslStartDateString != pinnedSslStartDateString) ||
             (currentWebsiteSslEndDateString != pinnedSslEndDateString)))) {

            // Get a handle for the pinned mismatch alert dialog.
            val pinnedMismatchDialogFragment: DialogFragment = displayDialog(nestedScrollWebView.webViewFragmentId)

            // Try to show the dialog.  Sometimes the window is not active.
            try {
                // Show the pinned mismatch alert dialog.
                pinnedMismatchDialogFragment.show(supportFragmentManager, pinnedMismatchString)
            } catch (exception: Exception) {
                // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                MainWebViewActivity.pendingDialogsArrayList.add(PendingDialogDataClass(pinnedMismatchDialogFragment, pinnedMismatchString))
            }
        }
    }
}
