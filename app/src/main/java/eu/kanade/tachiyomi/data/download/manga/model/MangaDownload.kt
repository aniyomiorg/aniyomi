package eu.kanade.tachiyomi.data.download.manga.model

import eu.kanade.domain.entries.manga.interactor.GetManga
import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.items.chapter.interactor.GetChapter
import eu.kanade.domain.items.chapter.model.Chapter
import eu.kanade.tachiyomi.source.manga.MangaSourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import rx.subjects.PublishSubject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class MangaDownload(
    val source: HttpSource,
    val manga: Manga,
    val chapter: Chapter,
    var pages: List<Page>? = null,
) {

    @Volatile
    @Transient
    var totalProgress: Int = 0

    @Volatile
    @Transient
    var downloadedImages: Int = 0

    @Volatile
    @Transient
    var status: State = State.NOT_DOWNLOADED
        set(status) {
            field = status
            statusSubject?.onNext(this)
            statusCallback?.invoke(this)
        }

    @Transient
    var statusSubject: PublishSubject<MangaDownload>? = null

    @Transient
    var statusCallback: ((MangaDownload) -> Unit)? = null

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }

    companion object {
        suspend fun fromChapterId(
            chapterId: Long,
            getChapter: GetChapter = Injekt.get(),
            getManga: GetManga = Injekt.get(),
            sourceManager: MangaSourceManager = Injekt.get(),
        ): MangaDownload? {
            val chapter = getChapter.await(chapterId) ?: return null
            val manga = getManga.await(chapter.mangaId) ?: return null
            val source = sourceManager.get(manga.source) as? HttpSource ?: return null

            return MangaDownload(source, manga, chapter)
        }
    }
}
