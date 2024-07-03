package mihon.domain.extensionrepo.anime.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository

class GetAnimeExtensionRepo(
    private val extensionRepoRepository: AnimeExtensionRepoRepository,
) {
    fun subscribeAll(): Flow<List<ExtensionRepo>> = extensionRepoRepository.subscribeAll()

    suspend fun getAll(): List<ExtensionRepo> = extensionRepoRepository.getAll()
}
