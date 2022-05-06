package eu.kanade.data.anime

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow

class AnimeRepositoryImpl(
    private val databaseHandler: AnimeDatabaseHandler
) : AnimeRepository {

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>> {
        return databaseHandler.subscribeToList { animesQueries.getFavoriteBySourceId(sourceId, animeMapper) }
    }
}
