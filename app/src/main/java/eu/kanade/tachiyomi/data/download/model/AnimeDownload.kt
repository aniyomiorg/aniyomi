package eu.kanade.tachiyomi.data.download.model

import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.AnimeHttpSource
import rx.subjects.PublishSubject

class AnimeDownload(val source: AnimeHttpSource, val anime: Anime, val episode: Episode) {

    var pages: List<Page>? = null

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
    private var statusSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    private var statusCallback: ((AnimeDownload) -> Unit)? = null

    val progress: Int
        get() {
            val pages = pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    fun setStatusSubject(subject: PublishSubject<AnimeDownload>?) {
        statusSubject = subject
    }

    fun setStatusCallback(f: ((AnimeDownload) -> Unit)?) {
        statusCallback = f
    }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }
}
