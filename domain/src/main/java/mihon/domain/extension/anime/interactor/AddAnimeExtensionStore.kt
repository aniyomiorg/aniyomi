package mihon.domain.extension.anime.interactor

import mihon.domain.extension.anime.repository.AnimeExtensionStoreRepository

class AddAnimeExtensionStore(
    private val repository: AnimeExtensionStoreRepository,
) {
    suspend operator fun invoke(indexUrl: String): Result<Unit> {
        return repository.insert(indexUrl)
    }
}
