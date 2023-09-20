package eu.kanade.domain.items.chapter.model

import eu.kanade.domain.entries.manga.model.downloadedFilter
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.ui.entries.manga.ChapterItem
import tachiyomi.domain.entries.applyFilter
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.items.chapter.service.getChapterSort
import tachiyomi.source.local.entries.manga.isLocal

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
fun List<Chapter>.applyFilters(manga: Manga, downloadManager: MangaDownloadManager): List<Chapter> {
    val isLocalManga = manga.isLocal()
    val unreadFilter = manga.unreadFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter

    return filter { chapter -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { chapter -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { chapter ->
            applyFilter(downloadedFilter) {
                val downloaded = downloadManager.isChapterDownloaded(chapter.name, chapter.scanlator, manga.title, manga.source)
                downloaded || isLocalManga
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
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalManga } }
        .sortedWith { (chapter1), (chapter2) -> getChapterSort(manga).invoke(chapter1, chapter2) }
}
