package eu.kanade.data.animeupdates

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.animeupdates.model.AnimeUpdatesWithRelations
import eu.kanade.domain.animeupdates.repository.AnimeUpdatesRepository
import kotlinx.coroutines.flow.Flow

class AnimeUpdatesRepositoryImpl(
    val databaseHandler: AnimeDatabaseHandler,
) : AnimeUpdatesRepository {

    override fun subscribeAll(after: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            animeupdatesViewQueries.animeupdates(after, updateWithRelationMapper)
        }
    }
}
