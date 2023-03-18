package eu.kanade.tachiyomi.ui.player

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.core.util.asFlow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.download.service.DownloadPreferences
import eu.kanade.domain.entries.anime.interactor.GetAnime
import eu.kanade.domain.entries.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.domain.entries.anime.model.isLocal
import eu.kanade.domain.history.anime.interactor.GetNextEpisodes
import eu.kanade.domain.history.anime.interactor.UpsertAnimeHistory
import eu.kanade.domain.history.anime.model.AnimeHistoryUpdate
import eu.kanade.domain.items.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.items.episode.interactor.UpdateEpisode
import eu.kanade.domain.items.episode.model.EpisodeUpdate
import eu.kanade.domain.items.episode.model.toDbEpisode
import eu.kanade.domain.track.anime.interactor.GetAnimeTracks
import eu.kanade.domain.track.anime.interactor.InsertAnimeTrack
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.domain.track.anime.service.DelayedAnimeTrackingUpdateJob
import eu.kanade.domain.track.anime.store.DelayedAnimeTrackingStore
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.data.database.models.anime.toDomainEpisode
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.reader.SaveImageNotifier
import eu.kanade.tachiyomi.util.AniSkipApi
import eu.kanade.tachiyomi.util.Stamp
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.takeBytes
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.logcat
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.util.Date

class PlayerViewModel(
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val imageSaver: ImageSaver = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val playerPreferences: PlayerPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    private val delayedTrackingStore: DelayedAnimeTrackingStore = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val upsertHistory: UpsertAnimeHistory = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get(),
) : ViewModel() {

    val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    /**
     * The anime loaded in the player. It can be null when instantiated for a short time.
     */
    val anime: Anime?
        get() = state.value.anime

    /**
     * The episode id of the currently loaded episode. Used to restore from process kill.
     */
    private var episodeId = savedState.get<Long>("episode_id") ?: -1L
        set(value) {
            savedState["episode_id"] = value
            field = value
        }

    private var episodeToDownload: AnimeDownload? = null

    var source: AnimeSource? = null

    var currentEpisode: Episode? = null

    private var currentVideoList: List<Video>? = null

    private var requestedSecond: Long = 0L

    /**
     * Episode list for the active anime. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    lateinit var episodeList: List<Episode>

    private fun initEpisodeList(): List<Episode> {
        val anime = anime!!
        val episodes = runBlocking { getEpisodeByAnimeId.await(anime.id) }

        val selectedEpisode = episodes.find { it.id == episodeId }
            ?: error("Requested episode of id $episodeId not found in episode list")

        val episodesForPlayer = episodes.filterNot {
            anime.unseenFilterRaw == Anime.EPISODE_SHOW_SEEN && !it.seen ||
                anime.unseenFilterRaw == Anime.EPISODE_SHOW_UNSEEN && it.seen ||
                anime.downloadedFilterRaw == Anime.EPISODE_SHOW_DOWNLOADED && !downloadManager.isEpisodeDownloaded(it.name, it.scanlator, anime.title, anime.source) ||
                anime.downloadedFilterRaw == Anime.EPISODE_SHOW_NOT_DOWNLOADED && downloadManager.isEpisodeDownloaded(it.name, it.scanlator, anime.title, anime.source) ||
                anime.bookmarkedFilterRaw == Anime.EPISODE_SHOW_BOOKMARKED && !it.bookmark ||
                anime.bookmarkedFilterRaw == Anime.EPISODE_SHOW_NOT_BOOKMARKED && it.bookmark
        }.toMutableList()

        if (episodesForPlayer.all { it.id != episodeId }) {
            episodesForPlayer += listOf(selectedEpisode)
        }

        return episodesForPlayer
            .sortedWith(getEpisodeSort(anime, sortDescending = false))
            .map { it.toDbEpisode() }
    }

    fun getCurrentEpisodeIndex(): Int {
        return episodeList.indexOfFirst { currentEpisode?.id == it.id }
    }

    private var hasTrackers: Boolean = false

    private val checkTrackers: (Anime) -> Unit = { anime ->
        val tracks = runBlocking { getTracks.await(anime.id) }
        hasTrackers = tracks.isNotEmpty()
    }

    private val incognitoMode = preferences.incognitoMode().get()

    override fun onCleared() {
        if (currentEpisode != null) {
            saveWatchingProgress(currentEpisode!!)
            episodeToDownload?.let {
                downloadManager.addDownloadsToStartOfQueue(listOf(it))
            }
        }
    }

    /**
     * Called when the activity is saved and not changing configurations. It updates the database
     * to persist the current progress of the active episode.
     */
    fun onSaveInstanceStateNonConfigurationChange() {
        val currentEpisode = currentEpisode ?: return
        viewModelScope.launchNonCancellable {
            saveEpisodeProgress(currentEpisode)
        }
    }

    init {
        // To save state
        state.map { currentEpisode }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { currentEpisode ->
                if (!currentEpisode.seen) {
                    requestedSecond = currentEpisode.last_second_seen
                }
                episodeId = currentEpisode.id!!
            }
            .launchIn(viewModelScope)
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
    suspend fun init(animeId: Long, initialEpisodeId: Long): Pair<List<Video>?, Result<Boolean>> {
        if (!needsInit()) return Pair(currentVideoList, Result.success(true))
        return withIOContext {
            try {
                val anime = getAnime.await(animeId)
                if (anime != null) {
                    mutableState.update { it.copy(anime = anime) }
                    if (episodeId == -1L) episodeId = initialEpisodeId

                    checkTrackers(anime)
                    val source = sourceManager.getOrStub(anime.source)
                    this@PlayerViewModel.source = source

                    episodeList = initEpisodeList()
                    currentEpisode = episodeList.first { initialEpisodeId == it.id }

                    val currentEpisode = currentEpisode ?: throw Exception("No episode loaded.")

                    currentVideoList = EpisodeLoader.getLinks(currentEpisode, anime, source).asFlow().first()
                    episodeId = currentEpisode.id!!

                    Pair(currentVideoList, Result.success(true))
                } else {
                    // Unlikely but okay
                    Pair(currentVideoList, Result.success(false))
                }
            } catch (e: Throwable) {
                Pair(currentVideoList, Result.failure(e))
            }
        }
    }

    fun isEpisodeOnline(): Boolean? {
        val anime = anime ?: return null
        val episode = currentEpisode ?: return null
        return source is AnimeHttpSource && !EpisodeLoader.isDownloaded(episode, anime)
    }

    suspend fun nextEpisode(): Pair<List<Video>?, String?>? {
        val anime = anime ?: return null
        val source = sourceManager.getOrStub(anime.source)

        val index = getCurrentEpisodeIndex()
        if (index == episodeList.lastIndex) return null
        currentEpisode = episodeList[index + 1]

        return withIOContext {
            try {
                val currentEpisode = currentEpisode ?: throw Exception("No episode loaded.")
                currentVideoList = EpisodeLoader.getLinks(currentEpisode, anime, source).asFlow().first()
                episodeId = currentEpisode.id!!
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "Error getting links." }
            }

            Pair(currentVideoList, anime.title + " - " + episodeList[index + 1].name)
        }
    }

    suspend fun previousEpisode(): Pair<List<Video>?, String?>? {
        val anime = anime ?: return null
        val source = sourceManager.getOrStub(anime.source)

        val index = getCurrentEpisodeIndex()
        if (index == 0) return null
        currentEpisode = episodeList[index - 1]

        return withIOContext {
            try {
                val currentEpisode = currentEpisode ?: throw Exception("No episode loaded.")
                currentVideoList = EpisodeLoader.getLinks(currentEpisode, anime, source).asFlow().first()
                episodeId = currentEpisode.id!!
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "Error getting links." }
            }
            Pair(currentVideoList, anime.title + " - " + episodeList[index - 1].name)
        }
    }

    /**
     * Called every time a second is reached in the player. Used to mark the flag of episode being
     * seen, update tracking services, enqueue downloaded episode deletion and download next episode.
     */
    fun onSecondReached(position: Int, duration: Int) {
        if (state.value.isLoadingAdjacentEpisode) return
        val currentEpisode = currentEpisode ?: return
        if (episodeId == -1L) return

        val seconds = position * 1000L
        val totalSeconds = duration * 1000L
        // Save last second seen and mark as seen if needed
        currentEpisode.last_second_seen = seconds
        currentEpisode.total_seconds = totalSeconds

        val progress = playerPreferences.progressPreference().get()
        val shouldTrack = !incognitoMode || hasTrackers
        if (currentEpisode.last_second_seen >= currentEpisode.total_seconds * progress && shouldTrack) {
            currentEpisode.seen = true
            updateTrackEpisodeSeen(currentEpisode)
            deleteEpisodeIfNeeded(currentEpisode)
        }

        saveWatchingProgress(currentEpisode)

        val inDownloadRange = seconds / totalSeconds > 0.35
        if (inDownloadRange) {
            downloadNextEpisodes()
        }
    }

    private fun downloadNextEpisodes() {
        val anime = anime ?: return
        val amount = downloadPreferences.autoDownloadWhileWatching().get()
        if (amount == 0 || !anime.favorite) return

        // Only download ahead if current + next chapter is already downloaded too to avoid jank
        if (getCurrentEpisodeIndex() == episodeList.lastIndex) return

        val currentEpisode = currentEpisode ?: return
        val nextEpisode = episodeList[getCurrentEpisodeIndex() + 1]
        viewModelScope.launchIO {
            if (EpisodeLoader.isDownloaded(currentEpisode, anime) &&
                EpisodeLoader.isDownloaded(nextEpisode, anime)
            ) {
                return@launchIO
            }
            val episodesToDownload = getNextEpisodes.await(anime.id, nextEpisode.id!!)
                .take(amount)
            downloadManager.downloadEpisodes(
                anime,
                episodesToDownload,
            )
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last episode actually exists.
     * If both conditions are satisfied enqueues episode for delete
     * @param currentEpisode current episode, which is going to be marked as seen.
     */
    private fun deleteEpisodeIfNeeded(currentEpisode: Episode) {
        // Determine which episode should be deleted and enqueue
        val currentEpisodePosition = episodeList.indexOf(currentEpisode)
        val removeAfterSeenSlots = downloadPreferences.removeAfterReadSlots().get()
        val episodeToDelete = episodeList.getOrNull(currentEpisodePosition - removeAfterSeenSlots)
        // If episode is completely seen no need to download it
        episodeToDownload = null
        // Check if deleting option is enabled and episode exists
        if (removeAfterSeenSlots != -1 && episodeToDelete != null) {
            enqueueDeleteSeenEpisodes(episodeToDelete)
        }
    }

    fun saveCurrentEpisodeWatchingProgress() {
        currentEpisode?.let { saveWatchingProgress(it) }
    }

    /**
     * Called when episode is changed in player or when activity is paused.
     */
    private fun saveWatchingProgress(episode: Episode) {
        viewModelScope.launchNonCancellable {
            saveEpisodeProgress(episode)
            saveEpisodeHistory(episode)
        }
    }

    /**
     * Saves this [episode] progress (last second seen and whether it's seen).
     * If incognito mode isn't on or has at least 1 tracker
     */
    private suspend fun saveEpisodeProgress(episode: Episode) {
        if (!incognitoMode || hasTrackers) {
            updateEpisode.await(
                EpisodeUpdate(
                    id = episode.id!!,
                    seen = episode.seen,
                    bookmark = episode.bookmark,
                    lastSecondSeen = episode.last_second_seen,
                    totalSeconds = episode.total_seconds,
                ),
            )
        }
    }

    /**
     * Saves this [episode] last seen history if incognito mode isn't on.
     */
    private suspend fun saveEpisodeHistory(episode: Episode) {
        if (!incognitoMode) {
            val episodeId = episode.id!!
            val seenAt = Date()
            upsertHistory.await(
                AnimeHistoryUpdate(episodeId, seenAt),
            )
        }
    }

    /**
     * Bookmarks the currently active episode.
     */
    fun bookmarkCurrentEpisode(bookmarked: Boolean) {
        val episode = currentEpisode ?: return
        episode.bookmark = bookmarked // Otherwise the bookmark icon doesn't update
        viewModelScope.launchNonCancellable {
            updateEpisode.await(
                EpisodeUpdate(
                    id = episode.id!!.toLong(),
                    bookmark = bookmarked,
                ),
            )
        }
    }

    /**
     * Saves the screenshot on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage(imageStream: () -> InputStream, timePos: Int?) {
        val anime = anime ?: return

        val context = Injekt.get<Application>()
        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        val seconds = timePos?.let { Utils.prettyTime(it) } ?: return
        val filename = generateFilename(anime, seconds) ?: return

        // Pictures directory.
        val relativePath = DiskUtil.buildValidFilename(anime.title)

        // Copy file in background.
        viewModelScope.launchNonCancellable {
            try {
                val uri = imageSaver.save(
                    image = Image.Page(
                        inputStream = imageStream,
                        name = filename,
                        location = Location.Pictures.create(relativePath),
                    ),
                )
                notifier.onComplete(uri)
                eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
            } catch (e: Throwable) {
                notifier.onError(e.message)
                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
            }
        }
    }

    /**
     * Shares the screenshot and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(imageStream: () -> InputStream, timePos: Int?) {
        val anime = anime ?: return

        val context = Injekt.get<Application>()
        val destDir = context.cacheImageDir

        val seconds = timePos?.let { Utils.prettyTime(it) } ?: return
        val filename = generateFilename(anime, seconds) ?: return

        try {
            viewModelScope.launchIO {
                destDir.deleteRecursively()
                val uri = imageSaver.save(
                    image = Image.Page(
                        inputStream = imageStream,
                        name = filename,
                        location = Location.Cache,
                    ),
                )
                eventChannel.send(Event.ShareImage(uri, seconds))
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
        }
    }

    /**
     * Sets the screenshot as cover and notifies the UI of the result.
     */
    fun setAsCover(context: Context, image: InputStream?) {
        val anime = anime ?: return
        val imageStream = image ?: return

        viewModelScope.launchNonCancellable {
            val result = try {
                anime.editCover(context, imageStream)
                if (anime.isLocal() || anime.favorite) {
                    SetAsCoverResult.Success
                } else {
                    SetAsCoverResult.AddToLibraryFirst
                }
            } catch (e: Exception) {
                SetAsCoverResult.Error
            }
            eventChannel.send(Event.SetCoverResult(result))
        }
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
        class Success(val uri: Uri) : SaveImageResult()
        class Error(val error: Throwable) : SaveImageResult()
    }

    private fun updateTrackEpisodeSeen(episode: Episode) {
        if (!trackPreferences.autoUpdateTrack().get()) return
        val anime = anime ?: return

        val episodeSeen = episode.episode_number.toDouble()

        val trackManager = Injekt.get<TrackManager>()
        val context = Injekt.get<Application>()

        viewModelScope.launchNonCancellable {
            getTracks.await(anime.id)
                .mapNotNull { track ->
                    val service = trackManager.getService(track.syncId)
                    if (service != null && service.isLogged && episodeSeen > track.lastEpisodeSeen) {
                        val updatedTrack = track.copy(lastEpisodeSeen = episodeSeen)

                        // We want these to execute even if the presenter is destroyed and leaks
                        // for a while. The view can still be garbage collected.
                        async {
                            runCatching {
                                if (context.isOnline()) {
                                    service.animeService.update(updatedTrack.toDbTrack(), true)
                                    insertTrack.await(updatedTrack)
                                } else {
                                    delayedTrackingStore.addAnimeItem(updatedTrack)
                                    DelayedAnimeTrackingUpdateJob.setupTask(context)
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
     * Enqueues this [episode] to be deleted when [deletePendingEpisodes] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteSeenEpisodes(episode: Episode) {
        if (!episode.seen) return
        val anime = anime ?: return

        viewModelScope.launchNonCancellable {
            downloadManager.enqueueEpisodesToDelete(listOf(episode.toDomainEpisode()!!), anime)
        }
    }

    /**
     * Deletes all the pending episodes. This operation will run in a background thread and errors
     * are ignored.
     */
    fun deletePendingEpisodes() {
        viewModelScope.launchNonCancellable {
            downloadManager.deletePendingEpisodes()
        }
    }

    /**
     * Returns the skipIntroLength used by this anime or the default one.
     */
    fun getAnimeSkipIntroLength(resolveDefault: Boolean = true): Int {
        val default = playerPreferences.defaultIntroLength().get()
        val anime = anime ?: return default
        val skipIntroLength = anime.skipIntroLength
        return when {
            resolveDefault && skipIntroLength <= 0 -> default
            else -> anime.skipIntroLength.toInt()
        }
    }

    /**
     * Updates the skipIntroLength for the open anime.
     */
    fun setAnimeSkipIntroLength(skipIntroLength: Int) {
        val anime = anime ?: return
        viewModelScope.launchIO {
            setAnimeViewerFlags.awaitSetSkipIntroLength(anime.id, skipIntroLength.toLong())
            logcat(LogPriority.INFO) { "New Skip Intro Length is ${anime.skipIntroLength}" }
            mutableState.update {
                it.copy(
                    anime = getAnime.await(anime.id),
                )
            }
            eventChannel.send(Event.SetAnimeSkipIntro(getAnimeSkipIntroLength()))
        }
    }

    /**
     * Generate a filename for the given [anime] and [timePos]
     */
    private fun generateFilename(
        anime: Anime,
        timePos: String,
    ): String? {
        val episode = currentEpisode ?: return null
        val filenameSuffix = " - $timePos"
        return DiskUtil.buildValidFilename(
            "${anime.title} - ${episode.name}".takeBytes(MAX_FILE_NAME_BYTES - filenameSuffix.byteSize()),
        ) + filenameSuffix
    }

    /**
     * Returns the response of the AniSkipApi for this episode.
     * just works if tracking is enabled.
     */
    suspend fun aniSkipResponse(playerDuration: Int?): List<Stamp>? {
        val animeId = anime?.id ?: return null
        val trackManager = Injekt.get<TrackManager>()
        var malId: Long?
        val episodeNumber = currentEpisode?.episode_number?.toInt() ?: return null
        if (getTracks.await(animeId).isEmpty()) {
            logcat { "AniSkip: No tracks found for anime $animeId" }
            return null
        }

        getTracks.await(animeId).map { track ->
            val service = trackManager.getService(track.syncId)
            malId = when (service) {
                is MyAnimeList -> track.remoteId
                is Anilist -> AniSkipApi().getMalIdFromAL(track.remoteId)
                else -> null
            }
            val duration = playerDuration ?: return null
            return malId?.let {
                AniSkipApi().getResult(it.toInt(), episodeNumber, duration.toLong())
            }
        }
        return null
    }

    data class State(
        val anime: Anime? = null,
        val isLoadingAdjacentEpisode: Boolean = false,
    )

    sealed class Event {
        data class SetAnimeSkipIntro(val duration: Int) : Event()
        data class SetCoverResult(val result: SetAsCoverResult) : Event()

        data class SavedImage(val result: SaveImageResult) : Event()
        data class ShareImage(val uri: Uri, val seconds: String) : Event()
    }
}

private const val MAX_FILE_NAME_BYTES = 250
