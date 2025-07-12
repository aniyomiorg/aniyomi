package tachiyomi.data.updates.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations
import tachiyomi.domain.updates.anime.repository.AnimeUpdatesRepository

class AnimeUpdatesRepositoryImpl(
    private val databaseHandler: AnimeDatabaseHandler,
) : AnimeUpdatesRepository {

    override suspend fun awaitWithSeen(seen: Boolean, after: Long, limit: Long): List<AnimeUpdatesWithRelations> {
        return databaseHandler.awaitList {
            animeupdatesViewQueries.getUpdatesBySeenStatus(
                seen = seen,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    override fun subscribeAllAnimeUpdates(after: Long, limit: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            animeupdatesViewQueries.getRecentAnimeUpdates(
                after,
                limit,
                ::mapUpdatesWithRelations,
            )
        }
    }

    override fun subscribeWithSeen(seen: Boolean, after: Long, limit: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            animeupdatesViewQueries.getUpdatesBySeenStatus(
                seen = seen,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    private fun mapUpdatesWithRelations(
        animeId: Long,
        animeTitle: String,
        episodeId: Long,
        episodeName: String,
        scanlator: String?,
        seen: Boolean,
        bookmark: Boolean,
        fillermark: Boolean,
        lastSecondSeen: Long,
        totalSeconds: Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
    ): AnimeUpdatesWithRelations = AnimeUpdatesWithRelations(
        animeId = animeId,
        animeTitle = animeTitle,
        episodeId = episodeId,
        episodeName = episodeName,
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        fillermark = fillermark,
        lastSecondSeen = lastSecondSeen,
        totalSeconds = totalSeconds,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = AnimeCover(
            animeId = animeId,
            sourceId = sourceId,
            isAnimeFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )
}
