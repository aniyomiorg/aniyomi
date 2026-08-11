package mihon.domain.extension.anime.interactor

import mihon.domain.extension.anime.repository.AnimeExtensionStoreRepository

class GetAnimeExtensionStoreCountAsFlow(
    private val repository: AnimeExtensionStoreRepository,
) {
    operator fun invoke() = repository.getCountAsFlow()
}
