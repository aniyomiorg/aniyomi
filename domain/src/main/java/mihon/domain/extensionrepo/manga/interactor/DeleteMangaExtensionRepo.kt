package mihon.domain.extensionrepo.manga.interactor

import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository

class DeleteMangaExtensionRepo(
    private val repository: MangaExtensionRepoRepository,
) {
    suspend fun await(baseUrl: String) {
        repository.deleteRepo(baseUrl)
    }
}
