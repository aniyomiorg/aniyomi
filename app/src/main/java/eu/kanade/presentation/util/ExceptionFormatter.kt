package eu.kanade.presentation.util

import android.content.Context
import tachiyomi.i18n.MR
import tachiyomi.core.i18n.localize
import tachiyomi.presentation.core.i18n.localize

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
            is HttpException -> return localize(MR.strings.exception_http, code)
            is UnknownHostException -> {
                return if (!isOnline()) {
                    localize(MR.strings.exception_offline)
                } else {
                    localize(MR.strings.exception_unknown_host, message ?: "")
                }
            }
            is NoChaptersException, is NoEpisodesException -> return localize(
                MR.strings.no_results_found,
            )
            is SourceNotInstalledException, is AnimeSourceNotInstalledException -> return localize(
                MR.strings.loader_not_implemented_error,
            )
            is LicensedEntryItemsException -> return localize(
                MR.strings.licensed_manga_chapters_error,
            )
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }
