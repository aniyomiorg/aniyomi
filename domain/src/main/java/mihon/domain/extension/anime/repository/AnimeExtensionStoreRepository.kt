package mihon.domain.extension.anime.repository

import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.anime.model.AnimeExtensionStore

interface AnimeExtensionStoreRepository {
    suspend fun insert(indexUrl: String): Result<Unit>

    suspend fun insertFromPreference(indexUrl: String, name: String)

    suspend fun refreshAll()

    suspend fun fetchExtensions(): List<AnimeExtension.Available>

    suspend fun getAll(): List<AnimeExtensionStore>

    fun getAllAsFlow(): Flow<List<AnimeExtensionStore>>

    fun getCountAsFlow(): Flow<Long>

    suspend fun remove(indexUrl: String)
}
