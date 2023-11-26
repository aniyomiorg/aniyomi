package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.download.manga.MangaDownloadCache
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.source.local.entries.manga.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Returns a copy of the list with not downloaded chapters removed.
 */
fun List<Chapter>.filterDownloadedChapters(manga: Manga): List<Chapter> {
    if (manga.isLocal()) return this

    val downloadCache: MangaDownloadCache = Injekt.get()

    return filter {
        downloadCache.isChapterDownloaded(
            it.name,
            it.scanlator,
            manga.title,
            manga.source,
            false,
        )
    }
}
