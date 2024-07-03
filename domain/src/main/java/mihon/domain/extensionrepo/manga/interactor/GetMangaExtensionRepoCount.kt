package mihon.domain.extensionrepo.manga.interactor

import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository

class GetMangaExtensionRepoCount(
    private val extensionRepoRepository: MangaExtensionRepoRepository,
) {
    fun subscribe() = extensionRepoRepository.getCount()
}
