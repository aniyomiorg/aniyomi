package mihon.domain.extensionrepo.manga.interactor

import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo

class ReplaceMangaExtensionRepo(
    private val extensionRepoRepository: MangaExtensionRepoRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        extensionRepoRepository.replaceRepository(repo)
    }
}
