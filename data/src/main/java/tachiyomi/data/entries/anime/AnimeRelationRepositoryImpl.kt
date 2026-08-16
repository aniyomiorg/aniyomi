package tachiyomi.data.entries.anime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.entries.anime.model.AnimeRelationGroup
import tachiyomi.domain.entries.anime.repository.AnimeRelationRepository

class AnimeRelationRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeRelationRepository {

    override suspend fun getLastFetchedAt(animeId: Long): Long? {
        return handler.awaitOneOrNull { anime_relationsQueries.getLastFetchedAt(animeId) }
    }

    override suspend fun replaceRelations(
        animeId: Long,
        groups: List<Pair<String, List<Long>>>,
        fetchedAt: Long,
    ) {
        handler.await(inTransaction = true) {
            anime_relationsQueries.deleteByAnimeId(animeId)
            var sortOrder = 0L
            groups.forEach { (name, relatedIds) ->
                relatedIds.forEach { relatedId ->
                    anime_relationsQueries.insert(
                        animeId = animeId,
                        relatedAnimeId = relatedId,
                        name = name,
                        sortOrder = sortOrder++,
                        lastFetchedAt = fetchedAt,
                    )
                }
            }
        }
    }

    override fun subscribeRelatedAnime(animeId: Long): Flow<List<AnimeRelationGroup>> {
        return handler
            .subscribeToList {
                anime_relationsQueries.getRelatedAnimeByAnimeId(
                    animeId,
                    AnimeMapper::mapRelatedAnime,
                )
            }
            .map { rows ->
                // The query is already ordered, and groupBy preserves encounter order.
                rows.groupBy({ it.first }, { it.second })
                    .map { (name, anime) -> AnimeRelationGroup(name, anime) }
            }
    }
}
