package mihon.domain.extensionrepo.manga.interactor


import kotlinx.coroutines.flow.Flow
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository

class GetMangaExtensionRepo(
    private val extensionRepoRepository: MangaExtensionRepoRepository,
) {
    fun subscribeAll(): Flow<List<ExtensionRepo>> = extensionRepoRepository.subscribeAll()

    suspend fun getAll(): List<ExtensionRepo> = extensionRepoRepository.getAll()
}
