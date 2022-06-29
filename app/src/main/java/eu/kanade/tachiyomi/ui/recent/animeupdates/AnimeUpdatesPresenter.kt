package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.os.Bundle
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.AnimeEpisode
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TreeMap

class AnimeUpdatesPresenter : BasePresenter<AnimeUpdatesController>() {

    val preferences: PreferencesHelper by injectLazy()
    private val db: AnimeDatabaseHelper by injectLazy()
    private val downloadManager: AnimeDownloadManager by injectLazy()
    private val sourceManager: AnimeSourceManager by injectLazy()

    private val relativeTime: Int = preferences.relativeTime().get()
    private val dateFormat: DateFormat = preferences.dateFormat()

    /**
     * List containing episode and anime information
     */
    private var episodes: List<AnimeUpdatesItem> = emptyList()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        getUpdatesObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(AnimeUpdatesController::onNextRecentEpisodes)

        downloadManager.queue.getStatusObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { view, it ->
                    onDownloadStatusChange(it)
                    view.onEpisodeDownloadUpdate(it)
                },
                { _, error ->
                    logcat(LogPriority.ERROR, error)
                },
            )

        downloadManager.queue.getProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(AnimeUpdatesController::onEpisodeDownloadUpdate) { _, error ->
                logcat(LogPriority.ERROR, error)
            }

        downloadManager.queue.getPreciseProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureLatest()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(AnimeUpdatesController::onEpisodeDownloadUpdate) { _, error ->
                logcat(LogPriority.ERROR, error)
            }
    }

    /**
     * Get observable containing recent episodes and date
     *
     * @return observable containing recent episodes and date
     */
    private fun getUpdatesObservable(): Observable<List<AnimeUpdatesItem>> {
        // Set date limit for recent episodes
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.MONTH, -3)
        }

        return db.getRecentEpisodes(cal.time).asRxObservable()
            // Convert to a list of recent episodes.
            .map { animeEpisodes ->
                val map = TreeMap<Date, MutableList<AnimeEpisode>> { d1, d2 -> d2.compareTo(d1) }
                val byDay = animeEpisodes
                    .groupByTo(map) { it.episode.date_fetch.toDateKey() }
                byDay.flatMap { entry ->
                    val dateItem = DateSectionItem(entry.key, relativeTime, dateFormat)
                    entry.value
                        .sortedWith(compareBy({ it.episode.date_fetch }, { it.episode.episode_number })).asReversed()
                        .map { AnimeUpdatesItem(it.episode, it.anime, dateItem) }
                }
            }
            .doOnNext { list ->
                list.forEach { item ->
                    // Find an active download for this episode.
                    val download = downloadManager.queue.find { it.episode.id == item.episode.id }

                    // If there's an active download, assign it, otherwise ask the manager if
                    // the episode is downloaded and assign it to the status.
                    if (download != null) {
                        item.download = download
                    }
                }
                setDownloadedEpisodes(list)
                episodes = list

                // Set unseen episode count for bottom bar badge
                preferences.unseenUpdatesCount().set(list.count { !it.seen })
            }
    }

    /**
     * Finds and assigns the list of downloaded episodes.
     *
     * @param items the list of episode from the database.
     */
    private fun setDownloadedEpisodes(items: List<AnimeUpdatesItem>) {
        for (item in items) {
            val anime = item.anime
            val episode = item.episode

            if (downloadManager.isEpisodeDownloaded(episode, anime)) {
                item.status = AnimeDownload.State.DOWNLOADED
            }
        }
    }

    /**
     * Update status of episodes.
     *
     * @param download download object containing progress.
     */
    private fun onDownloadStatusChange(download: AnimeDownload) {
        // Assign the download to the model object.
        if (download.status == AnimeDownload.State.QUEUE) {
            val episode = episodes.find { it.episode.id == download.episode.id }
            if (episode != null && episode.download == null) {
                episode.download = download
            }
        }
    }

    fun startDownloadingNow(episode: Episode) {
        downloadManager.startDownloadNow(episode.id)
    }

    /**
     * Mark selected episode as read
     *
     * @param items list of selected episodes
     * @param seen seen/unseen status
     */
    fun markEpisodeRead(items: List<AnimeUpdatesItem>, seen: Boolean) {
        val episodes = items.map { it.episode }
        episodes.forEach {
            it.seen = seen
            if (!seen) {
                it.last_second_seen = 0
            }
        }

        Observable.fromCallable { db.updateEpisodesProgress(episodes).executeAsBlocking() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Delete selected episodes
     *
     * @param episodes list of episodes
     */
    fun deleteEpisodes(episodes: List<AnimeUpdatesItem>) {
        Observable.just(episodes)
            .doOnNext { deleteEpisodesInternal(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ ->
                    view.onEpisodesDeleted()
                },
                AnimeUpdatesController::onEpisodesDeletedError,
            )
    }

    /**
     * Mark selected episodes as bookmarked
     * @param items list of selected episodes
     * @param bookmarked bookmark status
     */
    fun bookmarkEpisodes(items: List<AnimeUpdatesItem>, bookmarked: Boolean) {
        val episodes = items.map { it.episode }
        episodes.forEach {
            it.bookmark = bookmarked
        }

        Observable.fromCallable { db.updateEpisodesProgress(episodes).executeAsBlocking() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Download selected episodes
     * @param items list of recent episodes seleted.
     */
    fun downloadEpisodes(items: List<AnimeUpdatesItem>) {
        items.forEach { downloadManager.downloadEpisodes(it.anime, listOf(it.episode)) }
    }

    /**
     * Download selected episodes
     * @param items list of recent episodes seleted.
     */
    fun downloadEpisodesExternally(items: List<AnimeUpdatesItem>) {
        items.forEach { downloadManager.downloadEpisodesAlt(it.anime, listOf(it.episode)) }
    }

    /**
     * Delete selected episodes
     *
     * @param episodeItems episodes selected
     */
    private fun deleteEpisodesInternal(episodeItems: List<AnimeUpdatesItem>) {
        val itemsByAnime = episodeItems.groupBy { it.anime.id }
        for ((_, items) in itemsByAnime) {
            val anime = items.first().anime
            val source = sourceManager.get(anime.source) ?: continue
            val episodes = items.map { it.episode }

            downloadManager.deleteEpisodes(episodes, anime, source)
            items.forEach {
                it.status = AnimeDownload.State.NOT_DOWNLOADED
                it.download = null
            }
        }
    }
}
