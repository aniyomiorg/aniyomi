package mihon.domain.extensionrepo.anime.interactor

import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository

class GetAnimeExtensionRepoCount(
    private val extensionRepoRepository: AnimeExtensionRepoRepository,
) {
    fun subscribe() = extensionRepoRepository.getCount()
}
