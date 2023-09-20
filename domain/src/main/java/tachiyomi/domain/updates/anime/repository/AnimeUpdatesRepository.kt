package tachiyomi.domain.updates.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations

interface AnimeUpdatesRepository {

    suspend fun awaitWithSeen(seen: Boolean, after: Long): List<AnimeUpdatesWithRelations>

    fun subscribeAllAnimeUpdates(after: Long): Flow<List<AnimeUpdatesWithRelations>>

    fun subscribeWithSeen(seen: Boolean, after: Long): Flow<List<AnimeUpdatesWithRelations>>
}
