package eu.kanade.data.anime

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority

class AnimeRepositoryImpl(
    private val databaseHandler: AnimeDatabaseHandler,
) : AnimeRepository {

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>> {
        return databaseHandler.subscribeToList { animesQueries.getFavoriteBySourceId(sourceId, animeMapper) }
    }

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            databaseHandler.await { animesQueries.resetViewerFlags() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
