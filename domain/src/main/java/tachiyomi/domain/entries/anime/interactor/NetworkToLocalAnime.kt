package tachiyomi.domain.entries.anime.interactor

import eu.kanade.tachiyomi.animesource.model.FetchType
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.source.anime.service.AnimeSourceManager

class NetworkToLocalAnime(
    private val animeRepository: AnimeRepository,
    private val sourceManager: AnimeSourceManager,
) {

    suspend fun await(anime: Anime): Anime {
        val localAnime = getAnime(anime.url, anime.source)
        return when {
            localAnime == null -> {
                val id = insertAnime(anime)
                anime.copy(id = id!!)
            }
            !localAnime.favorite -> {
                // if the anime isn't a favorite, set its display title from source
                // if it later becomes a favorite, updated title will go to db
                localAnime.copy(title = anime.title)
            }
            else -> {
                localAnime
            }
        }
    }

    private suspend fun getAnime(url: String, sourceId: Long): Anime? {
        return animeRepository.getAnimeByUrlAndSourceId(url, sourceId)
    }

    private suspend fun insertAnime(anime: Anime): Long? {
        // TODO(16): Remove check
        val source = sourceManager.getOrStub(anime.source)
        val isNewExtLib = source.javaClass.declaredMethods.any {
            it.name in listOf("getSeasonList", "seasonListRequest", "seasonListParse")
        }
        val toInsert = if (isNewExtLib) {
            anime
        } else {
            anime.copy(fetchType = FetchType.Episodes)
        }

        return animeRepository.insertAnime(toInsert)
    }
}
