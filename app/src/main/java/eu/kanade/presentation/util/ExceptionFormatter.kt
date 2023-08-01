package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.network.HttpException
import tachiyomi.domain.items.chapter.model.NoChaptersException
import tachiyomi.domain.items.episode.model.NoEpisodesException
import tachiyomi.domain.source.anime.model.AnimeSourceNotInstalledException
import tachiyomi.domain.source.manga.model.SourceNotInstalledException

context(Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is NoChaptersException, is NoEpisodesException -> return getString(R.string.no_results_found)
            is SourceNotInstalledException, is AnimeSourceNotInstalledException -> return getString(R.string.loader_not_implemented_error)
            is HttpException -> return "$message: ${getString(R.string.http_error_hint)}"
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }
