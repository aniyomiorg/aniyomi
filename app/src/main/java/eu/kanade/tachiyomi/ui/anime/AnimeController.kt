package eu.kanade.tachiyomi.ui.anime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.data.episode.NoEpisodesException
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.anime.AnimeScreen
import eu.kanade.presentation.anime.components.DeleteEpisodesDialog
import eu.kanade.presentation.components.ChangeCategoryDialog
import eu.kanade.presentation.components.DuplicateAnimeDialog
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.DownloadCustomAmountDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.isLocalOrStub
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadService
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.databinding.PrefSkipIntroLengthBinding
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.ui.HistoryTabsController
import eu.kanade.tachiyomi.ui.UpdatesTabsController
import eu.kanade.tachiyomi.ui.anime.AnimePresenter.Dialog
import eu.kanade.tachiyomi.ui.anime.episode.EpisodesSettingsSheet
import eu.kanade.tachiyomi.ui.anime.info.AnimeFullCoverDialog
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.anime.track.TrackSearchDialog
import eu.kanade.tachiyomi.ui.anime.track.TrackSheet
import eu.kanade.tachiyomi.ui.animecategory.AnimeCategoryController
import eu.kanade.tachiyomi.ui.animelib.AnimelibController
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.browse.migration.search.AnimeSearchController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.isTabletUi
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.episode.model.Episode as DomainEpisode

class AnimeController : FullComposeController<AnimePresenter> {

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

    private val playerPreferences: PlayerPreferences = Injekt.get()

    private val trackPreferences: TrackPreferences = Injekt.get()

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
        if (state is AnimeScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as AnimeScreenState.Success
        val isHttpSource = remember { successState.source is AnimeHttpSource }
        val scope = rememberCoroutineScope()

        val configuration = LocalConfiguration.current
        val isTabletUi = remember { configuration.isTabletUi() } // won't survive config change

        AnimeScreen(
            state = successState,
            snackbarHostState = snackbarHostState,
            isTabletUi = isTabletUi,
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
            onEditCategoryClicked = presenter::promptChangeCategories.takeIf { successState.anime.favorite },
            onMigrateClicked = this::migrateAnime.takeIf { successState.anime.favorite },
            onMultiBookmarkClicked = presenter::bookmarkEpisodes,
            onMultiMarkAsSeenClicked = presenter::markEpisodesSeen,
            onMarkPreviousAsSeenClicked = presenter::markPreviousEpisodeSeen,
            onMultiDeleteClicked = presenter::showDeleteEpisodeDialog,
            changeAnimeSkipIntro = this::changeAnimeSkipIntro.takeIf { successState.anime.favorite },
            onEpisodeSelected = presenter::toggleSelection,
            onAllEpisodeSelected = presenter::toggleAllSelection,
            onInvertSelection = presenter::invertSelection,
        )

        val onDismissRequest = { presenter.dismissDialog() }
        when (val dialog = (state as? AnimeScreenState.Success)?.dialog) {
            is Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        router.pushController(AnimeCategoryController())
                    },
                    onConfirm = { include, _ ->
                        if (!dialog.anime.favorite) onFavoriteAdded()
                        presenter.moveAnimeToCategoriesAndAddToLibrary(dialog.anime, include)
                    },
                )
            }
            is Dialog.DeleteEpisodes -> {
                DeleteEpisodesDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        presenter.toggleAllSelection(false)
                        deleteEpisodes(dialog.episodes)
                    },
                )
            }
            is Dialog.DownloadCustomAmount -> {
                DownloadCustomAmountDialog(
                    maxAmount = dialog.max,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { amount ->
                        val episodesToDownload = presenter.getUnseenEpisodesSorted().take(amount)
                        if (episodesToDownload.isNotEmpty()) {
                            scope.launch { downloadEpisodes(episodesToDownload) }
                        }
                    },
                )
            }
            is Dialog.DuplicateAnime -> {
                DuplicateAnimeDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        presenter.toggleFavorite(
                            onRemoved = {},
                            onAdded = {},
                            checkDuplicate = false,
                        )
                    },
                    onOpenAnime = { router.pushController(AnimeController(dialog.duplicate.id)) },
                    duplicateFrom = presenter.getAnimeSourceOrStub(dialog.duplicate),
                )
            }
            null -> {}
        }
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
            source.getAnimeUrl(anime.toSAnime())
        } catch (e: Exception) {
            return
        }

        val activity = activity ?: return
        val intent = WebViewActivity.newIntent(activity, url, source.id, anime.title)
        startActivity(intent)
    }

    private fun changeAnimeSkipIntro() {
        val anime = presenter.anime ?: return
        val playerActivity = PlayerActivity()
        var newSkipIntroLength = playerActivity.presenter.getAnimeSkipIntroLength()
        val binding = PrefSkipIntroLengthBinding.inflate(LayoutInflater.from(activity))

        playerActivity.presenter.anime = anime.toDbAnime()
        with(binding.skipIntroColumn) {
            value = playerActivity.presenter.getAnimeSkipIntroLength()
            setOnValueChangedListener { _, _, newValue ->
                newSkipIntroLength = newValue
            }
        }
        activity?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(R.string.action_change_intro_length)
                .setView(binding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    playerActivity.presenter.setAnimeSkipIntroLength(newSkipIntroLength)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun shareAnime() {
        val context = view?.context ?: return
        val anime = presenter.anime ?: return
        val source = presenter.source as? AnimeHttpSource ?: return
        try {
            val url = source.getAnimeUrl(anime.toSAnime())
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    private fun onFavoriteClick() {
        presenter.toggleFavorite(
            onRemoved = this::onFavoriteRemoved,
            onAdded = this::onFavoriteAdded,
        )
    }

    private fun onFavoriteAdded() {
        val successState = presenter.state.value as AnimeScreenState.Success
        if (trackPreferences.trackOnAddingToLibrary().get() && successState.trackingAvailable) {
            trackSheet.show()
            trackSheet.setOnDismissListener { activity?.toast(R.string.manga_added_library) }
        } else {
            activity?.toast(R.string.manga_added_library)
        }
    }

    private fun onFavoriteRemoved() {
        val context = activity ?: return
        context.toast(R.string.manga_removed_library)
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
            is UpdatesTabsController,
            is HistoryTabsController,
            -> {
                // Manually navigate to AnimelibController
                router.handleBack()
                (router.activity as MainActivity).setSelectedNavItem(R.id.nav_animelib)
                val controller = router.getControllerWithTag(R.id.nav_animelib.toString()) as AnimelibController
                controller.search(query)
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
        val controller = AnimeSearchController(anime)
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
        if (playerPreferences.alwaysUseExternalPlayer().get() != altPlayer) {
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
                EpisodeLoader.getLink(episode.toDbEpisode(), anime.toDbAnime(), source)
                    .awaitSingle()
            } catch (e: Exception) {
                launchUI { context.toast(e.message) }
                return@launchIO
            }
            if (video != null) {
                EXT_EPISODE = episode
                EXT_ANIME = anime

                val extIntent = ExternalIntents(anime, source).getExternalIntent(
                    episode,
                    video,
                    context,
                )
                if (extIntent != null) {
                    try {
                        startActivityForResult(extIntent, REQUEST_EXTERNAL)
                    } catch (e: Exception) {
                        launchUI { context.toast(e.message) }
                        return@launchIO
                    }
                }
            } else {
                launchUI { context.toast("Couldn't find any video links.") }
                return@launchIO
            }
        }
    }

    fun onFetchEpisodesError(error: Throwable) {
        if (error is NoEpisodesException) {
            activity?.toast(R.string.no_episodes_error)
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
                    downloadEpisodes(items.map { it.episode }, startNow = true)
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

    private suspend fun downloadEpisodes(episodes: List<DomainEpisode>, startNow: Boolean = false, alt: Boolean = false) {
        if (startNow) {
            val episodeId = episodes.singleOrNull()?.id ?: return
            presenter.startDownloadingNow(episodeId)
        } else {
            presenter.downloadEpisodes(episodes, alt)
        }

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

    private fun deleteEpisodes(episodes: List<DomainEpisode>) {
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
                presenter.showDownloadCustomDialog()
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
