package eu.kanade.domain.entries.anime.interactor

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.AnimeUpdate
import tachiyomi.domain.entries.anime.repository.AnimeRepository

class SetAnimeViewerFlags(
    private val animeRepository: AnimeRepository,
) {

    suspend fun awaitSetSkipIntroLength(id: Long, flag: Long) {
        val anime = animeRepository.getAnimeById(id)
        animeRepository.updateAnime(
            AnimeUpdate(
                id = id,
                viewerFlags = anime.viewerFlags.setFlag(flag, Anime.ANIME_INTRO_MASK),
            ),
        )
    }

    suspend fun awaitSetNextEpisodeAiringAt(id: Long, flag: Long) {
        val anime = animeRepository.getAnimeById(id)
        animeRepository.updateAnime(
            AnimeUpdate(
                id = id,
                viewerFlags = anime.viewerFlags.setFlag(flag, Anime.ANIME_AIRING_MASK),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
