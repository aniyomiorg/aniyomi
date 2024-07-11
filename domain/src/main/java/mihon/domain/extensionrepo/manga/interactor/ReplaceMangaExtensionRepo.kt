package mihon.domain.extensionrepo.manga.interactor

import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo

class ReplaceMangaExtensionRepo(
    private val repository: MangaExtensionRepoRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        repository.replaceRepo(repo)
    }
}
