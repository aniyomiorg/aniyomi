package eu.kanade.tachiyomi.ui.watcher.viewer

import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import kotlin.math.floor

private val pattern = Regex("""\d+""")

fun hasMissingEpisodes(higherWatcherEpisode: WatcherEpisode?, lowerWatcherEpisode: WatcherEpisode?): Boolean {
    if (higherWatcherEpisode == null || lowerWatcherEpisode == null) return false
    return hasMissingEpisodes(higherWatcherEpisode.episode, lowerWatcherEpisode.episode)
}

fun hasMissingEpisodes(higherEpisode: Episode?, lowerEpisode: Episode?): Boolean {
    if (higherEpisode == null || lowerEpisode == null) return false
    // Check if name contains a number that is potential episode number
    if (!pattern.containsMatchIn(higherEpisode.name) || !pattern.containsMatchIn(lowerEpisode.name)) return false
    // Check if potential episode number was recognized as episode number
    if (!higherEpisode.isRecognizedNumber || !lowerEpisode.isRecognizedNumber) return false
    return hasMissingEpisodes(higherEpisode.episode_number, lowerEpisode.episode_number)
}

fun hasMissingEpisodes(higherEpisodeNumber: Float, lowerEpisodeNumber: Float): Boolean {
    if (higherEpisodeNumber < 0f || lowerEpisodeNumber < 0f) return false
    return calculateEpisodeDifference(higherEpisodeNumber, lowerEpisodeNumber) > 0f
}

fun calculateEpisodeDifference(higherWatcherEpisode: WatcherEpisode?, lowerWatcherEpisode: WatcherEpisode?): Float {
    if (higherWatcherEpisode == null || lowerWatcherEpisode == null) return 0f
    return calculateEpisodeDifference(higherWatcherEpisode.episode, lowerWatcherEpisode.episode)
}

fun calculateEpisodeDifference(higherEpisode: Episode?, lowerEpisode: Episode?): Float {
    if (higherEpisode == null || lowerEpisode == null) return 0f
    // Check if name contains a number that is potential episode number
    if (!pattern.containsMatchIn(higherEpisode.name) || !pattern.containsMatchIn(lowerEpisode.name)) return 0f
    // Check if potential episode number was recognized as episode number
    if (!higherEpisode.isRecognizedNumber || !lowerEpisode.isRecognizedNumber) return 0f
    return calculateEpisodeDifference(higherEpisode.episode_number, lowerEpisode.episode_number)
}

fun calculateEpisodeDifference(higherEpisodeNumber: Float, lowerEpisodeNumber: Float): Float {
    if (higherEpisodeNumber < 0f || lowerEpisodeNumber < 0f) return 0f
    return floor(higherEpisodeNumber) - floor(lowerEpisodeNumber) - 1f
}
