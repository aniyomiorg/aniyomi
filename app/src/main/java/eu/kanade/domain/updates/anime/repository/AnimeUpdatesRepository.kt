package eu.kanade.domain.updates.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations

interface AnimeUpdatesRepository {

    fun subscribeAllAnimeUpdates(after: Long): Flow<List<AnimeUpdatesWithRelations>>
}
