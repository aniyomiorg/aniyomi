package eu.kanade.domain.updates.anime.repository

import eu.kanade.domain.updates.anime.model.AnimeUpdatesWithRelations
import kotlinx.coroutines.flow.Flow

interface AnimeUpdatesRepository {

    fun subscribeAllAnimeUpdates(after: Long): Flow<List<AnimeUpdatesWithRelations>>
}
