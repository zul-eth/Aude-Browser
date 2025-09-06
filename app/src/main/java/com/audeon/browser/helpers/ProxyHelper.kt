/*
 * Copyright © 2016-2022 Soren Stoutner <soren@stoutner.com>.
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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View

import androidx.preference.PreferenceManager
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

import com.audeon.browser.R
import com.audeon.browser.activities.MainWebViewActivity

import com.google.android.material.snackbar.Snackbar

import java.lang.Exception
import java.lang.IllegalArgumentException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketAddress

class ProxyHelper {
    companion object {
        // Define the public static companion object constants.
        const val NONE = "None"
        const val TOR = "Tor"
        const val I2P = "I2P"
        const val CUSTOM = "Custom"
        const val ORBOT_STATUS_ON = "ON"
    }

    fun setProxy(context: Context, activityView: View, proxyMode: String) {
        // Create a proxy config builder.
        val proxyConfigBuilder = ProxyConfig.Builder()

        // Run the commands that correlate to the proxy mode.
        when (proxyMode) {
            TOR -> {
                // Add the proxy to the builder.  The proxy config builder can use a SOCKS proxy.
                proxyConfigBuilder.addProxyRule("socks://localhost:9050")

                // Ask Orbot to connect if its current status is not `"ON"`.
                if (MainWebViewActivity.orbotStatus != ORBOT_STATUS_ON) {
                    // Create an intent to request Orbot to start.
                    val orbotIntent = Intent("org.torproject.android.intent.action.START")

                    // Send the intent to the Orbot package.
                    orbotIntent.setPackage("org.torproject.android")

                    // Request a status response be sent back to this package.
                    orbotIntent.putExtra("org.torproject.android.intent.extra.PACKAGE_NAME", context.packageName)

                    // Make it so.
                    context.sendBroadcast(orbotIntent)
                }
            }

            I2P -> {
                // Add the proxy to the builder.
                proxyConfigBuilder.addProxyRule("http://localhost:4444")
            }

            CUSTOM -> {
                // Get a handle for the shared preferences.
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

                // Get the custom proxy URL string.
                val customProxyUrlString = sharedPreferences.getString(context.getString(R.string.proxy_custom_url_key), context.getString(R.string.proxy_custom_url_default_value))

                // Parse the custom proxy URL.
                try {
                    // Add the proxy to the builder.
                    proxyConfigBuilder.addProxyRule(customProxyUrlString!!)
                } catch (exception: Exception) {  // The custom proxy URL is invalid.
                    // Display a Snackbar.
                    Snackbar.make(activityView, R.string.custom_proxy_invalid, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        // Apply the proxy settings
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            // Convert the proxy config builder into a proxy config.
            val proxyConfig = proxyConfigBuilder.build()

            // Get the proxy controller.
            val proxyController = ProxyController.getInstance()

            // Apply the proxy settings.
            if (proxyMode == NONE) {  // Remove the proxy.  A default executor and runnable are used.
                proxyController.clearProxyOverride({}, {})
            } else {  // Apply the proxy.
                try {
                    // Apply the proxy.  A default executor and runnable are used.
                    proxyController.setProxyOverride(proxyConfig, {}, {})
                } catch (exception: IllegalArgumentException) {  // The proxy config is invalid.
                    // Display a Snackbar.
                    Snackbar.make(activityView, R.string.custom_proxy_invalid, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    fun getCurrentProxy(context: Context): Proxy {
        // Get the proxy according to the current proxy mode.
        val proxy = when (MainWebViewActivity.proxyMode) {
            TOR -> {
                // Use localhost port 9050 as the socket address.
                val torSocketAddress: SocketAddress = InetSocketAddress.createUnresolved("localhost", 9050)

                // Create a SOCKS proxy.
                Proxy(Proxy.Type.SOCKS, torSocketAddress)
            }

            I2P -> {
                // Use localhost port 4444 as the socket address.
                val i2pSocketAddress: SocketAddress = InetSocketAddress.createUnresolved("localhost", 4444)

                // Create an HTTP proxy.
                Proxy(Proxy.Type.HTTP, i2pSocketAddress)
            }

            CUSTOM -> {
                // Get the shared preferences.
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

                // Get the custom proxy URL string.
                val customProxyUrlString = sharedPreferences.getString(context.getString(R.string.proxy_custom_url_key), context.getString(R.string.proxy_custom_url_default_value))

                // Parse the custom proxy URL.
                try {
                    // Convert the custom proxy URL string to a URI.
                    val customProxyUri = Uri.parse(customProxyUrlString)

                    // Get the custom socket address.
                    val customSocketAddress: SocketAddress = InetSocketAddress.createUnresolved(customProxyUri.host, customProxyUri.port)

                    // Get the custom proxy scheme.
                    val customProxyScheme = customProxyUri.scheme

                    // Create a proxy according to the scheme.
                    if (customProxyScheme != null && customProxyScheme.startsWith("socks")) {  // A SOCKS proxy is specified.
                        // Create a SOCKS proxy.
                        Proxy(Proxy.Type.SOCKS, customSocketAddress)
                    } else {  // A SOCKS proxy is not specified.
                        // Create an HTTP proxy.
                        Proxy(Proxy.Type.HTTP, customSocketAddress)
                    }
                } catch (exception: Exception) {  // The custom proxy cannot be parsed.
                    // Disable the proxy.
                    Proxy.NO_PROXY
                }
            }

            else -> {
                // Create a direct proxy.
                Proxy.NO_PROXY
            }
        }

        // Return the proxy.
        return proxy
    }
}