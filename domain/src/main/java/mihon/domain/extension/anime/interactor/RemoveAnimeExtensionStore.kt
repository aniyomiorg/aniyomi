package mihon.domain.extension.anime.interactor

import mihon.domain.extension.anime.repository.AnimeExtensionStoreRepository

class RemoveAnimeExtensionStore(
    private val repository: AnimeExtensionStoreRepository,
) {
    suspend operator fun invoke(indexUrl: String) {
        repository.remove(indexUrl)
    }
}
