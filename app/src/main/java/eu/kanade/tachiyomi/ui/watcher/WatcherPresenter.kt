package eu.kanade.tachiyomi.ui.watcher

import android.app.Application
import android.os.Bundle
import android.os.Environment
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.watcher.loader.EpisodeLoader
import eu.kanade.tachiyomi.ui.watcher.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.util.isLocal
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.takeBytes
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.updateCoverLastModified
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Presenter used by the activity to perform background operations.
 */
class WatcherPresenter(
    private val db: DatabaseHelper = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<WatcherActivity>() {

    /**
     * The anime loaded in the watcher. It can be null when instantiated for a short time.
     */
    var anime: Anime? = null
        private set

    /**
     * The episode id of the currently loaded episode. Used to restore from process kill.
     */
    private var episodeId = -1L

    /**
     * The episode loader for the loaded anime. It'll be null until [anime] is set.
     */
    private var loader: EpisodeLoader? = null

    /**
     * Subscription to prevent setting episodes as active from multiple threads.
     */
    private var activeEpisodeSubscription: Subscription? = null

    /**
     * Relay for currently active viewer episodes.
     */
    private val viewerEpisodesRelay = BehaviorRelay.create<ViewerEpisodes>()

    /**
     * Relay used when loading prev/next episode needed to lock the UI (with a dialog).
     */
    private val isLoadingAdjacentEpisodeRelay = BehaviorRelay.create<Boolean>()

    /**
     * Episode list for the active anime. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val episodeList by lazy {
        val anime = anime!!
        val dbEpisodes = db.getEpisodes(anime).executeAsBlocking()

        val selectedEpisode = dbEpisodes.find { it.id == episodeId }
            ?: error("Requested episode of id $episodeId not found in episode list")

        val episodesForWatcher =
            if (preferences.skipRead() || preferences.skipFiltered()) {
                val list = dbEpisodes
                    .filter {
                        if (preferences.skipRead() && it.read) {
                            return@filter false
                        } else if (preferences.skipFiltered()) {
                            if (
                                (anime.readFilter == Anime.SHOW_READ && !it.read) ||
                                (anime.readFilter == Anime.SHOW_UNREAD && it.read) ||
                                (
                                    anime.downloadedFilter == Anime.SHOW_DOWNLOADED &&
                                        !downloadManager.isEpisodeDownloaded(it, anime)
                                    ) ||
                                (anime.bookmarkedFilter == Anime.SHOW_BOOKMARKED && !it.bookmark)
                            ) {
                                return@filter false
                            }
                        }

                        true
                    }
                    .toMutableList()

                val find = list.find { it.id == episodeId }
                if (find == null) {
                    list.add(selectedEpisode)
                }
                list
            } else {
                dbEpisodes
            }

        when (anime.sorting) {
            Anime.SORTING_SOURCE -> EpisodeLoadBySource().get(episodesForWatcher)
            Anime.SORTING_NUMBER -> EpisodeLoadByNumber().get(episodesForWatcher, selectedEpisode)
            Anime.SORTING_UPLOAD_DATE -> EpisodeLoadByUploadDate().get(episodesForWatcher)
            else -> error("Unknown sorting method")
        }.map(::WatcherEpisode)
    }

    private var hasTrackers: Boolean = false
    private val checkTrackers: (Anime) -> Unit = { anime ->
        val tracks = db.getTracks(anime).executeAsBlocking()

        hasTrackers = tracks.size > 0
    }

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
     * Called when the presenter is destroyed. It saves the current progress and cleans up
     * references on the currently active episodes.
     */
    override fun onDestroy() {
        super.onDestroy()
        val currentEpisodes = viewerEpisodesRelay.value
        if (currentEpisodes != null) {
            currentEpisodes.unref()
            saveEpisodeProgress(currentEpisodes.currEpisode)
            saveEpisodeHistory(currentEpisodes.currEpisode)
        }
    }

    /**
     * Called when the presenter instance is being saved. It saves the currently active episode
     * id and the last page read.
     */
    override fun onSave(state: Bundle) {
        super.onSave(state)
        val currentEpisode = getCurrentEpisode()
        if (currentEpisode != null) {
            currentEpisode.requestedPage = currentEpisode.episode.last_page_read
            state.putLong(::episodeId.name, currentEpisode.episode.id!!)
        }
    }

    /**
     * Called when the user pressed the back button and is going to leave the watcher. Used to
     * trigger deletion of the downloaded episodes.
     */
    fun onBackPressed() {
        deletePendingEpisodes()
    }

    /**
     * Called when the activity is saved and not changing configurations. It updates the database
     * to persist the current progress of the active episode.
     */
    fun onSaveInstanceStateNonConfigurationChange() {
        val currentEpisode = getCurrentEpisode() ?: return
        saveEpisodeProgress(currentEpisode)
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
                WatcherActivity::setInitialEpisodeError
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

        val context = Injekt.get<Application>()
        val source = sourceManager.getOrStub(anime.source)
        loader = EpisodeLoader(context, downloadManager, anime, source)

        Observable.just(anime).subscribeLatestCache(WatcherActivity::setAnime)
        viewerEpisodesRelay.subscribeLatestCache(WatcherActivity::setEpisodes)
        isLoadingAdjacentEpisodeRelay.subscribeLatestCache(WatcherActivity::setProgressDialog)

        // Read episodeList from an io thread because it's retrieved lazily and would block main.
        activeEpisodeSubscription?.unsubscribe()
        activeEpisodeSubscription = Observable
            .fromCallable { episodeList.first { episodeId == it.episode.id } }
            .flatMap { getLoadObservable(loader!!, it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { _, _ ->
                    // Ignore onNext event
                },
                WatcherActivity::setInitialEpisodeError
            )
    }

    /**
     * Returns an observable that loads the given [episode] with this [loader]. This observable
     * handles main thread synchronization and updating the currently active episodes on
     * [viewerEpisodesRelay], however callers must ensure there won't be more than one
     * subscription active by unsubscribing any existing [activeEpisodeSubscription] before.
     * Callers must also handle the onError event.
     */
    private fun getLoadObservable(
        loader: EpisodeLoader,
        episode: WatcherEpisode
    ): Observable<ViewerEpisodes> {
        return loader.loadEpisode(episode)
            .andThen(
                Observable.fromCallable {
                    val episodePos = episodeList.indexOf(episode)

                    ViewerEpisodes(
                        episode,
                        episodeList.getOrNull(episodePos - 1),
                        episodeList.getOrNull(episodePos + 1)
                    )
                }
            )
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { newEpisodes ->
                val oldEpisodes = viewerEpisodesRelay.value

                // Add new references first to avoid unnecessary recycling
                newEpisodes.ref()
                oldEpisodes?.unref()

                viewerEpisodesRelay.call(newEpisodes)
            }
    }

    /**
     * Called when the user changed to the given [episode] when changing pages from the viewer.
     * It's used only to set this episode as active.
     */
    private fun loadNewEpisode(episode: WatcherEpisode) {
        val loader = loader ?: return

        Timber.d("Loading ${episode.episode.url}")

        activeEpisodeSubscription?.unsubscribe()
        activeEpisodeSubscription = getLoadObservable(loader, episode)
            .toCompletable()
            .onErrorComplete()
            .subscribe()
            .also(::add)
    }

    /**
     * Called when the user is going to load the prev/next episode through the menu button. It
     * sets the [isLoadingAdjacentEpisodeRelay] that the view uses to prevent any further
     * interaction until the episode is loaded.
     */
    private fun loadAdjacent(episode: WatcherEpisode) {
        val loader = loader ?: return

        Timber.d("Loading adjacent ${episode.episode.url}")

        activeEpisodeSubscription?.unsubscribe()
        activeEpisodeSubscription = getLoadObservable(loader, episode)
            .doOnSubscribe { isLoadingAdjacentEpisodeRelay.call(true) }
            .doOnUnsubscribe { isLoadingAdjacentEpisodeRelay.call(false) }
            .subscribeFirst(
                { view, _ ->
                    view.moveToPageIndex(0)
                },
                { _, _ ->
                    // Ignore onError event, viewers handle that state
                }
            )
    }

    /**
     * Called when the viewers decide it's a good time to preload a [episode] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    private fun preload(episode: WatcherEpisode) {
        if (episode.state != WatcherEpisode.State.Wait && episode.state !is WatcherEpisode.State.Error) {
            return
        }

        Timber.d("Preloading ${episode.episode.url}")

        val loader = loader ?: return

        loader.loadEpisode(episode)
            .observeOn(AndroidSchedulers.mainThread())
            // Update current episodes whenever a episode is preloaded
            .doOnCompleted { viewerEpisodesRelay.value?.let(viewerEpisodesRelay::call) }
            .onErrorComplete()
            .subscribe()
            .also(::add)
    }

    /**
     * Called every time a page changes on the watcher. Used to mark the flag of episodes being
     * read, update tracking services, enqueue downloaded episode deletion, and updating the active episode if this
     * [page]'s episode is different from the currently active.
     */
    fun onPageSelected(page: WatcherPage) {
        val currentEpisodes = viewerEpisodesRelay.value ?: return

        val selectedEpisode = page.episode

        // Save last page read and mark as read if needed
        selectedEpisode.episode.last_page_read = page.index
        val shouldTrack = !preferences.incognitoMode().get() || hasTrackers
        if (selectedEpisode.pages?.lastIndex == page.index && shouldTrack) {
            selectedEpisode.episode.read = true
            updateTrackEpisodeRead(selectedEpisode)
            deleteEpisodeIfNeeded(selectedEpisode)
            deleteEpisodeFromDownloadQueue(currentEpisodes.currEpisode)
        }

        if (selectedEpisode != currentEpisodes.currEpisode) {
            Timber.d("Setting ${selectedEpisode.episode.url} as active")
            onEpisodeChanged(currentEpisodes.currEpisode)
            loadNewEpisode(selectedEpisode)
        }
    }

    /**
     * Removes [currentEpisode] from download queue
     * if setting is enabled and [currentEpisode] is queued for download
     */
    private fun deleteEpisodeFromDownloadQueue(currentEpisode: WatcherEpisode) {
        downloadManager.getEpisodeDownloadOrNull(currentEpisode.episode)?.let { download ->
            downloadManager.deletePendingDownload(download)
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last episode actually exists.
     * If both conditions are satisfied enqueues episode for delete
     * @param currentEpisode current episode, which is going to be marked as read.
     */
    private fun deleteEpisodeIfNeeded(currentEpisode: WatcherEpisode) {
        // Determine which episode should be deleted and enqueue
        val currentEpisodePosition = episodeList.indexOf(currentEpisode)
        val removeAfterReadSlots = preferences.removeAfterReadSlots()
        val episodeToDelete = episodeList.getOrNull(currentEpisodePosition - removeAfterReadSlots)
        // Check if deleting option is enabled and episode exists
        if (removeAfterReadSlots != -1 && episodeToDelete != null) {
            enqueueDeleteReadEpisodes(episodeToDelete)
        }
    }

    /**
     * Called when a episode changed from [fromEpisode] to [toEpisode]. It updates [fromEpisode]
     * on the database.
     */
    private fun onEpisodeChanged(fromEpisode: WatcherEpisode) {
        saveEpisodeProgress(fromEpisode)
        saveEpisodeHistory(fromEpisode)
    }

    /**
     * Saves this [episode] progress (last read page and whether it's read).
     * If incognito mode isn't on or has at least 1 tracker
     */
    private fun saveEpisodeProgress(episode: WatcherEpisode) {
        if (!preferences.incognitoMode().get() || hasTrackers) {
            db.updateEpisodeProgress(episode.episode).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    /**
     * Saves this [episode] last read history if incognito mode isn't on.
     */
    private fun saveEpisodeHistory(episode: WatcherEpisode) {
        if (!preferences.incognitoMode().get()) {
            val history = History.create(episode.episode).apply { last_read = Date().time }
            db.updateHistoryLastRead(history).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    /**
     * Called from the activity to preload the given [episode].
     */
    fun preloadEpisode(episode: WatcherEpisode) {
        preload(episode)
    }

    /**
     * Called from the activity to load and set the next episode as active.
     */
    fun loadNextEpisode() {
        val nextEpisode = viewerEpisodesRelay.value?.nextEpisode ?: return
        loadAdjacent(nextEpisode)
    }

    /**
     * Called from the activity to load and set the previous episode as active.
     */
    fun loadPreviousEpisode() {
        val prevEpisode = viewerEpisodesRelay.value?.prevEpisode ?: return
        loadAdjacent(prevEpisode)
    }

    /**
     * Returns the currently active episode.
     */
    fun getCurrentEpisode(): WatcherEpisode? {
        return viewerEpisodesRelay.value?.currEpisode
    }

    /**
     * Bookmarks the currently active episode.
     */
    fun bookmarkCurrentEpisode(bookmarked: Boolean) {
        if (getCurrentEpisode()?.episode == null) {
            return
        }

        val episode = getCurrentEpisode()?.episode!!
        episode.bookmark = bookmarked
        db.updateEpisodeProgress(episode).executeAsBlocking()
    }

    /**
     * Returns the viewer position used by this anime or the default one.
     */
    fun getAnimeViewer(resolveDefault: Boolean = true): Int {
        val anime = anime ?: return preferences.defaultViewer()
        return if (resolveDefault && anime.viewer == 0) preferences.defaultViewer() else anime.viewer
    }

    /**
     * Updates the viewer position for the open anime.
     */
    fun setAnimeViewer(viewer: Int) {
        val anime = anime ?: return
        anime.viewer = viewer
        db.updateAnimeViewer(anime).executeAsBlocking()

        Observable.timer(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribeFirst({ view, _ ->
                val currEpisodes = viewerEpisodesRelay.value
                if (currEpisodes != null) {
                    // Save current page
                    val currEpisode = currEpisodes.currEpisode
                    currEpisode.requestedPage = currEpisode.episode.last_page_read

                    // Emit anime and episodes to the new viewer
                    view.setAnime(anime)
                    view.setEpisodes(currEpisodes)
                }
            })
    }

    /**
     * Saves the image of this [page] in the given [directory] and returns the file location.
     */
    private fun saveImage(page: WatcherPage, directory: File, anime: Anime): File {
        val stream = page.stream!!
        val type = ImageUtil.findImageType(stream) ?: throw Exception("Not an image")

        directory.mkdirs()

        val episode = page.episode.episode

        // Build destination file.
        val filenameSuffix = " - ${page.number}.${type.extension}"
        val filename = DiskUtil.buildValidFilename(
            "${anime.title} - ${episode.name}".takeBytes(MAX_FILE_NAME_BYTES - filenameSuffix.byteSize())
        ) + filenameSuffix

        val destFile = File(directory, filename)
        stream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    /**
     * Saves the image of this [page] on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage(page: WatcherPage) {
        if (page.status != Page.READY) return
        val anime = anime ?: return
        val context = Injekt.get<Application>()

        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        // Pictures directory.
        val destDir = File(
            Environment.getExternalStorageDirectory().absolutePath +
                File.separator + Environment.DIRECTORY_PICTURES +
                File.separator + context.getString(R.string.app_name)
        )

        // Copy file in background.
        Observable.fromCallable { saveImage(page, destDir, anime) }
            .doOnNext { file ->
                DiskUtil.scanMedia(context, file)
                notifier.onComplete(file)
            }
            .doOnError { notifier.onError(it.message) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, file -> view.onSaveImageResult(SaveImageResult.Success(file)) },
                { view, error -> view.onSaveImageResult(SaveImageResult.Error(error)) }
            )
    }

    /**
     * Shares the image of this [page] and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped episode, in which case it's not possible to directly
     * get a path to the file and it has to be decompresssed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(page: WatcherPage) {
        if (page.status != Page.READY) return
        val anime = anime ?: return
        val context = Injekt.get<Application>()

        val destDir = File(context.cacheDir, "shared_image")

        Observable.fromCallable { destDir.deleteRecursively() } // Keep only the last shared file
            .map { saveImage(page, destDir, anime) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, file -> view.onShareImageResult(file, page) },
                { _, _ -> /* Empty */ }
            )
    }

    /**
     * Sets the image of this [page] as cover and notifies the UI of the result.
     */
    fun setAsCover(page: WatcherPage) {
        if (page.status != Page.READY) return
        val anime = anime ?: return
        val stream = page.stream ?: return

        Observable
            .fromCallable {
                if (anime.isLocal()) {
                    val context = Injekt.get<Application>()
                    LocalSource.updateCover(context, anime, stream())
                    anime.updateCoverLastModified(db)
                    R.string.cover_updated
                    SetAsCoverResult.Success
                } else {
                    if (anime.favorite) {
                        coverCache.setCustomCoverToCache(anime, stream())
                        anime.updateCoverLastModified(db)
                        SetAsCoverResult.Success
                    } else {
                        SetAsCoverResult.AddToLibraryFirst
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, result -> view.onSetAsCoverResult(result) },
                { view, _ -> view.onSetAsCoverResult(SetAsCoverResult.Error) }
            )
    }

    /**
     * Results of the set as cover feature.
     */
    enum class SetAsCoverResult {
        Success, AddToLibraryFirst, Error
    }

    /**
     * Results of the save image feature.
     */
    sealed class SaveImageResult {
        class Success(val file: File) : SaveImageResult()
        class Error(val error: Throwable) : SaveImageResult()
    }

    /**
     * Starts the service that updates the last episode read in sync services. This operation
     * will run in a background thread and errors are ignored.
     */
    private fun updateTrackEpisodeRead(watcherEpisode: WatcherEpisode) {
        if (!preferences.autoUpdateTrack()) return
        val anime = anime ?: return

        val episodeRead = watcherEpisode.episode.episode_number.toInt()

        val trackManager = Injekt.get<TrackManager>()

        launchIO {
            db.getTracks(anime).executeAsBlocking()
                .mapNotNull { track ->
                    val service = trackManager.getService(track.sync_id)
                    if (service != null && service.isLogged && episodeRead > track.last_episode_read) {
                        track.last_episode_read = episodeRead

                        // We want these to execute even if the presenter is destroyed and leaks
                        // for a while. The view can still be garbage collected.
                        async {
                            runCatching {
                                service.update(track)
                                db.insertTrack(track).executeAsBlocking()
                            }
                        }
                    } else {
                        null
                    }
                }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { Timber.w(it) }
        }
    }

    /**
     * Enqueues this [episode] to be deleted when [deletePendingEpisodes] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadEpisodes(episode: WatcherEpisode) {
        if (!episode.episode.read) return
        val anime = anime ?: return

        launchIO {
            downloadManager.enqueueDeleteEpisodes(listOf(episode.episode), anime)
        }
    }

    /**
     * Deletes all the pending episodes. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingEpisodes() {
        launchIO {
            downloadManager.deletePendingEpisodes()
        }
    }

    companion object {
        // Safe theoretical max filename size is 255 bytes and 1 char = 2-4 bytes (UTF-8)
        private const val MAX_FILE_NAME_BYTES = 250
    }
}
