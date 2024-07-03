package mihon.domain.extensionrepo.anime.interactor

import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository

class DeleteAnimeExtensionRepo(
    private val repository: AnimeExtensionRepoRepository,
) {
    suspend fun await(baseUrl: String) {
        repository.deleteRepo(baseUrl)
    }
}
