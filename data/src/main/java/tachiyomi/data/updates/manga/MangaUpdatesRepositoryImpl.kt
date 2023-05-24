package tachiyomi.data.updates.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.domain.updates.manga.repository.MangaUpdatesRepository

class MangaUpdatesRepositoryImpl(
    private val databaseHandler: MangaDatabaseHandler,
) : MangaUpdatesRepository {

    override fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.updates(after, mangaUpdateWithRelationMapper)
        }
    }
}
