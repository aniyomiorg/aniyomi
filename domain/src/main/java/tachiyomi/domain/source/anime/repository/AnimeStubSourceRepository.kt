
package tachiyomi.domain.source.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.StubAnimeSource

interface AnimeStubSourceRepository {
    fun subscribeAllAnime(): Flow<List<StubAnimeSource>>

    suspend fun getStubAnimeSource(id: Long): StubAnimeSource?

    suspend fun upsertStubAnimeSource(id: Long, lang: String, name: String)
}
