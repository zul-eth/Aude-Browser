/*
 * Copyright 2020-2023 Soren Stoutner <soren@stoutner.com>.
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

import android.content.Context

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

import com.audeon.browser.R
import com.audeon.browser.activities.MainWebViewActivity
import com.audeon.browser.dataclasses.PendingDialogDataClass
import com.audeon.browser.dialogs.SaveDialog
import com.audeon.browser.helpers.UrlHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.lang.Exception

object PrepareSaveDialogCoroutine {
    fun prepareSaveDialog(context: Context, supportFragmentManager: FragmentManager, urlString: String, userAgent: String, cookiesEnabled: Boolean) {
        // Use a coroutine to prepare the save dialog.
        CoroutineScope(Dispatchers.Main).launch {
            // Make the network requests on the IO thread.
            withContext(Dispatchers.IO) {
                // Get the file name and size.
                val fileNameAndSize = UrlHelper.getNameAndSize(context, urlString, userAgent, cookiesEnabled)

                // Display the dialog on the main thread.
                withContext(Dispatchers.Main) {
                    // Instantiate the save dialog.
                    val saveDialogFragment: DialogFragment = SaveDialog.saveUrl(urlString, fileNameAndSize.first, fileNameAndSize.second, userAgent, cookiesEnabled)

                    // Try to show the dialog.  Sometimes the window is not active.
                    try {
                        // Show the save dialog.
                        saveDialogFragment.show(supportFragmentManager, context.getString(R.string.save_dialog))
                    } catch (exception: Exception) {
                        // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                        MainWebViewActivity.pendingDialogsArrayList.add(PendingDialogDataClass(saveDialogFragment, context.getString(R.string.save_dialog)))
                    }
                }
            }
        }
    }
}
