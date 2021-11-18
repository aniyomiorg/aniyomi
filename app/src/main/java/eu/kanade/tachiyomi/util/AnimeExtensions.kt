package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.util.Date

fun Anime.isLocal() = source == LocalAnimeSource.ID

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
        isLocal() -> {
            cover_last_modified = Date().time
        }
        hasCustomCover(coverCache) -> {
            coverCache.deleteFromCache(this, false)
        }
        else -> {
            cover_last_modified = Date().time
            coverCache.deleteFromCache(this, false)
        }
    }
}

fun Anime.hasCustomCover(coverCache: AnimeCoverCache): Boolean {
    return coverCache.getCustomCoverFile(this).exists()
}

fun Anime.removeCovers(coverCache: AnimeCoverCache) {
    if (isLocal()) return

    cover_last_modified = Date().time
    coverCache.deleteFromCache(this, true)
}

fun Anime.updateCoverLastModified(db: AnimeDatabaseHelper) {
    cover_last_modified = Date().time
    db.updateAnimeCoverLastModified(this).executeAsBlocking()
}

fun Anime.shouldDownloadNewEpisodes(db: AnimeDatabaseHelper, prefs: PreferencesHelper): Boolean {
    if (!favorite) return false

    // Boolean to determine if user wants to automatically download new episodes.
    val downloadNew = prefs.downloadNew().get()
    if (!downloadNew) return false

    val categoriesToDownload = prefs.downloadNewCategoriesAnime().get().map(String::toInt)
    val categoriesToExclude = prefs.downloadNewCategoriesAnimeExclude().get().map(String::toInt)

    // Default: download from all categories
    if (categoriesToDownload.isEmpty() && categoriesToExclude.isEmpty()) return true

    // Get all categories, else default category (0)
    val categoriesForAnime =
        db.getCategoriesForAnime(this).executeAsBlocking()
            .mapNotNull { it.id }
            .takeUnless { it.isEmpty() } ?: listOf(0)

    // In excluded category
    if (categoriesForAnime.intersect(categoriesToExclude).isNotEmpty()) return false

    // In included category
    return categoriesForAnime.intersect(categoriesToDownload).isNotEmpty()
}
