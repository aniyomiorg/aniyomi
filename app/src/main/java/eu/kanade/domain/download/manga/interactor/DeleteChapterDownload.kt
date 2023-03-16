package eu.kanade.domain.download.manga.interactor

import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.items.chapter.model.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext

class DeleteChapterDownload(
    private val sourceManager: SourceManager,
    private val downloadManager: DownloadManager,
) {

    suspend fun awaitAll(manga: Manga, vararg chapters: Chapter) = withNonCancellableContext {
        sourceManager.get(manga.source)?.let { source ->
            downloadManager.deleteChapters(chapters.toList(), manga, source)
        }
    }
}
