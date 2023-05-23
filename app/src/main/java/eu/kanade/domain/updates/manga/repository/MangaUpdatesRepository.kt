package eu.kanade.domain.updates.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations

interface MangaUpdatesRepository {

    fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>>
}
