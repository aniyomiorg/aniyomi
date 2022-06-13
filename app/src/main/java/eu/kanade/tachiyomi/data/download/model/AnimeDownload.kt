package eu.kanade.tachiyomi.data.download.model

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import rx.subjects.PublishSubject

data class AnimeDownload(
    val source: AnimeHttpSource,
    val anime: Anime,
    val episode: Episode,
    val changeDownloader: Boolean = false,
    var video: Video? = null,
) {

    @Volatile
    @Transient
    var totalProgress: Int = 0
        set(progress) {
            field = progress
            progressSubject?.onNext(this)
            progressCallback?.invoke(this)
        }

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
    private var progressSubject: PublishSubject<AnimeDownload>? = null

    @Transient
    private var statusCallback: ((AnimeDownload) -> Unit)? = null

    @Transient
    private var progressCallback: ((AnimeDownload) -> Unit)? = null

    val progress: Int
        get() {
            val video = video ?: return 0
            return video.progress
        }

    fun setStatusSubject(subject: PublishSubject<AnimeDownload>?) {
        statusSubject = subject
    }

    fun setProgressSubject(subject: PublishSubject<AnimeDownload>?) {
        progressSubject = subject
    }

    fun setStatusCallback(f: ((AnimeDownload) -> Unit)?) {
        statusCallback = f
    }

    fun setProgressCallback(f: ((AnimeDownload) -> Unit)?) {
        progressCallback = f
    }

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }
}
