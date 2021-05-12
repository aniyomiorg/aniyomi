package eu.kanade.tachiyomi.data.database.models

/**
 * Object containing manga, chapter and history
 *
 * @param manga object containing manga
 * @param chapter object containing chater
 * @param animehistory object containing history
 */
data class AnimeEpisodeHistory(val anime: Anime, val episode: Episode, val animehistory: AnimeHistory)
