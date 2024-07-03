package mihon.domain.extensionrepo.anime.interactor

import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mihon.domain.extensionrepo.anime.repository.AnimeExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.service.ExtensionRepoService

class UpdateAnimeExtensionRepo(
    private val extensionRepoRepository: AnimeExtensionRepoRepository,
    networkService: NetworkHelper,
) {

    private val extensionRepoService = ExtensionRepoService(networkService.client)

    suspend fun awaitAll() = coroutineScope {
        extensionRepoRepository.getAll()
            .map { async { await(it) } }
            .awaitAll()
    }

    suspend fun await(repo: ExtensionRepo) {
        val newRepo = extensionRepoService.fetchRepoDetails(repo.baseUrl) ?: return
        if (
            repo.signingKeyFingerprint.startsWith("NOFINGERPRINT") ||
            repo.signingKeyFingerprint == newRepo.signingKeyFingerprint
        ) {
            extensionRepoRepository.upsertRepository(newRepo)
        }
    }
}
