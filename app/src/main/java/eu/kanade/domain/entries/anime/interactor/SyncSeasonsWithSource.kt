package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.model.NoSeasonsException
import tachiyomi.domain.entries.anime.model.toAnimeUpdate
import tachiyomi.source.local.entries.anime.isLocal
import java.time.ZonedDateTime

class SyncSeasonsWithSource(
    private val updateAnime: UpdateAnime,
    private val networkToLocalAnime: NetworkToLocalAnime,
) {
    suspend fun await(
        rawSourceSeasons: List<SAnime>,
        anime: Anime,
        source: AnimeSource,
    ) {
        if (rawSourceSeasons.isEmpty() && !source.isLocal()) {
            throw NoSeasonsException()
        }

        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()

        val sourceSeasons = rawSourceSeasons
            .distinctBy { it.url }
            .mapIndexed { i, sAnime ->
                networkToLocalAnime.await(sAnime.toDomainAnime(source.id))
                    .copy(parentId = anime.id)
            }

        val seasonUpdates = sourceSeasons.map { it.toAnimeUpdate() }
        updateAnime.awaitAll(seasonUpdates)
    }
}
