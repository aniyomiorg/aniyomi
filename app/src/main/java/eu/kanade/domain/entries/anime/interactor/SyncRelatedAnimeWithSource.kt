package eu.kanade.domain.entries.anime.interactor

import eu.kanade.domain.entries.anime.model.toDomainAnime
import eu.kanade.domain.entries.anime.model.toSAnime
import tachiyomi.domain.entries.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRelationRepository
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import java.time.Instant
import kotlin.time.Duration.Companion.days

class SyncRelatedAnimeWithSource(
    private val sourceManager: AnimeSourceManager,
    private val networkToLocalAnime: NetworkToLocalAnime,
    private val relationRepository: AnimeRelationRepository,
) {

    suspend fun await(anime: Anime, forceRefresh: Boolean = false) {
        val source = sourceManager.get(anime.source)?.takeIf { it.supportsRelatedAnime } ?: return

        val now = Instant.now().toEpochMilli()
        if (!forceRefresh) {
            val lastFetchedAt = relationRepository.getLastFetchedAt(anime.id)
            if (lastFetchedAt != null && now - lastFetchedAt < TTL) return
        }

        val groups = source.getRelatedAnimeList(anime.toSAnime())
            .map { relation ->
                relation.name to relation.animes
                    .distinctBy { it.url }
                    .filterNot { it.url == anime.url }
                    .take(MAX_PER_GROUP)
                    .map { networkToLocalAnime.await(it.toDomainAnime(anime.source)).id }
            }
            .filter { (_, ids) -> ids.isNotEmpty() }

        relationRepository.replaceRelations(anime.id, groups, now)
    }

    companion object {
        private val TTL = 7.days.inWholeMilliseconds
        private const val MAX_PER_GROUP = 10
    }
}
