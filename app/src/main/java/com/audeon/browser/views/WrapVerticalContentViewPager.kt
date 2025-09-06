/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2017, 2019, 2021-2022 Soren Stoutner <soren@stoutner.com>
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

package com.audeon.browser.views

import android.content.Context
import android.util.AttributeSet

import androidx.viewpager.widget.ViewPager

class WrapVerticalContentViewPager : ViewPager {
    // The constructors.
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Perform an initial `super.onMeasure`, which populates the child count.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Initialize the maximum height variable.
        var maximumHeight = 0

        // Find the maximum height of each of the child views.
        for (i in 0 until childCount) {
            // Get the child view.
            val childView = getChildAt(i)

            // Measure the child view height with no constraints.
            childView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))

            // Store the child's height if it is larger than the maximum height.
            if (childView.measuredHeight > maximumHeight) {
                maximumHeight = childView.measuredHeight
            }
        }

        // Perform a final `super.onMeasure` to set the maximum height.
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maximumHeight, MeasureSpec.EXACTLY))
    }
}