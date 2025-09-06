/*
 * Copyright 2018-2019, 2021-2024 Soren Stoutner <soren@stoutner.com>.
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

import android.content.res.AssetManager

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import java.util.ArrayList

// Define the schema of the string array in each entry of the resource requests array list.
const val REQUEST_DISPOSITION = 0
const val REQUEST_URL = 1
const val REQUEST_BLOCKLIST = 2
const val REQUEST_SUBLIST = 3
const val REQUEST_BLOCKLIST_ENTRIES = 4
const val REQUEST_BLOCKLIST_ORIGINAL_ENTRY = 5

// Define the allow lists.
const val MAIN_ALLOWLIST = "1"
const val FINAL_ALLOWLIST = "2"
const val DOMAIN_ALLOWLIST = "3"
const val DOMAIN_INITIAL_ALLOWLIST = "4"
const val DOMAIN_FINAL_ALLOWLIST = "5"
const val THIRD_PARTY_ALLOWLIST = "6"
const val THIRD_PARTY_DOMAIN_ALLOWLIST = "7"
const val THIRD_PARTY_DOMAIN_INITIAL_ALLOWLIST = "8"

// Define the block lists.
const val MAIN_BLOCKLIST = "9"
const val INITIAL_BLOCKLIST = "10"
const val FINAL_BLOCKLIST = "11"
const val DOMAIN_BLOCKLIST = "12"
const val DOMAIN_INITIAL_BLOCKLIST = "13"
const val DOMAIN_FINAL_BLOCKLIST = "14"
const val DOMAIN_REGULAR_EXPRESSION_BLOCKLIST = "15"
const val THIRD_PARTY_BLOCKLIST = "16"
const val THIRD_PARTY_INITIAL_BLOCKLIST = "17"
const val THIRD_PARTY_DOMAIN_BLOCKLIST = "18"
const val THIRD_PARTY_DOMAIN_INITIAL_BLOCKLIST = "19"
const val THIRD_PARTY_REGULAR_EXPRESSION_BLOCKLIST = "20"
const val THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLOCKLIST = "21"
const val REGULAR_EXPRESSION_BLOCKLIST = "22"

class ParseFilterListHelper {
    fun parseFilterList(assetManager: AssetManager, filterListName: String): ArrayList<List<Array<String>>> {
        // Initialize the header list.
        val headers: MutableList<Array<String>> = ArrayList()  // 0.

        // Initialize the allow lists.
        val mainAllowList: MutableList<Array<String>> = ArrayList()  // 1.
        val finalAllowList: MutableList<Array<String>> = ArrayList()  // 2.
        val domainAllowList: MutableList<Array<String>> = ArrayList()  // 3.
        val domainInitialAllowList: MutableList<Array<String>> = ArrayList()  // 4.
        val domainFinalAllowList: MutableList<Array<String>> = ArrayList()  // 5.
        val thirdPartyAllowList: MutableList<Array<String>> = ArrayList()  // 6.
        val thirdPartyDomainAllowList: MutableList<Array<String>> = ArrayList()  // 7.
        val thirdPartyDomainInitialAllowList: MutableList<Array<String>> = ArrayList()  // 8.

        // Initialize the block lists.
        val mainBlockList: MutableList<Array<String>> = ArrayList()  // 9.
        val initialBlockList: MutableList<Array<String>> = ArrayList()  // 10.
        val finalBlockList: MutableList<Array<String>> = ArrayList()  // 11.
        val domainBlockList: MutableList<Array<String>> = ArrayList()  // 12.
        val domainInitialBlockList: MutableList<Array<String>> = ArrayList()  // 13.
        val domainFinalBlockList: MutableList<Array<String>> = ArrayList()  // 14.
        val domainRegularExpressionBlockList: MutableList<Array<String>> = ArrayList()  // 15.
        val thirdPartyBlockList: MutableList<Array<String>> = ArrayList()  // 16.
        val thirdPartyInitialBlockList: MutableList<Array<String>> = ArrayList()  // 17.
        val thirdPartyDomainBlockList: MutableList<Array<String>> = ArrayList()  // 18.
        val thirdPartyDomainInitialBlockList: MutableList<Array<String>> = ArrayList()  // 19.
        val regularExpressionBlockList: MutableList<Array<String>> = ArrayList()  // 20.
        val thirdPartyRegularExpressionBlockList: MutableList<Array<String>> = ArrayList()  // 21.
        val thirdPartyDomainRegularExpressionBlockList: MutableList<Array<String>> = ArrayList()  // 22.

        // Parse the filter list.  The `try` is required by input stream reader.
        try {
            // Load the filter list into a buffered reader.
            val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(filterListName)))

            // Create strings for storing the filter list entries.
            var filterListEntry: String
            var originalFilterListEntry: String

            // Parse the filter list.
            bufferedReader.forEachLine {
                // Store the original filter list entry.
                originalFilterListEntry = it

                // Remove any `^` from the filter list entry.  Privacy Browser does not process them in the interest of efficiency.
                filterListEntry = it.replace("^", "")

                // Parse the entry.
                if (filterListEntry.contains("##") || filterListEntry.contains("#?#") || filterListEntry.contains("#@#") || filterListEntry.startsWith("[")) {
                    // Entries that contain `##`, `#?#`, and `#@#` are for hiding elements in the main page's HTML.  Entries that start with `[` describe the AdBlock compatibility level.
                    // Do nothing.  Privacy Browser does not currently use these entries.

                    //Log.i("FilterLists", "Not added:  " + filterListEntry)
                } else if (filterListEntry.contains("\$csp=script-src")) {  // Ignore entries that contain `$csp=script-src`.
                    // Do nothing.  It is uncertain what this directive is even supposed to mean, and it is blocking entire websites like androidcentral.com.  https://redmine.stoutner.com/issues/306.

                    //Log.i("FilterLists", "Not added:  " + originalFilterListEntry)
                } else if (filterListEntry.contains("\$websocket") || filterListEntry.contains("\$third-party,websocket") || filterListEntry.contains("\$script,websocket")) {
                    // Ignore entries with `websocket`.
                    // Do nothing.  Privacy Browser does not differentiate between websocket requests and other requests and these entries cause a lot of false positives.

                    //Log.i("FilterLists", "Not added:  " + originalFilterlistEntry)
                } else if (filterListEntry.startsWith("!")) {   // Comment entries.
                    if (filterListEntry.startsWith("! Version:")) {
                        // Get the list version number.
                        val listVersion = arrayOf(filterListEntry.substring(11))

                        // Store the list version in the headers list.
                        headers.add(listVersion)
                    }

                    if (filterListEntry.startsWith("! Title:")) {
                        // Get the list title.
                        val listTitle = arrayOf(filterListEntry.substring(9))

                        // Store the list title in the headers list.
                        headers.add(listTitle)
                    }

                    //Log.i("FilterLists", "Not added:  " + filterListEntry);
                } else if (filterListEntry.startsWith("@@")) {  // Entries that begin with `@@` are allowed entries.
                    // Remove the `@@`
                    filterListEntry = filterListEntry.substring(2)

                    // Strip out any initial `||`.  Privacy Browser doesn't differentiate items that only match against the end of the domain name.
                    if (filterListEntry.startsWith("||"))
                        filterListEntry = filterListEntry.substring(2)

                    // Check if the entry contains an Adblock filter option (indicated by `$`).
                    if (filterListEntry.contains("$")) {  // The entry contains a filter option.
                        if (filterListEntry.contains("~third-party")) {  // Ignore entries that contain `~third-party`.
                            // Do nothing.

                            //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                        } else if (filterListEntry.contains("third-party")) {  // Third-party allowed entries.
                            // Check if the entry only applies to certain domains.
                            if (filterListEntry.contains("domain=")) {  // Third-party domain allowed entries.
                                // Parse the entry.
                                var entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))
                                val filters = filterListEntry.substring(filterListEntry.indexOf("$") + 1)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                if (domains.contains("~")) {  // It is uncertain what a `~` domain means inside an `@@` entry.
                                    // Do Nothing

                                    //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                                } else if (filterListEntry.startsWith("|")) {  // Third-party domain initial allowed entries.
                                    // Strip out the initial `|`.
                                    entry = entry.substring(1)

                                    if (entry == "http://" || entry == "https://") {  // Ignore generic entries.
                                        // Do nothing.  These entries are designed for filter options that Privacy Browser does not use.

                                        //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry);
                                    } else {  // Process third-party domain initial allowed entries.
                                        // Process each domain.
                                        do {
                                            // Create a string to keep track of the current domain.
                                            var domain: String

                                            // Populate the current domain.
                                            if (domains.contains("|")) {  // There is more than one domain in the list.
                                                // Get the first domain from the list.
                                                domain = domains.substring(0, domains.indexOf("|"))

                                                // Remove the first domain from the list.
                                                domains = domains.substring(domains.indexOf("|") + 1)
                                            } else {  // There is only one domain in the list.
                                                domain = domains
                                            }

                                            // Process the domain entry.
                                            if (entry.contains("*")) {  // Process a third-party domain initial allowed double entry.
                                                // Get the index of the wildcard.
                                                val wildcardIndex = entry.indexOf("*")

                                                // Split the entry into components.
                                                val firstEntry = entry.substring(0, wildcardIndex)
                                                val secondEntry = entry.substring(wildcardIndex + 1)

                                                // Create an entry string array.
                                                val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                                // Add the entry to the allow list.
                                                thirdPartyDomainInitialAllowList.add(domainDoubleEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " third-party domain initial allow list added:  " + domain + " , " + firstEntry + " , " + secondEntry +
                                                //        "  -  " + originalFilterListEntry)
                                            } else {  // Process a third-party domain initial allowed single entry.
                                                // Create a domain entry string array.
                                                val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                                // Add the entry to the third party domain initial allow list.
                                                thirdPartyDomainInitialAllowList.add(domainEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " third-party domain initial allow list added: " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                            }
                                        // Repeat until all the domains have been processed.
                                        } while (domains.contains("|"))
                                    }
                                } else {  // Third-party domain entries.
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        // Populate the current domain.
                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        // Remove any trailing `*` from the entry.
                                        if (entry.endsWith("*")) {
                                            entry = entry.substring(0, entry.length - 1)
                                        }

                                        // Process the domain entry.
                                        if (entry.contains("*")) {  // Process a third-party domain double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            thirdPartyDomainAllowList.add(domainDoubleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain allow list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalFilterListEntry)
                                        } else {  // Process a third-party domain single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            thirdPartyDomainAllowList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain allow list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                        }
                                    // Repeat until all the domains have been processed.
                                    } while (domains.contains("|"))
                                }
                            } else {  // Process third-party allow list entries.
                                // Parse the entry
                                val entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                                // Process the entry.
                                if (entry.contains("*")) {  // There are two or more entries.
                                    // Get the index of the wildcard.
                                    val wildcardIndex = entry.indexOf("*")

                                    // Split the entry into components.
                                    val firstEntry = entry.substring(0, wildcardIndex)
                                    val secondEntry = entry.substring(wildcardIndex + 1)

                                    // Process the second entry.
                                    if (secondEntry.contains("*")) {  // There are three or more entries.
                                        // Get the index of the wildcard.
                                        val secondWildcardIndex = secondEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                        val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                        // Process the third entry.
                                        if (thirdEntry.contains("*")) {  // There are four or more entries.
                                            // Get the index of the wildcard.
                                            val thirdWildcardIndex = thirdEntry.indexOf("*")

                                            // Split the entry into components.
                                            val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                            val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                            // Process the fourth entry.
                                            if (fourthEntry.contains("*")) {  // Process a third-party allow list quintuple entry.
                                                // Get the index of the wildcard.
                                                val fourthWildcardIndex = fourthEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realFourthEntry = fourthEntry.substring(0, fourthWildcardIndex)
                                                val fifthEntry = fourthEntry.substring(fourthWildcardIndex + 1)

                                                // Create an entry string array.
                                                val quintupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, realFourthEntry, fifthEntry, originalFilterListEntry)

                                                // Add the entry to the allow list.
                                                thirdPartyAllowList.add(quintupleEntry)

                                                //Log.i("FilerLists", headers.get(1)[0] + " third-party allow list added:  " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " +
                                                //        realFourthEntry + " , " + fifthEntry + "  -  " + originalFilterListEntry)
                                            } else {  // Third-party allow list quadruple entry.
                                                // Create an entry string array.
                                                val quadrupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalFilterListEntry)

                                                // Add the entry to the allow list.
                                                thirdPartyAllowList.add(quadrupleEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " third-party allow list added:  " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " +
                                                //        fourthEntry + "  -  " + originalAllowListEntry)
                                            }
                                        } else {  // Process a third-party allow list triple entry.
                                            // Create an entry string array.
                                            val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            thirdPartyAllowList.add(tripleEntry)

                                            //Log.i("AllowLists", headers.get(1)[0] + " third-party allow list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " +
                                            //        originalFilterListEntry)
                                        }
                                    } else {  // Process a third-party allow list double entry.
                                        // Create an entry string array.
                                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                        // Add the entry to the allow list.
                                        thirdPartyAllowList.add(doubleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " third-party allow list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                                    }
                                } else {  // Process a third-party allow list single entry.
                                    // Create an entry string array.
                                    val singleEntry = arrayOf(entry, originalFilterListEntry)

                                    // Add the entry to the allow list.
                                    thirdPartyAllowList.add(singleEntry)

                                    //Log.i("FilterLists", headers.get(1)[0] + " third-party domain allow list added:  " + entry + "  -  " + originalFilterListEntry)
                                }
                            }
                        } else if (filterListEntry.contains("domain=")) {  // Process domain allow list entries.
                            // Parse the entry
                            var entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))
                            val filters = filterListEntry.substring(filterListEntry.indexOf("$") + 1)
                            var domains = filters.substring(filters.indexOf("domain=") + 7)

                            // Process the entry.
                            if (entry.startsWith("|")) {  // Initial domain allow list entries.
                                // Strip the initial `|`.
                                entry = entry.substring(1)

                                // Process the entry.
                                if (entry == "http://" || entry == "https://") {  // Ignore generic entries.
                                    // Do nothing.  These entries are designed for filter options that Privacy Browser does not use.

                                    //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                                } else {  // Initial domain allow list entry.
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        // Get the first domain.
                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        // Process the entry.
                                        if (entry.contains("*")) {  // There are two or more entries.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Process the second entry.
                                            if (secondEntry.contains("*")) {  // Process a domain initial triple entry.
                                                // Get the index of the wildcard.
                                                val secondWildcardIndex = secondEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                                // Create an entry string array.
                                                val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                                // Add the entry to the allow list.
                                                domainInitialAllowList.add(domainTripleEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " domain initial allow list entry added:  " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " +
                                                //        thirdEntry + "  -  " + originalFilterListEntry)
                                            } else {  // Process a domain initial double entry.
                                                // Create an entry string array.
                                                val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                                // Add the entry to the allow list.
                                                domainInitialAllowList.add(domainDoubleEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " domain initial allow list entry added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                                //        originalFilterListEntry)
                                            }
                                        } else {  // Process a domain initial single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            domainInitialAllowList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain initial allow list entry added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                        }
                                    // Repeat until all the domains have been processed.
                                    } while (domains.contains("|"))
                                }
                            } else if (entry.endsWith("|")) {  // Final domain allow list entries.
                                // Strip the `|` from the end of the entry.
                                entry = entry.substring(0, entry.length - 1)

                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    // Get the first domain.
                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // There is only one domain in the list.
                                        domain = domains
                                    }

                                    if (entry.contains("*")) {  // Domain final allow list double entry.
                                        // Get the index of the wildcard.
                                        val wildcardIndex = entry.indexOf("*")

                                        // Split the entry into components.
                                        val firstEntry = entry.substring(0, wildcardIndex)
                                        val secondEntry = entry.substring(wildcardIndex + 1)

                                        // Create an entry string array.
                                        val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                        // Add the entry to the allow list.
                                        domainFinalAllowList.add(domainDoubleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " domain final allow list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                        //        originalFilterListEntry);
                                    } else {  // Process a domain final allow list single entry.
                                        // create an entry string array.
                                        val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                        // Add the entry to the allow list.
                                        domainFinalAllowList.add(domainEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " domain final allow list added: " + domain + " , " + entry + "  -  " + originalFilterListEntry);
                                    }
                                // Repeat until all the domains have been processed.
                                } while (domains.contains("|"))
                            } else {  // Standard domain allow list entries with filters.
                                if (domains.contains("~")) {  // It is uncertain what a `~` domain means inside an `@@` entry.
                                    // Do Nothing

                                    //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                                } else {
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        // Get the first domain.
                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        // Process the entry.
                                        if (entry.contains("*")) {  // There are two or more entries.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Process the second entry.
                                            if (secondEntry.contains("*")) {  // There are three or more entries.
                                                // Get the index of the wildcard.
                                                val secondWildcardIndex = secondEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                                // Process the third entry.
                                                if (thirdEntry.contains("*")) {  // Process a domain allow list quadruple entry.
                                                    // Get the index of the wildcard.
                                                    val thirdWildcardIndex = thirdEntry.indexOf("*")

                                                    // Split the entry into components.
                                                    val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                                    val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                                    // Create an entry string array.
                                                    val domainQuadrupleEntry = arrayOf(domain, firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalFilterListEntry)

                                                    // Add the entry to the allow list.
                                                    domainAllowList.add(domainQuadrupleEntry)

                                                    //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " +
                                                    //        realThirdEntry + " , " + fourthEntry + "  -  " + originalFilterListEntry)
                                                } else {  // Process a domain allow list triple entry.
                                                    // Create an entry string array.
                                                    val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                                    // Add the entry to the allow list.
                                                    domainAllowList.add(domainTripleEntry)

                                                    //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " +
                                                    //        thirdEntry + "  -  " + originalFilterListEntry)
                                                }
                                            } else {  // Process a domain allow list double entry.
                                                // Create an entry string array.
                                                val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                                // Add the entry to the allow list.
                                                domainAllowList.add(domainDoubleEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " originalFilterListEntry)
                                            }
                                        } else {  // Process a domain allow list single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            domainAllowList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                        }
                                    // Repeat until all the domains have been processed.
                                    } while (domains.contains("|"))
                                }
                            }
                        }  // Ignore all other filter entries.
                    } else if (filterListEntry.endsWith("|")) {  // Final allow list entries.
                        // Remove the final `|` from the entry.
                        val entry = filterListEntry.substring(0, filterListEntry.length - 1)

                        // Process the entry.
                        if (entry.contains("*")) {  // Process a final allow list double entry
                            // Get the index of the wildcard.
                            val wildcardIndex = entry.indexOf("*")

                            // split the entry into components.
                            val firstEntry = entry.substring(0, wildcardIndex)
                            val secondEntry = entry.substring(wildcardIndex + 1)

                            // Create an entry string array.
                            val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                            // Add the entry to the allow list.
                            finalAllowList.add(doubleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " final allow list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                        } else {  // Process a final allow list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalFilterListEntry)

                            // Add the entry to the allow list.
                            finalAllowList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " final allow list added:  " + entry + "  -  " + originalFilterListEntry)
                        }
                    } else {  // Main allow list entries.
                        // Process the entry.
                        if (filterListEntry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = filterListEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = filterListEntry.substring(0, wildcardIndex)
                            val secondEntry = filterListEntry.substring(wildcardIndex + 1)

                            // Process the second entry.
                            if (secondEntry.contains("*")) {  // Main allow list triple entry.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Create an entry string array.
                                val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                // Add the entry to the allow list.
                                mainAllowList.add(tripleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " main allow list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalFilterListEntry)
                            } else {  // Process a main allow list double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                // Add the entry to the allow list.
                                mainAllowList.add(doubleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " main allow list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                            }
                        } else {  // Process a main allow list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(filterListEntry, originalFilterListEntry)

                            // Add the entry to the allow list.
                            mainAllowList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " main allow list added:  " + filterListEntry + "  -  " + originalFilterListEntry)
                        }
                    }
                } else if (filterListEntry.endsWith("|")) {  // Final block list entries.
                    // Strip out the final "|"
                    var entry = filterListEntry.substring(0, filterListEntry.length - 1)

                    // Strip out any initial `||`.  They are redundant in this case because the block list entry is being matched against the end of the URL.
                    if (entry.startsWith("||"))
                        entry = entry.substring(2)

                    // Process the entry.
                    if (entry.contains("*")) {  // Process a final block list double entry.
                        // Get the index of the wildcard.
                        val wildcardIndex = entry.indexOf("*")

                        // Split the entry into components.
                        val firstEntry = entry.substring(0, wildcardIndex)
                        val secondEntry = entry.substring(wildcardIndex + 1)

                        // Create an entry string array.
                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                        // Add the entry to the block list.
                        finalBlockList.add(doubleEntry)

                        //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                    } else {  // Process a final block list single entry.
                        // Create an entry string array.
                        val singleEntry = arrayOf(entry, originalFilterListEntry)

                        // Add the entry to the block list.
                        finalBlockList.add(singleEntry)

                        //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + entry + "  -  " + originalFilterListEntry)
                    }
                } else if (filterListEntry.contains("$")) {  // Entries with filter options.
                    // Strip out any initial `||`.  These will be treated like any other entry.
                    if (filterListEntry.startsWith("||"))
                        filterListEntry = filterListEntry.substring(2)

                    // Process the entry.
                    if (filterListEntry.contains("third-party")) {  // Third-party entries.
                        // Process the entry.
                        if (filterListEntry.contains("~third-party")) {  // Third-party filter allow list entries.
                            // Do not process these allow list entries.  They are designed to combine with block filters that Privacy Browser doesn't use, like `subdocument` and `xmlhttprequest`.

                            //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                        } else if (filterListEntry.contains("domain=")) {  // Third-party domain entries.
                            // Process the domain entry.
                            if (filterListEntry.startsWith("|")) {  // Third-party domain initial entries.
                                // Strip the initial `|`.
                                filterListEntry = filterListEntry.substring(1)

                                // Parse the entry
                                val entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))
                                val filters = filterListEntry.substring(filterListEntry.indexOf("$") + 1)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                // Process the entry.
                                if (entry == "http:" || entry == "https:" || entry == "http://" || entry == "https://") {  // Ignore generic entries.
                                    // Do nothing.  These entries will almost entirely disable the website.
                                    // Often the original entry blocks filter options like `$script`, which Privacy Browser does not differentiate.

                                    //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                                } else {  // Third-party domain initial entries.
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        // Get the first domain.
                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        // Process the entry.
                                        if (entry.contains("*")) {  // Three are two or more entries.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Process the second entry.
                                            if (secondEntry.contains("*")) {  // Third-party domain initial block list triple entry.
                                                // Get the index of the wildcard.
                                                val secondWildcardIndex = secondEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                                // Create an entry string array.
                                                val tripleDomainEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                                // Add the entry to the block list.
                                                thirdPartyDomainInitialBlockList.add(tripleDomainEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " third-party domain initial block list added:  " + domain + " , " + firstEntry + " , " + realSecondEntry +
                                                //        " , " + thirdEntry + "  -  " + originalFilterListEntry)
                                            } else {  // Process a third-party domain initial block list double entry.
                                                // Create an entry string array.
                                                val doubleDomainEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                                // Add the entry to the block list.
                                                thirdPartyDomainInitialBlockList.add(doubleDomainEntry)

                                                //Log.i("FilterLists", headers.get(1)[0] + " third-party domain initial block list added: " + domain + " , " + firstEntry + " , " + secondEntry +
                                                //        "  -  " + originalFilterListEntry)
                                            }
                                        } else {  // Process a third-party domain initial block list single entry.
                                            // Create an entry string array.
                                            val singleEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            thirdPartyDomainInitialBlockList.add(singleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain initial block list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                        }
                                    // Repeat until all the domains have been processed.
                                    } while (domains.contains("|"))
                                }
                            } else if (filterListEntry.contains("\\")) {  // Process a third-party domain block list regular expression.
                                // Parse the entry.  At least one regular expression in this entry contains `$`, so the parser uses `/$`.
                                val entry = filterListEntry.substring(0, filterListEntry.indexOf("/$") + 1)
                                val filters = filterListEntry.substring(filterListEntry.indexOf("/$") + 2)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    // Get the first domain.
                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // There is only one domain in the list.
                                        domain = domains
                                    }

                                    // Create an entry string array.
                                    val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                    // Add the entry to the block list.
                                    thirdPartyDomainRegularExpressionBlockList.add(domainEntry)

                                    //Log.i("FilterLists", headers.get(1)[0] + " third-party domain regular expression block list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                // Repeat until all the domains have been processed.
                                } while (domains.contains("|"))
                            } else {  // Third-party domain entries.
                                // Parse the entry
                                var entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))
                                val filters = filterListEntry.substring(filterListEntry.indexOf("$") + 1)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                // Strip any trailing "*" from the entry.
                                if (entry.endsWith("*"))
                                    entry = entry.substring(0, entry.length - 1)

                                // Create an allow domain tracker.
                                var allowDomain = false

                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    // Get the first domain.
                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // The is only one domain in the list.
                                        domain = domains
                                    }

                                    // Differentiate between block list domains and allow list domains.
                                    if (domain.startsWith("~")) {  // Allow list third-party domain entry.
                                        // Strip the initial `~`.
                                        domain = domain.substring(1)

                                        // Set the allow list domain flag.
                                        allowDomain = true

                                        // Process the entry.
                                        if (entry.contains("*")) {  // Third-party domain allow list double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            thirdPartyDomainAllowList.add(domainDoubleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain allow list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalFilterListEntry)
                                        } else {  // Process a third-party domain allow list single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the allow list.
                                            thirdPartyDomainAllowList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain allow list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                        }
                                    } else {  // Third-party domain block list entries.
                                        if (entry.contains("*")) {  // Process a third-party domain block list double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                            // Add the entry to the block list
                                            thirdPartyDomainBlockList.add(domainDoubleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain block list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalFilterListEntry)
                                        } else {  // Process a third-party domain block list single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            thirdPartyDomainBlockList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " third-party domain block list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                        }
                                    }
                                // Repeat until all the domains have been processed.
                                } while (domains.contains("|"))

                                // Add a third-party block list entry if an allow list domain was processed.
                                if (allowDomain) {
                                    if (entry.contains("*")) {  // Third-party block list double entry.
                                        // Get the index of the wildcard.
                                        val wildcardIndex = entry.indexOf("*")

                                        // Split the entry into components.
                                        val firstEntry = entry.substring(0, wildcardIndex)
                                        val secondEntry = entry.substring(wildcardIndex + 1)

                                        // Create an entry string array.
                                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        thirdPartyBlockList.add(doubleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " third-party block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                                    } else {  // Process a third-party block list single entry.
                                        // Create an entry string array.
                                        val singleEntry = arrayOf(entry, originalFilterListEntry)

                                        // Add an entry to the block list.
                                        thirdPartyBlockList.add(singleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " third-party block list added:  " + entry + "  -  " + originalFilterListEntry)
                                    }
                                }
                            }
                        } else if (filterListEntry.startsWith("|")) {  // Third-party initial block list entries.
                            // Strip the initial `|`.
                            filterListEntry = filterListEntry.substring(1)

                            // Get the entry.
                            val entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                            // Process the entry.
                            if (entry.contains("*")) {  // Process a third-party initial block list double entry.
                                // Get the index of the wildcard.
                                val wildcardIndex = entry.indexOf("*")

                                // Split the entry into components.
                                val firstEntry = entry.substring(0, wildcardIndex)
                                val secondEntry = entry.substring(wildcardIndex + 1)

                                // Create an entry string array.
                                val thirdPartyDoubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                thirdPartyInitialBlockList.add(thirdPartyDoubleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " third-party initial block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                            } else {  // Process a third-party initial block list single entry.
                                // Create an entry string array.
                                val singleEntry = arrayOf(entry, originalFilterListEntry)

                                // Add the entry to the block list.
                                thirdPartyInitialBlockList.add(singleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " third-party initial block list added:  " + entry + "  -  " + originalFilterListEntry)
                            }
                        } else if (filterListEntry.contains("\\")) {  // Third-party regular expression block list entry.
                            // Get the entry.
                            val entry = if (filterListEntry.contains("$/$"))  // The first `$` is part of the regular expression.
                                filterListEntry.substring(0, filterListEntry.indexOf("$/$") + 2)
                            else  // The only `$` indicates the filter options.
                                filterListEntry.substring(0, filterListEntry.indexOf("$"))

                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalFilterListEntry)

                            // Add the entry to the block list.
                            thirdPartyRegularExpressionBlockList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " third-party regular expression block list added:  " + entry + "  -  " + originalFilterListEntry)
                        } else if (filterListEntry.contains("*")) {  // Third-party and regular expression block list entries.
                            // Get the entry.
                            var entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                            // Process the entry.
                            if (entry.endsWith("*")) {  // Third-party block list single entry.
                                // Strip the final `*`.
                                entry = entry.substring(0, entry.length - 1)

                                // Create an entry string array.
                                val singleEntry = arrayOf(entry, originalFilterListEntry)

                                // Add the entry to the block list.
                                thirdPartyBlockList.add(singleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " third party block list added:  " + entry + "  -  " + originalFilterListEntry)
                            } else {  // There are two or more entries.
                                // Get the index of the wildcard.
                                val wildcardIndex = entry.indexOf("*")

                                // Split the entry into components.
                                val firstEntry = entry.substring(0, wildcardIndex)
                                val secondEntry = entry.substring(wildcardIndex + 1)

                                // Process the second entry.
                                if (secondEntry.contains("*")) {  // There are three or more entries.
                                    // Get the index of the wildcard.
                                    val secondWildcardIndex = secondEntry.indexOf("*")

                                    // Split the entry into components.
                                    val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                    val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                    // Process a third entry.
                                    if (thirdEntry.contains("*")) {  // Third-party block list quadruple entry.
                                        // Get the index of the wildcard.
                                        val thirdWildcardIndex = thirdEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                        val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                        // Create an entry string array.
                                        val quadrupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        thirdPartyBlockList.add(quadrupleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " third-party block list added:  " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " +
                                        //        fourthEntry + "  -  " + originalFilterListEntry);
                                    } else {  // Third-party block list triple entry.
                                        // Create an entry string array.
                                        val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        thirdPartyBlockList.add(tripleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " third-party block list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " +
                                        //        originalFilterListEntry)
                                    }
                                } else {  // Third-party block list double entry.
                                    // Create an entry string array.
                                    val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                    // Add the entry to the block list.
                                    thirdPartyBlockList.add(doubleEntry)

                                    //Log.i("FilterLists", headers.get(1)[0] + " third-party block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                                }
                            }
                        } else {  // Third party block list single entry.
                            // Get the entry.
                            val entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalFilterListEntry)

                            // Add the entry to the block list.
                            thirdPartyBlockList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " third party block list added:  " + entry + "  -  " + originalFilterListEntry)
                        }
                    } else if (filterListEntry.substring(filterListEntry.indexOf("$")).contains("domain=")) {  // Domain entries.
                        // Process the entry.
                        if (filterListEntry.contains("~")) {  // Domain allow list entries.
                            // Separate the filters.
                            var entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))
                            val filters = filterListEntry.substring(filterListEntry.indexOf("$") + 1)
                            var domains = filters.substring(filters.indexOf("domain=") + 7)

                            // Strip any final `*` from the entry.  They are redundant.
                            if (entry.endsWith("*"))
                                entry = entry.substring(0, entry.length - 1)

                            // Process each domain.
                            do {
                                // Create a string to keep track of the current domain.
                                var domain: String

                                // Get the first domain.
                                if (domains.contains("|")) {  // There is more than one domain in the list.
                                    // Get the first domain from the list.
                                    domain = domains.substring(0, domains.indexOf("|"))

                                    // Remove the first domain from the list.
                                    domains = domains.substring(domains.indexOf("|") + 1)
                                } else {  // There is only one domain in the list.
                                    domain = domains
                                }

                                // Strip the initial `~`.
                                domain = domain.substring(1)

                                // Process the entry.
                                if (entry.contains("*")) {  // There are two or more entries.
                                    // Get the index of the wildcard.
                                    val wildcardIndex = entry.indexOf("*")

                                    // Split the entry into components.
                                    val firstEntry = entry.substring(0, wildcardIndex)
                                    val secondEntry = entry.substring(wildcardIndex + 1)

                                    // Process the second entry.
                                    if (secondEntry.contains("*")) {  // Domain allow list triple entry.
                                        // Get the index of the wildcard.
                                        val secondWildcardIndex = secondEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                        val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                        // Create an entry string array.
                                        val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                        // Add the entry to the allow list.
                                        domainAllowList.add(domainTripleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry +
                                        //        "  -  " + originalFilterListEntry)
                                    } else {  // Process a domain allow list double entry.
                                        // Create an entry string array.
                                        val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                        // Add the entry to the allow list.
                                        domainAllowList.add(domainDoubleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                                    }
                                } else {  // Process a domain allow list single entry.
                                    // Create an entry string array.
                                    val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                    // Add the entry to the allow list.
                                    domainAllowList.add(domainEntry)

                                    //Log.i("FilterLists", headers.get(1)[0] + " domain allow list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                }
                            // Repeat until all the domains have been processed.
                            } while (domains.contains("|"))
                        } else {  // Domain block list entries.
                            // Separate the filters.
                            var entry = filterListEntry.substring(0, filterListEntry.indexOf("$"))
                            val filters = filterListEntry.substring(filterListEntry.indexOf("$") + 1)
                            var domains = filters.substring(filters.indexOf("domain=") + 7)

                            // Only process the item if the entry is not empty.  For example, some lines begin with `$websocket`, which create an empty entry.
                            if (entry.isNotEmpty()) {
                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    // Get the first domain.
                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // There is only one domain in the list.
                                        domain = domains
                                    }

                                    // Process the entry.
                                    if (entry.startsWith("|")) {  // Domain initial block list entries.
                                        // Remove the initial `|`;
                                        entry = entry.substring(1)

                                        // Process the entry.
                                        if (entry == "http://" || entry == "https://") {
                                            // Do nothing.  These entries will entirely block the website.
                                            // Often the original entry blocks `$script` but Privacy Browser does not currently differentiate between scripts and other entries.

                                            //Log.i("FilterLists", headers.get(1)[0] + " not added:  " + originalFilterListEntry)
                                        } else {  // Domain initial block list entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            domainInitialBlockList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain initial block list added:  " + domain + " , " + entryBase + "  -  " + originalFilterListEntry)
                                        }
                                    } else if (entry.endsWith("|")) {  // Domain final block list entries.
                                        // Remove the final `|`.
                                        entry = entry.substring(0, entry.length - 1)

                                        // Process the entry.
                                        if (entry.contains("*")) {  // Process a domain final block list double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            domainFinalBlockList.add(domainDoubleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain final block list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalFilterListEntry)
                                        } else {  // Domain final block list single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            domainFinalBlockList.add(domainEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain final block list added:  " + domain + " , " + entryBase + "  -  " + originalFilterListEntry)
                                        }
                                    } else if (entry.contains("\\")) {  // Domain regular expression block list entry.
                                        // Create an entry string array.
                                        val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        domainRegularExpressionBlockList.add(domainEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " domain regular expression block list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                    } else if (entry.contains("*")) {  // There are two or more entries.
                                        // Get the index of the wildcard.
                                        val wildcardIndex = entry.indexOf("*")

                                        // Split the entry into components.
                                        val firstEntry = entry.substring(0, wildcardIndex)
                                        val secondEntry = entry.substring(wildcardIndex + 1)

                                        // Process the second entry.
                                        if (secondEntry.contains("*")) {  // Process a domain block list triple entry.
                                            // Get the index of the wildcard.
                                            val secondWildcardIndex = secondEntry.indexOf("*")

                                            // Split the entry into components.
                                            val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                            val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            domainBlockList.add(domainTripleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain block list added:  " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry +
                                            //        "  -  " + originalFilterListEntry)
                                        } else {  // Process a domain block list double entry.
                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalFilterListEntry)

                                            // Add the entry to the block list.
                                            domainBlockList.add(domainDoubleEntry)

                                            //Log.i("FilterLists", headers.get(1)[0] + " domain block list added:  " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalFilterListEntry)
                                        }
                                    } else {  // Process a domain block list single entry.
                                        // Create an entry string array.
                                        val domainEntry = arrayOf(domain, entry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        domainBlockList.add(domainEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " domain block list added:  " + domain + " , " + entry + "  -  " + originalFilterListEntry)
                                    }
                                // Repeat until all the domains have been processed.
                                } while (domains.contains("|"))
                            }
                        }
                    } else if (filterListEntry.contains("~")) {  // Allow list entries.  Privacy Browser does not differentiate against these filter options, so they are just generally allowed.
                        // Remove the filter options.
                        filterListEntry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                        // Strip any trailing `*`.
                        if (filterListEntry.endsWith("*"))
                            filterListEntry = filterListEntry.substring(0, filterListEntry.length - 1)

                        // Process the entry.
                        if (filterListEntry.contains("*")) {  // Allow list double entry.
                            // Get the index of the wildcard.
                            val wildcardIndex = filterListEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = filterListEntry.substring(0, wildcardIndex)
                            val secondEntry = filterListEntry.substring(wildcardIndex + 1)

                            // Create an entry string array.
                            val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                            // Add the entry to the allow list.
                            mainAllowList.add(doubleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " main allow list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                        } else {  // Allow list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(filterListEntry, originalFilterListEntry)

                            // Add the entry to the allow list.
                            mainAllowList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " main allow list added:  " + filterListEntry + "  -  + " + originalFilterListEntry)
                        }
                    } else if (filterListEntry.contains("\\")) {  // Regular expression block list entry.
                        // Remove the filter options.
                        filterListEntry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                        // Create an entry string array.
                        val singleEntry = arrayOf(filterListEntry, originalFilterListEntry)

                        // Add the entry to the block list.
                        regularExpressionBlockList.add(singleEntry)

                        //Log.i("FilterLists", headers.get(1)[0] + " regular expression block list added: " + filterListEntry + "  -  " + originalFilterListEntry);
                    } else {  // Block list entries.
                        // Remove the filter options.
                        if (!filterListEntry.contains("\$file"))  // EasyPrivacy contains an entry with `$file` that does not have filter options.
                            filterListEntry = filterListEntry.substring(0, filterListEntry.indexOf("$"))

                        // Strip any trailing `*`.  These are redundant.
                        if (filterListEntry.endsWith("*"))
                            filterListEntry = filterListEntry.substring(0, filterListEntry.length - 1)

                        // Process the entry.
                        if (filterListEntry.startsWith("|")) {  // Initial block list entries.
                            // Strip the initial `|`.
                            val entry = filterListEntry.substring(1)

                            if (entry.contains("*")) {  // Process an initial block list double entry.
                                // Get the index of the wildcard.
                                val wildcardIndex = entry.indexOf("*")

                                // Split the entry into components.
                                val firstEntry = entry.substring(0, wildcardIndex)
                                val secondEntry = entry.substring(wildcardIndex + 1)

                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                initialBlockList.add(doubleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " initial block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                            } else {  // Initial blocklist single entry.
                                // Create an entry string array.
                                val singleEntry = arrayOf(entry, originalFilterListEntry)

                                // Add the entry to the block list.
                                initialBlockList.add(singleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " initial block list added:  " + entry + "  -  " + originalFilterListEntry)
                            }
                        } else if (filterListEntry.endsWith("|")) {  // Final block list entries.
                            // Ignore entries with `object` filters.  They can block entire websites and don't have any meaning in the context of Privacy Browser.
                            if (!originalFilterListEntry.contains("\$object")) {
                                // Strip the final `|`.
                                val entry = filterListEntry.substring(0, filterListEntry.length - 1)

                                // Process the entry.
                                if (entry.contains("*")) {  // There are two or more entries.
                                    // Get the index of the wildcard.
                                    val wildcardIndex = entry.indexOf("*")

                                    // Split the entry into components.
                                    val firstEntry = entry.substring(0, wildcardIndex)
                                    val secondEntry = entry.substring(wildcardIndex + 1)

                                    // Process the second entry.
                                    if (secondEntry.contains("*")) {  // Final block list triple entry.
                                        // Get the index of the wildcard.
                                        val secondWildcardIndex = secondEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                        val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                        // Create an entry string array.
                                        val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        finalBlockList.add(tripleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalFilterListEntry)
                                    } else {  // Final block list double entry.
                                        // Create an entry string array.
                                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        finalBlockList.add(doubleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                                    }
                                } else {  // Final block list single entry.
                                    // Create an entry sting array.
                                    val singleEntry = arrayOf(entry, originalFilterListEntry)

                                    // Add the entry to the block list.
                                    finalBlockList.add(singleEntry)

                                    //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + entry + "  -  " + originalFilterListEntry)
                                }
                            }
                        } else if (filterListEntry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = filterListEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = filterListEntry.substring(0, wildcardIndex)
                            val secondEntry = filterListEntry.substring(wildcardIndex + 1)

                            // Process the second entry.
                            if (secondEntry.contains("*")) {  // Main block list triple entry.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Create an entry string array.
                                val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                mainBlockList.add(tripleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalFilterListEntry)
                            } else {  // Main block list double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                mainBlockList.add(doubleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                            }
                        } else if (filterListEntry.isNotBlank()){  // Main block list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(filterListEntry, originalFilterListEntry)

                            // Add the entry to the block list.
                            mainBlockList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + filterListEntry + "  -  " + originalFilterListEntry)
                        } else {  // The entry is blank (empty or all white spaces).
                            // Do nothing.

                            //Log.i("FilterLists", "${headers[1][0]} not added:  $filterListEntry,  $originalFilterListEntry")
                        }
                    }
                } else {  // Main block list entries.
                    // Strip out any initial `||`.  These will be treated like any other entry.
                    if (filterListEntry.startsWith("||"))
                        filterListEntry = filterListEntry.substring(2)

                    // Strip out any initial `*`.
                    if (filterListEntry.startsWith("*"))
                        filterListEntry = filterListEntry.substring(1)

                    // Strip out any trailing `*`.
                    if (filterListEntry.endsWith("*"))
                        filterListEntry = filterListEntry.substring(0, filterListEntry.length - 1)

                    // Process the entry.
                    if (filterListEntry.startsWith("|")) {  // Initial block list entries.
                        // Strip the initial `|`.
                        val entry = filterListEntry.substring(1)

                        // Process the entry.
                        if (entry.contains("*")) {  // Initial block list double entry.
                            // Get the index of the wildcard.
                            val wildcardIndex = entry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = entry.substring(0, wildcardIndex)
                            val secondEntry = entry.substring(wildcardIndex + 1)

                            // Create an entry string array.
                            val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                            // Add the entry to the block list.
                            initialBlockList.add(doubleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " initial block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                        } else {  // Initial block list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalFilterListEntry)

                            // Add the entry to the block list.
                            initialBlockList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " initial block list added:  " + entry + "  -  " + originalFilterListEntry)
                        }
                    } else if (filterListEntry.endsWith("|")) {  // Final block list entries.
                        // Strip the final `|`.
                        val entry = filterListEntry.substring(0, filterListEntry.length - 1)

                        // Process the entry.
                        if (entry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = entry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = entry.substring(0, wildcardIndex)
                            val secondEntry = entry.substring(wildcardIndex + 1)

                            // Process the second entry.
                            if (secondEntry.contains("*")) {  // Final block list triple entry.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Create an entry string array.
                                val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                finalBlockList.add(tripleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalFilterListEntry)
                            } else {  // Final block list double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                finalBlockList.add(doubleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                            }
                        } else {  // Final block list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalFilterListEntry)

                            // Add the entry to the block list.
                            finalBlockList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " final block list added:  " + entry + "  -  " + originalFilterListEntry)
                        }
                    } else {  // Main block list entries.
                        if (filterListEntry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = filterListEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = filterListEntry.substring(0, wildcardIndex)
                            val secondEntry = filterListEntry.substring(wildcardIndex + 1)

                            // Process the second entry.
                            if (secondEntry.contains("*")) {  // There are three or more entries.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Process the third entry.
                                if (thirdEntry.contains("*")) {  // There are four or more entries.
                                    // Get the index of the wildcard.
                                    val thirdWildcardIndex = thirdEntry.indexOf("*")

                                    // Split the entry into components.
                                    val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                    val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                    // Process the fourth entry.
                                    if (fourthEntry.contains("*")) {  // Main block list quintuple entry.
                                        // Get the index of the wildcard.
                                        val fourthWildcardIndex = fourthEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realFourthEntry = fourthEntry.substring(0, fourthWildcardIndex)
                                        val fifthEntry = fourthEntry.substring(fourthWildcardIndex + 1)

                                        // Create an entry string array.
                                        val quintupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, realFourthEntry, fifthEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        mainBlockList.add(quintupleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " + realFourthEntry + " , " +
                                        //      fifthEntry + "  -  " + originalFilterListEntry)
                                    } else {  // Main block list quadruple entry.
                                        // Create an entry string array.
                                        val quadrupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalFilterListEntry)

                                        // Add the entry to the block list.
                                        mainBlockList.add(quadrupleEntry)

                                        //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " + fourthEntry + "  -  " +
                                        //      originalFilterListEntry)
                                    }
                                } else {  // Main block list triple entry.
                                    // Create an entry string array.
                                    val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalFilterListEntry)

                                    // Add the entry to the block list.
                                    mainBlockList.add(tripleEntry)

                                    //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalFilterListEntry)
                                }
                            } else {  // Main block list double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalFilterListEntry)

                                // Add the entry to the block list.
                                mainBlockList.add(doubleEntry)

                                //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + firstEntry + " , " + secondEntry + "  -  " + originalFilterListEntry)
                            }
                        } else if (filterListEntry.isNotBlank()){  // Main block list single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(filterListEntry, originalFilterListEntry)

                            // Add the entry to the block list.
                            mainBlockList.add(singleEntry)

                            //Log.i("FilterLists", headers.get(1)[0] + " main block list added:  " + filterListEntry + "  -  " + originalFilterListEntry)
                        } else {  // The entry is blank (empty or all white spaces).
                            // Do nothing.

                            //Log.i("FilterLists", "${headers[1][0]} not added:  $filterListEntry,  $originalFilterListEntry")
                        }
                    }
                }
            }
            // Close the buffered reader.
            bufferedReader.close()
        } catch (e: IOException) {
            // Do nothing.
        }

        // Initialize the combined list.
        val combinedLists = ArrayList<List<Array<String>>>()

        // Add the headers (0).
        combinedLists.add(headers) // 0.

        // Add the allow lists (1-8).
        combinedLists.add(mainAllowList) // 1.
        combinedLists.add(finalAllowList) // 2.
        combinedLists.add(domainAllowList) // 3.
        combinedLists.add(domainInitialAllowList) // 4.
        combinedLists.add(domainFinalAllowList) // 5.
        combinedLists.add(thirdPartyAllowList) // 6.
        combinedLists.add(thirdPartyDomainAllowList) // 7.
        combinedLists.add(thirdPartyDomainInitialAllowList) // 8.

        // Add the block lists (9-22).
        combinedLists.add(mainBlockList) // 9.
        combinedLists.add(initialBlockList) // 10.
        combinedLists.add(finalBlockList) // 11.
        combinedLists.add(domainBlockList) //  12.
        combinedLists.add(domainInitialBlockList) // 13.
        combinedLists.add(domainFinalBlockList) // 14.
        combinedLists.add(domainRegularExpressionBlockList) // 15.
        combinedLists.add(thirdPartyBlockList) // 16.
        combinedLists.add(thirdPartyInitialBlockList) // 17.
        combinedLists.add(thirdPartyDomainBlockList) // 18.
        combinedLists.add(thirdPartyDomainInitialBlockList) // 19.
        combinedLists.add(thirdPartyRegularExpressionBlockList) // 20.
        combinedLists.add(thirdPartyDomainRegularExpressionBlockList) // 21.
        combinedLists.add(regularExpressionBlockList) // 22.

        // Return the combined lists.
        return combinedLists
    }
}
