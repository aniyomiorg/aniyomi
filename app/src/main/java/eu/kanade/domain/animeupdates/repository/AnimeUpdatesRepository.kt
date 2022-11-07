package eu.kanade.domain.animeupdates.repository

import eu.kanade.domain.animeupdates.model.AnimeUpdatesWithRelations
import kotlinx.coroutines.flow.Flow

interface AnimeUpdatesRepository {

    fun subscribeAll(after: Long): Flow<List<AnimeUpdatesWithRelations>>
}
