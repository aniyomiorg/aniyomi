package mihon.domain.extensionrepo.anime.interactor

import kotlinx.coroutines.flow.Flow
import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo

class GetAnimeExtensionRepo(
    private val extensionRepoRepository: AnimeExtensionRepoRepository,
) {
    fun subscribeAll(): Flow<List<ExtensionRepo>> = extensionRepoRepository.subscribeAll()

    suspend fun getAll(): List<ExtensionRepo> = extensionRepoRepository.getAll()
}
