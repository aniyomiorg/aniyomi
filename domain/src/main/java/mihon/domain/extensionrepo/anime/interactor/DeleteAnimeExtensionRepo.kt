package mihon.domain.extensionrepo.anime.interactor

import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository

class DeleteAnimeExtensionRepo(
    private val extensionRepoRepository: AnimeExtensionRepoRepository,
) {
    suspend fun await(baseUrl: String) {
        extensionRepoRepository.deleteRepository(baseUrl)
    }
}
