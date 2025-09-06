
/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2019,2021-2023 Soren Stoutner <soren@stoutner.com>
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

import java.util.ArrayList
import java.util.regex.Pattern

// Define the request disposition options.
const val REQUEST_DEFAULT = "0"
const val REQUEST_ALLOWED = "1"
const val REQUEST_THIRD_PARTY = "2"
const val REQUEST_BLOCKED = "3"

class CheckFilterListHelper {
    fun checkFilterList(currentDomain: String?, resourceUrl: String, isThirdPartyRequest: Boolean, filterList: ArrayList<List<Array<String>>>): Array<String> {
        // Get the filter list name.
        val filterListName = filterList[0][1][0]

        // Process the allow lists.
        // Main allow list.
        for (allowListEntry in filterList[MAIN_ALLOWLIST.toInt()]) {
            when (allowListEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.contains(allowListEntry[0])) {
                    // Allow the request.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, MAIN_ALLOWLIST, allowListEntry[0], allowListEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1])) {
                    // Allow the request.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, MAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}", allowListEntry[2])
                }

                // There are three entries.
                4 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2])) {
                    // Allow the request.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, MAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                }
            }
        }

        // Final allow list.
        for (allowListEntry in filterList[FINAL_ALLOWLIST.toInt()]) {
            when (allowListEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.contains(allowListEntry[0])) {
                    // Allow the request.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, FINAL_ALLOWLIST, allowListEntry[0], allowListEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1])) {
                    // Allow the request.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, FINAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}", allowListEntry[2])
                }
            }
        }

        // Only check the domain lists if the current domain is not null (like `about:blank`).
        if (currentDomain != null) {
            // Domain allow list.
            for (allowListEntry in filterList[DOMAIN_ALLOWLIST.toInt()]) {
                when (allowListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}", allowListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2]) && resourceUrl.contains(allowListEntry[3])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}\n${allowListEntry[3]}",
                            allowListEntry[4])
                    }

                    // There are four entries.
                    6 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2]) && resourceUrl.contains(allowListEntry[3]) &&
                        resourceUrl.contains(allowListEntry[4])) {

                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_ALLOWLIST,
                            "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}\n${allowListEntry[3]}\n${allowListEntry[4]}", allowListEntry[5])
                    }
                }
            }

            // Domain initial allow list.
            for (allowListEntry in filterList[DOMAIN_INITIAL_ALLOWLIST.toInt()]) {
                when (allowListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.startsWith(allowListEntry[1])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_INITIAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}", allowListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.startsWith(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_INITIAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.startsWith(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2]) && resourceUrl.startsWith(allowListEntry[3])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_INITIAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}\n${allowListEntry[3]}",
                            allowListEntry[4])
                    }
                }
            }

            // Domain final allow list.
            for (allowListEntry in filterList[DOMAIN_FINAL_ALLOWLIST.toInt()]) {
                when (allowListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.endsWith(allowListEntry[1])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_FINAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}", allowListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.endsWith(allowListEntry[2])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, DOMAIN_FINAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                    }
                }
            }
        }

        // Only check the third-party allow lists if this is a third-party request.
        if (isThirdPartyRequest) {
            // Third-party allow list.
            for (allowListEntry in filterList[THIRD_PARTY_ALLOWLIST.toInt()]) {
                when (allowListEntry.size) {
                    // There is one entry.
                    2 -> if (resourceUrl.contains(allowListEntry[0])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_ALLOWLIST, allowListEntry[0], allowListEntry[1])
                    }

                    // There are two entries.
                    3 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}", allowListEntry[2])
                    }

                    // There are three entries.
                    4 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                    }

                    // There are four entries.
                    5 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2]) && resourceUrl.contains(allowListEntry[3])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}\n${allowListEntry[3]}",
                            allowListEntry[4])
                    }

                    // There are five entries.
                    6 -> if (resourceUrl.contains(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2]) && resourceUrl.contains(allowListEntry[3]) &&
                        resourceUrl.contains(allowListEntry[4])) {

                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_ALLOWLIST,
                            "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}\n${allowListEntry[3]}\n${allowListEntry[4]}", allowListEntry[5])
                    }
                }
            }

            // Third-party domain allow list.
            for (allowListEntry in filterList[THIRD_PARTY_DOMAIN_ALLOWLIST.toInt()]) {
                when (allowListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n", allowListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(allowListEntry[0]) && resourceUrl.contains(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                    }
                }
            }

            // Third-party domain initial allow list.
            for (allowListEntry in filterList[THIRD_PARTY_DOMAIN_INITIAL_ALLOWLIST.toInt()]) {
                when (allowListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(allowListEntry[0]) && resourceUrl.startsWith(allowListEntry[1])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_INITIAL_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n", allowListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(allowListEntry[0]) && resourceUrl.startsWith(allowListEntry[1]) && resourceUrl.contains(allowListEntry[2])) {
                        // Allow the request.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_ALLOWLIST, "${allowListEntry[0]}\n${allowListEntry[1]}\n${allowListEntry[2]}", allowListEntry[3])
                    }
                }
            }
        }

        // Process the block lists.
        // Main block list.
        for (blockListEntry in filterList[MAIN_BLOCKLIST.toInt()]) {
            when (blockListEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.contains(blockListEntry[0])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, MAIN_BLOCKLIST, blockListEntry[0], blockListEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, MAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                }

                // There are three entries.
                4 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, MAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                }

                // There are four entries.
                5 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2]) && resourceUrl.contains(blockListEntry[3])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, MAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}\n${blockListEntry[3]}", blockListEntry[4])
                }

                // There are five entries.
                6 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2]) && resourceUrl.contains(blockListEntry[3]) &&
                    resourceUrl.contains(blockListEntry[4])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, MAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}\n${blockListEntry[3]}\n${blockListEntry[4]}",
                        blockListEntry[5])
                }
            }
        }

        // Initial block list.
        for (blockListEntry in filterList[INITIAL_BLOCKLIST.toInt()]) {
            when (blockListEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.startsWith(blockListEntry[0])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, INITIAL_BLOCKLIST, blockListEntry[0], blockListEntry[1])
                }

                // There are two entries
                3 -> if (resourceUrl.startsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, INITIAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                }
            }
        }

        // Final block list.
        for (blockListEntry in filterList[FINAL_BLOCKLIST.toInt()]) {
            when (blockListEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.endsWith(blockListEntry[0])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, FINAL_BLOCKLIST, blockListEntry[0], blockListEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.endsWith(blockListEntry[1])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, FINAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                }

                // There are three entries.
                4 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.endsWith(blockListEntry[2])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, FINAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                }
            }
        }

        // Only check the domain lists if the current domain is not null (like `about:blank`).
        if (currentDomain != null) {
            // Domain block list.
            for (blockListEntry in filterList[DOMAIN_BLOCKLIST.toInt()]) {
                when (blockListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain.endsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2]) && resourceUrl.contains(blockListEntry[3])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}\n${blockListEntry[3]}",
                            blockListEntry[4])
                    }
                }
            }

            // Domain initial block list.
            for (blockListEntry in filterList[DOMAIN_INITIAL_BLOCKLIST.toInt()]) {
                // Store the entry in the resource request log.
                if (currentDomain.endsWith(blockListEntry[0]) && resourceUrl.startsWith(blockListEntry[1])) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_INITIAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                }
            }

            // Domain final block list.
            for (blockListEntry in filterList[DOMAIN_FINAL_BLOCKLIST.toInt()]) {
                when (blockListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(blockListEntry[0]) && resourceUrl.endsWith(blockListEntry[1])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_FINAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.endsWith(blockListEntry[2])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_FINAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                    }
                }
            }

            // Domain regular expression block list.
            for (blockListEntry in filterList[DOMAIN_REGULAR_EXPRESSION_BLOCKLIST.toInt()]) {
                if (currentDomain.endsWith(blockListEntry[0]) && Pattern.matches(blockListEntry[1], resourceUrl)) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, DOMAIN_REGULAR_EXPRESSION_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                }
            }
        }

        // Only check the third-party block lists if this is a third-party request.
        if (isThirdPartyRequest) {
            // Third-party block list.
            for (blockListEntry in filterList[THIRD_PARTY_BLOCKLIST.toInt()]) {
                when (blockListEntry.size) {
                    // There is one entry.
                    2 -> if (resourceUrl.contains(blockListEntry[0])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_BLOCKLIST, blockListEntry[0], blockListEntry[1])
                    }

                    // There are two entries.
                    3 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n", blockListEntry[2])
                    }

                    // There are three entries.
                    4 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                    }

                    // There are four entries.
                    5 -> if (resourceUrl.contains(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2]) && resourceUrl.contains(blockListEntry[3])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}\n${blockListEntry[3]}",
                            blockListEntry[4])
                    }
                }
            }

            // Third-party initial block list.
            for (blockListEntry in filterList[THIRD_PARTY_INITIAL_BLOCKLIST.toInt()]) {
                when (blockListEntry.size) {
                    // There is one entry.
                    2 -> if (resourceUrl.startsWith(blockListEntry[0])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_INITIAL_BLOCKLIST, blockListEntry[0], blockListEntry[1])
                    }

                    // There are two entries.
                    3 -> if (resourceUrl.startsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_INITIAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                    }
                }
            }

            // Third-party domain block list.
            for (blockListEntry in filterList[THIRD_PARTY_DOMAIN_BLOCKLIST.toInt()]) {
                when (blockListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(blockListEntry[0]) && resourceUrl.contains(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                    }
                }
            }

            // Third-party domain initial block list.
            for (blockListEntry in filterList[THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST.toInt()]) {
                when (blockListEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(blockListEntry[0]) && resourceUrl.startsWith(blockListEntry[1])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n", blockListEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(blockListEntry[0]) && resourceUrl.startsWith(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}", blockListEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain!!.endsWith(blockListEntry[0]) && resourceUrl.startsWith(blockListEntry[1]) && resourceUrl.contains(blockListEntry[2]) && resourceUrl.contains(blockListEntry[3])) {
                        // Block the request.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST,
                            "${blockListEntry[0]}\n${blockListEntry[1]}\n${blockListEntry[2]}\n${blockListEntry[3]}", blockListEntry[4])
                    }
                }
            }

            // Third-party regular expression block list.
            for (blockListEntry in filterList[THIRD_PARTY_REGULAR_EXPRESSION_BLOCKLIST.toInt()]) {
                if (Pattern.matches(blockListEntry[0], resourceUrl)) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_REGULAR_EXPRESSION_BLOCKLIST, blockListEntry[0], blockListEntry[1])
                }
            }

            // Third-party domain regular expression block list.
            for (blockListEntry in filterList[THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLOCKLIST.toInt()]) {
                if (currentDomain!!.endsWith(blockListEntry[0]) && Pattern.matches(blockListEntry[1], resourceUrl)) {
                    // Block the request.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLOCKLIST, "${blockListEntry[0]}\n${blockListEntry[1]}", blockListEntry[2])
                }
            }
        }

        // Regular expression block list.
        for (blockListEntry in filterList[REGULAR_EXPRESSION_BLOCKLIST.toInt()]) {
            if (Pattern.matches(blockListEntry[0], resourceUrl)) {
                // Block the request.
                return arrayOf(REQUEST_BLOCKED, resourceUrl, filterListName, REGULAR_EXPRESSION_BLOCKLIST, blockListEntry[0], blockListEntry[1])
            }
        }

        // Return a default result.
        return arrayOf(REQUEST_DEFAULT)
    }
}
