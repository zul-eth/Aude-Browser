/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2019, 2021-2022 Soren Stoutner <soren@stoutner.com>
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
 *
 *
 * * This file is a modified version of <https://android.googlesource.com/platform/packages/apps/Camera/+/master/src/com/android/camera/ui/CheckedLinearLayout.java>.
 *
 * The original licensing information is below.
 *
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.audeon.browser.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.LinearLayout

// Define the class constants.

class CheckedLinearLayout : LinearLayout, Checkable {
    // Define the class variables.
    private var isCurrentlyChecked = false
    private val checkedStateSet = intArrayOf(android.R.attr.state_checked)

    // The constructors.
    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet?, defaultStyleAttribute: Int) : super(context, attributeSet, defaultStyleAttribute)

    // This constructor can only be added once the minimum API >= 21.
    // constructor(context: Context, attributeSet: AttributeSet?, defaultStyleAttribute: Int, defaultStyleResource: Int) : super(context, attributeSet, defaultStyleAttribute, defaultStyleResource)

    override fun isChecked(): Boolean {
        // Return the checked status.
        return isCurrentlyChecked
    }

    override fun setChecked(checked: Boolean) {
        // Only process the command if a change is requested.
        if (isCurrentlyChecked != checked) {
            // Update the status tracker.
            isCurrentlyChecked = checked

            // Refresh the drawable state.
            refreshDrawableState()

            // Propagate the checked status to the child views.
            for (i in 0 until childCount) {
                // Get a handle for the child view.
                val childView = getChildAt(i)

                // Propagate the checked status if the child view is checkable.
                if (childView is Checkable) {
                    // Cast the child view to `Checkable`.
                    val checkableChildView = childView as Checkable

                    // Set the checked status.
                    checkableChildView.isChecked = checked
                }
            }
        }
    }

    override fun toggle() {
        // Toggle the state.
        isChecked = !isCurrentlyChecked
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        // Run the default commands.
        val drawableState = super.onCreateDrawableState(extraSpace + 1)

        if (isCurrentlyChecked) {
            // Merge the drawable states.
            mergeDrawableStates(drawableState, checkedStateSet)
        }

        // Return the drawable state.
        return drawableState
    }
}