package eu.kanade.domain.items.chapter.model

import eu.kanade.domain.entries.TriStateFilter
import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.entries.manga.model.isLocal
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.entries.manga.ChapterItem
import eu.kanade.tachiyomi.util.chapter.getChapterSort

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
fun List<Chapter>.applyFilters(manga: Manga, downloadManager: MangaDownloadManager): List<Chapter> {
    val isLocalManga = manga.isLocal()
    val unreadFilter = manga.unreadFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter

    return filter { chapter ->
        when (unreadFilter) {
            TriStateFilter.DISABLED -> true
            TriStateFilter.ENABLED_IS -> !chapter.read
            TriStateFilter.ENABLED_NOT -> chapter.read
        }
    }
        .filter { chapter ->
            when (bookmarkedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> chapter.bookmark
                TriStateFilter.ENABLED_NOT -> !chapter.bookmark
            }
        }
        .filter { chapter ->
            val downloaded = downloadManager.isChapterDownloaded(chapter.name, chapter.scanlator, manga.title, manga.source)
            val downloadState = when {
                downloaded -> MangaDownload.State.DOWNLOADED
                else -> MangaDownload.State.NOT_DOWNLOADED
            }
            when (downloadedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> downloadState == MangaDownload.State.DOWNLOADED || isLocalManga
                TriStateFilter.ENABLED_NOT -> downloadState != MangaDownload.State.DOWNLOADED && !isLocalManga
            }
        }
        .sortedWith(getChapterSort(manga))
}

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
fun List<ChapterItem>.applyFilters(manga: Manga): Sequence<ChapterItem> {
    val isLocalManga = manga.isLocal()
    val unreadFilter = manga.unreadFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter
    return asSequence()
        .filter { (chapter) ->
            when (unreadFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> !chapter.read
                TriStateFilter.ENABLED_NOT -> chapter.read
            }
        }
        .filter { (chapter) ->
            when (bookmarkedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> chapter.bookmark
                TriStateFilter.ENABLED_NOT -> !chapter.bookmark
            }
        }
        .filter {
            when (downloadedFilter) {
                TriStateFilter.DISABLED -> true
                TriStateFilter.ENABLED_IS -> it.isDownloaded || isLocalManga
                TriStateFilter.ENABLED_NOT -> !it.isDownloaded && !isLocalManga
            }
        }
        .sortedWith { (chapter1), (chapter2) -> getChapterSort(manga).invoke(chapter1, chapter2) }
}
