package eu.kanade.tachiyomi.ui.watcher

import eu.kanade.tachiyomi.data.database.models.Episode

/**
 * Load strategy using the source order. This is the default ordering.
 */
class EpisodeLoadBySource {
    fun get(allEpisodes: List<Episode>): List<Episode> {
        return allEpisodes.sortedByDescending { it.source_order }
    }
}

/**
 * Load strategy using unique episode numbers with same scanlator preference.
 */
class EpisodeLoadByNumber {
    fun get(allEpisodes: List<Episode>, selectedEpisode: Episode): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val episodesByNumber = allEpisodes.groupBy { it.episode_number }

        for ((number, episodesForNumber) in episodesByNumber) {
            val preferredEpisode = when {
                // Make sure the selected episode is always present
                number == selectedEpisode.episode_number -> selectedEpisode
                // If there is only one episode for this number, use it
                episodesForNumber.size == 1 -> episodesForNumber.first()
                // Prefer a episode of the same scanlator as the selected
                else ->
                    episodesForNumber.find { it.scanlator == selectedEpisode.scanlator }
                        ?: episodesForNumber.first()
            }
            episodes.add(preferredEpisode)
        }
        return episodes.sortedBy { it.episode_number }
    }
}

/**
 * Load strategy using the episode upload date. This ordering ignores scanlators
 */
class EpisodeLoadByUploadDate {
    fun get(allEpisodes: List<Episode>): List<Episode> {
        return allEpisodes.sortedBy { it.date_upload }
    }
}
