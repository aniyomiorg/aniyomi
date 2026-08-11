package mihon.domain.extension.anime.interactor

import mihon.domain.extension.anime.repository.AnimeExtensionStoreRepository

class UpdateAnimeExtensionStores(
    private val repository: AnimeExtensionStoreRepository,
) {
    suspend operator fun invoke() {
        repository.refreshAll()
    }
}
