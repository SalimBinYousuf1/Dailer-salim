package com.example.util

import com.example.data.model.ContactItem

object T9Search {

    private val charToDigitMap = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun convertNameToDigits(name: String): String {
        return buildString {
            for (char in name.lowercase()) {
                val digit = charToDigitMap[char]
                if (digit != null) {
                    append(digit)
                } else if (char.isWhitespace()) {
                    append(' ')
                }
            }
        }
    }

    data class T9MatchResult(
        val contact: ContactItem,
        val matchedInName: Boolean,
        val matchedNumber: String?,
        val matchStartIndex: Int,
        val matchEndIndex: Int
    )

    fun searchContacts(query: String, contacts: List<ContactItem>): List<T9MatchResult> {
        val cleanQuery = query.filter { it.isDigit() }
        if (cleanQuery.isBlank()) return emptyList()

        val results = mutableListOf<T9MatchResult>()

        for (contact in contacts) {
            val name = contact.displayName
            val nameDigits = convertNameToDigits(name)

            // 1. Check match in name digits
            val words = name.split(" ")
            var foundInName = false

            // Substring search on entire converted name (excluding spaces)
            val nameNoSpaces = nameDigits.replace(" ", "")
            val indexInName = nameNoSpaces.indexOf(cleanQuery)
            if (indexInName >= 0) {
                results.add(
                    T9MatchResult(
                        contact = contact,
                        matchedInName = true,
                        matchedNumber = contact.phoneNumbers.firstOrNull(),
                        matchStartIndex = indexInName,
                        matchEndIndex = indexInName + cleanQuery.length
                    )
                )
                foundInName = true
            } else {
                // Check word beginnings (e.g. initials or start of first/last name)
                var currentPos = 0
                for (word in words) {
                    val wordDigits = convertNameToDigits(word)
                    if (wordDigits.startsWith(cleanQuery)) {
                        results.add(
                            T9MatchResult(
                                contact = contact,
                                matchedInName = true,
                                matchedNumber = contact.phoneNumbers.firstOrNull(),
                                matchStartIndex = currentPos,
                                matchEndIndex = currentPos + cleanQuery.length
                            )
                        )
                        foundInName = true
                        break
                    }
                    currentPos += word.length + 1
                }
            }

            // 2. If not found in name, check phone numbers
            if (!foundInName) {
                for (number in contact.phoneNumbers) {
                    val cleanNum = number.filter { it.isDigit() }
                    val numIndex = cleanNum.indexOf(cleanQuery)
                    if (numIndex >= 0) {
                        results.add(
                            T9MatchResult(
                                contact = contact,
                                matchedInName = false,
                                matchedNumber = number,
                                matchStartIndex = numIndex,
                                matchEndIndex = numIndex + cleanQuery.length
                            )
                        )
                        break
                    }
                }
            }
        }

        // Sort: name matches first, then shorter names
        return results.sortedWith(
            compareByDescending<T9MatchResult> { it.matchedInName }
                .thenBy { it.contact.displayName.length }
        )
    }
}
