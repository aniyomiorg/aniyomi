package tachiyomi.domain.updates.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations

interface MangaUpdatesRepository {

    suspend fun awaitWithRead(read: Boolean, after: Long, limit: Long): List<MangaUpdatesWithRelations>

    fun subscribeAllMangaUpdates(after: Long, limit: Long): Flow<List<MangaUpdatesWithRelations>>

    fun subscribeWithRead(read: Boolean, after: Long, limit: Long): Flow<List<MangaUpdatesWithRelations>>
}
