package eu.kanade.domain.updates.repository

import eu.kanade.domain.updates.model.UpdatesWithRelations
import kotlinx.coroutines.flow.Flow

interface UpdatesRepository {

    fun subscribeAllMangaUpdates(after: Long): Flow<List<UpdatesWithRelations>>
}
