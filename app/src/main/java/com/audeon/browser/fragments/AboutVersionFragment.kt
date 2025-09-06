/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2025 Soren Stoutner <soren@stoutner.com>
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

package com.audeon.browser.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

import com.google.android.material.snackbar.Snackbar

import com.audeon.browser.BuildConfig
import com.audeon.browser.R
import com.audeon.browser.coroutines.SaveAboutVersionImageCoroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.text.NumberFormat

import kotlin.text.StringBuilder

// Define the class constants.
private const val FILTERLISTS_VERSIONS = "A"
private const val SCROLL_Y = "B"
private const val MEBIBYTE = 1048576

class AboutVersionFragment : Fragment() {
    // Define the class variables.
    private var updateMemoryUsageBoolean = true

    // Declare the class variables.
    private lateinit var aboutVersionLayout: View
    private lateinit var activityManager: ActivityManager
    private lateinit var appAvailableMemoryLabel: String
    private lateinit var appConsumedMemoryLabel: String
    private lateinit var appMaximumMemoryLabel: String
    private lateinit var appTotalMemoryLabel: String
    private lateinit var blueColorSpan: ForegroundColorSpan
    private lateinit var filterListsVersions: Array<String>
    private lateinit var memoryInfo: ActivityManager.MemoryInfo
    private lateinit var numberFormat: NumberFormat
    private lateinit var runtime: Runtime
    private lateinit var systemAvailableMemoryLabel: String
    private lateinit var systemConsumedMemoryLabel: String
    private lateinit var systemTotalMemoryLabel: String

    // Declare the class views.
    private lateinit var androidTextView: TextView
    private lateinit var appAvailableMemoryTextView: TextView
    private lateinit var appConsumedMemoryTextView: TextView
    private lateinit var appMaximumMemoryTextView: TextView
    private lateinit var appTotalMemoryTextView: TextView
    private lateinit var brandTextView: TextView
    private lateinit var bootloaderTextView: TextView
    private lateinit var certificateEndDateTextView: TextView
    private lateinit var certificateIssuerDnTextView: TextView
    private lateinit var certificateSerialNumberTextView: TextView
    private lateinit var certificateSignatureAlgorithmTextView: TextView
    private lateinit var certificateStartDateTextView: TextView
    private lateinit var certificateSubjectDnTextView: TextView
    private lateinit var certificateVersionTextView: TextView
    private lateinit var buildTextView: TextView
    private lateinit var deviceTextView: TextView
    private lateinit var easyListTextView: TextView
    private lateinit var easyPrivacyTextView: TextView
    private lateinit var fanboyAnnoyanceTextView: TextView
    private lateinit var fanboySocialTextView: TextView
    private lateinit var filterListsTextView: TextView
    private lateinit var hardwareTextView: TextView
    private lateinit var i2pTextView: TextView
    private lateinit var kernelTextView: TextView
    private lateinit var manufacturerTextView: TextView
    private lateinit var memoryUsageTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var openKeychainTextView: TextView
    private lateinit var orbotTextView: TextView
    private lateinit var packageSignatureTextView: TextView
    private lateinit var audeonbrowserTextView: TextView
    private lateinit var radioTextView: TextView
    private lateinit var securityPatchTextView: TextView
    private lateinit var softwareTextView: TextView
    private lateinit var systemAvailableMemoryTextView: TextView
    private lateinit var systemConsumedMemoryTextView: TextView
    private lateinit var systemTotalMemoryTextView: TextView
    private lateinit var versionTextView: TextView
    private lateinit var ultraListTextView: TextView
    private lateinit var ultraPrivacyTextView: TextView
    private lateinit var webViewProviderTextView: TextView
    private lateinit var webViewVersionTextView: TextView

    companion object {
        fun createTab(filterListsVersions: Array<String>): AboutVersionFragment {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putStringArray(FILTERLISTS_VERSIONS, filterListsVersions)

            // Create a new instance of the tab fragment.
            val aboutVersionFragment = AboutVersionFragment()

            // Add the arguments bundle to the fragment.
            aboutVersionFragment.arguments = argumentsBundle

            // Return the new fragment.
            return aboutVersionFragment
        }
    }

    // Define the save about version text activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveAboutVersionTextActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { fileUri ->
        // Only save the file if the URI is not null, which happens if the user exited the file picker by pressing back.
        if (fileUri != null) {
            // Get a cursor from the content resolver.
            val contentResolverCursor = requireActivity().contentResolver.query(fileUri, null, null, null)!!

            // Move to the first row.
            contentResolverCursor.moveToFirst()

            // Get the file name from the cursor.
            val fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

            // Close the cursor.
            contentResolverCursor.close()

            try {
                // Get the about version string.
                val aboutVersionString = getAboutVersionString()

                // Open an output stream.
                val outputStream = requireActivity().contentResolver.openOutputStream(fileUri)!!

                // Save about version using a coroutine with Dispatchers.IO.
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        // Write the about version string to the output stream.
                        outputStream.write(aboutVersionString.toByteArray(StandardCharsets.UTF_8))

                        // Close the output stream.
                        outputStream.close()
                    }
                }

                // Display a snackbar with the saved logcat information.
                Snackbar.make(aboutVersionLayout, getString(R.string.saved, fileNameString), Snackbar.LENGTH_SHORT).show()
            } catch (exception: Exception) {
                // Display a snackbar with the error message.
                Snackbar.make(aboutVersionLayout, getString(R.string.error_saving_file, fileNameString, exception.toString()), Snackbar.LENGTH_INDEFINITE).show()
            }
        }
    }

    // Define the save about version image activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveAboutVersionImageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { fileUri ->
        // Save the file if the URI is not null, which happens if the user exits the file picker by pressing back.
        if (fileUri != null)
            SaveAboutVersionImageCoroutine.saveImage(requireActivity(), fileUri, aboutVersionLayout.findViewById(R.id.about_version_linearlayout))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Store the arguments in class variables.
        filterListsVersions = requireArguments().getStringArray(FILTERLISTS_VERSIONS)!!
    }

    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Add an options menu.
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate the about version menu.
                menuInflater.inflate(R.menu.about_version_options_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Run the appropriate commands.
                when (menuItem.itemId) {
                    R.id.copy -> {  // Copy.
                        // Get the about version string.
                        val aboutVersionString = getAboutVersionString()

                        // Get a handle for the clipboard manager.
                        val clipboardManager = (requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)

                        // Place the about version string in a clip data.
                        val aboutVersionClipData = ClipData.newPlainText(getString(R.string.about), aboutVersionString)

                        // Place the clip data on the clipboard.
                        clipboardManager.setPrimaryClip(aboutVersionClipData)

                        // Display a snackbar if the API <= 32 (Android 12L).  Beginning in Android 13 the OS displays a notification that covers up the snackbar.
                        if (Build.VERSION.SDK_INT <= 32)
                            Snackbar.make(aboutVersionLayout, R.string.version_info_copied, Snackbar.LENGTH_SHORT).show()

                        // Consume the event.
                        return true
                    }

                    R.id.share -> {  // Share.
                        // Get the about version string.
                        val aboutString = getAboutVersionString()

                        // Create a share intent.
                        val shareIntent = Intent(Intent.ACTION_SEND)

                        // Add the about version string to the intent.
                        shareIntent.putExtra(Intent.EXTRA_TEXT, aboutString)

                        // Set the MIME type.
                        shareIntent.type = "text/plain"

                        // Set the intent to open in a new task.
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        // Make it so.
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))

                        // Consume the event.
                        return true
                    }

                    R.id.save_text -> {  // Save text.
                        // Open the file picker.
                        saveAboutVersionTextActivityResultLauncher.launch(getString(R.string.audeon_browser_version_txt, BuildConfig.VERSION_NAME))

                        // Consume the event.
                        return true
                    }

                    R.id.save_image -> {  // Save image.
                        // Open the file picker.
                        saveAboutVersionImageActivityResultLauncher.launch(getString(R.string.audeon_browser_version_png, BuildConfig.VERSION_NAME))

                        // Consume the event.
                        return true
                    }

                    else -> {  // The home button was selected.
                        // Do not consume the event.
                        return false
                    }
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Inflate the layout.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.  The fragment will take care of attaching the root automatically.
        aboutVersionLayout = layoutInflater.inflate(R.layout.about_version_scrollview, container, false)

        // Get handles for the views.
        audeonbrowserTextView = aboutVersionLayout.findViewById(R.id.audeon_browser_textview)
        versionTextView = aboutVersionLayout.findViewById(R.id.version)
        hardwareTextView = aboutVersionLayout.findViewById(R.id.hardware)
        brandTextView = aboutVersionLayout.findViewById(R.id.brand)
        manufacturerTextView = aboutVersionLayout.findViewById(R.id.manufacturer)
        modelTextView = aboutVersionLayout.findViewById(R.id.model)
        deviceTextView = aboutVersionLayout.findViewById(R.id.device)
        bootloaderTextView = aboutVersionLayout.findViewById(R.id.bootloader)
        radioTextView = aboutVersionLayout.findViewById(R.id.radio)
        softwareTextView = aboutVersionLayout.findViewById(R.id.software)
        androidTextView = aboutVersionLayout.findViewById(R.id.android)
        securityPatchTextView = aboutVersionLayout.findViewById(R.id.security_patch)
        buildTextView = aboutVersionLayout.findViewById(R.id.build)
        kernelTextView = aboutVersionLayout.findViewById(R.id.kernel)
        webViewProviderTextView = aboutVersionLayout.findViewById(R.id.webview_provider)
        webViewVersionTextView = aboutVersionLayout.findViewById(R.id.webview_version)
        orbotTextView = aboutVersionLayout.findViewById(R.id.orbot)
        i2pTextView = aboutVersionLayout.findViewById(R.id.i2p)
        openKeychainTextView = aboutVersionLayout.findViewById(R.id.open_keychain)
        memoryUsageTextView = aboutVersionLayout.findViewById(R.id.memory_usage)
        appConsumedMemoryTextView = aboutVersionLayout.findViewById(R.id.app_consumed_memory)
        appAvailableMemoryTextView = aboutVersionLayout.findViewById(R.id.app_available_memory)
        appTotalMemoryTextView = aboutVersionLayout.findViewById(R.id.app_total_memory)
        appMaximumMemoryTextView = aboutVersionLayout.findViewById(R.id.app_maximum_memory)
        systemConsumedMemoryTextView = aboutVersionLayout.findViewById(R.id.system_consumed_memory)
        systemAvailableMemoryTextView = aboutVersionLayout.findViewById(R.id.system_available_memory)
        systemTotalMemoryTextView = aboutVersionLayout.findViewById(R.id.system_total_memory)
        filterListsTextView = aboutVersionLayout.findViewById(R.id.filterlists)
        easyListTextView = aboutVersionLayout.findViewById(R.id.easylist)
        easyPrivacyTextView = aboutVersionLayout.findViewById(R.id.easyprivacy)
        fanboyAnnoyanceTextView = aboutVersionLayout.findViewById(R.id.fanboy_annoyance)
        fanboySocialTextView = aboutVersionLayout.findViewById(R.id.fanboy_social)
        ultraListTextView = aboutVersionLayout.findViewById(R.id.ultralist)
        ultraPrivacyTextView = aboutVersionLayout.findViewById(R.id.ultraprivacy)
        packageSignatureTextView = aboutVersionLayout.findViewById(R.id.package_signature)
        certificateIssuerDnTextView = aboutVersionLayout.findViewById(R.id.certificate_issuer_dn)
        certificateSubjectDnTextView = aboutVersionLayout.findViewById(R.id.certificate_subject_dn)
        certificateStartDateTextView = aboutVersionLayout.findViewById(R.id.certificate_start_date)
        certificateEndDateTextView = aboutVersionLayout.findViewById(R.id.certificate_end_date)
        certificateVersionTextView = aboutVersionLayout.findViewById(R.id.certificate_version)
        certificateSerialNumberTextView = aboutVersionLayout.findViewById(R.id.certificate_serial_number)
        certificateSignatureAlgorithmTextView = aboutVersionLayout.findViewById(R.id.certificate_signature_algorithm)

        // Setup the labels.
        val version = getString(R.string.version_code, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        val brandLabel = getString(R.string.brand)
        val manufacturerLabel = getString(R.string.manufacturer)
        val modelLabel = getString(R.string.model)
        val deviceLabel = getString(R.string.device)
        val bootloaderLabel = getString(R.string.bootloader)
        val androidLabel = getString(R.string.android)
        val securityPatchLabel = getString(R.string.security_patch)
        val buildLabel = getString(R.string.build)
        val kernelLabel = getString(R.string.kernel)
        val webViewProviderLabel = getString(R.string.webview_provider)
        val webViewVersionLabel = getString(R.string.webview_version)
        appConsumedMemoryLabel = getString(R.string.app_consumed_memory)
        appAvailableMemoryLabel = getString(R.string.app_available_memory)
        appTotalMemoryLabel = getString(R.string.app_total_memory)
        appMaximumMemoryLabel = getString(R.string.app_maximum_memory)
        systemConsumedMemoryLabel = getString(R.string.system_consumed_memory)
        systemAvailableMemoryLabel = getString(R.string.system_available_memory)
        systemTotalMemoryLabel = getString(R.string.system_total_memory)
        val easyListLabel = getString(R.string.easylist_label)
        val easyPrivacyLabel = getString(R.string.easyprivacy_label)
        val fanboyAnnoyanceLabel = getString(R.string.fanboys_annoyance_label)
        val fanboySocialLabel = getString(R.string.fanboys_social_label)
        val ultraListLabel = getString(R.string.ultralist_label)
        val ultraPrivacyLabel = getString(R.string.ultraprivacy_label)
        val issuerDNLabel = getString(R.string.issuer_dn)
        val subjectDNLabel = getString(R.string.subject_dn)
        val startDateLabel = getString(R.string.start_date)
        val endDateLabel = getString(R.string.end_date)
        val certificateVersionLabel = getString(R.string.certificate_version)
        val serialNumberLabel = getString(R.string.serial_number)
        val signatureAlgorithmLabel = getString(R.string.signature_algorithm)

        // Get the current WebView package info.
        val webViewPackageInfo = WebView.getCurrentWebViewPackage()!!

        // Get the device's information and store it in strings.
        val brand = Build.BRAND
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val device = Build.DEVICE
        val bootloader = Build.BOOTLOADER
        val radio = Build.getRadioVersion()
        val android = getString(R.string.api, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
        val securityPatch = Build.VERSION.SECURITY_PATCH
        val build = Build.DISPLAY
        val kernel = System.getProperty("os.version")
        val webViewPackageName = webViewPackageInfo.packageName
        val webViewVersion = webViewPackageInfo.versionName

        // Get the Orbot version name if Orbot is installed.
        val orbot: String = try {
            // If the safe call (`?.`) is null, the Elvis operator (`?"`) returns the following value instead, which is an empty string.
            requireContext().packageManager.getPackageInfo("org.torproject.android", 0).versionName ?: ""
        } catch (exception: PackageManager.NameNotFoundException) {
            // Store an empty string.
            ""
        }

        // Get the I2P version name if I2P is installed.
        val i2p: String = try {
            // Check to see if the F-Droid flavor is installed.
            getString(R.string.fdroid_flavor, requireContext().packageManager.getPackageInfo("net.i2p.android.router", 0).versionName)
        } catch (exception: PackageManager.NameNotFoundException) {  // The F-Droid flavor is not installed.
            try {
                // Check to see if the F-Droid flavor is installed.
                getString(R.string.google_play_flavor, requireContext().packageManager.getPackageInfo("net.i2p.android", 0).versionName)
            } catch (exception: PackageManager.NameNotFoundException) {  // The Google Play flavor is not installed either.
                // Store an empty string.
                ""
            }
        }

        // Get the OpenKeychain version name if it is installed.
        val openKeychain: String = try {
            // If the safe call (`?.`) is null, the Elvis operator (`?"`) returns the following value instead, which is an empty string.
            requireContext().packageManager.getPackageInfo("org.sufficientlysecure.keychain", 0).versionName ?: ""
        } catch (exception: PackageManager.NameNotFoundException) {
            // Store an empty string.
            ""
        }

        // Create a spannable string builder for the hardware and software text views that need multiple colors of text.
        val brandStringBuilder = SpannableStringBuilder(brandLabel + brand)
        val manufacturerStringBuilder = SpannableStringBuilder(manufacturerLabel + manufacturer)
        val modelStringBuilder = SpannableStringBuilder(modelLabel + model)
        val deviceStringBuilder = SpannableStringBuilder(deviceLabel + device)
        val bootloaderStringBuilder = SpannableStringBuilder(bootloaderLabel + bootloader)
        val androidStringBuilder = SpannableStringBuilder(androidLabel + android)
        val securityPatchStringBuilder = SpannableStringBuilder(securityPatchLabel + securityPatch)
        val buildStringBuilder = SpannableStringBuilder(buildLabel + build)
        val kernelStringBuilder = SpannableStringBuilder(kernelLabel + kernel)
        val webViewProviderStringBuilder = SpannableStringBuilder(webViewProviderLabel + webViewPackageName)
        val webViewVersionStringBuilder = SpannableStringBuilder(webViewVersionLabel + webViewVersion)
        val easyListStringBuilder = SpannableStringBuilder(easyListLabel + filterListsVersions[0])
        val easyPrivacyStringBuilder = SpannableStringBuilder(easyPrivacyLabel + filterListsVersions[1])
        val fanboyAnnoyanceStringBuilder = SpannableStringBuilder(fanboyAnnoyanceLabel + filterListsVersions[2])
        val fanboySocialStringBuilder = SpannableStringBuilder(fanboySocialLabel + filterListsVersions[3])
        val ultraListStringBuilder = SpannableStringBuilder(ultraListLabel + filterListsVersions[4])
        val ultraPrivacyStringBuilder = SpannableStringBuilder(ultraPrivacyLabel + filterListsVersions[5])

        // Set the blue color span according to the theme.  The deprecated `getColor()` must be used until the minimum API >= 23.
        blueColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.alt_blue_text))

        // Set the spans to display the device information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
        brandStringBuilder.setSpan(blueColorSpan, brandLabel.length, brandStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        manufacturerStringBuilder.setSpan(blueColorSpan, manufacturerLabel.length, manufacturerStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        modelStringBuilder.setSpan(blueColorSpan, modelLabel.length, modelStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        deviceStringBuilder.setSpan(blueColorSpan, deviceLabel.length, deviceStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        bootloaderStringBuilder.setSpan(blueColorSpan, bootloaderLabel.length, bootloaderStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        androidStringBuilder.setSpan(blueColorSpan, androidLabel.length, androidStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        securityPatchStringBuilder.setSpan(blueColorSpan, securityPatchLabel.length, securityPatchStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        buildStringBuilder.setSpan(blueColorSpan, buildLabel.length, buildStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        kernelStringBuilder.setSpan(blueColorSpan, kernelLabel.length, kernelStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        webViewProviderStringBuilder.setSpan(blueColorSpan, webViewProviderLabel.length, webViewProviderStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        webViewVersionStringBuilder.setSpan(blueColorSpan, webViewVersionLabel.length, webViewVersionStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        easyListStringBuilder.setSpan(blueColorSpan, easyListLabel.length, easyListStringBuilder.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        easyPrivacyStringBuilder.setSpan(blueColorSpan, easyPrivacyLabel.length, easyPrivacyStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        fanboyAnnoyanceStringBuilder.setSpan(blueColorSpan, fanboyAnnoyanceLabel.length, fanboyAnnoyanceStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        fanboySocialStringBuilder.setSpan(blueColorSpan, fanboySocialLabel.length, fanboySocialStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        ultraListStringBuilder.setSpan(blueColorSpan, ultraListLabel.length, ultraListStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        ultraPrivacyStringBuilder.setSpan(blueColorSpan, ultraPrivacyLabel.length, ultraPrivacyStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Display the strings in the text boxes.
        versionTextView.text = version
        brandTextView.text = brandStringBuilder
        manufacturerTextView.text = manufacturerStringBuilder
        modelTextView.text = modelStringBuilder
        deviceTextView.text = deviceStringBuilder
        bootloaderTextView.text = bootloaderStringBuilder
        androidTextView.text = androidStringBuilder
        securityPatchTextView.text = securityPatchStringBuilder
        buildTextView.text = buildStringBuilder
        kernelTextView.text = kernelStringBuilder
        webViewProviderTextView.text = webViewProviderStringBuilder
        webViewVersionTextView.text = webViewVersionStringBuilder
        easyListTextView.text = easyListStringBuilder
        easyPrivacyTextView.text = easyPrivacyStringBuilder
        fanboyAnnoyanceTextView.text = fanboyAnnoyanceStringBuilder
        fanboySocialTextView.text = fanboySocialStringBuilder
        ultraListTextView.text = ultraListStringBuilder
        ultraPrivacyTextView.text = ultraPrivacyStringBuilder

        // Only populate the radio text view if there is a radio in the device.
        // Null must be checked because some Samsung tablets report a null value for the radio instead of an empty string.  Grrrr.  <https://redmine.stoutner.com/issues/701>
        if (radio != null && radio.isNotEmpty()) {
            // Setup the label.
            val radioLabel = getString(R.string.radio)

            // Create a spannable string builder.
            val radioStringBuilder = SpannableStringBuilder(radioLabel + radio)

            // Set the span to display the radio in blue.
            radioStringBuilder.setSpan(blueColorSpan, radioLabel.length, radioStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Display the string in the text view.
            radioTextView.text = radioStringBuilder
        } else {  // This device does not have a radio.
            // Hide the radio text view.
            radioTextView.visibility = View.GONE
        }

        // Only populate the Orbot text view if it is installed.
        if (orbot.isNotEmpty()) {
            // Setup the label.
            val orbotLabel = getString(R.string.orbot)

            // Create a spannable string builder.
            val orbotStringBuilder = SpannableStringBuilder(orbotLabel + orbot)

            // Set the span to display the Orbot version.
            orbotStringBuilder.setSpan(blueColorSpan, orbotLabel.length, orbotStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Display the string in the text view.
            orbotTextView.text = orbotStringBuilder
        } else {  // Orbot is not installed.
            // Hide the Orbot text view.
            orbotTextView.visibility = View.GONE
        }

        // Only populate the I2P text view if it is installed.
        if (i2p.isNotEmpty()) {
            // Setup the label.
            val i2pLabel = getString(R.string.i2p)

            // Create a spannable string builder.
            val i2pStringBuilder = SpannableStringBuilder(i2pLabel + i2p)

            // Set the span to display the I2P version.
            i2pStringBuilder.setSpan(blueColorSpan, i2pLabel.length, i2pStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Display the string in the text view.
            i2pTextView.text = i2pStringBuilder
        } else {  // I2P is not installed.
            // Hide the I2P text view.
            i2pTextView.visibility = View.GONE
        }

        // Only populate the OpenKeychain text view if it is installed.
        if (openKeychain.isNotEmpty()) {
            // Setup the label.
            val openKeychainLabel = getString(R.string.openkeychain)

            // Create a spannable string builder.
            val openKeychainStringBuilder = SpannableStringBuilder(openKeychainLabel + openKeychain)

            // Set the span to display the OpenKeychain version.
            openKeychainStringBuilder.setSpan(blueColorSpan, openKeychainLabel.length, openKeychainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Display the string in the text view.
            openKeychainTextView.text = openKeychainStringBuilder
        } else {  //OpenKeychain is not installed.
            // Hide the OpenKeychain text view.
            openKeychainTextView.visibility = View.GONE
        }

        // Display the package signature.
        try {
            // Get the first package signature.  Suppress the lint warning about the need to be careful in implementing comparison of certificates for security purposes.
            // Once the minimum API >= 28, `GET_SIGNING_CERTIFICATES` can be used instead.  Once the minimum API >= 33, the newer `getPackageInfo()` may be used.
            @Suppress("DEPRECATION")
            @SuppressLint("PackageManagerGetSignatures") val packageSignature = requireContext().packageManager.getPackageInfo(requireContext().packageName,PackageManager.GET_SIGNATURES).signatures!![0]

            // Convert the signature to a byte array input stream.
            val certificateByteArrayInputStream: InputStream = ByteArrayInputStream(packageSignature.toByteArray())

            // Display the certificate information on the screen.
            try {
                // Instantiate a certificate factory.
                val certificateFactory = CertificateFactory.getInstance("X509")

                // Generate an X509 certificate.
                val x509Certificate = certificateFactory.generateCertificate(certificateByteArrayInputStream) as X509Certificate

                // Store the individual sections of the certificate.
                val issuerDNPrincipal = x509Certificate.issuerDN
                val subjectDNPrincipal = x509Certificate.subjectDN
                val startDate = x509Certificate.notBefore
                val endDate = x509Certificate.notAfter
                val certificateVersion = x509Certificate.version
                val serialNumberBigInteger = x509Certificate.serialNumber
                val signatureAlgorithmNameString = x509Certificate.sigAlgName

                // Create a spannable string builder for each text view that needs multiple colors of text.
                val issuerDNStringBuilder = SpannableStringBuilder(issuerDNLabel + issuerDNPrincipal.toString())
                val subjectDNStringBuilder = SpannableStringBuilder(subjectDNLabel + subjectDNPrincipal.toString())
                val startDateStringBuilder = SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(startDate))
                val endDataStringBuilder = SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(endDate))
                val certificateVersionStringBuilder = SpannableStringBuilder(certificateVersionLabel + certificateVersion)
                val serialNumberStringBuilder = SpannableStringBuilder(serialNumberLabel + serialNumberBigInteger)
                val signatureAlgorithmStringBuilder = SpannableStringBuilder(signatureAlgorithmLabel + signatureAlgorithmNameString)

                // Setup the spans to display the device information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
                issuerDNStringBuilder.setSpan(blueColorSpan, issuerDNLabel.length, issuerDNStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                subjectDNStringBuilder.setSpan(blueColorSpan, subjectDNLabel.length, subjectDNStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                startDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                endDataStringBuilder.setSpan(blueColorSpan, endDateLabel.length, endDataStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                certificateVersionStringBuilder.setSpan(blueColorSpan, certificateVersionLabel.length, certificateVersionStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                serialNumberStringBuilder.setSpan(blueColorSpan, serialNumberLabel.length, serialNumberStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                signatureAlgorithmStringBuilder.setSpan(blueColorSpan, signatureAlgorithmLabel.length, signatureAlgorithmStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Display the strings in the text boxes.
                certificateIssuerDnTextView.text = issuerDNStringBuilder
                certificateSubjectDnTextView.text = subjectDNStringBuilder
                certificateStartDateTextView.text = startDateStringBuilder
                certificateEndDateTextView.text = endDataStringBuilder
                certificateVersionTextView.text = certificateVersionStringBuilder
                certificateSerialNumberTextView.text = serialNumberStringBuilder
                certificateSignatureAlgorithmTextView.text = signatureAlgorithmStringBuilder
            } catch (certificateException: CertificateException) {
                // Do nothing if there is a certificate error.
            }

            // Get a handle for the runtime.
            runtime = Runtime.getRuntime()

            // Get a handle for the activity manager.
            activityManager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // Instantiate a memory info variable.
            memoryInfo = ActivityManager.MemoryInfo()

            // Define a number format.
            numberFormat = NumberFormat.getInstance()

            // Set the minimum and maximum number of fraction digits.
            numberFormat.minimumFractionDigits = 2
            numberFormat.maximumFractionDigits = 2

            // Update the memory usage.
            updateMemoryUsage(requireActivity())
        } catch (e: PackageManager.NameNotFoundException) {
            // Do nothing if the package manager says Privacy Browser isn't installed.
        }

        // Scroll the tab if the saved instance state is not null.
        if (savedInstanceState != null) {
            aboutVersionLayout.post {
                aboutVersionLayout.scrollY = savedInstanceState.getInt(SCROLL_Y)
            }
        }

        // Return the tab layout.
        return aboutVersionLayout
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Save the scroll position.
        savedInstanceState.putInt(SCROLL_Y, aboutVersionLayout.scrollY)
    }

    override fun onPause() {
        // Run the default commands.
        super.onPause()

        // Pause the updating of the memory usage.
        updateMemoryUsageBoolean = false
    }

    override fun onResume() {
        // Run the default commands.
        super.onResume()

        // Resume the updating of the memory usage.
        updateMemoryUsageBoolean = true
    }

    private fun updateMemoryUsage(activity: Activity) {
        try {
            // Update the memory usage if enabled.
            if (updateMemoryUsageBoolean) {
                // Populate the memory info variable.
                activityManager.getMemoryInfo(memoryInfo)

                // Get the app memory information.
                val appAvailableMemoryLong = runtime.freeMemory()
                val appTotalMemoryLong = runtime.totalMemory()
                val appMaximumMemoryLong = runtime.maxMemory()

                // Calculate the app consumed memory.
                val appConsumedMemoryLong = appTotalMemoryLong - appAvailableMemoryLong

                // Get the system memory information.
                val systemTotalMemoryLong = memoryInfo.totalMem
                val systemAvailableMemoryLong = memoryInfo.availMem

                // Calculate the system consumed memory.
                val systemConsumedMemoryLong = systemTotalMemoryLong - systemAvailableMemoryLong

                // Convert the memory information into mebibytes.
                val appConsumedMemoryFloat = appConsumedMemoryLong.toFloat() / MEBIBYTE
                val appAvailableMemoryFloat = appAvailableMemoryLong.toFloat() / MEBIBYTE
                val appTotalMemoryFloat = appTotalMemoryLong.toFloat() / MEBIBYTE
                val appMaximumMemoryFloat = appMaximumMemoryLong.toFloat() / MEBIBYTE
                val systemConsumedMemoryFloat = systemConsumedMemoryLong.toFloat() / MEBIBYTE
                val systemAvailableMemoryFloat = systemAvailableMemoryLong.toFloat() / MEBIBYTE
                val systemTotalMemoryFloat = systemTotalMemoryLong.toFloat() / MEBIBYTE

                // Get the mebibyte string.
                val mebibyte = getString(R.string.mebibyte)

                // Calculate the mebibyte length.
                val mebibyteLength = mebibyte.length

                // Create spannable string builders.
                val appConsumedMemoryStringBuilder = SpannableStringBuilder(appConsumedMemoryLabel + numberFormat.format(appConsumedMemoryFloat.toDouble()) + " " + mebibyte)
                val appAvailableMemoryStringBuilder = SpannableStringBuilder(appAvailableMemoryLabel + numberFormat.format(appAvailableMemoryFloat.toDouble()) + " " + mebibyte)
                val appTotalMemoryStringBuilder = SpannableStringBuilder(appTotalMemoryLabel + numberFormat.format(appTotalMemoryFloat.toDouble()) + " " + mebibyte)
                val appMaximumMemoryStringBuilder = SpannableStringBuilder(appMaximumMemoryLabel + numberFormat.format(appMaximumMemoryFloat.toDouble()) + " " + mebibyte)
                val systemConsumedMemoryStringBuilder = SpannableStringBuilder(systemConsumedMemoryLabel + numberFormat.format(systemConsumedMemoryFloat.toDouble()) + " " + mebibyte)
                val systemAvailableMemoryStringBuilder = SpannableStringBuilder(systemAvailableMemoryLabel + numberFormat.format(systemAvailableMemoryFloat.toDouble()) + " " + mebibyte)
                val systemTotalMemoryStringBuilder = SpannableStringBuilder(systemTotalMemoryLabel + numberFormat.format(systemTotalMemoryFloat.toDouble()) + " " + mebibyte)

                // Setup the spans to display the memory information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
                appConsumedMemoryStringBuilder.setSpan(blueColorSpan, appConsumedMemoryLabel.length, appConsumedMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                appAvailableMemoryStringBuilder.setSpan(blueColorSpan, appAvailableMemoryLabel.length, appAvailableMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                appTotalMemoryStringBuilder.setSpan(blueColorSpan, appTotalMemoryLabel.length, appTotalMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                appMaximumMemoryStringBuilder.setSpan(blueColorSpan, appMaximumMemoryLabel.length, appMaximumMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                systemConsumedMemoryStringBuilder.setSpan(blueColorSpan, systemConsumedMemoryLabel.length, systemConsumedMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                systemAvailableMemoryStringBuilder.setSpan(blueColorSpan, systemAvailableMemoryLabel.length, systemAvailableMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                systemTotalMemoryStringBuilder.setSpan(blueColorSpan, systemTotalMemoryLabel.length, systemTotalMemoryStringBuilder.length - mebibyteLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Display the string in the text boxes.
                appConsumedMemoryTextView.text = appConsumedMemoryStringBuilder
                appAvailableMemoryTextView.text = appAvailableMemoryStringBuilder
                appTotalMemoryTextView.text = appTotalMemoryStringBuilder
                appMaximumMemoryTextView.text = appMaximumMemoryStringBuilder
                systemConsumedMemoryTextView.text = systemConsumedMemoryStringBuilder
                systemAvailableMemoryTextView.text = systemAvailableMemoryStringBuilder
                systemTotalMemoryTextView.text = systemTotalMemoryStringBuilder
            }

            // Schedule another memory update if the activity has not been destroyed.
            if (!activity.isDestroyed) {
                // Create a handler to update the memory usage.
                val updateMemoryUsageHandler = Handler(Looper.getMainLooper())

                // Create a runnable to update the memory usage.
                val updateMemoryUsageRunnable = Runnable { updateMemoryUsage(activity) }

                // Update the memory usage after 1000 milliseconds
                updateMemoryUsageHandler.postDelayed(updateMemoryUsageRunnable, 1000)
            }
        } catch (exception: Exception) {
            // Do nothing.
        }
    }

    private fun getAboutVersionString(): String {
        // Initialize an about version string builder.
        val aboutVersionStringBuilder = StringBuilder()

        // Populate the about version string builder.
        aboutVersionStringBuilder.append(audeonbrowserTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(versionTextView.text)
        aboutVersionStringBuilder.append("\n\n")
        aboutVersionStringBuilder.append(hardwareTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(brandTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(manufacturerTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(modelTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(deviceTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(bootloaderTextView.text)
        aboutVersionStringBuilder.append("\n")
        if (radioTextView.visibility == View.VISIBLE) {
            aboutVersionStringBuilder.append(radioTextView.text)
            aboutVersionStringBuilder.append("\n")
        }
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(softwareTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(androidTextView.text)
        aboutVersionStringBuilder.append("\n")
        if (securityPatchTextView.visibility == View.VISIBLE) {
            aboutVersionStringBuilder.append(securityPatchTextView.text)
            aboutVersionStringBuilder.append("\n")
        }
        aboutVersionStringBuilder.append(buildTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(kernelTextView.text)
        aboutVersionStringBuilder.append("\n")
        if (webViewProviderTextView.visibility == View.VISIBLE) {
            aboutVersionStringBuilder.append(webViewProviderTextView.text)
            aboutVersionStringBuilder.append("\n")
        }
        aboutVersionStringBuilder.append(webViewVersionTextView.text)
        aboutVersionStringBuilder.append("\n")
        if (orbotTextView.visibility == View.VISIBLE) {
            aboutVersionStringBuilder.append(orbotTextView.text)
            aboutVersionStringBuilder.append("\n")
        }
        if (i2pTextView.visibility == View.VISIBLE) {
            aboutVersionStringBuilder.append(i2pTextView.text)
            aboutVersionStringBuilder.append("\n")
        }
        if (openKeychainTextView.visibility == View.VISIBLE) {
            aboutVersionStringBuilder.append(openKeychainTextView.text)
            aboutVersionStringBuilder.append("\n")
        }
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(memoryUsageTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(appConsumedMemoryTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(appAvailableMemoryTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(appTotalMemoryTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(appMaximumMemoryTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(systemConsumedMemoryTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(systemAvailableMemoryTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(systemTotalMemoryTextView.text)
        aboutVersionStringBuilder.append("\n\n")
        aboutVersionStringBuilder.append(filterListsTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(easyListTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(easyPrivacyTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(fanboyAnnoyanceTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(fanboySocialTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(ultraListTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(ultraPrivacyTextView.text)
        aboutVersionStringBuilder.append("\n\n")
        aboutVersionStringBuilder.append(packageSignatureTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateIssuerDnTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateSubjectDnTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateStartDateTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateEndDateTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateVersionTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateSerialNumberTextView.text)
        aboutVersionStringBuilder.append("\n")
        aboutVersionStringBuilder.append(certificateSignatureAlgorithmTextView.text)

        // Return the string.
        return aboutVersionStringBuilder.toString()
    }
}
