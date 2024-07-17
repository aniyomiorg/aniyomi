package mihon.domain.extensionrepo.manga.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo

class GetMangaExtensionRepo(
    private val repository: MangaExtensionRepoRepository,
) {
    fun subscribeAll(): Flow<List<ExtensionRepo>> = repository.subscribeAll()

    suspend fun getAll(): List<ExtensionRepo> = repository.getAll()
}
