package tachiyomi.domain.items.service

import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.episode.model.Episode
import kotlin.math.floor

fun List<Double>.missingItemsCount(): Int {
    if (this.isEmpty()) {
        return 0
    }

    val items = this
        // Ignore unknown item numbers
        .filter { it != -1.0 }
        // Convert to integers, as we cannot check if 16.5 is missing
        .map(Double::toInt)
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

fun calculateChapterGap(higherChapter: Chapter?, lowerChapter: Chapter?): Int {
    if (higherChapter == null || lowerChapter == null) return 0
    if (!higherChapter.isRecognizedNumber || !lowerChapter.isRecognizedNumber) return 0
    return calculateChapterGap(higherChapter.chapterNumber, lowerChapter.chapterNumber)
}

fun calculateChapterGap(higherChapterNumber: Double, lowerChapterNumber: Double): Int {
    if (higherChapterNumber < 0.0 || lowerChapterNumber < 0.0) return 0
    return floor(higherChapterNumber).toInt() - floor(lowerChapterNumber).toInt() - 1
}

fun calculateEpisodeGap(higherEpisode: Episode?, lowerEpisode: Episode?): Int {
    if (higherEpisode == null || lowerEpisode == null) return 0
    if (!higherEpisode.isRecognizedNumber || !lowerEpisode.isRecognizedNumber) return 0
    return calculateChapterGap(higherEpisode.episodeNumber, lowerEpisode.episodeNumber)
}

fun calculateEpisodeGap(higherEpisodeNumber: Double, lowerEpisodeNumber: Double): Int {
    if (higherEpisodeNumber < 0.0 || lowerEpisodeNumber < 0.0) return 0
    return floor(higherEpisodeNumber).toInt() - floor(lowerEpisodeNumber).toInt() - 1
}
