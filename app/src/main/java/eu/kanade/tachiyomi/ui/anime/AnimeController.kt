package eu.kanade.tachiyomi.ui.anime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.data.episode.NoEpisodesException
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.presentation.anime.AnimeScreen
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.EpisodeDownloadAction
import eu.kanade.presentation.util.calculateWindowWidthSizeClass
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.isLocalOrStub
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.ui.anime.episode.DownloadCustomEpisodesDialog
import eu.kanade.tachiyomi.ui.anime.episode.EpisodesSettingsSheet
import eu.kanade.tachiyomi.ui.anime.info.AnimeFullCoverDialog
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.anime.track.TrackSearchDialog
import eu.kanade.tachiyomi.ui.anime.track.TrackSheet
import eu.kanade.tachiyomi.ui.animelib.AnimelibController
import eu.kanade.tachiyomi.ui.animelib.ChangeAnimeCategoriesDialog
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.browse.animesource.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.browse.migration.search.AnimeSearchController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.recent.animehistory.AnimeHistoryController
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.materialdialogs.QuadStateTextView
import eu.kanade.tachiyomi.widget.materialdialogs.await
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.episode.model.Episode as DomainEpisode

class AnimeController :
    FullComposeController<AnimePresenter>,
    ChangeAnimeCategoriesDialog.Listener,
    DownloadCustomEpisodesDialog.Listener {

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(ANIME_EXTRA))

    constructor(
        animeId: Long,
        fromSource: Boolean = false,
    ) : super(bundleOf(ANIME_EXTRA to animeId, FROM_SOURCE_EXTRA to fromSource)) {
        this.animeId = animeId
    }

    var animeId: Long

    val fromSource: Boolean
        get() = presenter.isFromSource

    // Sheet containing filter/sort/display items.
    private lateinit var settingsSheet: EpisodesSettingsSheet

    private lateinit var trackSheet: TrackSheet

    private val snackbarHostState = SnackbarHostState()

    private val preferences: PreferencesHelper = Injekt.get()

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        val actionBar = (activity as? AppCompatActivity)?.supportActionBar
        if (type.isEnter) {
            actionBar?.hide()
        } else {
            actionBar?.show()
        }
    }

    override fun createPresenter(): AnimePresenter {
        return AnimePresenter(
            animeId = animeId,
            isFromSource = args.getBoolean(FROM_SOURCE_EXTRA, false),
        )
    }

    @Composable
    override fun ComposeContent() {
        val state by presenter.state.collectAsState()
        if (state is AnimeScreenState.Success) {
            val successState = state as AnimeScreenState.Success
            val isHttpSource = remember { successState.source is AnimeHttpSource }
            AnimeScreen(
                state = successState,
                snackbarHostState = snackbarHostState,
                windowWidthSizeClass = calculateWindowWidthSizeClass(),
                onBackClicked = router::popCurrentController,
                onEpisodeClicked = this::openEpisode,
                onDownloadEpisode = this::onDownloadEpisodes.takeIf { !successState.source.isLocalOrStub() },
                onAddToLibraryClicked = this::onFavoriteClick,
                onWebViewClicked = this::openAnimeInWebView.takeIf { isHttpSource },
                onTrackingClicked = trackSheet::show.takeIf { successState.trackingAvailable },
                onTagClicked = this::performGenreSearch,
                onFilterButtonClicked = settingsSheet::show,
                onRefresh = presenter::fetchAllFromSource,
                onContinueWatching = this::continueWatching,
                onSearch = this::performSearch,
                onCoverClicked = this::openCoverDialog,
                onShareClicked = this::shareAnime.takeIf { isHttpSource },
                onDownloadActionClicked = this::runDownloadEpisodeAction.takeIf { !successState.source.isLocalOrStub() },
                onEditCategoryClicked = this::onCategoriesClick.takeIf { successState.anime.favorite },
                onMigrateClicked = this::migrateAnime.takeIf { successState.anime.favorite },
                onMultiBookmarkClicked = presenter::bookmarkEpisodes,
                onMultiMarkAsSeenClicked = presenter::markEpisodesSeen,
                onMarkPreviousAsSeenClicked = presenter::markPreviousEpisodeSeen,
                onMultiDeleteClicked = this::deleteEpisodesWithConfirmation,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    // Let compose view handle this
    override fun handleBack(): Boolean {
        (activity as? OnBackPressedDispatcherOwner)?.onBackPressedDispatcher?.onBackPressed()
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        settingsSheet = EpisodesSettingsSheet(router, presenter)
        trackSheet = TrackSheet(this, (activity as MainActivity).supportFragmentManager)
        return super.onCreateView(inflater, container, savedViewState)
    }

    // Anime info - start

    fun onFetchAnimeInfoError(error: Throwable) {
        // Ignore early hints "errors" that aren't handled by OkHttp
        if (error is HttpException && error.code == 103) {
            return
        }
        activity?.toast(error.message)
    }

    private fun openAnimeInWebView() {
        val anime = presenter.anime ?: return
        val source = presenter.source as? AnimeHttpSource ?: return

        val url = try {
            source.animeDetailsRequest(anime.toDbAnime()).url.toString()
        } catch (e: Exception) {
            return
        }

        val activity = activity ?: return
        val intent = WebViewActivity.newIntent(activity, url, source.id, anime.title)
        startActivity(intent)
    }

    fun shareAnime() {
        val context = view?.context ?: return
        val anime = presenter.anime ?: return
        val source = presenter.source as? AnimeHttpSource ?: return
        try {
            val url = source.animeDetailsRequest(anime.toDbAnime()).url.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    private fun onFavoriteClick(checkDuplicate: Boolean = true) {
        presenter.toggleFavorite(
            onRemoved = this::onFavoriteRemoved,
            onAdded = { activity?.toast(activity?.getString(R.string.manga_added_library)) },
            onDuplicateExists = if (checkDuplicate) {
                {
                    AddDuplicateAnimeDialog(
                        target = this,
                        libraryAnime = it.toDbAnime(),
                        onAddToLibrary = { onFavoriteClick(checkDuplicate = false) },
                    ).showDialog(router)
                }
            } else null,
            onRequireCategory = { anime, categories ->
                val ids = presenter.getAnimeCategoryIds(anime)
                val preselected = categories.map {
                    if (it.id in ids) {
                        QuadStateTextView.State.CHECKED.ordinal
                    } else {
                        QuadStateTextView.State.UNCHECKED.ordinal
                    }
                }.toTypedArray()
                showChangeCategoryDialog(anime.toDbAnime(), categories, preselected)
            },
        )
    }

    private fun onFavoriteRemoved() {
        val context = activity ?: return
        context.toast(activity?.getString(R.string.manga_removed_library))
        viewScope.launch {
            if (!presenter.hasDownloads()) return@launch
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.delete_downloads_for_anime),
                actionLabel = context.getString(R.string.action_delete),
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) {
                presenter.deleteDownloads()
            }
        }
    }

    fun onTrackingClick() {
        trackSheet.show()
    }

    private fun onCategoriesClick() {
        val anime = presenter.anime ?: return
        val categories = presenter.getCategories()

        val ids = presenter.getAnimeCategoryIds(anime)
        val preselected = categories.map {
            if (it.id in ids) {
                QuadStateTextView.State.CHECKED.ordinal
            } else {
                QuadStateTextView.State.UNCHECKED.ordinal
            }
        }.toTypedArray()
        showChangeCategoryDialog(anime.toDbAnime(), categories, preselected)
    }

    private fun showChangeCategoryDialog(anime: Anime, categories: List<Category>, preselected: Array<Int>) {
        ChangeAnimeCategoriesDialog(this, listOf(anime), categories, preselected.toIntArray())
            .showDialog(router)
    }

    override fun updateCategoriesForAnimes(
        animes: List<Anime>,
        addCategories: List<Category>,
        removeCategories: List<Category>,
    ) {
        val changed = animes.firstOrNull() ?: return
        presenter.moveAnimeToCategoriesAndAddToLibrary(changed, addCategories)
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    private fun performSearch(query: String, global: Boolean) {
        if (global) {
            router.pushController(GlobalAnimeSearchController(query))
            return
        }

        if (router.backstackSize < 2) {
            return
        }

        when (val previousController = router.backstack[router.backstackSize - 2].controller) {
            is AnimelibController -> {
                router.handleBack()
                previousController.search(query)
            }
            is AnimeUpdatesController,
            is AnimeHistoryController, -> {
                // Manually navigate to AnimelibController
                router.handleBack()
                (router.activity as MainActivity).setSelectedNavItem(R.id.nav_animelib)
                val controller = router.getControllerWithTag(R.id.nav_animelib.toString()) as AnimelibController
                controller.search(query)
            }
            is LatestUpdatesController -> {
                // Search doesn't currently work in source Latest view
                return
            }
            is BrowseAnimeSourceController -> {
                router.handleBack()
                previousController.searchWithQuery(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     *
     * @param genreName the search genre to the parent controller
     */
    private fun performGenreSearch(genreName: String) {
        if (router.backstackSize < 2) {
            return
        }

        val previousController = router.backstack[router.backstackSize - 2].controller
        val presenterSource = presenter.source

        if (previousController is BrowseAnimeSourceController &&
            presenterSource is AnimeHttpSource
        ) {
            router.handleBack()
            previousController.searchWithGenre(genreName)
        } else {
            performSearch(genreName, global = false)
        }
    }

    private fun openCoverDialog() {
        val animeId = presenter.anime?.id ?: return
        router.pushController(AnimeFullCoverDialog(animeId).withFadeTransaction())
    }

    /**
     * Initiates source migration for the specific anime.
     */
    private fun migrateAnime() {
        val anime = presenter.anime ?: return
        val controller = AnimeSearchController(anime.toDbAnime())
        controller.targetController = this
        router.pushController(controller)
    }

    // Anime info - end

    // Episodes list - start

    private fun continueWatching() {
        val episode = presenter.getNextUnseenEpisode()
        if (episode != null) openEpisode(episode)
    }

    private fun openEpisode(episode: DomainEpisode, altPlayer: Boolean = false) {
        if (preferences.alwaysUseExternalPlayer() != altPlayer) {
            openEpisodeExternal(episode)
        } else {
            openEpisodeInternal(episode)
        }
    }

    private fun openEpisodeInternal(episode: DomainEpisode) {
        activity?.run {
            startActivity(PlayerActivity.newIntent(this, episode.animeId, episode.id))
        }
    }

    private fun openEpisodeExternal(episode: DomainEpisode) {
        val context = activity ?: return
        val anime = presenter.anime ?: return
        val source = presenter.source ?: return
        launchIO {
            val video = try {
                EpisodeLoader.getLink(episode.toDbEpisode(), anime.toDbAnime(), source).awaitSingle()
            } catch (e: Exception) {
                launchUI { context.toast(e.message) }
                return@launchIO
            }
            if (video != null) {
                EXT_EPISODE = episode
                EXT_ANIME = presenter.anime

                val extIntent = ExternalIntents(anime, source).getExternalIntent(
                    episode,
                    video,
                    context,
                )
                if (extIntent != null) try {
                    startActivityForResult(extIntent, REQUEST_EXTERNAL)
                } catch (e: Exception) {
                    launchUI { context.toast(e.message) }
                    return@launchIO
                }
            } else {
                launchUI { context.toast("Couldn't find any video links.") }
                return@launchIO
            }
        }
    }

    fun onFetchEpisodesError(error: Throwable) {
        if (error is NoEpisodesException) {
            activity?.toast(activity?.getString(R.string.no_episodes_error))
        } else {
            activity?.toast(error.message)
        }
    }

    // SELECTION MODE ACTIONS

    private fun onDownloadEpisodes(
        items: List<EpisodeItem>,
        action: EpisodeDownloadAction,
    ) {
        viewScope.launch {
            when (action) {
                EpisodeDownloadAction.START -> {
                    downloadEpisodes(items.map { it.episode })
                    if (items.any { it.downloadState == AnimeDownload.State.ERROR }) {
                        AnimeDownloadService.start(activity!!)
                    }
                }
                EpisodeDownloadAction.START_NOW -> {
                    val episodeId = items.singleOrNull()?.episode?.id ?: return@launch
                    presenter.startDownloadingNow(episodeId)
                }
                EpisodeDownloadAction.CANCEL -> {
                    val episodeId = items.singleOrNull()?.episode?.id ?: return@launch
                    presenter.cancelDownload(episodeId)
                }
                EpisodeDownloadAction.DELETE -> {
                    deleteEpisodes(items.map { it.episode })
                }
                EpisodeDownloadAction.START_ALT -> {
                    downloadEpisodes(items.map { it.episode }, alt = true)
                    if (items.any { it.downloadState == AnimeDownload.State.ERROR }) {
                        AnimeDownloadService.start(activity!!)
                    }
                }
            }
        }
    }

    private suspend fun downloadEpisodes(
        episodes: List<DomainEpisode>,
        alt: Boolean = false,
    ) {
        presenter.downloadEpisodes(episodes, alt = alt)

        if (!presenter.isFavoritedAnime) {
            val result = snackbarHostState.showSnackbar(
                message = activity!!.getString(R.string.snack_add_to_animelib),
                actionLabel = activity!!.getString(R.string.action_add),
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed && !presenter.isFavoritedAnime) {
                onFavoriteClick()
            }
        }
    }

    private fun deleteEpisodesWithConfirmation(episodes: List<DomainEpisode>) {
        viewScope.launch {
            val result = MaterialAlertDialogBuilder(activity!!)
                .setMessage(R.string.confirm_delete_episodes)
                .await(android.R.string.ok, android.R.string.cancel)
            if (result == AlertDialog.BUTTON_POSITIVE) deleteEpisodes(episodes)
        }
    }

    fun deleteEpisodes(episodes: List<DomainEpisode>) {
        if (episodes.isEmpty()) return
        presenter.deleteEpisodes(episodes)
    }

    // OVERFLOW MENU DIALOGS

    private fun runDownloadEpisodeAction(action: DownloadAction) {
        val episodesToDownload = when (action) {
            DownloadAction.NEXT_1_CHAPTER -> presenter.getUnseenEpisodesSorted().take(1)
            DownloadAction.NEXT_5_CHAPTERS -> presenter.getUnseenEpisodesSorted().take(5)
            DownloadAction.NEXT_10_CHAPTERS -> presenter.getUnseenEpisodesSorted().take(10)
            DownloadAction.CUSTOM -> {
                showCustomDownloadDialog()
                return
            }
            DownloadAction.UNREAD_CHAPTERS -> presenter.getUnseenEpisodes()
            DownloadAction.ALL_CHAPTERS -> {
                (presenter.state.value as? AnimeScreenState.Success)?.episodes?.map { it.episode }
            }
        }
        if (!episodesToDownload.isNullOrEmpty()) {
            viewScope.launch { downloadEpisodes(episodesToDownload) }
        }
    }

    private fun showCustomDownloadDialog() {
        val availableEpisodes = presenter.processedEpisodes?.count() ?: return
        DownloadCustomEpisodesDialog(
            this,
            availableEpisodes,
        ).showDialog(router)
    }

    override fun downloadCustomEpisodes(amount: Int) {
        val episodesToDownload = presenter.getUnseenEpisodesSorted().take(amount)
        if (episodesToDownload.isNotEmpty()) {
            viewScope.launch { downloadEpisodes(episodesToDownload) }
        }
    }

    // Episodes list - end

    // Tracker sheet - start
    fun onNextTrackers(trackers: List<TrackItem>) {
        trackSheet.onNextTrackers(trackers)
    }

    fun onTrackingRefreshDone() {
    }

    fun onTrackingRefreshError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
        activity?.toast(error.message)
    }

    fun onTrackingSearchResults(results: List<AnimeTrackSearch>) {
        getTrackingSearchDialog()?.onSearchResults(results)
    }

    fun onTrackingSearchResultsError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
        getTrackingSearchDialog()?.onSearchResultsError(error.message)
    }

    private fun getTrackingSearchDialog(): TrackSearchDialog? {
        return trackSheet.getSearchDialog()
    }

    // Tracker sheet - end

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ExternalIntents.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val FROM_SOURCE_EXTRA = "from_source"
        const val ANIME_EXTRA = "anime"

        var EXT_EPISODE: DomainEpisode? = null
        var EXT_ANIME: DomainAnime? = null

        const val REQUEST_INTERNAL = 102
        const val REQUEST_EXTERNAL = 103
    }
}
