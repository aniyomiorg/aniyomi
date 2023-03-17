package eu.kanade.domain.updates.manga.repository

import eu.kanade.domain.updates.manga.model.MangaUpdatesWithRelations
import kotlinx.coroutines.flow.Flow

interface MangaUpdatesRepository {

    fun subscribeAllMangaUpdates(after: Long): Flow<List<MangaUpdatesWithRelations>>
}
