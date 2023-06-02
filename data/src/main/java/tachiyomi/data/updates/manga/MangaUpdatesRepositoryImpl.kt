package tachiyomi.data.updates.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.domain.updates.manga.repository.MangaUpdatesRepository

class MangaUpdatesRepositoryImpl(
    private val databaseHandler: MangaDatabaseHandler,
) : MangaUpdatesRepository {

    override suspend fun awaitWithRead(read: Boolean, after: Long): List<MangaUpdatesWithRelations> {
        return databaseHandler.awaitList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                mapper = mangaUpdateWithRelationMapper,
            )
        }
    }

    override fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.updates(after, mangaUpdateWithRelationMapper)
        }
    }

    override fun subscribeWithRead(read: Boolean, after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                mapper = mangaUpdateWithRelationMapper,
            )
        }
    }
}
