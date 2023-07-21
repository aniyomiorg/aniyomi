package tachiyomi.domain.items.service

import kotlin.math.floor

fun countMissingItems(itemsInput: List<Float>): Int? {
    if (itemsInput.isEmpty()) {
        return 0
    }

    val items = itemsInput
        // Remove any invalid chapters
        .filter { it != -1f }
        // Convert to integers, as we cannot check if 16.5 is missing
        .map { floor(it.toDouble()).toInt() }
        // Only keep unique chapters so that -1 or 16 are not counted multiple times
        .distinct()
        .sorted()

    if (items.isEmpty()) {
        return null
    }

    var missingItemsCount = 0
    var previousItem = 0 // The actual chapter number, not the array index

    // We go from 0 to lastChapter - Make sure to use the current index instead of the value
    for (i in items.indices) {
        val currentItem = items[i]
        if (currentItem > previousItem + 1) {
            // Add the amount of missing chapters
            missingItemsCount += currentItem - previousItem - 1
        }
        previousItem = currentItem
    }

    return missingItemsCount
}
