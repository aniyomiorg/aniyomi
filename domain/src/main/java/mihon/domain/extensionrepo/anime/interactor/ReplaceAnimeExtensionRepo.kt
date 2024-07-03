package mihon.domain.extensionrepo.anime.interactor

import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository

class ReplaceAnimeExtensionRepo(
    private val extensionRepoRepository: AnimeExtensionRepoRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        extensionRepoRepository.replaceRepository(repo)
    }
}
