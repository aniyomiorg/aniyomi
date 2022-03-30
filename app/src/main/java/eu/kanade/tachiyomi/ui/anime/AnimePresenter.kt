package eu.kanade.tachiyomi.ui.anime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import coil.imageLoader
import coil.memory.MemoryCache
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.toSAnime
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.episode.EpisodeSettingsHelper
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithSource
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithTrackServiceTwoWay
import eu.kanade.tachiyomi.util.isLocal
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.prepUpdateCover
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.getPicturesDir
import eu.kanade.tachiyomi.util.storage.getTempShareDir
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.updateCoverLastModified
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Date

class AnimePresenter(
    val anime: Anime,
    val source: AnimeSource,
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: AnimeDatabaseHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get()
) : BasePresenter<AnimeController>() {

    /**
     * Subscription to update the anime from the source.
     */
    private var fetchAnimeJob: Job? = null

    /**
     * List of episodes of the anime. It's always unfiltered and unsorted.
     */
    var allEpisodes: List<EpisodeItem> = emptyList()
        private set
    var filteredAndSortedEpisodes: List<EpisodeItem> = emptyList()

    /**
     * Subject of list of episodes to allow updating the view without going to DB.
     */
    private val episodesRelay: PublishRelay<List<EpisodeItem>> by lazy {
        PublishRelay.create<List<EpisodeItem>>()
    }

    /**
     * Whether the episode list has been requested to the source.
     */
    var hasRequested = false
        private set

    /**
     * Subscription to retrieve the new list of episodes from the source.
     */
    private var fetchEpisodesJob: Job? = null

    /**
     * Subscription to retrieve the new link of episodes from the source.
     */
    private var fetchEpisodeLinksJob: Job? = null

    /**
     * Subscription to observe download status changes.
     */
    private var observeDownloadsStatusSubscription: Subscription? = null
    private var observeDownloadsPageSubscription: Subscription? = null
    private var observeDownloadsProgressSubscription: Subscription? = null

    private var _trackList: List<TrackItem> = emptyList()
    val trackList get() = _trackList

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged } }

    private var trackSubscription: Subscription? = null
    private var searchTrackerJob: Job? = null
    private var refreshTrackersJob: Job? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (!anime.favorite) {
            EpisodeSettingsHelper.applySettingDefaults(anime)
        }

        // Anime info - start

        getAnimeObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache({ view, anime -> view.onNextAnimeInfo(anime, source) })

        getTrackingObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(AnimeController::onTrackingCount) { _, error -> logcat(LogPriority.ERROR, error) }

        // Prepare the relay.
        episodesRelay.flatMap { applyEpisodeFilters(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { _, episodes ->
                    filteredAndSortedEpisodes = episodes
                    view?.onNextEpisodes(episodes)
                },
                { _, error -> logcat(LogPriority.ERROR, error) }
            )

        // Anime info - end

        // Episodes list - start

        // Add the subscription that retrieves the episodes from the database, keeps subscribed to
        // changes, and sends the list of episodes to the relay.
        add(
            db.getEpisodes(anime).asRxObservable()
                .map { episodes ->
                    // Convert every episode to a model.
                    episodes.map { it.toModel() }
                }
                .doOnNext { episodes ->
                    // Find downloaded episodes
                    setDownloadedEpisodes(episodes)

                    // Store the last emission
                    this.allEpisodes = episodes

                    // Listen for download status changes
                    observeDownloads()
                }
                .subscribe { episodesRelay.call(it) }
        )

        // Episodes list - end

        fetchTrackers()
    }

    // Anime info - start

    private fun getAnimeObservable(): Observable<Anime> {
        return db.getAnime(anime.url, anime.source).asRxObservable()
    }

    private fun getTrackingObservable(): Observable<Int> {
        if (!trackManager.hasLoggedServices()) {
            return Observable.just(0)
        }

        return db.getTracks(anime).asRxObservable()
            .map { tracks ->
                val loggedServices = trackManager.services.filter { it.isLogged }.map { it.id }
                tracks.filter { it.sync_id in loggedServices }
            }
            .map { it.size }
    }

    /**
     * Fetch anime information from source.
     */
    fun fetchAnimeFromSource(manualFetch: Boolean = false) {
        if (fetchAnimeJob?.isActive == true) return
        fetchAnimeJob = presenterScope.launchIO {
            try {
                val networkAnime = source.getAnimeDetails(anime.toAnimeInfo())
                val sAnime = networkAnime.toSAnime()
                anime.prepUpdateCover(coverCache, sAnime, manualFetch)
                anime.copyFrom(sAnime)
                if (!anime.favorite) {
                    // if the anime isn't a favorite, set its title from source and update in db
                    anime.title = sAnime.title
                }
                anime.initialized = true
                db.insertAnime(anime).executeAsBlocking()

                withUIContext { view?.onFetchAnimeInfoDone() }
            } catch (e: Throwable) {
                withUIContext { view?.onFetchAnimeInfoError(e) }
            }
        }
    }

    /**
     * Update favorite status of anime, (removes / adds) anime (to / from) library.
     *
     * @return the new status of the anime.
     */
    fun toggleFavorite(): Boolean {
        anime.favorite = !anime.favorite
        anime.date_added = when (anime.favorite) {
            true -> Date().time
            false -> 0
        }
        if (!anime.favorite) {
            anime.removeCovers(coverCache)
        }
        db.insertAnime(anime).executeAsBlocking()
        return anime.favorite
    }

    /**
     * Returns true if the anime has any downloads.
     */
    fun hasDownloads(): Boolean {
        return downloadManager.getDownloadCount(anime) > 0
    }

    /**
     * Deletes all the downloads for the anime.
     */
    fun deleteDownloads() {
        downloadManager.deleteAnime(anime, source)
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    fun getCategories(): List<Category> {
        return db.getCategories().executeAsBlocking()
    }

    /**
     * Gets the category id's the anime is in, if the anime is not in a category, returns the default id.
     *
     * @param anime the anime to get categories from.
     * @return Array of category ids the anime is in, if none returns default id
     */
    fun getAnimeCategoryIds(anime: Anime): IntArray {
        val categories = db.getCategoriesForAnime(anime).executeAsBlocking()
        return categories.mapNotNull { it.id }.toIntArray()
    }

    /**
     * Move the given anime to categories.
     *
     * @param anime the anime to move.
     * @param categories the selected categories.
     */
    fun moveAnimeToCategories(anime: Anime, categories: List<Category>) {
        val ac = categories.filter { it.id != 0 }.map { AnimeCategory.create(anime, it) }
        db.setAnimeCategories(ac, listOf(anime))
    }

    /**
     * Move the given anime to the category.
     *
     * @param anime the anime to move.
     * @param category the selected category, or null for default category.
     */
    fun moveAnimeToCategory(anime: Anime, category: Category?) {
        moveAnimeToCategories(anime, listOfNotNull(category))
    }

    /**
     * Get the manga cover as a Bitmap, either from the CoverCache (only works for library manga)
     * or from the Coil ImageLoader cache.
     *
     * @param context the context used to get the Coil ImageLoader
     * @param memoryCacheKey Coil MemoryCache.Key that points to the cover Bitmap cache location
     * @return manga cover as Bitmap
     */
    fun getCoverBitmap(context: Context, memoryCacheKey: MemoryCache.Key?): Bitmap {
        var resultBitmap = coverBitmapFromCoverCache()
        if (resultBitmap == null && memoryCacheKey != null) {
            resultBitmap = coverBitmapFromImageLoader(context, memoryCacheKey)
        }

        return resultBitmap ?: throw Exception("Cover not in cache")
    }

    /**
     * Attempt manga cover retrieval from the CoverCache.
     *
     * @return cover as Bitmap or null if CoverCache does not contain cover for manga
     */
    private fun coverBitmapFromCoverCache(): Bitmap? {
        val cover = coverCache.getCoverFile(anime)
        return if (cover != null) {
            BitmapFactory.decodeFile(cover.path)
        } else {
            null
        }
    }

    /**
     * Attempt manga cover retrieval from the Coil ImageLoader memoryCache.
     *
     * @param context the context used to get the Coil ImageLoader
     * @param memoryCacheKey Coil MemoryCache.Key that points to the cover Bitmap cache location
     * @return cover as Bitmap or null if there is no thumbnail cached with the memoryCacheKey
     */
    private fun coverBitmapFromImageLoader(context: Context, memoryCacheKey: MemoryCache.Key): Bitmap? {
        return context.imageLoader.memoryCache?.get(memoryCacheKey)?.bitmap
    }

    /**
     * Save manga cover Bitmap to temporary share directory.
     *
     * @param context for the temporary share directory
     * @param coverBitmap the cover to save (as Bitmap)
     * @return cover File in temporary share directory
     */
    fun shareCover(context: Context, coverBitmap: Bitmap): File {
        return saveCover(getTempShareDir(context), coverBitmap)
    }

    /**
     * Save manga cover to pictures directory of the device.
     *
     * @param context for the pictures directory of the user
     * @param coverBitmap the cover to save (as Bitmap)
     * @return cover File in pictures directory
     */
    fun saveCover(context: Context, coverBitmap: Bitmap) {
        saveCover(getPicturesDir(context), coverBitmap)
    }

    /**
     * Save a manga cover Bitmap to a new File in a given directory.
     * Overwrites file if it already exists.
     *
     * @param directory The directory in which the new file will be created
     * @param coverBitmap The manga cover to save
     * @return the newly created File
     */
    private fun saveCover(directory: File, coverBitmap: Bitmap): File {
        directory.mkdirs()
        val filename = DiskUtil.buildValidFilename("${anime.title}.${ImageUtil.ImageType.PNG}")

        val destFile = File(directory, filename)
        destFile.outputStream().use { desFileOutputStream ->
            coverBitmap.compress(Bitmap.CompressFormat.PNG, 100, desFileOutputStream)
        }
        return destFile
    }

    /**
     * Update cover with local file.
     *
     * @param anime the anime edited.
     * @param context Context.
     * @param data uri of the cover resource.
     */
    fun editCover(anime: Anime, context: Context, data: Uri) {
        Observable
            .fromCallable {
                context.contentResolver.openInputStream(data)?.use {
                    if (anime.isLocal()) {
                        LocalAnimeSource.updateCover(context, anime, it)
                        anime.updateCoverLastModified(db)
                        db.insertAnime(anime).executeAsBlocking()
                        coverCache.clearMemoryCache()
                    } else if (anime.favorite) {
                        coverCache.setCustomCoverToCache(anime, it)
                        anime.updateCoverLastModified(db)
                        coverCache.clearMemoryCache()
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ -> view.onSetCoverSuccess() },
                { view, e -> view.onSetCoverError(e) }
            )
    }

    fun deleteCustomCover(anime: Anime) {
        Observable
            .fromCallable {
                coverCache.deleteCustomCover(anime)
                anime.updateCoverLastModified(db)
                coverCache.clearMemoryCache()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, _ -> view.onSetCoverSuccess() },
                { view, e -> view.onSetCoverError(e) }
            )
    }

    // Anime info - end

    // Episodes list - start

    private fun observeDownloads() {
        observeDownloadsStatusSubscription?.let { remove(it) }
        observeDownloadsStatusSubscription = downloadManager.queue.getStatusObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .filter { download -> download.anime.id == anime.id }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { view, it ->
                    onDownloadStatusChange(it)
                    view.onEpisodeDownloadUpdate(it)
                },
                { _, error ->
                    logcat(LogPriority.ERROR, error)
                }
            )

        observeDownloadsPageSubscription?.let { remove(it) }
        observeDownloadsPageSubscription = downloadManager.queue.getProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .filter { download -> download.anime.id == anime.id }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestAnimeCache(AnimeController::onEpisodeDownloadUpdate) { _, error ->
                logcat(LogPriority.ERROR, error)
            }

        observeDownloadsProgressSubscription?.let { remove(it) }
        observeDownloadsProgressSubscription = downloadManager.queue.getPreciseProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureLatest()
            .filter { download -> download.anime.id == anime.id }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestAnimeCache(AnimeController::onEpisodeDownloadUpdate) { _, error ->
                logcat(LogPriority.ERROR, error)
            }
    }

    /**
     * Converts a episode from the database to an extended model, allowing to store new fields.
     */
    private fun Episode.toModel(): EpisodeItem {
        // Create the model object.
        val model = EpisodeItem(this, anime)

        // Find an active download for this episode.
        val download = downloadManager.queue.find { it.episode.id == id }

        if (download != null) {
            // If there's an active download, assign it.
            model.download = download
        }
        return model
    }

    /**
     * Finds and assigns the list of downloaded episodes.
     *
     * @param episodes the list of episode from the database.
     */
    private fun setDownloadedEpisodes(episodes: List<EpisodeItem>) {
        episodes
            .filter { downloadManager.isEpisodeDownloaded(it, anime) }
            .forEach { it.status = AnimeDownload.State.DOWNLOADED }
    }

    /**
     * Requests an updated list of episodes from the source.
     */
    fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        hasRequested = true

        if (fetchEpisodesJob?.isActive == true) return
        fetchEpisodesJob = presenterScope.launchIO {
            try {
                val episodes = source.getEpisodeList(anime.toAnimeInfo())
                    .map { it.toSEpisode() }

                val (newEpisodes, _) = syncEpisodesWithSource(db, episodes, anime, source)
                if (manualFetch) {
                    downloadNewEpisodes(newEpisodes)
                }

                withUIContext { view?.onFetchEpisodesDone() }
            } catch (e: Throwable) {
                withUIContext { view?.onFetchEpisodesError(e) }
            }
        }
    }

    /**
     * Updates the UI after applying the filters.
     */
    private fun refreshEpisodes() {
        episodesRelay.call(allEpisodes)
    }

    /**
     * Applies the view filters to the list of episodes obtained from the database.
     * @param episodes the list of episodes from the database
     * @return an observable of the list of episodes filtered and sorted.
     */
    private fun applyEpisodeFilters(episodes: List<EpisodeItem>): Observable<List<EpisodeItem>> {
        var observable = Observable.from(episodes).subscribeOn(Schedulers.io())

        val unreadFilter = onlyUnread()
        if (unreadFilter == State.INCLUDE) {
            observable = observable.filter { !it.seen }
        } else if (unreadFilter == State.EXCLUDE) {
            observable = observable.filter { it.seen }
        }

        val downloadedFilter = onlyDownloaded()
        if (downloadedFilter == State.INCLUDE) {
            observable = observable.filter { it.isDownloaded || it.anime.isLocal() }
        } else if (downloadedFilter == State.EXCLUDE) {
            observable = observable.filter { !it.isDownloaded && !it.anime.isLocal() }
        }

        val bookmarkedFilter = onlyBookmarked()
        if (bookmarkedFilter == State.INCLUDE) {
            observable = observable.filter { it.bookmark }
        } else if (bookmarkedFilter == State.EXCLUDE) {
            observable = observable.filter { !it.bookmark }
        }

        return observable.toSortedList(getEpisodeSort(anime))
    }

    /**
     * Called when a download for the active anime changes status.
     * @param download the download whose status changed.
     */
    private fun onDownloadStatusChange(download: AnimeDownload) {
        // Assign the download to the model object.
        if (download.status == AnimeDownload.State.QUEUE) {
            allEpisodes.find { it.id == download.episode.id }?.let {
                if (it.download == null) {
                    it.download = download
                }
            }
        }

        // Force UI update if downloaded filter active and download finished.
        if (onlyDownloaded() != State.IGNORE && download.status == AnimeDownload.State.DOWNLOADED) {
            refreshEpisodes()
        }
    }

    /**
     * Returns the next unread episode or null if everything is read.
     */
    fun getNextUnseenEpisode(): EpisodeItem? {
        return if (sortDescending()) {
            return filteredAndSortedEpisodes.findLast { !it.seen }
        } else {
            filteredAndSortedEpisodes.find { !it.seen }
        }
    }

    fun getUnseenEpisodesSorted(): List<EpisodeItem> {
        val episodes = allEpisodes
            .sortedWith(getEpisodeSort(anime))
            .filter { !it.seen && it.status == AnimeDownload.State.NOT_DOWNLOADED }
            .distinctBy { it.name }
        return if (sortDescending()) {
            episodes.reversed()
        } else {
            episodes
        }
    }

    fun startDownloadingNow(episode: Episode) {
        downloadManager.startDownloadNow(episode)
    }

    /**
     * Mark the selected episode list as read/unread.
     * @param selectedEpisodes the list of selected episodes.
     * @param read whether to mark episodes as read or unread.
     */
    fun markEpisodesRead(selectedEpisodes: List<EpisodeItem>, read: Boolean) {
        val episodes = selectedEpisodes.map { episode ->
            episode.seen = read
            if (!read) {
                episode.last_second_seen = 0
            }
            episode
        }

        launchIO {
            db.updateEpisodesProgress(episodes).executeAsBlocking()

            if (preferences.removeAfterMarkedAsRead()) {
                deleteEpisodes(episodes.filter { it.seen })
            }
        }
    }

    /**
     * Mark the selected episode list as read/unread.
     * @param selectedEpisodes the list of selected episodes.
     * @param read whether to mark episodes as read or unread.
     */
    fun setEpisodesProgress(selectedEpisodes: List<EpisodeItem>) {
        val episodes = selectedEpisodes.map { episode ->
            val progress = preferences.progressPreference()
            if (!episode.seen) episode.seen = episode.last_second_seen >= episode.total_seconds * progress
            episode
        }

        launchIO {
            db.updateEpisodesProgress(episodes).executeAsBlocking()

            if (preferences.removeAfterMarkedAsRead()) {
                deleteEpisodes(episodes.filter { it.seen })
            }
        }
    }

    /**
     * Downloads the given list of episodes with the manager.
     * @param episodes the list of episodes to download.
     */
    fun downloadEpisodes(episodes: List<Episode>) {
        downloadManager.downloadEpisodes(anime, episodes)
    }

    /**
     * Downloads the given list of episodes with the manager.
     * @param episodes the list of episodes to download.
     */
    fun downloadEpisodesExternally(episodes: List<Episode>) {
        downloadManager.downloadEpisodesExternally(anime, episodes)
    }

    /**
     * Bookmarks the given list of episodes.
     * @param selectedEpisodes the list of episodes to bookmark.
     */
    fun bookmarkEpisodes(selectedEpisodes: List<EpisodeItem>, bookmarked: Boolean) {
        launchIO {
            selectedEpisodes
                .forEach {
                    it.bookmark = bookmarked
                    db.updateEpisodeProgress(it).executeAsBlocking()
                }
        }
    }

    /**
     * Deletes the given list of episode.
     * @param episodes the list of episodes to delete.
     */
    fun deleteEpisodes(episodes: List<EpisodeItem>) {
        launchIO {
            try {
                downloadManager.deleteEpisodes(episodes, anime, source).forEach {
                    if (it is EpisodeItem) {
                        it.status = AnimeDownload.State.NOT_DOWNLOADED
                        it.download = null
                    }
                }

                if (onlyDownloaded() != State.IGNORE) {
                    refreshEpisodes()
                }

                view?.onEpisodesDeleted(episodes)
            } catch (e: Throwable) {
                view?.onEpisodesDeletedError(e)
            }
        }
    }

    private fun downloadNewEpisodes(episodes: List<Episode>) {
        if (episodes.isEmpty() || !anime.shouldDownloadNewEpisodes(db, preferences)) return

        downloadEpisodes(episodes)
    }

    /**
     * Reverses the sorting and requests an UI update.
     */
    fun reverseSortOrder() {
        anime.setEpisodeOrder(if (sortDescending()) Anime.EPISODE_SORT_ASC else Anime.EPISODE_SORT_DESC)
        db.updateEpisodeFlags(anime).executeAsBlocking()
        refreshEpisodes()
    }

    /**
     * Sets the read filter and requests an UI update.
     * @param state whether to display only unread episodes or all episodes.
     */
    fun setUnreadFilter(state: State) {
        anime.seenFilter = when (state) {
            State.IGNORE -> Anime.SHOW_ALL
            State.INCLUDE -> Anime.EPISODE_SHOW_UNSEEN
            State.EXCLUDE -> Anime.EPISODE_SHOW_SEEN
        }
        db.updateEpisodeFlags(anime).executeAsBlocking()
        refreshEpisodes()
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param state whether to display only downloaded episodes or all episodes.
     */
    fun setDownloadedFilter(state: State) {
        anime.downloadedFilter = when (state) {
            State.IGNORE -> Anime.SHOW_ALL
            State.INCLUDE -> Anime.EPISODE_SHOW_DOWNLOADED
            State.EXCLUDE -> Anime.EPISODE_SHOW_NOT_DOWNLOADED
        }
        db.updateEpisodeFlags(anime).executeAsBlocking()
        refreshEpisodes()
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param state whether to display only bookmarked episodes or all episodes.
     */
    fun setBookmarkedFilter(state: State) {
        anime.bookmarkedFilter = when (state) {
            State.IGNORE -> Anime.SHOW_ALL
            State.INCLUDE -> Anime.EPISODE_SHOW_BOOKMARKED
            State.EXCLUDE -> Anime.EPISODE_SHOW_NOT_BOOKMARKED
        }
        db.updateEpisodeFlags(anime).executeAsBlocking()
        refreshEpisodes()
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Int) {
        anime.displayMode = mode
        db.updateEpisodeFlags(anime).executeAsBlocking()
        refreshEpisodes()
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Int) {
        anime.sorting = sort
        db.updateEpisodeFlags(anime).executeAsBlocking()
        refreshEpisodes()
    }

    /**
     * Whether downloaded only mode is enabled.
     */
    fun forceDownloaded(): Boolean {
        return anime.favorite && preferences.downloadedOnly().get()
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyDownloaded(): State {
        if (forceDownloaded()) {
            return State.INCLUDE
        }
        return when (anime.downloadedFilter) {
            Anime.EPISODE_SHOW_DOWNLOADED -> State.INCLUDE
            Anime.EPISODE_SHOW_NOT_DOWNLOADED -> State.EXCLUDE
            else -> State.IGNORE
        }
    }

    /**
     * Whether the display only downloaded filter is enabled.
     */
    fun onlyBookmarked(): State {
        return when (anime.bookmarkedFilter) {
            Anime.EPISODE_SHOW_BOOKMARKED -> State.INCLUDE
            Anime.EPISODE_SHOW_NOT_BOOKMARKED -> State.EXCLUDE
            else -> State.IGNORE
        }
    }

    /**
     * Whether the display only unread filter is enabled.
     */
    fun onlyUnread(): State {
        return when (anime.seenFilter) {
            Anime.EPISODE_SHOW_UNSEEN -> State.INCLUDE
            Anime.EPISODE_SHOW_SEEN -> State.EXCLUDE
            else -> State.IGNORE
        }
    }

    /**
     * Whether the sorting method is descending or ascending.
     */
    fun sortDescending(): Boolean {
        return anime.sortDescending()
    }

    // Episodes list - end

    // Track sheet - start

    private fun fetchTrackers() {
        trackSubscription?.let { remove(it) }
        trackSubscription = db.getTracks(anime)
            .asRxObservable()
            .map { tracks ->
                loggedServices.map { service ->
                    TrackItem(tracks.find { it.sync_id == service.id }, service)
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { _trackList = it }
            .subscribeLatestCache(AnimeController::onNextTrackers)
    }

    fun refreshTrackers() {
        refreshTrackersJob?.cancel()
        refreshTrackersJob = launchIO {
            supervisorScope {
                try {
                    trackList
                        .filter { it.track != null }
                        .map {
                            async {
                                val track = it.service.refresh(it.track!!)
                                db.insertTrack(track).executeAsBlocking()

                                if (it.service is EnhancedTrackService) {
                                    syncEpisodesWithTrackServiceTwoWay(db, allEpisodes, track, it.service)
                                }
                            }
                        }
                        .awaitAll()
                } catch (e: Throwable) {
                    withUIContext { view?.onTrackingRefreshError(e) }
                }
            }
        }
    }

    fun trackingSearch(query: String, service: TrackService) {
        searchTrackerJob?.cancel()
        searchTrackerJob = launchIO {
            try {
                val results = service.searchAnime(query)
                withUIContext { view?.onTrackingSearchResults(results) }
            } catch (e: Throwable) {
                withUIContext { view?.onTrackingSearchResultsError(e) }
            }
        }
    }

    fun registerTracking(item: AnimeTrack?, service: TrackService) {
        if (item != null) {
            item.anime_id = anime.id!!
            launchIO {
                try {
                    val hasReadChapters = allEpisodes.any { it.seen }
                    service.bind(item, hasReadChapters)
                    db.insertTrack(item).executeAsBlocking()

                    if (service is EnhancedTrackService) {
                        syncEpisodesWithTrackServiceTwoWay(db, allEpisodes, item, service)
                    }
                } catch (e: Throwable) {
                    withUIContext { view?.applicationContext?.toast(e.message) }
                }
            }
        } else {
            unregisterTracking(service)
        }
    }

    fun unregisterTracking(service: TrackService) {
        db.deleteTrackForAnime(anime, service).executeAsBlocking()
    }

    private fun updateRemote(track: AnimeTrack, service: TrackService) {
        launchIO {
            try {
                service.update(track)
                db.insertTrack(track).executeAsBlocking()
            } catch (e: Throwable) {
                withUIContext { view?.onTrackingRefreshError(e) }

                // Restart on error to set old values
                fetchTrackers()
            }
        }
    }

    fun setTrackerStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusListAnime()[index]
        if (track.status == item.service.getCompletionStatus() && track.total_episodes != 0) {
            track.last_episode_seen = track.total_episodes.toFloat()
        }
        updateRemote(track, item.service)
    }

    fun setTrackerScore(item: TrackItem, index: Int) {
        val track = item.track!!
        track.score = item.service.indexToScore(index)
        updateRemote(track, item.service)
    }

    fun setTrackerLastEpisodeSeen(item: TrackItem, episodeNumber: Int) {
        val track = item.track!!
        if (track.last_episode_seen == 0F && track.last_episode_seen < episodeNumber && track.status != item.service.getRewatchingStatus()) {
            track.status = item.service.getWatchingStatus()
        }
        track.last_episode_seen = episodeNumber.toFloat()
        if (track.total_episodes != 0 && track.last_episode_seen.toInt() == track.total_episodes) {
            track.status = item.service.getCompletionStatus()
        }
        updateRemote(track, item.service)
    }

    fun setTrackerStartDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.started_watching_date = date
        updateRemote(track, item.service)
    }

    fun setTrackerFinishDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.finished_watching_date = date
        updateRemote(track, item.service)
    }

    // Track sheet - end
}
