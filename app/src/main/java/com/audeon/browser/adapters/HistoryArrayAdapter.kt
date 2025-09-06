/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2016-2019, 2021-2022 Soren Stoutner <soren@stoutner.com>
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
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import com.audeon.browser.R
import com.audeon.browser.dataclasses.HistoryDataClass

import java.util.ArrayList

class HistoryArrayAdapter(context: Context, historyDataClassArrayList: ArrayList<HistoryDataClass>, private val currentPageId: Int) : ArrayAdapter<HistoryDataClass>(context, 0, historyDataClassArrayList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Initialize a populated view from the convert view.
        var populatedView = convertView

        // Inflate the view if it is null.
        if (populatedView == null) {
            populatedView = LayoutInflater.from(context).inflate(R.layout.url_history_item_linearlayout, parent, false)
        }

        // Get handles for the views.
        val favoriteIconImageView = populatedView!!.findViewById<ImageView>(R.id.history_favorite_icon_imageview)
        val urlTextView = populatedView.findViewById<TextView>(R.id.history_url_textview)

        // Get the URL history for this position.
        val history = getItem(position)!!

        // Populate the views.
        favoriteIconImageView.setImageBitmap(history.favoriteIcon)
        urlTextView.text = history.url

        // Set the URL text for the current page to be bold.
        if (position == currentPageId) {
            urlTextView.typeface = Typeface.DEFAULT_BOLD
        } else {  // Set the default typeface for all the other entries.
            urlTextView.typeface = Typeface.DEFAULT
        }

        // Return the populated view.
        return populatedView
    }
}
