package eu.kanade.data.updates.anime

import eu.kanade.domain.updates.anime.repository.AnimeUpdatesRepository
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations

class AnimeUpdatesRepositoryImpl(
    private val databaseHandler: AnimeDatabaseHandler,
) : AnimeUpdatesRepository {

    override fun subscribeAllAnimeUpdates(after: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            animeupdatesViewQueries.animeupdates(after, animeUpdateWithRelationMapper)
        }
    }
}
