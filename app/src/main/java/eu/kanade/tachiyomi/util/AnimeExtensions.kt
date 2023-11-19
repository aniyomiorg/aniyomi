package eu.kanade.tachiyomi.util

import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.domain.entries.anime.model.toSAnime
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.source.local.entries.anime.isLocal
import tachiyomi.source.local.image.anime.LocalAnimeCoverManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.util.Date

/**
 * Call before updating [Anime.thumbnail_url] to ensure old cover can be cleared from cache
 */
fun Anime.prepUpdateCover(coverCache: AnimeCoverCache, remoteAnime: SAnime, refreshSameUrl: Boolean): Anime {
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

fun Anime.removeCovers(coverCache: AnimeCoverCache = Injekt.get()): Anime {
    if (isLocal()) return this
    return if (coverCache.deleteFromCache(this, true) > 0) {
        return copy(coverLastModified = Date().time)
    } else {
        this
    }
}

fun Anime.shouldDownloadNewEpisodes(dbCategories: List<Long>, preferences: DownloadPreferences): Boolean {
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

suspend fun Anime.editCover(
    coverManager: LocalAnimeCoverManager,
    stream: InputStream,
    updateAnime: UpdateAnime = Injekt.get(),
    coverCache: AnimeCoverCache = Injekt.get(),
) {
    if (isLocal()) {
        coverManager.update(toSAnime(), stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    } else if (favorite) {
        coverCache.setCustomCoverToCache(this, stream)
        updateAnime.awaitUpdateCoverLastModified(id)
    }
}
