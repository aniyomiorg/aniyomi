package eu.kanade.tachiyomi.util

import android.content.Context
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.util.Date
import eu.kanade.domain.anime.model.Anime as DomainAnime

/**
 * Call before updating [Anime.thumbnail_url] to ensure old cover can be cleared from cache
 */
fun DomainAnime.prepUpdateCover(coverCache: AnimeCoverCache, remoteAnime: SAnime, refreshSameUrl: Boolean): DomainAnime {
    // Never refresh covers if the new url is null, as the current url has possibly become invalid
    val newUrl = remoteAnime.thumbnail_url ?: return this

    // Never refresh covers if the url is empty to avoid "losing" existing covers
    if (newUrl.isEmpty()) return this

    if (!refreshSameUrl && thumbnailUrl == newUrl) return this

    return when {
        isLocal() -> {
            this.copy(coverLastModified = Date().time)
        }
        hasCustomCover(coverCache) -> {
            coverCache.deleteFromCache(this, false)
            this
        }
        else -> {
            coverCache.deleteFromCache(this, false)
            this.copy(coverLastModified = Date().time)
        }
    }
}

fun Anime.removeCovers(coverCache: AnimeCoverCache = Injekt.get()): Int {
    if (toDomainAnime()!!.isLocal()) return 0

    cover_last_modified = Date().time
    return coverCache.deleteFromCache(this, true)
}

fun DomainAnime.removeCovers(coverCache: AnimeCoverCache = Injekt.get()): DomainAnime {
    if (isLocal()) return this
    coverCache.deleteFromCache(this, true)
    return copy(coverLastModified = Date().time)
}

fun DomainAnime.shouldDownloadNewEpisodes(dbCategories: List<Long>, preferences: DownloadPreferences): Boolean {
    if (!favorite) return false

    val categories = dbCategories.ifEmpty { listOf(0L) }

    // Boolean to determine if user wants to automatically download new episodes.
    val downloadNewEpisodes = preferences.downloadNewEpisodes().get()
    if (!downloadNewEpisodes) return false

    val includedCategories = preferences.downloadNewEpisodeCategories().get().map { it.toLong() }
    val excludedCategories = preferences.downloadNewEpisodeCategoriesExclude().get().map { it.toLong() }

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
) {
    if (isLocal()) {
        LocalAnimeSource.updateCover(context, toSAnime(), stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(toDbAnime(), stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    }
}
