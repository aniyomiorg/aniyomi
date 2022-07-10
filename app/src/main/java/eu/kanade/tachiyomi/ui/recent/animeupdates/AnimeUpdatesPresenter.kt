package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.os.Bundle
import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.anime.animeEpisodeMapper
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.recent.DateSectionItem
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.toDateKey
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
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
    private val downloadManager: AnimeDownloadManager by injectLazy()
    private val sourceManager: AnimeSourceManager by injectLazy()

    private val handler: AnimeDatabaseHandler by injectLazy()
    private val updateEpisode: UpdateEpisode by injectLazy()
    private val setSeenStatus: SetSeenStatus by injectLazy()

    private val relativeTime: Int = preferences.relativeTime().get()
    private val dateFormat: DateFormat = preferences.dateFormat()

    private val _updates: MutableStateFlow<List<AnimeUpdatesItem>> = MutableStateFlow(listOf())
    val updates: StateFlow<List<AnimeUpdatesItem>> = _updates.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        getUpdatesObservable()

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
    private fun getUpdatesObservable() {
        // Set date limit for recent episodes
        presenterScope.launchIO {
            val cal = Calendar.getInstance().apply {
                time = Date()
                add(Calendar.MONTH, -3)
            }

            handler
                .subscribeToList {
                    animesQueries.getRecentlyUpdated(after = cal.timeInMillis, animeEpisodeMapper)
                }
                .map { animeEpisode ->
                    val map = TreeMap<Date, MutableList<Pair<Anime, Episode>>> { d1, d2 -> d2.compareTo(d1) }
                    val byDate = animeEpisode.groupByTo(map) { it.second.dateFetch.toDateKey() }
                    byDate.flatMap { entry ->
                        val dateItem = DateSectionItem(entry.key, relativeTime, dateFormat)
                        entry.value
                            .sortedWith(compareBy({ it.second.dateFetch }, { it.second.episodeNumber })).asReversed()
                            .map { AnimeUpdatesItem(it.second, it.first, dateItem) }
                    }
                }
                .collectLatest { list ->
                    // TODO: remove this workaround when updates aren't cluttered anymore
                    _updates.value = list
                    list.forEach { item ->
                        // Find an active download for this episode.
                        val download = downloadManager.queue.find { it.episode.id == item.episode.id }

                        // If there's an active download, assign it, otherwise ask the manager if
                        // the chapter is downloaded and assign it to the status.
                        if (download != null) {
                            item.download = download
                        }
                    }
                    setDownloadedEpisodes(list)

                    _updates.value = list

                    // Set unseen episode count for bottom bar badge
                    preferences.unseenUpdatesCount().set(list.count { !it.episode.seen })
                }
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

            if (downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)) {
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
            val episodes = (view?.adapter?.currentItems ?: emptyList()).filterIsInstance<AnimeUpdatesItem>()
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
        presenterScope.launchIO {
            setSeenStatus.await(
                seen = seen,
                values = items
                    .map { it.episode }
                    .toTypedArray(),
            )
        }
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
        presenterScope.launchIO {
            val toUpdate = items.map {
                EpisodeUpdate(
                    bookmark = bookmarked,
                    id = it.episode.id,
                )
            }
            updateEpisode.awaitAll(toUpdate)
        }
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
