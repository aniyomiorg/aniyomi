package tachiyomi.domain.items.service

fun List<Float>.missingItemsCount(): Int {
    if (this.isEmpty()) {
        return 0
    }

    val items = this
        // Ignore unknown item numbers
        .filter { it != -1f }
        // Convert to integers, as we cannot check if 16.5 is missing
        .map(Float::toInt)
        // Only keep unique chapters so that -1 or 16 are not counted multiple times
        .distinct()
        .sorted()

    if (items.isEmpty()) {
        return 0
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
