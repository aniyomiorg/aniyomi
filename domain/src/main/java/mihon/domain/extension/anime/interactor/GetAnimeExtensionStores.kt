package mihon.domain.extension.anime.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.extension.anime.model.AnimeExtensionStore
import mihon.domain.extension.anime.repository.AnimeExtensionStoreRepository

class GetAnimeExtensionStores(
    private val repository: AnimeExtensionStoreRepository,
) {
    suspend fun get(): List<AnimeExtensionStore> = repository.getAll()

    fun subscribe(): Flow<List<AnimeExtensionStore>> = repository.getAllAsFlow()
}
