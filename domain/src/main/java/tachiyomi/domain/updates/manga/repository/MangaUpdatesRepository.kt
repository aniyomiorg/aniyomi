package tachiyomi.domain.updates.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations

interface MangaUpdatesRepository {

    suspend fun awaitWithRead(read: Boolean, after: Long): List<MangaUpdatesWithRelations>

    fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>>

    fun subscribeWithRead(read: Boolean, after: Long): Flow<List<MangaUpdatesWithRelations>>
}
