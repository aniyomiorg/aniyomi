package eu.kanade.tachiyomi.ui.player

import android.app.Application
import android.os.Bundle
import android.webkit.WebSettings
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.*

class NewPlayerPresenter(
    private val db: AnimeDatabaseHelper = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val delayedTrackingStore: DelayedTrackingStore = Injekt.get(),
) : BasePresenter<NewPlayerActivity>() {
    /**
     * The anime loaded in the player. It can be null when instantiated for a short time.
     */
    var anime: Anime? = null
        private set

    /**
     * The episode id of the currently loaded episode. Used to restore from process kill.
     */
    private var episodeId = -1L

    private var source: AnimeSource? = null

    var currentEpisode: Episode? = null

    private var currentVideoList: List<Video>? = null

    /**
     * Episode list for the active anime. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    val episodeList by lazy {
        val anime = anime!!
        val dbEpisodes = db.getEpisodes(anime).executeAsBlocking()

        val selectedEpisode = dbEpisodes.find { it.id == episodeId }
            ?: error("Requested episode of id $episodeId not found in episode list")

        val episodesForPlayer = when {
            preferences.skipRead() || preferences.skipFiltered() -> {
                val filteredEpisodes = dbEpisodes.filterNot {
                    when {
                        preferences.skipRead() && it.seen -> true
                        preferences.skipFiltered() -> {
                            anime.seenFilter == Anime.EPISODE_SHOW_SEEN && !it.seen ||
                                anime.seenFilter == Anime.EPISODE_SHOW_UNSEEN && it.seen ||
                                anime.downloadedFilter == Anime.EPISODE_SHOW_DOWNLOADED && !downloadManager.isEpisodeDownloaded(it, anime) ||
                                anime.downloadedFilter == Anime.EPISODE_SHOW_NOT_DOWNLOADED && downloadManager.isEpisodeDownloaded(it, anime) ||
                                anime.bookmarkedFilter == Anime.EPISODE_SHOW_BOOKMARKED && !it.bookmark ||
                                anime.bookmarkedFilter == Anime.EPISODE_SHOW_NOT_BOOKMARKED && it.bookmark
                        }
                        else -> false
                    }
                }

                if (filteredEpisodes.any { it.id == episodeId }) {
                    filteredEpisodes
                } else {
                    filteredEpisodes + listOf(selectedEpisode)
                }
            }
            else -> dbEpisodes
        }

        episodesForPlayer
            .sortedWith(getEpisodeSort(anime, sortDescending = false))
    }

    fun getCurrentEpisodeIndex(): Int {
        return episodeList.indexOfFirst { currentEpisode?.id == it.id }
    }

    private var hasTrackers: Boolean = false
    private val checkTrackers: (Anime) -> Unit = { anime ->
        val tracks = db.getTracks(anime).executeAsBlocking()

        hasTrackers = tracks.size > 0
    }

    private val incognitoMode = preferences.incognitoMode().get()

    /**
     * Called when the presenter is created. It retrieves the saved active episode if the process
     * was restored.
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        if (savedState != null) {
            episodeId = savedState.getLong(::episodeId.name, -1)
        }
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        return anime == null
    }

    /**
     * Initializes this presenter with the given [animeId] and [initialEpisodeId]. This method will
     * fetch the anime from the database and initialize the initial episode.
     */
    fun init(animeId: Long, initialEpisodeId: Long) {
        if (!needsInit()) return

        db.getAnime(animeId).asRxObservable()
            .first()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { init(it, initialEpisodeId) }
            .subscribeFirst(
                { _, _ ->
                    // Ignore onNext event
                },
                NewPlayerActivity::setInitialEpisodeError
            )
    }

    /**
     * Initializes this presenter with the given [anime] and [initialEpisodeId]. This method will
     * set the episode loader, view subscriptions and trigger an initial load.
     */
    private fun init(anime: Anime, initialEpisodeId: Long) {
        if (!needsInit()) return

        this.anime = anime
        if (episodeId == -1L) episodeId = initialEpisodeId

        checkTrackers(anime)

        source = sourceManager.getOrStub(anime.source)

        currentEpisode = episodeList.first { initialEpisodeId == it.id }
        launchIO {
            try {
                val currentEpisode = currentEpisode ?: throw Exception("bruh")
                EpisodeLoader.getLinks(currentEpisode, anime, source!!)
                    .subscribeFirst(
                        { activity, it ->
                            currentVideoList = it
                            if (source is AnimeHttpSource &&
                                !EpisodeLoader.isDownloaded(currentEpisode, anime)
                            ) {
                                activity.setHttpOptions(getHeaders(it, source as AnimeHttpSource, activity))
                            }
                            activity.setVideoList(it)
                        },
                        NewPlayerActivity::setInitialEpisodeError
                    )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "error getting links" }
            }
        }
    }

    private fun getHeaders(videos: List<Video>, source: AnimeHttpSource, activity: NewPlayerActivity): Map<String, String> {
        val currentHeaders = videos.firstOrNull()?.headers
        val headers = currentHeaders?.toMultimap()
            ?.mapValues { it.value.getOrNull(0) ?: "" }
            ?.toMutableMap()
            ?: source.headers.toMultimap()
                .mapValues { it.value.getOrNull(0) ?: "" }
                .toMutableMap()
        if (headers["user-agent"] == null) {
            headers["user-agent"] = WebSettings.getDefaultUserAgent(activity)
        }
        return headers
    }

    fun nextEpisode() {
        val anime = anime ?: return
        val source = sourceManager.getOrStub(anime.source)

        val index = getCurrentEpisodeIndex()
        if (index == episodeList.lastIndex) return
        currentEpisode = episodeList[index + 1]
        launchIO {
            try {
                val currentEpisode = currentEpisode ?: throw Exception("bruh")
                EpisodeLoader.getLinks(currentEpisode, anime, source)
                    .subscribeFirst(
                        { activity, it ->
                            currentVideoList = it
                            activity.setVideoList(it)
                        },
                        NewPlayerActivity::setInitialEpisodeError
                    )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "error getting links" }
            }
        }
    }

    fun previousEpisode() {
        val anime = anime ?: return
        val source = sourceManager.getOrStub(anime.source)

        val index = getCurrentEpisodeIndex()
        if (index == 0) return
        currentEpisode = episodeList[index - 1]
        launchIO {
            try {
                val currentEpisode = currentEpisode ?: throw Exception("bruh")
                EpisodeLoader.getLinks(currentEpisode, anime, source)
                    .subscribeFirst(
                        { activity, it ->
                            currentVideoList = it
                            activity.setVideoList(it)
                        },
                        NewPlayerActivity::setInitialEpisodeError
                    )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "error getting links" }
            }
        }
    }

    fun saveEpisodeHistory() {
        val episode = currentEpisode ?: return
        val history = AnimeHistory.create(episode).apply { last_seen = Date().time }
        db.updateAnimeHistoryLastSeen(history).asRxCompletable()
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    fun saveEpisodeProgress(pos: Int?, duration: Int?) {
        if (incognitoMode) return
        val episode = currentEpisode ?: return
        val anime = anime ?: return
        val seconds = (pos ?: return) * 1000L
        val totalSeconds = (duration ?: return) * 1000L
        if (totalSeconds > 0L) {
            episode.last_second_seen = seconds
            episode.total_seconds = totalSeconds
            val progress = preferences.progressPreference()
            if (!episode.seen) episode.seen = episode.last_second_seen >= episode.total_seconds * progress
            val episodes = listOf(EpisodeItem(episode, anime))
            launchIO {
                db.updateEpisodesProgress(episodes).executeAsBlocking()
                if (preferences.autoUpdateTrack() && episode.seen) {
                    updateTrackEpisodeSeen(episode)
                }
                if (episode.seen) {
                    deleteEpisodeIfNeeded(episode)
                    deleteEpisodeFromDownloadQueue(episode)
                }
            }
        }
    }

    private fun deleteEpisodeFromDownloadQueue(episode: Episode) {
        downloadManager.getEpisodeDownloadOrNull(episode)?.let { download ->
            downloadManager.deletePendingDownload(download)
        }
    }

    private fun deleteEpisodeIfNeeded(episode: Episode) {
        val anime = anime ?: return
        // Determine which chapter should be deleted and enqueue
        val sortFunction: (Episode, Episode) -> Int = when (anime.sorting) {
            Anime.EPISODE_SORTING_SOURCE -> { c1, c2 -> c2.source_order.compareTo(c1.source_order) }
            Anime.EPISODE_SORTING_NUMBER -> { c1, c2 -> c1.episode_number.compareTo(c2.episode_number) }
            Anime.EPISODE_SORTING_UPLOAD_DATE -> { c1, c2 -> c1.date_upload.compareTo(c2.date_upload) }
            else -> throw NotImplementedError("Unknown sorting method")
        }

        val episodes = db.getEpisodes(anime).executeAsBlocking()
            .sortedWith { e1, e2 -> sortFunction(e1, e2) }

        val currentEpisodePosition = episodes.indexOf(episode)
        val removeAfterReadSlots = preferences.removeAfterReadSlots()
        val episodeToDelete = episodes.getOrNull(currentEpisodePosition - removeAfterReadSlots)

        // Check if deleting option is enabled and chapter exists
        if (removeAfterReadSlots != -1 && episodeToDelete != null) {
            enqueueDeleteSeenEpisodes(episodeToDelete)
        }
    }

    private fun enqueueDeleteSeenEpisodes(episode: Episode) {
        val anime = anime ?: return
        if (!episode.seen) return

        launchIO {
            downloadManager.enqueueDeleteEpisodes(listOf(episode), anime)
        }
    }

    private fun updateTrackEpisodeSeen(episode: Episode) {
        if (!preferences.autoUpdateTrack()) return
        val anime = anime ?: return

        val episodeSeen = episode.episode_number

        val trackManager = Injekt.get<TrackManager>()
        val context = Injekt.get<Application>()

        launchIO {
            db.getTracks(anime).executeAsBlocking()
                .mapNotNull { track ->
                    val service = trackManager.getService(track.sync_id)
                    if (service != null && service.isLogged && episodeSeen > track.last_episode_seen) {
                        track.last_episode_seen = episodeSeen

                        // We want these to execute even if the presenter is destroyed and leaks
                        // for a while. The view can still be garbage collected.
                        async {
                            runCatching {
                                if (context.isOnline()) {
                                    service.update(track, true)
                                    db.insertTrack(track).executeAsBlocking()
                                } else {
                                    delayedTrackingStore.addItem(track)
                                    DelayedTrackingUpdateJob.setupTask(context)
                                }
                            }
                        }
                    } else {
                        null
                    }
                }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.INFO, it) }
        }
    }

    /**
     * Deletes all the pending episodes. This operation will run in a background thread and errors
     * are ignored.
     */
    fun deletePendingEpisodes() {
        launchIO {
            downloadManager.deletePendingEpisodes()
        }
    }
}
