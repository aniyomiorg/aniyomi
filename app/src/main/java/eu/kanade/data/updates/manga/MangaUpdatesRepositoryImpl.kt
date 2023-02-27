package eu.kanade.data.updates.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.updates.manga.model.MangaUpdatesWithRelations
import eu.kanade.domain.updates.manga.repository.MangaUpdatesRepository
import kotlinx.coroutines.flow.Flow

class MangaUpdatesRepositoryImpl(
    private val databaseHandler: MangaDatabaseHandler,
) : MangaUpdatesRepository {

    override fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.updates(after, mangaUpdateWithRelationMapper)
        }
    }
}
