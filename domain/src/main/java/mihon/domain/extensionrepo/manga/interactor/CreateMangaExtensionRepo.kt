package mihon.domain.extensionrepo.manga.interactor

import eu.kanade.tachiyomi.network.NetworkHelper
import logcat.LogPriority
import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoService
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy

class CreateMangaExtensionRepo(
    private val extensionRepoRepository: MangaExtensionRepoRepository,
) {
    private val repoRegex = """^https://.*/index\.min\.json$""".toRegex()

    private val networkService: NetworkHelper by injectLazy()

    private val client: OkHttpClient
        get() = networkService.client

    private val extensionRepoService = ExtensionRepoService(client)

    suspend fun await(repoUrl: String): Result {
        if (!repoUrl.matches(repoRegex)) {
            return Result.InvalidUrl
        }

        val baseUrl = repoUrl.removeSuffix("/index.min.json")
        return extensionRepoService.fetchRepoDetails(baseUrl)?.let { insert(it) } ?: Result.InvalidUrl
    }

    private suspend fun insert(repo: ExtensionRepo): Result {
        return try {
            extensionRepoRepository.insertRepository(
                repo.baseUrl,
                repo.name,
                repo.shortName,
                repo.website,
                repo.signingKeyFingerprint,
            )
            Result.Success
        } catch (e: SaveExtensionRepoException) {
            logcat(LogPriority.WARN, e) { "SQL Conflict attempting to add new manga repository ${repo.baseUrl}" }
            return handleInsertionError(repo)
        }
    }

    /**
     * Error Handler for insert when there are trying to create new repositories
     *
     * SaveExtensionRepoException doesn't provide constraint info in exceptions.
     * First check if the conflict was on primary key. if so return RepoAlreadyExists
     * Then check if the conflict was on fingerprint. if so Return DuplicateFingerprint
     * If neither are found, there was some other Error, and return Result.Error
     *
     * @param repo Extension Repo holder for passing to DB/Error Dialog
     */
    @Suppress("ReturnCount")
    private suspend fun handleInsertionError(repo: ExtensionRepo): Result {
        val repoExists = extensionRepoRepository.getRepository(repo.baseUrl)
        if (repoExists != null) {
            return Result.RepoAlreadyExists
        }
        val matchingFingerprintRepo =
            extensionRepoRepository.getRepositoryBySigningKeyFingerprint(repo.signingKeyFingerprint)
        if (matchingFingerprintRepo != null) {
            return Result.DuplicateFingerprint(matchingFingerprintRepo, repo)
        }
        return Result.Error
    }

    sealed interface Result {
        data class DuplicateFingerprint(val oldRepo: ExtensionRepo, val newRepo: ExtensionRepo) : Result
        data object InvalidUrl : Result
        data object RepoAlreadyExists : Result
        data object Success : Result
        data object Error : Result
    }
}
