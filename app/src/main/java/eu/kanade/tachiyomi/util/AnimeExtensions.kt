package eu.kanade.tachiyomi.util

import android.content.Context
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.util.Date
import eu.kanade.domain.anime.model.Anime as DomainAnime

/**
 * Call before updating [Anime.thumbnail_url] to ensure old cover can be cleared from cache
 */
fun Anime.prepUpdateCover(coverCache: AnimeCoverCache, remoteAnime: SAnime, refreshSameUrl: Boolean) {
    // Never refresh covers if the new url is null, as the current url has possibly become invalid
    val newUrl = remoteAnime.thumbnail_url ?: return

    // Never refresh covers if the url is empty to avoid "losing" existing covers
    if (newUrl.isEmpty()) return

    if (!refreshSameUrl && thumbnail_url == newUrl) return

    when {
        toDomainAnime()!!.isLocal() -> {
            cover_last_modified = Date().time
        }
        toDomainAnime()!!.hasCustomCover(coverCache) -> {
            coverCache.deleteFromCache(this, false)
        }
        else -> {
            cover_last_modified = Date().time
            coverCache.deleteFromCache(this, false)
        }
    }
}

fun Anime.removeCovers(coverCache: AnimeCoverCache = Injekt.get()): Int {
    if (toDomainAnime()!!.isLocal()) return 0

    cover_last_modified = Date().time
    return coverCache.deleteFromCache(this, true)
}

fun DomainAnime.shouldDownloadNewEpisodes(dbCategories: List<Long>, prefs: PreferencesHelper): Boolean {
    if (!favorite) return false

    val categories = dbCategories.ifEmpty { listOf(0) }

    // Boolean to determine if user wants to automatically download new episodes.
    val downloadNewEpisode = prefs.downloadNewChapter().get()
    if (!downloadNewEpisode) return false

    val includedCategories = prefs.downloadNewEpisodeCategories().get().map { it.toLong() }
    val excludedCategories = prefs.downloadNewEpisodeCategoriesExclude().get().map { it.toLong() }

    // Default: Download from all categories
    if (includedCategories.isEmpty() && excludedCategories.isEmpty()) return true

    // In excluded category
    if (categories.any { it in excludedCategories }) return false

    // Included category not selected
    if (includedCategories.isEmpty()) return true

    // In included category
    return categories.any { it in includedCategories }
}

suspend fun DomainAnime.editCover(
    context: Context,
    stream: InputStream,
    updateAnime: UpdateAnime = Injekt.get(),
    coverCache: AnimeCoverCache = Injekt.get(),
): Boolean {
    return if (isLocal()) {
        LocalAnimeSource.updateCover(context, toDbAnime(), stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(toDbAnime(), stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    } else {
        // We should never reach this block
        false
    }
}
