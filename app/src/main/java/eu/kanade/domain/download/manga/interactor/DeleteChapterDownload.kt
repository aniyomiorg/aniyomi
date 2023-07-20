package eu.kanade.domain.download.manga.interactor

import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.source.manga.service.MangaSourceManager

class DeleteChapterDownload(
    private val sourceManager: MangaSourceManager,
    private val downloadManager: MangaDownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) = withNonCancellableContext {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
