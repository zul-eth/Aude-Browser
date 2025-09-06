/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2020-2024 Soren Stoutner <soren@stoutner.com>
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

package com.audeon.browser.helpers

import android.content.Context
import android.net.Uri
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.EditText

import com.audeon.browser.R

import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat

object UrlHelper {
    // Content dispositions can contain other text besides the file name, and they can be in any order.
    // Elements are separated by semicolons.  Sometimes the file names are contained in quotes.
    fun getFileName(context: Context, contentDispositionString: String?, contentTypeString: String?, urlString: String): String {
        // Define a file name string.
        var fileNameString: String

        // Only process the content disposition string if it isn't null.
        if (contentDispositionString != null) {  // The content disposition is not null.
            // Check to see if the content disposition contains a file name.
            if (contentDispositionString.contains("filename=")) {  // The content disposition contains a filename.
                // Get the part of the content disposition after `filename=`.
                fileNameString = contentDispositionString.substring(contentDispositionString.indexOf("filename=") + 9)

                // Remove any `;` and anything after it.  This removes any entries after the filename.
                if (fileNameString.contains(";"))
                    fileNameString = fileNameString.substring(0, fileNameString.indexOf(";") - 1)

                // Remove any `"` at the beginning of the string.
                if (fileNameString.startsWith("\""))
                    fileNameString = fileNameString.substring(1)

                // Remove any `"` at the end of the string.
                if (fileNameString.endsWith("\""))
                    fileNameString = fileNameString.substring(0, fileNameString.length - 1)
            } else {  // The headers contain no useful information.
                // Get the file name string from the URL.
                fileNameString = getFileNameFromUrl(context, urlString, contentTypeString)
            }
        } else {  // The content disposition is null.
            // Get the file name string from the URL.
            fileNameString = getFileNameFromUrl(context, urlString, contentTypeString)
        }

        // Return the file name string.
        return fileNameString
    }

    private fun getFileNameFromUrl(context: Context, urlString: String, contentTypeString: String?): String {
        // Convert the URL string to a URI.
        val uri = Uri.parse(urlString)

        // Get the last path segment.
        var lastPathSegment = uri.lastPathSegment

        // Use a default file name if the last path segment is null.
        if (lastPathSegment == null) {
            // Set the last path segment to be the generic file name.
            lastPathSegment = context.getString(R.string.file)

            // Add a file extension if it can be detected.
            if (MimeTypeMap.getSingleton().hasMimeType(contentTypeString))
                lastPathSegment = lastPathSegment + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(contentTypeString)
        }

        // Return the last path segment as the file name.
        return lastPathSegment
    }

    fun getNameAndSize(context: Context, urlString: String, userAgent: String, cookiesEnabled: Boolean): Pair<String, String> {
        // Define the strings.
        var fileNameString: String
        var formattedFileSize: String

        // Populate the file size and name strings.
        if (urlString.startsWith("data:")) {  // The URL contains the entire data of an image.
            // Remove `data:` from the beginning of the URL.
            val urlWithoutData = urlString.substring(5)

            // Get the URL MIME type, which ends with a `;`.
            val urlMimeType = urlWithoutData.substring(0, urlWithoutData.indexOf(";"))

            // Get the Base64 data, which begins after a `,`.
            val base64DataString = urlWithoutData.substring(urlWithoutData.indexOf(",") + 1)

            // Calculate the file size of the data URL.  Each Base64 character represents 6 bits.
            formattedFileSize = NumberFormat.getInstance().format(base64DataString.length * 3L / 4) + " " + context.getString(R.string.bytes)

            // Set the file name according to the MIME type.
            fileNameString = context.getString(R.string.file) + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(urlMimeType)
        } else {  // The URL refers to the location of the data.
            // Initialize the formatted file size string.
            formattedFileSize = context.getString(R.string.unknown_size)

            // Because everything relating to requesting data from a webserver can throw errors, the entire section must catch exceptions.
            try {
                // Convert the URL string to a URL.
                val url = URL(urlString)

                // Instantiate the proxy helper.
                val proxyHelper = ProxyHelper()

                // Get the current proxy.
                val proxy = proxyHelper.getCurrentProxy(context)

                // Open a connection to the URL.  No data is actually sent at this point.
                val httpUrlConnection = url.openConnection(proxy) as HttpURLConnection

                // Add the user agent to the header property.
                httpUrlConnection.setRequestProperty("User-Agent", userAgent)

                // Add the cookies if they are enabled.
                if (cookiesEnabled) {
                    // Get the cookies for the current domain.
                    val cookiesString = CookieManager.getInstance().getCookie(url.toString())

                    // Add the cookies if they are not null.
                    if (cookiesString != null)
                        httpUrlConnection.setRequestProperty("Cookie", cookiesString)
                }

                // The actual network request is in a `try` bracket so that `disconnect()` is run in the `finally` section even if an error is encountered in the main block.
                try {
                    // Get the status code.  This initiates a network connection.
                    val responseCode = httpUrlConnection.responseCode

                    // Check the response code.
                    if (responseCode >= 400) {  // The response code is an error message.
                        // Set the formatted file size to indicate a bad URL.
                        formattedFileSize = context.getString(R.string.invalid_url)

                        // Set the file name according to the URL.
                        fileNameString = getFileNameFromUrl(context, urlString, null)
                    } else {  // The response code is not an error message.
                        // Get the headers.
                        val contentLengthString = httpUrlConnection.getHeaderField("Content-Length")
                        val contentDispositionString = httpUrlConnection.getHeaderField("Content-Disposition")
                        var contentTypeString = httpUrlConnection.contentType

                        // Remove anything after the MIME type in the content type string.
                        if (contentTypeString.contains(";"))
                            contentTypeString = contentTypeString.substring(0, contentTypeString.indexOf(";"))

                        // Only process the content length string if it isn't null.
                        if (contentLengthString != null) {
                            // Convert the content length string to a long.
                            val fileSize = contentLengthString.toLong()

                            // Format the file size.
                            formattedFileSize = NumberFormat.getInstance().format(fileSize) + " " + context.getString(R.string.bytes)
                        }

                        // Get the file name string from the content disposition.
                        fileNameString = getFileName(context, contentDispositionString, contentTypeString, urlString)
                    }
                } finally {
                    // Disconnect the HTTP URL connection.
                    httpUrlConnection.disconnect()
                }
            } catch (exception: Exception) {
                // Set the formatted file size to indicate a bad URL.
                formattedFileSize = context.getString(R.string.invalid_url)

                // Set the file name according to the URL.
                fileNameString = getFileNameFromUrl(context, urlString, null)
            }
        }

        // Return the file name and size.
        return Pair(fileNameString, formattedFileSize)
    }

    fun highlightSyntax(urlEditText: EditText, initialGrayColorSpan: ForegroundColorSpan, finalGrayColorSpan: ForegroundColorSpan, redColorSpan: ForegroundColorSpan) {
        // Get the URL string.
        val urlString: String = urlEditText.text.toString()

        // Highlight the URL according to the protocol.
        if (urlString.startsWith("file://") || urlString.startsWith("content://")) {  // This is a file or content URL.
            // De-emphasize everything before the file name.
            urlEditText.text.setSpan(initialGrayColorSpan, 0, urlString.lastIndexOf("/") + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {  // This is a web URL.
            // Get the index of the `/` immediately after the domain name.
            val endOfDomainName = urlString.indexOf("/", urlString.indexOf("//") + 2)

            // Get the base URL.
            val baseUrl: String = if (endOfDomainName > 0)  // There is at least one character after the base URL.
                urlString.substring(0, endOfDomainName)
            else  // There are no characters after the base URL.
                urlString

            // Get the index of the last `.` in the domain.
            val lastDotIndex = baseUrl.lastIndexOf(".")

            // Get the index of the penultimate `.` in the domain.
            val penultimateDotIndex = baseUrl.lastIndexOf(".", lastDotIndex - 1)

            // Markup the beginning of the URL.
            if (urlString.startsWith("https://")) {  // The protocol is encrypted.
                // De-emphasize the protocol of connections that are encrypted.
                if (penultimateDotIndex > 0)  // There is more than one subdomain in the domain name.  De-emphasize the protocol and the additional subdomains.
                    urlEditText.text.setSpan(initialGrayColorSpan, 0, penultimateDotIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                else  // There is only one subdomain in the domain name.  De-emphasize only the protocol.
                    urlEditText.text.setSpan(initialGrayColorSpan, 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (urlString.startsWith("http://")) {  // The protocol is not encrypted.
                // Highlight the protocol in red.
                urlEditText.text.setSpan(redColorSpan, 0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                // De-emphasize subdomains.
                if (penultimateDotIndex > 0) // There is more than one subdomain in the domain name.
                    urlEditText.text.setSpan(initialGrayColorSpan, 7, penultimateDotIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (urlString.startsWith("view-source:https://")) {  // A secure source is being viewed.
                // De-emphasize the protocol of connections that are encrypted.
                if (penultimateDotIndex > 0)  // There is more than one subdomain in the domain name.  De-emphasize the protocol and the additional subdomains.
                    urlEditText.text.setSpan(initialGrayColorSpan, 0, penultimateDotIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                else  // There is only one subdomain in the domain name.  De-emphasize only the protocol.
                    urlEditText.text.setSpan(initialGrayColorSpan, 0, 20, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (urlString.startsWith("view-source:http://")) {  // An insecure source is being viewed.
                // Check to see if subdomains should be de-emphasized.
                if (penultimateDotIndex > 0) {  // There are subdomains that should be de-emphasized.
                    // De-emphasize the `view-source:` text.
                    urlEditText.text.setSpan(initialGrayColorSpan, 0, penultimateDotIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {  // There are no subdomains that need to be de-emphasized.
                    // De-emphasize the `view-source:` text.
                    urlEditText.text.setSpan(initialGrayColorSpan, 0, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                // Highlight the protocol in red.
                urlEditText.text.setSpan(redColorSpan, 12, 19, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // De-emphasize the text after the domain name.
            if (endOfDomainName > 0)
                urlEditText.text.setSpan(finalGrayColorSpan, endOfDomainName, urlString.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
    }
}
