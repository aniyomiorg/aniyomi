package eu.kanade.domain.extension.manga.interactor

import eu.kanade.domain.source.service.SourcePreferences
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.core.preference.plusAssign

class CreateMangaExtensionRepo(private val preferences: SourcePreferences) {

    fun await(name: String): Result {
        if (name.matches(githubRepoRegex)) {
            val rawUrl = name.toHttpUrl().newBuilder().apply {
                removePathSegment(2) // Remove /blob/
                host("raw.githubusercontent.com")
            }.build().toString()

            preferences.mangaExtensionRepos() += rawUrl.removeSuffix("/index.min.json")
            return Result.Success
        }

        // Do not allow invalid formats
        if (!name.matches(repoRegex)) {
            return Result.InvalidUrl
        }

        preferences.mangaExtensionRepos() += name.removeSuffix("/index.min.json")

        return Result.Success
    }

    sealed interface Result {
        data object InvalidUrl : Result
        data object Success : Result
    }
}

private val repoRegex = """^https://.*/index\.min\.json$""".toRegex()
private val githubRepoRegex = """https://github\.com/[^/]+/[^/]+/blob/(?:[^/]+/)+index\.min\.json$""".toRegex()
