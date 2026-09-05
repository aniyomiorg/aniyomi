package mihon.domain.extensionrepo.manga.interactor

import logcat.LogPriority
import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.service.ExtensionRepoService
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat

class CreateMangaExtensionRepo(
    private val repository: MangaExtensionRepoRepository,
    private val service: ExtensionRepoService,
) {
    suspend fun await(indexUrl: String): Result {
        // accept index.json or index.pb
        val entryUrl = indexUrl.toHttpUrlOrNull()
            ?.takeIf { it.scheme == "https" }
            ?.toString()
            ?.takeIf { it.endsWith(".json") || it.endsWith(".pb") }
            ?: return Result.InvalidUrl

        return service.fetchRepoDetails(entryUrl)?.let { insert(it) } ?: Result.InvalidUrl
    }

    private suspend fun insert(repo: ExtensionRepo): Result {
        return try {
            repository.insertRepo(
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
    private suspend fun handleInsertionError(repo: ExtensionRepo): Result {
        val repoExists = repository.getRepo(repo.baseUrl)
        if (repoExists != null) {
            return Result.RepoAlreadyExists
        }
        val matchingFingerprintRepo = repository.getRepoBySigningKeyFingerprint(repo.signingKeyFingerprint)
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
