package eu.kanade.data.updates.anime

import eu.kanade.data.handlers.anime.AnimeDatabaseHandler
import eu.kanade.domain.updates.anime.model.AnimeUpdatesWithRelations
import eu.kanade.domain.updates.anime.repository.AnimeUpdatesRepository
import kotlinx.coroutines.flow.Flow

class AnimeUpdatesRepositoryImpl(
    private val databaseHandler: AnimeDatabaseHandler,
) : AnimeUpdatesRepository {

    override fun subscribeAllAnimeUpdates(after: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            animeupdatesViewQueries.animeupdates(after, animeUpdateWithRelationMapper)
        }
    }
}
