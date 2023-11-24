package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.online.LicensedEntryItemsException
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.system.isOnline
import tachiyomi.domain.items.chapter.model.NoChaptersException
import tachiyomi.domain.items.episode.model.NoEpisodesException
import tachiyomi.domain.source.anime.model.AnimeSourceNotInstalledException
import tachiyomi.domain.source.manga.model.SourceNotInstalledException
import java.net.UnknownHostException

context(Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return getString(R.string.exception_http, code)
            is UnknownHostException -> {
                return if (!isOnline()) {
                    getString(R.string.exception_offline)
                } else {
                    getString(R.string.exception_unknown_host, message)
                }
            }
            is NoChaptersException, is NoEpisodesException -> return getString(
                R.string.no_results_found,
            )
            is SourceNotInstalledException, is AnimeSourceNotInstalledException -> return getString(
                R.string.loader_not_implemented_error,
            )
            is LicensedEntryItemsException -> return getString(
                R.string.licensed_manga_chapters_error,
            )
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }
