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

package com.audeon.browser.backgroundtasks

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.webkit.CookieManager

import com.audeon.browser.R
import com.audeon.browser.viewmodels.HeadersViewModel

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class GetHeadersBackgroundTask {
    fun acquire(application: Application, urlString: String, userAgent: String, localeString: String, proxy: Proxy, contentResolver: ContentResolver, headersViewModel: HeadersViewModel, ignoreSslErrors: Boolean):
            Array<SpannableStringBuilder> {

        // Initialize the spannable string builders.
        val sslInformationBuilder = SpannableStringBuilder()
        val appliedCipherBuilder = SpannableStringBuilder()
        val availableCiphersBuilder = SpannableStringBuilder()
        val sslCertificateBuilder = SpannableStringBuilder()
        val requestHeadersBuilder = SpannableStringBuilder()
        val responseMessageBuilder = SpannableStringBuilder()
        val responseHeadersBuilder = SpannableStringBuilder()
        val responseBodyBuilder = SpannableStringBuilder()

        // Get the colon string.
        val colonString = application.getString(R.string.colon)
        val newLineString = System.lineSeparator()

        if (urlString.startsWith("content://")) {  // This is a content URL.
            // Attempt to read the content data.  Return an error if this fails.
            try {
                // Get a URI for the content URL.
                val contentUri = Uri.parse(urlString)

                // Get a cursor with metadata about the content URL.
                val contentCursor = contentResolver.query(contentUri, null, null, null, null)!!

                // Move the content cursor to the first row.
                contentCursor.moveToFirst()

                // Populate the response header.
                for (i in 0 until contentCursor.columnCount) {
                    // Add a new line if this is not the first entry.
                    if (i > 0)
                        responseHeadersBuilder.append(newLineString)

                    // Add each header to the string builder.
                    responseHeadersBuilder.append(contentCursor.getColumnName(i), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    responseHeadersBuilder.append(colonString)
                    responseHeadersBuilder.append(contentCursor.getString(i))
                }

                // Close the content cursor.
                contentCursor.close()

                // Create a buffered string reader for the content data.
                val bufferedReader = BufferedReader(InputStreamReader(contentResolver.openInputStream(contentUri)))

                // Create a content line string.
                var contentLineString: String?

                // Get the data from the buffered reader one line at a time.
                while (bufferedReader.readLine().also { contentLineString = it } != null) {
                    // Add the line to the response body builder.
                    responseBodyBuilder.append(contentLineString)

                    // Append a new line.
                    responseBodyBuilder.append("\n")
                }
            } catch (exception: Exception) {
                // Return the error message.
                headersViewModel.returnError(exception.toString())
            }
        } else {  // This is not a content URL.
            // Because everything relating to requesting data from a webserver can throw errors, the entire section must catch `IOExceptions`.
            try {
                // Get the current URL from the main activity.
                val url = URL(urlString)

                // Open a connection to the URL.  No data is actually sent at this point.
                val httpUrlConnection = url.openConnection(proxy) as HttpURLConnection


                // Set the `Host` header property.
                httpUrlConnection.setRequestProperty("Host", url.host)

                // Add the `Host` header to the string builder and format the text.
                requestHeadersBuilder.append("Host", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append(url.host)


                // Set the `Connection` header property.
                httpUrlConnection.setRequestProperty("Connection", "keep-alive")

                // Add the `Connection` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Connection", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("keep-alive")


                // Set the `Upgrade-Insecure-Requests` header property.
                httpUrlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1")

                // Add the `Upgrade-Insecure-Requests` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Upgrade-Insecure-Requests", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("1")


                // Set the `User-Agent` header property.
                httpUrlConnection.setRequestProperty("User-Agent", userAgent)

                // Add the `User-Agent` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("User-Agent", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append(userAgent)


                // Set the `Sec-Fetch-Site` header property.
                httpUrlConnection.setRequestProperty("Sec-Fetch-Site", "none")

                // Add the `Sec-Fetch-Site` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Sec-Fetch-Site", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("none")


                // Set the `Sec-Fetch-Mode` header property.
                httpUrlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate")

                // Add the `Sec-Fetch-Mode` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Sec-Fetch-Mode", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("navigate")


                // Set the `Sec-Fetch-User` header property.
                httpUrlConnection.setRequestProperty("Sec-Fetch-User", "?1")

                // Add the `Sec-Fetch-User` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Sec-Fetch-User", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("?1")


                // Set the `Sec-Fetch-Dest` header property.
                httpUrlConnection.setRequestProperty("Sec-Fetch-Dest", "document")

                // Add the `Sec-Fetch-User` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Sec-Fetch-Dest", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("document")


                // Set the `Accept` header property.
                httpUrlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")

                // Add the `Accept` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Accept", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")


                // Set the `Accept-Language` header property.
                httpUrlConnection.setRequestProperty("Accept-Language", localeString)

                // Add the `Accept-Language` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Accept-Language", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append(localeString)


                // Get the cookies for the current domain.
                val cookiesString = CookieManager.getInstance().getCookie(url.toString())

                // Only process the cookies if they are not null.
                if (cookiesString != null) {
                    // Add the cookies to the header property.
                    httpUrlConnection.setRequestProperty("Cookie", cookiesString)

                    // Add the cookie header to the string builder and format the text.
                    requestHeadersBuilder.append(newLineString)
                    requestHeadersBuilder.append("Cookie", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    requestHeadersBuilder.append(colonString)
                    requestHeadersBuilder.append(cookiesString)
                }


                // `HttpUrlConnection` sets `Accept-Encoding` to be `gzip` by default.  If the property is manually set, than `HttpUrlConnection` does not process the decoding.
                // Add the `Accept-Encoding` header to the string builder and format the text.
                requestHeadersBuilder.append(newLineString)
                requestHeadersBuilder.append("Accept-Encoding", StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                requestHeadersBuilder.append(colonString)
                requestHeadersBuilder.append("gzip")

                // Ignore SSL errors if requested.
                if (ignoreSslErrors) {
                    // Create a new host name verifier that allows all host names without checking for SSL errors.
                    val hostnameVerifier = HostnameVerifier { _: String?, _: SSLSession? -> true }

                    // Create a new trust manager.  Lint wants to warn us that it is hard to securely implement an X509 trust manager.
                    // But the point of this trust manager is that it should accept all certificates no matter what, so that isn't an issue in our case.
                    @SuppressLint("CustomX509TrustManager") val trustManager = arrayOf<TrustManager>(
                        object : X509TrustManager {
                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                                // Do nothing, which trusts all client certificates.
                            }

                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                                // Do nothing, which trusts all server certificates.
                            }

                            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                                return null
                            }
                        }
                    )

                    // Get an SSL context.  `TLS` provides a base instance available from API 1.  <https://developer.android.com/reference/javax/net/ssl/SSLContext>
                    val sslContext = SSLContext.getInstance("TLS")

                    // Initialize the SSL context with the blank trust manager.
                    sslContext.init(null, trustManager, SecureRandom())

                    // Get the SSL socket factory with the blank trust manager.
                    val socketFactory = sslContext.socketFactory

                    // Set the HTTPS URL Connection to use the blank host name verifier.
                    (httpUrlConnection as HttpsURLConnection).hostnameVerifier = hostnameVerifier

                    // Set the HTTPS URL connection to use the socket factory with the blank trust manager.
                    httpUrlConnection.sslSocketFactory = socketFactory
                }

                // The actual network request is in a `try` bracket so that `disconnect()` is run in the `finally` section even if an error is encountered in the main block.
                try {
                    // Get the response code, which causes the connection to the server to be made.
                    val responseCode = httpUrlConnection.responseCode

                    // Try to populate the SSL certificate information.
                    try {
                        // Get the applied cipher suite string.
                        val appliedCipherString = (httpUrlConnection as HttpsURLConnection).cipherSuite

                        // Populate the applied cipher builder, returned separately.
                        appliedCipherBuilder.append(appliedCipherString)

                        // Append the applied cipher suite to the SSL information builder.
                        sslInformationBuilder.append(application.getString(R.string.applied_cipher), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sslInformationBuilder.append(colonString)
                        sslInformationBuilder.append(appliedCipherString)
                        sslInformationBuilder.append(newLineString)

                        // Append the peer principal to the SSL information builder.
                        sslInformationBuilder.append(application.getString(R.string.peer_principal), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sslInformationBuilder.append(colonString)
                        sslInformationBuilder.append(httpUrlConnection.peerPrincipal.toString())
                        sslInformationBuilder.append(newLineString)

                        // Get the server certificate.
                        val serverCertificate = httpUrlConnection.serverCertificates[0]

                        // Append the certificate type to the SSL information builder.
                        sslInformationBuilder.append(application.getString(R.string.certificate_type), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sslInformationBuilder.append(colonString)
                        sslInformationBuilder.append(serverCertificate.type)
                        sslInformationBuilder.append(newLineString)

                        // Append the certificate hash code to the SSL information builder.
                        sslInformationBuilder.append(application.getString(R.string.certificate_hash_code), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sslInformationBuilder.append(colonString)
                        sslInformationBuilder.append(serverCertificate.hashCode().toString())

                        // Get the available cipher suites string array.
                        val availableCipherSuitesStringArray = httpUrlConnection.sslSocketFactory.defaultCipherSuites

                        // Get the available cipher suites string array size.
                        val availableCipherSuitesStringArraySize = availableCipherSuitesStringArray.size

                        // Populate the available cipher suites, returned separately.
                        for (i in 0 until availableCipherSuitesStringArraySize) {
                            // Append a new line if a cipher is already populated.
                            if (i > 0)
                                availableCiphersBuilder.append(newLineString)

                            // Get the current cipher suite.
                            val currentCipherSuite = availableCipherSuitesStringArray[i]

                            // Append the current cipher to the list.
                            availableCiphersBuilder.append(currentCipherSuite)
                        }

                        // Populate the SSL certificate, returned separately.
                        sslCertificateBuilder.append(serverCertificate.toString())
                    } catch (exception: Exception) {
                        // Do nothing.
                    }

                    // Populate the response message string builder.
                    responseMessageBuilder.append(responseCode.toString(), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    responseMessageBuilder.append(colonString)
                    responseMessageBuilder.append(httpUrlConnection.responseMessage)

                    // Initialize the iteration variable.
                    var i = 0

                    // Iterate through the received header fields.
                    while (httpUrlConnection.getHeaderField(i) != null) {
                        // Add a new line if there is already information in the string builder.
                        if (i > 0)
                            responseHeadersBuilder.append(newLineString)

                        // Add the header to the string builder and format the text.
                        responseHeadersBuilder.append(httpUrlConnection.getHeaderFieldKey(i), StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        responseHeadersBuilder.append(colonString)
                        responseHeadersBuilder.append(httpUrlConnection.getHeaderField(i))

                        // Increment the iteration variable.
                        i++
                    }

                    // Get the correct input stream based on the response code.
                    val inputStream: InputStream = if (responseCode == 404)  // Get the error stream.
                        BufferedInputStream(httpUrlConnection.errorStream)
                    else  // Get the response body stream.
                        BufferedInputStream(httpUrlConnection.inputStream)

                    // Initialize the byte array output stream and the conversion buffer byte array.
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val conversionBufferByteArray = ByteArray(1024)

                    // Define the buffer length variable.
                    var bufferLength: Int

                    try {
                        // Attempt to read data from the input stream and store it in the conversion buffer byte array.  Also store the amount of data read in the buffer length variable.
                        while (inputStream.read(conversionBufferByteArray).also { bufferLength = it } > 0) {  // Proceed while the amount of data stored in the buffer is > 0.
                            // Write the contents of the conversion buffer to the byte array output stream.
                            byteArrayOutputStream.write(conversionBufferByteArray, 0, bufferLength)
                        }
                    } catch (exception: IOException) {
                        // Return the error message.
                        headersViewModel.returnError(exception.toString())
                    }

                    // Close the input stream.
                    inputStream.close()

                    // Populate the response body string with the contents of the byte array output stream.
                    responseBodyBuilder.append(byteArrayOutputStream.toString())
                } finally {
                    // Disconnect HTTP URL connection.
                    httpUrlConnection.disconnect()
                }
            } catch (exception: Exception) {
                // Return the error message.
                headersViewModel.returnError(exception.toString())
            }
        }

        // Return the spannable string builders.
        return arrayOf(sslInformationBuilder, appliedCipherBuilder, availableCiphersBuilder, sslCertificateBuilder, requestHeadersBuilder, responseMessageBuilder, responseHeadersBuilder, responseBodyBuilder)
    }
}
