package eu.kanade.tachiyomi.ui.player

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.entries.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.items.episode.model.toDbEpisode
import eu.kanade.domain.track.anime.interactor.TrackEpisode
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SerializableVideo.Companion.toVideoList
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.data.database.models.anime.toDomainEpisode
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.player.loader.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.viewer.SeekState
import eu.kanade.tachiyomi.ui.player.viewer.SetAsCover
import eu.kanade.tachiyomi.ui.reader.SaveImageNotifier
import eu.kanade.tachiyomi.util.AniSkipApi
import eu.kanade.tachiyomi.util.Stamp
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.episode.filterDownloadedEpisodes
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.lang.takeBytes
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import `is`.xyz.mpv.MPVView
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.util.lang.launchIO
import tachiyomi.core.util.lang.launchNonCancellable
import tachiyomi.core.util.lang.withIOContext
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.history.anime.interactor.GetNextEpisodes
import tachiyomi.domain.history.anime.interactor.UpsertAnimeHistory
import tachiyomi.domain.history.anime.model.AnimeHistoryUpdate
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.items.episode.interactor.UpdateEpisode
import tachiyomi.domain.items.episode.model.EpisodeUpdate
import tachiyomi.domain.items.episode.service.getEpisodeSort
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.source.local.entries.anime.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.InputStream
import java.util.Date
import tachiyomi.i18n.MR

class PlayerViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val imageSaver: ImageSaver = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    private val trackEpisode: TrackEpisode = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val upsertHistory: UpsertAnimeHistory = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get(),
    internal val networkPreferences: NetworkPreferences = Injekt.get(),
    internal val playerPreferences: PlayerPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
    uiPreferences: UiPreferences = Injekt.get(),
) : ViewModel() {

    val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val incognitoMode = basePreferences.incognitoMode().get()
    private val downloadAheadAmount = downloadPreferences.autoDownloadWhileWatching().get()

    internal val relativeTime = uiPreferences.relativeTime().get()
    internal val dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat().get())

    /**
     * The episode playlist loaded in the player. It can be empty when instantiated for a short time.
     */
    val currentPlaylist: List<Episode>
        get() = filterEpisodeList(state.value.episodeList)

    /**
     * The episode loaded in the player. It can be null when instantiated for a short time.
     */
    val currentEpisode: Episode?
        get() = state.value.episode

    /**
     * The anime loaded in the player. It can be null when instantiated for a short time.
     */
    val currentAnime: Anime?
        get() = state.value.anime

    /**
     * The source used. It can be null when instantiated for a short time.
     */
    val currentSource: AnimeSource?
        get() = state.value.source

    /**
     * The position in the current video. Used to restore from process kill.
     */
    private var episodePosition = savedState.get<Long>("episode_position")
        set(value) {
            savedState["episode_position"] = value
            field = value
        }

    /**
     * The current video's quality index. Used to restore from process kill.
     */
    var qualityIndex = savedState.get<Int>("quality_index") ?: 0
        set(value) {
            savedState["quality_index"] = value
            field = value
        }

    /**
     * The episode id of the currently loaded episode. Used to restore from process kill.
     */
    private var episodeId = savedState.get<Long>("episode_id") ?: -1L
        set(value) {
            savedState["episode_id"] = value
            field = value
        }

    private var episodeToDownload: AnimeDownload? = null

    private var currentVideoList: List<Video>? = null

    private fun filterEpisodeList(episodes: List<Episode>): List<Episode> {
        val anime = currentAnime ?: return episodes
        val selectedEpisode = episodes.find { it.id == episodeId }
            ?: error("Requested episode of id $episodeId not found in episode list")

        val episodesForPlayer = episodes.filterNot {
            anime.unseenFilterRaw == Anime.EPISODE_SHOW_SEEN && !it.seen ||
                anime.unseenFilterRaw == Anime.EPISODE_SHOW_UNSEEN && it.seen ||
                anime.downloadedFilterRaw == Anime.EPISODE_SHOW_DOWNLOADED && !downloadManager.isEpisodeDownloaded(
                    it.name,
                    it.scanlator,
                    anime.title,
                    anime.source,
                ) ||
                anime.downloadedFilterRaw == Anime.EPISODE_SHOW_NOT_DOWNLOADED && downloadManager.isEpisodeDownloaded(
                    it.name,
                    it.scanlator,
                    anime.title,
                    anime.source,
                ) ||
                anime.bookmarkedFilterRaw == Anime.EPISODE_SHOW_BOOKMARKED && !it.bookmark ||
                anime.bookmarkedFilterRaw == Anime.EPISODE_SHOW_NOT_BOOKMARKED && it.bookmark
        }.toMutableList()

        if (episodesForPlayer.all { it.id != episodeId }) {
            episodesForPlayer += listOf(selectedEpisode)
        }

        return episodesForPlayer
    }

    fun getCurrentEpisodeIndex(): Int {
        return this.currentPlaylist.indexOfFirst { currentEpisode?.id == it.id }
    }

    fun getAdjacentEpisodeId(previous: Boolean): Long {
        val newIndex = if (previous) getCurrentEpisodeIndex() - 1 else getCurrentEpisodeIndex() + 1

        return when {
            previous && getCurrentEpisodeIndex() == 0 -> -1L
            !previous && this.currentPlaylist.lastIndex == getCurrentEpisodeIndex() -> -1L
            else -> this.currentPlaylist[newIndex].id ?: -1L
        }
    }

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

    /**
     * Whether this presenter is initialized yet.
     */
    private fun needsInit(): Boolean {
        return currentAnime == null || currentEpisode == null
    }

    /**
     * Initializes this presenter with the given [animeId] and [initialEpisodeId]. This method will
     * fetch the anime from the database and initialize the episode.
     */
    suspend fun init(
        animeId: Long,
        initialEpisodeId: Long,
        vidList: String,
        vidIndex: Int,
    ): Pair<InitResult, Result<Boolean>> {
        val defaultResult = InitResult(currentVideoList, 0, null)
        if (!needsInit()) return Pair(defaultResult, Result.success(true))
        return try {
            val anime = getAnime.await(animeId)
            if (anime != null) {
                if (episodeId == -1L) episodeId = initialEpisodeId

                checkTrackers(anime)

                mutableState.update { it.copy(episodeList = initEpisodeList(anime)) }
                val episode = this.currentPlaylist.first { it.id == episodeId }

                val source = sourceManager.getOrStub(anime.source)

                mutableState.update { it.copy(episode = episode, anime = anime, source = source) }

                val currentEp = currentEpisode ?: throw Exception("No episode loaded.")

                if (vidList.isNotBlank()) {
                    currentVideoList = vidList.toVideoList().ifEmpty {
                        currentVideoList = null
                        throw Exception("Video selected from empty list?")
                    }
                    qualityIndex = vidIndex
                } else {
                    EpisodeLoader.getLinks(currentEp.toDomainEpisode()!!, anime, source)
                        .takeIf { it.isNotEmpty() }
                        ?.also { currentVideoList = it }
                        ?: run {
                            currentVideoList = null
                            throw Exception("Video list is empty.")
                        }
                }

                val result = InitResult(
                    videoList = currentVideoList,
                    videoIndex = qualityIndex,
                    position = episodePosition,
                )
                Pair(result, Result.success(true))
            } else {
                // Unlikely but okay
                Pair(defaultResult, Result.success(false))
            }
        } catch (e: Throwable) {
            Pair(defaultResult, Result.failure(e))
        }
    }

    data class InitResult(
        val videoList: List<Video>?,
        val videoIndex: Int,
        val position: Long?,
    )

    private fun initEpisodeList(anime: Anime): List<Episode> {
        val episodes = runBlocking { getEpisodesByAnimeId.await(anime.id) }

        return episodes
            .sortedWith(getEpisodeSort(anime, sortDescending = false))
            .run {
                if (basePreferences.downloadedOnly().get()) {
                    filterDownloadedEpisodes(anime)
                } else {
                    this
                }
            }
            .map { it.toDbEpisode() }
    }

    private var hasTrackers: Boolean = false
    private val checkTrackers: (Anime) -> Unit = { anime ->
        val tracks = runBlocking { getTracks.await(anime.id) }
        hasTrackers = tracks.isNotEmpty()
    }

    fun isEpisodeOnline(): Boolean? {
        val anime = currentAnime ?: return null
        val episode = currentEpisode ?: return null
        return currentSource is AnimeHttpSource && !EpisodeLoader.isDownload(
            episode.toDomainEpisode()!!,
            anime,
        )
    }

    suspend fun loadEpisode(episodeId: Long?): Pair<List<Video>?, String>? {
        val anime = currentAnime ?: return null
        val source = sourceManager.getOrStub(anime.source)

        val chosenEpisode = this.currentPlaylist.firstOrNull { ep -> ep.id == episodeId } ?: return null

        mutableState.update { it.copy(episode = chosenEpisode) }

        return withIOContext {
            try {
                val currentEpisode = currentEpisode ?: throw Exception("No episode loaded.")
                currentVideoList = EpisodeLoader.getLinks(
                    currentEpisode.toDomainEpisode()!!,
                    anime,
                    source,
                )
                this@PlayerViewModel.episodeId = currentEpisode.id!!
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { e.message ?: "Error getting links" }
            }

            Pair(currentVideoList, anime.title + " - " + chosenEpisode.name)
        }
    }

    /**
     * Called every time a second is reached in the player. Used to mark the flag of episode being
     * seen, update tracking services, enqueue downloaded episode deletion and download next episode.
     */
    fun onSecondReached(position: Long, duration: Long) {
        if (state.value.isLoadingEpisode) return
        val currentEp = currentEpisode ?: return
        if (episodeId == -1L) return

        val seconds = position * 1000L
        val totalSeconds = duration * 1000L
        // Save last second seen and mark as seen if needed
        currentEp.last_second_seen = seconds
        currentEp.total_seconds = totalSeconds

        episodePosition = seconds

        val progress = playerPreferences.progressPreference().get()
        val shouldTrack = !incognitoMode || hasTrackers
        if (seconds >= totalSeconds * progress && shouldTrack) {
            currentEp.seen = true
            updateTrackEpisodeSeen(currentEp)
            deleteEpisodeIfNeeded(currentEp)
        }

        saveWatchingProgress(currentEp)

        val inDownloadRange = seconds.toDouble() / totalSeconds > 0.35
        if (inDownloadRange) {
            downloadNextEpisodes()
        }
    }

    private fun downloadNextEpisodes() {
        if (downloadAheadAmount == 0) return
        val anime = currentAnime ?: return

        // Only download ahead if current + next episode is already downloaded too to avoid jank
        if (getCurrentEpisodeIndex() == this.currentPlaylist.lastIndex) return
        val currentEpisode = currentEpisode ?: return

        val nextEpisode = this.currentPlaylist[getCurrentEpisodeIndex() + 1]
        val episodesAreDownloaded =
            EpisodeLoader.isDownload(currentEpisode.toDomainEpisode()!!, anime) &&
                EpisodeLoader.isDownload(nextEpisode.toDomainEpisode()!!, anime)

        viewModelScope.launchIO {
            if (!episodesAreDownloaded) {
                return@launchIO
            }
            val episodesToDownload = getNextEpisodes.await(anime.id, nextEpisode.id!!)
                .take(downloadAheadAmount)
            downloadManager.downloadEpisodes(anime, episodesToDownload)
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last episode actually exists.
     * If both conditions are satisfied enqueues episode for delete
     * @param chosenEpisode current episode, which is going to be marked as seen.
     */
    private fun deleteEpisodeIfNeeded(chosenEpisode: Episode) {
        // Determine which episode should be deleted and enqueue
        val currentEpisodePosition = this.currentPlaylist.indexOf(chosenEpisode)
        val removeAfterSeenSlots = downloadPreferences.removeAfterReadSlots().get()
        val episodeToDelete = this.currentPlaylist.getOrNull(
            currentEpisodePosition - removeAfterSeenSlots,
        )
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
    fun bookmarkEpisode(episodeId: Long?, bookmarked: Boolean) {
        viewModelScope.launchNonCancellable {
            updateEpisode.await(
                EpisodeUpdate(
                    id = episodeId!!,
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
        val anime = currentAnime ?: return

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
                        location = Location.Pictures(relativePath),
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
        val anime = currentAnime ?: return

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
    fun setAsCover(imageStream: () -> InputStream) {
        val anime = currentAnime ?: return

        viewModelScope.launchNonCancellable {
            val result = try {
                anime.editCover(Injekt.get(), imageStream())
                if (anime.isLocal() || anime.favorite) {
                    SetAsCover.Success
                } else {
                    SetAsCover.AddToLibraryFirst
                }
            } catch (e: Exception) {
                SetAsCover.Error
            }
            eventChannel.send(Event.SetCoverResult(result))
        }
    }

    /**
     * Results of the save image feature.
     */
    sealed class SaveImageResult {
        class Success(val uri: Uri) : SaveImageResult()
        class Error(val error: Throwable) : SaveImageResult()
    }

    private fun updateTrackEpisodeSeen(episode: Episode) {
        if (basePreferences.incognitoMode().get() || !hasTrackers) return
        if (!trackPreferences.autoUpdateTrack().get()) return

        val anime = currentAnime ?: return
        val context = Injekt.get<Application>()

        viewModelScope.launchNonCancellable {
            trackEpisode.await(context, anime.id, episode.episode_number.toDouble())
        }
    }

    /**
     * Enqueues this [episode] to be deleted when [deletePendingEpisodes] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteSeenEpisodes(episode: Episode) {
        if (!episode.seen) return
        val anime = currentAnime ?: return
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
        val anime = currentAnime ?: return default
        val skipIntroLength = anime.skipIntroLength
        return when {
            resolveDefault && skipIntroLength <= 0 -> default
            else -> anime.skipIntroLength
        }
    }

    /**
     * Updates the skipIntroLength for the open anime.
     */
    fun setAnimeSkipIntroLength(skipIntroLength: Long) {
        val anime = currentAnime ?: return
        viewModelScope.launchIO {
            setAnimeViewerFlags.awaitSetSkipIntroLength(anime.id, skipIntroLength)
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
            "${anime.title} - ${episode.name}".takeBytes(
                DiskUtil.MAX_FILE_NAME_BYTES - filenameSuffix.byteSize(),
            ),
        ) + filenameSuffix
    }

    /**
     * Returns the response of the AniSkipApi for this episode.
     * just works if tracking is enabled.
     */
    suspend fun aniSkipResponse(playerDuration: Int?): List<Stamp>? {
        val animeId = currentAnime?.id ?: return null
        val trackerManager = Injekt.get<TrackerManager>()
        var malId: Long?
        val episodeNumber = currentEpisode?.episode_number?.toInt() ?: return null
        if (getTracks.await(animeId).isEmpty()) {
            logcat { "AniSkip: No tracks found for anime $animeId" }
            return null
        }

        getTracks.await(animeId).map { track ->
            val tracker = trackerManager.get(track.trackerId)
            malId = when (tracker) {
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

    fun showEpisodeList() {
        mutableState.update { it.copy(dialog = Dialog.EpisodeList) }
    }

    fun showSpeedPicker() {
        mutableState.update { it.copy(dialog = Dialog.SpeedPicker) }
    }

    fun showSkipIntroLength() {
        mutableState.update { it.copy(dialog = Dialog.SkipIntroLength) }
    }

    fun showSubtitleSettings() {
        mutableState.update { it.copy(sheet = Sheet.SubtitleSettings) }
    }

    fun showScreenshotOptions() {
        mutableState.update { it.copy(sheet = Sheet.ScreenshotOptions) }
    }

    fun showPlayerSettings() {
        mutableState.update { it.copy(sheet = Sheet.PlayerSettings) }
    }

    fun showVideoChapters() {
        mutableState.update { it.copy(sheet = Sheet.VideoChapters) }
    }

    fun showStreamsCatalog() {
        mutableState.update { it.copy(sheet = Sheet.StreamsCatalog) }
    }

    fun closeDialogSheet() {
        mutableState.update { it.copy(dialog = null, sheet = null) }
    }

    fun updateSkipIntroText(text: String) {
        mutableState.update { it.copy(skipIntroText = text) }
    }

    fun updatePlayerInformation(stringResource: StringResource) {
        mutableState.update { it.copy(playerInformation = stringResource) }
    }

    fun updateSeekState(seekState: SeekState) {
        mutableState.update { it.copy(seekState = seekState) }
    }

    fun updatePlayerTime(paused: Boolean? = null, position: Long? = null, duration: Long? = null, readAhead: Long? = null) {
        with(state.value.timeData) {
            val pause = paused ?: this.paused
            val pos = position ?: this.position
            val dur = duration ?: this.duration
            val rea = readAhead ?: this.readAhead
            mutableState.update { it.copy(timeData = TimeData(pause, pos, dur, rea)) }
        }
    }

    @Immutable
    data class State(
        val timeData: TimeData = TimeData(),
        val episodeList: List<Episode> = emptyList(),
        val episode: Episode? = null,
        val anime: Anime? = null,
        val source: AnimeSource? = null,
        val videoStreams: VideoStreams = VideoStreams(),
        val isLoadingEpisode: Boolean = false,
        val dialog: Dialog? = null,
        val sheet: Sheet? = null,
        val videoChapters: List<MPVView.Chapter> = emptyList(),
        val skipIntroText: String = "",
        val playerInformation: StringResource = MR.strings.enable_auto_play,
        val seekState: SeekState = SeekState.NONE,
    )

    class TimeData(val paused: Boolean, val position: Long, val duration: Long, val readAhead: Long) {
        constructor() : this(false,0L, 0L, 0L)
    }

    class VideoStreams(val quality: Stream, val subtitle: Stream, val audio: Stream) {
        constructor() : this(Stream(), Stream(), Stream())
        class Stream(var index: Int = 0, var tracks: Array<Track> = emptyArray())
    }

    sealed class Dialog {
        data object EpisodeList : Dialog()
        data object SpeedPicker : Dialog()
        data object SkipIntroLength : Dialog()
    }

    sealed class Sheet {
        data object SubtitleSettings : Sheet()
        data object ScreenshotOptions : Sheet()
        data object PlayerSettings : Sheet()
        data object VideoChapters : Sheet()
        data object StreamsCatalog : Sheet()
    }

    sealed class Event {
        data class SetAnimeSkipIntro(val duration: Int) : Event()
        data class SetCoverResult(val result: SetAsCover) : Event()
        data class SavedImage(val result: SaveImageResult) : Event()
        data class ShareImage(val uri: Uri, val seconds: String) : Event()
    }
}
