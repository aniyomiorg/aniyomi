package eu.kanade.data.updates.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.updates.model.UpdatesWithRelations
import eu.kanade.domain.updates.repository.UpdatesRepository
import kotlinx.coroutines.flow.Flow

class MangaUpdatesRepositoryImpl(
    private val databaseHandler: MangaDatabaseHandler,
) : UpdatesRepository {

    override fun subscribeAllMangaUpdates(after: Long): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.updates(after, mangaUpdateWithRelationMapper)
        }
    }
}
