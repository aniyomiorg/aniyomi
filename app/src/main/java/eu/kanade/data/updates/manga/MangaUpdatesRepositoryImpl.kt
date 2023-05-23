package eu.kanade.data.updates.manga

import eu.kanade.domain.updates.manga.repository.MangaUpdatesRepository
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations

class MangaUpdatesRepositoryImpl(
    private val databaseHandler: MangaDatabaseHandler,
) : MangaUpdatesRepository {

    override fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.updates(after, mangaUpdateWithRelationMapper)
        }
    }
}
