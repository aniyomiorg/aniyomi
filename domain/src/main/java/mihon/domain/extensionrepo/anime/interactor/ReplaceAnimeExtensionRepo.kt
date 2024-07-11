package mihon.domain.extensionrepo.anime.interactor

import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo

class ReplaceAnimeExtensionRepo(
    private val repository: AnimeExtensionRepoRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        repository.replaceRepo(repo)
    }
}
