package mihon.domain.extensionrepo.manga.interactor

import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository

class DeleteMangaExtensionRepo(
    private val extensionRepoRepository: MangaExtensionRepoRepository,
) {
    suspend fun await(baseUrl: String) {
        extensionRepoRepository.deleteRepository(baseUrl)
    }
}
