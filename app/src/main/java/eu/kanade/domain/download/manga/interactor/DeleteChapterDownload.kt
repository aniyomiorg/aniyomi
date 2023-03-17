package eu.kanade.domain.download.manga.interactor

import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.items.chapter.model.Chapter
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadManager
import eu.kanade.tachiyomi.source.manga.MangaSourceManager
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext

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
