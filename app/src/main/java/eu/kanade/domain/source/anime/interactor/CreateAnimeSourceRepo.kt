package eu.kanade.domain.source.anime.interactor

import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.preference.plusAssign

class CreateAnimeSourceRepo(private val preferences: SourcePreferences) {

    fun await(name: String): Result {
        // Do not allow invalid formats
        if (!name.matches(repoRegex) || name.startsWith(OFFICIAL_REPO_BASE_URL)) {
            return Result.InvalidUrl
        }

        preferences.animeExtensionRepos() += name.substringBeforeLast("/index.min.json")

        return Result.Success
    }

    sealed interface Result {
        data object InvalidUrl : Result
        data object Success : Result
    }
}

const val OFFICIAL_REPO_BASE_URL = "https://raw.githubusercontent.com/aniyomiorg/aniyomi-extensions/repo"
private val repoRegex = """^https://.*/index\.min\.json$""".toRegex()
