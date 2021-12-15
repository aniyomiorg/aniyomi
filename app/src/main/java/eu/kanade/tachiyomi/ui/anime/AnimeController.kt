package eu.kanade.tachiyomi.ui.anime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.imageLoader
import coil.request.ImageRequest
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.databinding.MangaControllerBinding
import eu.kanade.tachiyomi.ui.anime.episode.AnimeEpisodesHeaderAdapter
import eu.kanade.tachiyomi.ui.anime.episode.DeleteEpisodesDialog
import eu.kanade.tachiyomi.ui.anime.episode.DownloadCustomEpisodesDialog
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.ui.anime.episode.EpisodesAdapter
import eu.kanade.tachiyomi.ui.anime.episode.EpisodesSettingsSheet
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.ui.anime.info.AnimeFullCoverDialog
import eu.kanade.tachiyomi.ui.anime.info.AnimeInfoHeaderAdapter
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.anime.track.TrackSearchDialog
import eu.kanade.tachiyomi.ui.anime.track.TrackSheet
import eu.kanade.tachiyomi.ui.animelib.AnimelibController
import eu.kanade.tachiyomi.ui.animelib.ChangeAnimeCategoriesDialog
import eu.kanade.tachiyomi.ui.animelib.ChangeAnimeCoverDialog
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.getMainAppBarHeight
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.browse.animesource.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.browse.migration.search.AnimeSearchController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.recent.HistoryTabsController
import eu.kanade.tachiyomi.ui.recent.UpdatesTabsController
import eu.kanade.tachiyomi.ui.recent.animehistory.AnimeHistoryController
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.episode.NoEpisodesException
import eu.kanade.tachiyomi.util.hasCustomCover
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.getCoordinates
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.widget.materialdialogs.QuadStateTextView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import reactivecircus.flowbinding.recyclerview.scrollStateChanges
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.ArrayDeque
import java.util.Date
import kotlin.math.min

class AnimeController :
    NucleusController<MangaControllerBinding, AnimePresenter>,
    FabController,
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    BaseEpisodesAdapter.OnEpisodeClickListener,
    ChangeAnimeCoverDialog.Listener,
    ChangeAnimeCategoriesDialog.Listener,
    DownloadCustomEpisodesDialog.Listener,
    DeleteEpisodesDialog.Listener {

    constructor(anime: Anime?, fromSource: Boolean = false) : super(
        bundleOf(
            ANIME_EXTRA to (anime?.id ?: 0),
            FROM_SOURCE_EXTRA to fromSource
        )
    ) {
        this.anime = anime
        if (anime != null) {
            source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
        }
    }

    constructor(animeId: Long) : this(
        Injekt.get<AnimeDatabaseHelper>().getAnime(animeId).executeAsBlocking()
    )

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(ANIME_EXTRA))

    var anime: Anime? = null
        private set

    var currentExtEpisode: Episode? = null
        private set

    var source: AnimeSource? = null
        private set

    val fromSource = args.getBoolean(FROM_SOURCE_EXTRA, false)

    private val preferences: PreferencesHelper by injectLazy()
    private val coverCache: AnimeCoverCache by injectLazy()

    private var animeInfoAdapter: AnimeInfoHeaderAdapter? = null
    private var episodesHeaderAdapter: AnimeEpisodesHeaderAdapter? = null
    private var episodesAdapter: EpisodesAdapter? = null

    // Sheet containing filter/sort/display items.
    private var settingsSheet: EpisodesSettingsSheet? = null

    private var actionFab: ExtendedFloatingActionButton? = null
    private var actionFabScrollListener: RecyclerView.OnScrollListener? = null

    // Snackbar to add anime to animelib after downloading episode(s)
    private var addSnackbar: Snackbar? = null

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Selected items. Used to restore selections after a rotation.
     */
    private val selectedEpisodes = mutableSetOf<EpisodeItem>()

    private val isLocalSource by lazy { presenter.source.id == LocalAnimeSource.ID }

    private var lastClickPositionStack = ArrayDeque(listOf(-1))

    private var isRefreshingInfo = false
    private var isRefreshingEpisodes = false

    private var trackSheet: TrackSheet? = null

    private var dialog: DialogController? = null

    private val incognitoMode = preferences.incognitoMode().get()

    private val db: AnimeDatabaseHelper = Injekt.get()

    /**
     * For [recyclerViewUpdatesToolbarTitleAlpha]
     */
    private var recyclerViewToolbarTitleAlphaUpdaterAdded = false
    private val recyclerViewToolbarTitleAlphaUpdater = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateToolbarTitleAlpha()
        }
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return anime?.title
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        // Hide toolbar title on enter
        // No need to update alpha for cover dialog
        if (dialog == null) {
            updateToolbarTitleAlpha(if (type.isEnter) 0F else 1F)
        }
        recyclerViewUpdatesToolbarTitleAlpha(type.isEnter)
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (anime == null || source == null) {
            activity?.toast(R.string.anime_not_in_db)
            router.popController(this)
        }
    }

    override fun createPresenter(): AnimePresenter {
        return AnimePresenter(
            anime!!,
            source!!
        )
    }

    override fun createBinding(inflater: LayoutInflater) = MangaControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        listOfNotNull(binding.fullRecycler, binding.infoRecycler, binding.chaptersRecycler)
            .forEach {
                it.applyInsetter {
                    type(navigationBars = true) {
                        padding()
                    }
                }

                it.layoutManager = LinearLayoutManager(view.context)
                it.setHasFixedSize(true)
            }
        binding.actionToolbar.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true, horizontal = true)
            }
        }

        if (anime == null || source == null) return

        // Init RecyclerView and adapter
        animeInfoAdapter = AnimeInfoHeaderAdapter(this, fromSource, binding.infoRecycler != null).apply {
            setHasStableIds(true)
        }
        episodesHeaderAdapter = AnimeEpisodesHeaderAdapter(this).apply {
            setHasStableIds(true)
        }
        episodesAdapter = EpisodesAdapter(this, view.context)

        // Phone layout
        binding.fullRecycler?.let {
            val config = ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(true)
                .setStableIdMode(ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS)
                .build()
            it.adapter = ConcatAdapter(config, animeInfoAdapter, episodesHeaderAdapter, episodesAdapter)

            // Skips directly to chapters list if navigated to from the library
            it.post {
                if (!fromSource && preferences.jumpToChapters()) {
                    val mainActivityAppBar = (activity as? MainActivity)?.binding?.appbar
                    (it.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        1,
                        mainActivityAppBar?.height ?: 0
                    )
                    mainActivityAppBar?.isLifted = true
                }
            }

            it.scrollStateChanges()
                .onEach { _ ->
                    // Disable swipe refresh when view is not at the top
                    val firstPos = (it.layoutManager as LinearLayoutManager)
                        .findFirstCompletelyVisibleItemPosition()
                    binding.swipeRefresh.isEnabled = firstPos <= 0
                }
                .launchIn(viewScope)

            binding.fastScroller.doOnLayout { scroller ->
                scroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin += getMainAppBarHeight()
                }
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.swipeRefresh) { swipeRefresh, windowInsets ->
                swipeRefresh as SwipeRefreshLayout
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
                swipeRefresh.isRefreshing = false
                swipeRefresh.setProgressViewEndTarget(false, getMainAppBarHeight() + insets.top)
                updateRefreshing()
                windowInsets
            }
        }

        // Tablet layout
        binding.infoRecycler?.adapter = animeInfoAdapter
        binding.chaptersRecycler?.adapter = ConcatAdapter(episodesHeaderAdapter, episodesAdapter)

        episodesAdapter?.fastScroller = binding.fastScroller

        actionFabScrollListener = actionFab?.shrinkOnScroll(episodeRecycler)
        // Initially set FAB invisible; will become visible if unseen episodes are present
        actionFab?.isVisible = false

        binding.swipeRefresh.refreshes()
            .onEach {
                fetchAnimeInfoFromSource(manualFetch = true)
                fetchEpisodesFromSource(manualFetch = true)
            }
            .launchIn(viewScope)

        settingsSheet = EpisodesSettingsSheet(router, presenter) { group ->
            if (group is EpisodesSettingsSheet.Filter.FilterGroup) {
                updateFilterIconState()
            }
        }

        trackSheet = TrackSheet(this, anime!!, (activity as MainActivity).supportFragmentManager)

        updateFilterIconState()
        recyclerViewUpdatesToolbarTitleAlpha(true)
    }

    private fun recyclerViewUpdatesToolbarTitleAlpha(enable: Boolean) {
        val recycler = binding.fullRecycler ?: binding.infoRecycler ?: return
        if (enable) {
            if (!recyclerViewToolbarTitleAlphaUpdaterAdded) {
                recycler.addOnScrollListener(recyclerViewToolbarTitleAlphaUpdater)
                recyclerViewToolbarTitleAlphaUpdaterAdded = true
            }
        } else if (recyclerViewToolbarTitleAlphaUpdaterAdded) {
            recycler.removeOnScrollListener(recyclerViewToolbarTitleAlphaUpdater)
            recyclerViewToolbarTitleAlphaUpdaterAdded = false
        }
    }

    private fun updateToolbarTitleAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float? = null) {
        val scrolledList = binding.fullRecycler ?: binding.infoRecycler!!
        (activity as? MainActivity)?.binding?.appbar?.titleTextAlpha = when {
            // Specific alpha provided
            alpha != null -> alpha

            // First item isn't in view, full opacity
            (scrolledList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 0 -> 1F

            // Based on scroll amount when first item is in view
            else -> min(scrolledList.computeVerticalScrollOffset(), 255) / 255F
        }
    }

    private fun updateFilterIconState() {
        episodesHeaderAdapter?.setHasActiveFilters(settingsSheet?.filters?.hasActiveFilters() == true)
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        actionFab = fab
        fab.setText(R.string.action_start)
        fab.setIconResource(R.drawable.ic_play_arrow_24dp)
        fab.setOnClickListener {
            val item = presenter.getNextUnseenEpisode()
            if (item != null) {
                // Get coordinates and start animation
                actionFab?.getCoordinates()?.let { coordinates ->
                    binding.revealView.showRevealEffect(
                        coordinates.x,
                        coordinates.y,
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator?) {
                                openEpisode(item.episode)
                            }
                        }
                    )
                }
            } else {
                view?.context?.toast(R.string.no_next_episode)
            }
        }
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener(null)
        actionFabScrollListener?.let { episodeRecycler.removeOnScrollListener(it) }
        actionFab = null
    }

    private fun updateFabVisibility() {
        val context = view?.context ?: return
        val adapter = episodesAdapter ?: return
        val fab = actionFab ?: return
        fab.isVisible = adapter.items.any { !it.seen }
        if (adapter.items.any { it.seen }) {
            fab.text = context.getString(R.string.action_resume)
        }
    }

    override fun onDestroyView(view: View) {
        recyclerViewUpdatesToolbarTitleAlpha(false)
        destroyActionModeIfNeeded()
        binding.actionToolbar.destroy()
        animeInfoAdapter = null
        episodesHeaderAdapter = null
        episodesAdapter = null
        settingsSheet = null
        addSnackbar?.dismiss()
        super.onDestroyView(view)
    }

    override fun onActivityResumed(activity: Activity) {
        if (view == null) return

        // Check if animation view is visible
        if (binding.revealView.isVisible) {
            // Show the unreveal effect
            actionFab?.getCoordinates()?.let { coordinates ->
                binding.revealView.hideRevealEffect(coordinates.x, coordinates.y, 1920)
            }
        }

        super.onActivityResumed(activity)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.anime, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Hide options for local anime
        menu.findItem(R.id.action_share).isVisible = !isLocalSource
        menu.findItem(R.id.download_group).isVisible = !isLocalSource

        // Hide options for non-animelib anime
        menu.findItem(R.id.action_edit_categories).isVisible = presenter.anime.favorite && presenter.getCategories().isNotEmpty()
        menu.findItem(R.id.action_migrate).isVisible = presenter.anime.favorite
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareAnime()
            R.id.download_next, R.id.download_next_5, R.id.download_next_10,
            R.id.download_custom, R.id.download_unread, R.id.download_all
            -> downloadEpisodes(item.itemId)

            R.id.action_edit_categories -> onCategoriesClick()
            R.id.action_migrate -> migrateAnime()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateRefreshing() {
        binding.swipeRefresh.isRefreshing = isRefreshingInfo || isRefreshingEpisodes
    }

    // Anime info - start

    /**
     * Check if anime is initialized.
     * If true update header with anime information,
     * if false fetch anime information
     *
     * @param anime anime object containing information about anime.
     * @param source the source of the anime.
     */
    fun onNextAnimeInfo(anime: Anime, source: AnimeSource) {
        if (anime.initialized) {
            // Update view.
            animeInfoAdapter?.update(anime, source)
        } else {
            // Initialize anime.
            fetchAnimeInfoFromSource()
        }
    }

    /**
     * Start fetching anime information from source.
     */
    private fun fetchAnimeInfoFromSource(manualFetch: Boolean = false) {
        isRefreshingInfo = true
        updateRefreshing()

        // Call presenter and start fetching anime information
        presenter.fetchAnimeFromSource(manualFetch)
    }

    fun onFetchAnimeInfoDone() {
        isRefreshingInfo = false
        updateRefreshing()
    }

    fun onFetchAnimeInfoError(error: Throwable) {
        isRefreshingInfo = false
        updateRefreshing()
        activity?.toast(error.message)
    }

    fun onTrackingCount(trackCount: Int) {
        animeInfoAdapter?.setTrackingCount(trackCount)
    }

    fun openAnimeInWebView() {
        val source = presenter.source as? AnimeHttpSource ?: return

        val url = try {
            source.animeDetailsRequest(presenter.anime).url.toString()
        } catch (e: Exception) {
            return
        }

        val activity = activity ?: return
        val intent = WebViewActivity.newIntent(activity, url, source.id, presenter.anime.title)
        startActivity(intent)
    }

    fun shareAnime() {
        val context = view?.context ?: return

        val source = presenter.source as? AnimeHttpSource ?: return
        try {
            val url = source.animeDetailsRequest(presenter.anime).url.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    fun onFavoriteClick() {
        val anime = presenter.anime

        if (anime.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_removed_library))
            activity?.invalidateOptionsMenu()
        } else {
            addToAnimelib(anime)
        }
    }

    fun onTrackingClick() {
        trackSheet?.show()
    }

    private fun addToAnimelib(anime: Anime) {
        val categories = presenter.getCategories()
        val defaultCategoryId = preferences.defaultCategory()
        val defaultCategory = categories.find { it.id == defaultCategoryId }

        when {
            // Default category set
            defaultCategory != null -> {
                toggleFavorite()
                presenter.moveAnimeToCategory(anime, defaultCategory)
                activity?.toast(activity?.getString(R.string.manga_added_library))
                activity?.invalidateOptionsMenu()
            }

            // Automatic 'Default' or no categories
            defaultCategoryId == 0 || categories.isEmpty() -> {
                toggleFavorite()
                presenter.moveAnimeToCategory(anime, null)
                activity?.toast(activity?.getString(R.string.manga_added_library))
                activity?.invalidateOptionsMenu()
            }

            // Choose a category
            else -> {
                val ids = presenter.getAnimeCategoryIds(anime)
                val preselected = categories.map {
                    if (it.id!! in ids) {
                        QuadStateTextView.State.CHECKED.ordinal
                    } else {
                        QuadStateTextView.State.UNCHECKED.ordinal
                    }
                }.toIntArray()

                showChangeCategoryDialog(anime, categories, preselected)
            }
        }

        if (source != null) {
            presenter.trackList
                .map { it.service }
                .filterIsInstance<EnhancedTrackService>()
                .filter { it.accept(source!!) }
                .forEach { service ->
                    launchIO {
                        try {
                            service.match(anime)?.let { track ->
                                presenter.registerTracking(track, service as TrackService)
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.WARN, e) { "Could not match anime: ${anime.title} with service $service" }
                        }
                    }
                }
        }
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded episodes.
     */
    private fun toggleFavorite() {
        val isNowFavorite = presenter.toggleFavorite()
        if (isNowFavorite) {
            addSnackbar?.dismiss()
        }
        if (activity != null && !isNowFavorite && presenter.hasDownloads()) {
            (activity as? MainActivity)?.binding?.rootCoordinator?.snack(activity!!.getString(R.string.delete_downloads_for_anime)) {
                setAction(R.string.action_delete) {
                    presenter.deleteDownloads()
                }
            }
        }
        animeInfoAdapter?.update()
    }

    fun onCategoriesClick() {
        val anime = presenter.anime
        val categories = presenter.getCategories()

        val ids = presenter.getAnimeCategoryIds(anime)
        val preselected = categories.map {
            if (it.id!! in ids) {
                QuadStateTextView.State.CHECKED.ordinal
            } else {
                QuadStateTextView.State.UNCHECKED.ordinal
            }
        }.toIntArray()

        showChangeCategoryDialog(anime, categories, preselected)
    }

    private fun showChangeCategoryDialog(anime: Anime, categories: List<Category>, preselected: IntArray) {
        if (dialog != null) return
        dialog = ChangeAnimeCategoriesDialog(this, listOf(anime), categories, preselected)
        dialog?.addLifecycleListener(
            object : LifecycleListener() {
                override fun postDestroy(controller: Controller) {
                    super.postDestroy(controller)
                    dialog = null
                }
            }
        )
        dialog?.showDialog(router)
    }

    override fun updateCategoriesForAnimes(animes: List<Anime>, addCategories: List<Category>, removeCategories: List<Category>) {
        val anime = animes.firstOrNull() ?: return

        if (!anime.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_added_library))
            activity?.invalidateOptionsMenu()
        }

        presenter.moveAnimeToCategories(anime, addCategories)
    }

    /**
     * Perform a global search using the provided query.
     *
     * @param query the search query to pass to the search controller
     */
    fun performGlobalSearch(query: String) {
        router.pushController(GlobalAnimeSearchController(query).withFadeTransaction())
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    fun performSearch(query: String) {
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
            is AnimeUpdatesController,
            is AnimeHistoryController -> {
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
    fun performGenreSearch(genreName: String) {
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
            performSearch(genreName)
        }
    }

    /**
     * Fetches the cover with Coil, turns it into Bitmap and does something with it (asynchronous)
     * @param context The context for building and executing the ImageRequest
     * @param coverHandler A function that describes what should be done with the Bitmap
     */
    private fun useCoverAsBitmap(context: Context, coverHandler: (Bitmap) -> Unit) {
        val req = ImageRequest.Builder(context)
            .data(anime)
            .target { result ->
                val coverBitmap = (result as BitmapDrawable).bitmap
                coverHandler(coverBitmap)
            }
            .build()
        context.imageLoader.enqueue(req)
    }

    fun showFullCoverDialog() {
        if (dialog != null) return
        val anime = anime ?: return
        dialog = AnimeFullCoverDialog(this, anime)
        dialog?.addLifecycleListener(
            object : LifecycleListener() {
                override fun postDestroy(controller: Controller) {
                    super.postDestroy(controller)
                    dialog = null
                }
            }
        )
        dialog?.showDialog(router)
    }

    fun shareCover() {
        try {
            val activity = activity!!
            useCoverAsBitmap(activity) { coverBitmap ->
                val cover = presenter.shareCover(activity, coverBitmap)
                val uri = cover.getUriCompat(activity)
                startActivity(uri.toShareIntent(activity))
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            activity?.toast(R.string.error_sharing_cover)
        }
    }

    fun saveCover() {
        try {
            val activity = activity!!
            useCoverAsBitmap(activity) { coverBitmap ->
                presenter.saveCover(activity, coverBitmap)
                activity.toast(R.string.cover_saved)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            activity?.toast(R.string.error_saving_cover)
        }
    }

    fun changeCover() {
        val anime = anime ?: return
        if (anime.hasCustomCover(coverCache)) {
            ChangeAnimeCoverDialog(this, anime).showDialog(router)
        } else {
            openAnimeCoverPicker(anime)
        }
    }

    override fun openAnimeCoverPicker(anime: Anime) {
        if (anime.favorite) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(
                Intent.createChooser(
                    intent,
                    resources?.getString(R.string.file_select_cover)
                ),
                REQUEST_IMAGE_OPEN
            )
        } else {
            activity?.toast(R.string.notification_first_add_to_library)
        }

        destroyActionModeIfNeeded()
    }

    override fun deleteAnimeCover(anime: Anime) {
        presenter.deleteCustomCover(anime)
        animeInfoAdapter?.notifyItemChanged(0, anime)
        destroyActionModeIfNeeded()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_OPEN) {
            val dataUri = data?.data
            if (dataUri == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return
            presenter.editCover(anime!!, activity, dataUri)
        }
        if (requestCode == REQUEST_EXTERNAL && resultCode == Activity.RESULT_OK) {
            val anime = anime ?: return
            val currentExtEpisode = currentExtEpisode ?: return
            val currentPosition: Long
            val duration: Long
            val cause = data!!.getStringExtra("end_by") ?: ""
            if (cause.isNotEmpty()) {
                val positionExtra = data.extras?.get("position")
                currentPosition = if (positionExtra is Int) {
                    positionExtra.toLong()
                } else {
                    positionExtra as? Long ?: 0L
                }
                val durationExtra = data.extras?.get("duration")
                duration = if (durationExtra is Int) {
                    durationExtra.toLong()
                } else {
                    durationExtra as? Long ?: 0L
                }
            } else {
                if (data.extras?.get("extra_position") != null) {
                    currentPosition = data.getLongExtra("extra_position", 0L)
                    duration = data.getLongExtra("extra_duration", 0L)
                } else {
                    currentPosition = data.getIntExtra("position", 0).toLong()
                    duration = 1440000L
                }
            }
            if (cause == "playback_completion") {
                setEpisodeProgress(currentExtEpisode, anime, 1L, 1L)
            } else {
                setEpisodeProgress(currentExtEpisode, anime, currentPosition, duration)
            }
            saveEpisodeHistory(EpisodeItem(currentExtEpisode, anime))
        }
    }

    private fun saveEpisodeHistory(episode: EpisodeItem) {
        if (!incognitoMode) {
            val history = AnimeHistory.create(episode.episode).apply { last_seen = Date().time }
            db.updateAnimeHistoryLastSeen(history).asRxCompletable()
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    private fun setEpisodeProgress(episode: Episode, anime: Anime, seconds: Long, totalSeconds: Long) {
        if (!incognitoMode) {
            if (totalSeconds > 0L) {
                episode.last_second_seen = seconds
                episode.total_seconds = totalSeconds
                val progress = preferences.progressPreference()
                if (!episode.seen) episode.seen = episode.last_second_seen >= episode.total_seconds * progress
                val episodes = listOf(EpisodeItem(episode, anime))
                launchIO {
                    db.updateEpisodesProgress(episodes).executeAsBlocking()
                    if (preferences.autoUpdateTrack() && episode.seen) {
                        updateTrackEpisodeSeen(episode, anime)
                    }
                    if (preferences.removeAfterMarkedAsRead()) {
                        launchIO {
                            try {
                                val downloadManager: AnimeDownloadManager = Injekt.get()
                                val source: AnimeSource = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
                                downloadManager.deleteEpisodes(episodes, anime, source).forEach {
                                    if (it is EpisodeItem) {
                                        it.status = AnimeDownload.State.NOT_DOWNLOADED
                                        it.download = null
                                    }
                                }
                            } catch (e: Throwable) {
                                throw e
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateTrackEpisodeSeen(episode: Episode, anime: Anime) {
        val episodeSeen = episode.episode_number

        val trackManager = Injekt.get<TrackManager>()

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
                .forEach { logcat(LogPriority.WARN, it) }
        }
    }

    fun onSetCoverSuccess() {
        animeInfoAdapter?.notifyItemChanged(0, this)
        (dialog as? AnimeFullCoverDialog)?.setImage(anime)
        activity?.toast(R.string.cover_updated)
    }

    fun onSetCoverError(error: Throwable) {
        activity?.toast(R.string.notification_cover_update_failed)
        logcat(LogPriority.ERROR, error)
    }

    /**
     * Initiates source migration for the specific anime.
     */
    private fun migrateAnime() {
        val controller = AnimeSearchController(presenter.anime)
        controller.targetController = this
        router.pushController(controller.withFadeTransaction())
    }

    // Anime info - end

    // Episodes list - start

    fun onNextEpisodes(episodes: List<EpisodeItem>) {
        // If the list is empty and it hasn't requested previously, fetch episodes from source
        // We use presenter episodes instead because they are always unfiltered
        if (!presenter.hasRequested && presenter.allEpisodes.isEmpty()) {
            fetchEpisodesFromSource()
        }

        val episodesHeader = episodesHeaderAdapter ?: return
        episodesHeader.setNumEpisodes(episodes.size)

        val adapter = episodesAdapter ?: return
        adapter.updateDataSet(episodes)

        if (selectedEpisodes.isNotEmpty()) {
            adapter.clearSelection() // we need to start from a clean state, index may have changed
            createActionModeIfNeeded()
            selectedEpisodes.forEach { item ->
                val position = adapter.indexOf(item)
                if (position != -1 && !adapter.isSelected(position)) {
                    adapter.toggleSelection(position)
                }
            }
            actionMode?.invalidate()
        }

        updateFabVisibility()
    }

    private fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        isRefreshingEpisodes = true
        updateRefreshing()

        presenter.fetchEpisodesFromSource(manualFetch)
    }

    fun onFetchEpisodesDone() {
        isRefreshingEpisodes = false
        updateRefreshing()
    }

    fun onFetchEpisodesError(error: Throwable) {
        isRefreshingEpisodes = false
        updateRefreshing()
        if (error is NoEpisodesException) {
            activity?.toast(activity?.getString(R.string.no_episodes_error))
        } else {
            activity?.toast(error.message)
        }
    }

    fun onEpisodeDownloadUpdate(download: AnimeDownload) {
        episodesAdapter?.currentItems?.find { it.id == download.episode.id }?.let {
            episodesAdapter?.updateItem(it, it.status)
        }
    }

    private fun openEpisode(episode: Episode, hasAnimation: Boolean = false, playerChangeRequested: Boolean = false) {
        val context = view?.context ?: return
        val intent = PlayerActivity.newIntent(context, presenter.anime, episode)
        val useInternal = preferences.alwaysUseExternalPlayer() == playerChangeRequested
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }

        if (!useInternal) launchIO {
            val video = try {
                EpisodeLoader.getLink(episode, anime!!, source!!).awaitSingle()
            } catch (e: Exception) {
                return@launchIO makeErrorToast(context, e)
            }
            if (video != null) {
                currentExtEpisode = episode

                val anime = anime ?: return@launchIO
                val source = source ?: return@launchIO
                val extIntent = ExternalIntents(anime, source).getExternalIntent(episode, video, context)
                if (extIntent != null) try {
                    startActivityForResult(extIntent, REQUEST_EXTERNAL)
                } catch (e: Exception) {
                    makeErrorToast(context, e)
                }
            } else {
                makeErrorToast(context, Exception("Couldn't find any video links."))
            }
        } else {
            startActivity(intent)
        }
    }

    private fun makeErrorToast(context: Context, e: Exception?) {
        launchUI { context.toast(e?.message ?: "Cannot open episode") }
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = episodesAdapter ?: return false
        val item = adapter.getItem(position) ?: return false
        return if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            if (adapter.isSelected(position)) {
                lastClickPositionStack.remove(position) // possible that it's not there, but no harm
            } else {
                lastClickPositionStack.push(position)
            }

            toggleSelection(position)
            true
        } else {
            openEpisode(item.episode)
            false
        }
    }

    override fun onItemLongClick(position: Int) {
        createActionModeIfNeeded()
        val lastClickPosition = lastClickPositionStack.peek()!!
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position -> {
                for (i in position until lastClickPosition) setSelection(i)
                episodesAdapter?.notifyItemRangeChanged(position, lastClickPosition, position)
            }
            lastClickPosition < position -> {
                for (i in lastClickPosition + 1..position) setSelection(i)
                episodesAdapter?.notifyItemRangeChanged(lastClickPosition + 1, position, position)
            }
            else -> setSelection(position)
        }
        if (lastClickPosition != position) {
            lastClickPositionStack.remove(position) // move to top if already exists
            lastClickPositionStack.push(position)
        }
    }

    fun showSettingsSheet() {
        settingsSheet?.show()
    }

    // SELECTIONS & ACTION MODE

    private fun toggleSelection(position: Int) {
        val adapter = episodesAdapter ?: return
        val item = adapter.getItem(position) ?: return
        adapter.toggleSelection(position)
        if (adapter.isSelected(position)) {
            selectedEpisodes.add(item)
        } else {
            selectedEpisodes.remove(item)
        }
        actionMode?.invalidate()
    }

    private fun setSelection(position: Int) {
        val adapter = episodesAdapter ?: return
        val item = adapter.getItem(position) ?: return
        if (!adapter.isSelected(position)) {
            adapter.toggleSelection(position)
            selectedEpisodes.add(item)
            actionMode?.invalidate()
        }
    }

    private fun getSelectedEpisodes(): List<EpisodeItem> {
        val adapter = episodesAdapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) }
    }

    private fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as? AppCompatActivity)?.startSupportActionMode(this)
            binding.actionToolbar.show(
                actionMode!!,
                R.menu.episode_selection
            ) { onActionItemClicked(it!!) }
        }
    }

    private fun destroyActionModeIfNeeded() {
        lastClickPositionStack.clear()
        lastClickPositionStack.push(-1)
        actionMode?.finish()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        episodesAdapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = episodesAdapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()

            val episodes = getSelectedEpisodes()
            binding.actionToolbar.findItem(R.id.action_download)?.isVisible = !isLocalSource && episodes.any { !it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_delete)?.isVisible = !isLocalSource && episodes.any { it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_bookmark)?.isVisible = episodes.any { !it.episode.bookmark }
            binding.actionToolbar.findItem(R.id.action_remove_bookmark)?.isVisible = episodes.all { it.episode.bookmark }
            binding.actionToolbar.findItem(R.id.action_mark_as_read)?.isVisible = episodes.any { !it.episode.seen }
            binding.actionToolbar.findItem(R.id.action_mark_as_unread)?.isVisible = episodes.all { it.episode.seen }
            binding.actionToolbar.findItem(R.id.action_play_externally)?.isVisible = !preferences.alwaysUseExternalPlayer()
            binding.actionToolbar.findItem(R.id.action_play_internally)?.isVisible = preferences.alwaysUseExternalPlayer()

            // Hide FAB to avoid interfering with the bottom action toolbar
            actionFab?.isVisible = false
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return onActionItemClicked(item)
    }

    private fun onActionItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_select_inverse -> selectInverse()
            R.id.action_download -> downloadEpisodes(getSelectedEpisodes())
            R.id.action_delete -> showDeleteEpisodesConfirmationDialog()
            R.id.action_bookmark -> bookmarkEpisodes(getSelectedEpisodes(), true)
            R.id.action_remove_bookmark -> bookmarkEpisodes(getSelectedEpisodes(), false)
            R.id.action_mark_as_read -> markAsRead(getSelectedEpisodes())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedEpisodes())
            R.id.action_mark_previous_as_read -> markPreviousAsRead(getSelectedEpisodes())
            R.id.action_play_internally -> openEpisode(getSelectedEpisodes().last().episode, playerChangeRequested = true)
            R.id.action_play_externally -> openEpisode(getSelectedEpisodes().last().episode, playerChangeRequested = true)
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        binding.actionToolbar.hide()
        episodesAdapter?.mode = SelectableAdapter.Mode.SINGLE
        episodesAdapter?.clearSelection()
        selectedEpisodes.clear()
        actionMode = null
        updateFabVisibility()
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        super.onDetach(view)
    }

    override fun downloadEpisode(position: Int) {
        val item = episodesAdapter?.getItem(position) ?: return
        if (item.status == AnimeDownload.State.ERROR) {
            AnimeDownloadService.start(activity!!)
        } else {
            downloadEpisodes(listOf(item))
        }
        episodesAdapter?.updateItem(item)
    }

    override fun downloadEpisodeExternally(position: Int) {
        val item = episodesAdapter?.getItem(position) ?: return
        if (item.status == AnimeDownload.State.ERROR) {
            AnimeDownloadService.start(activity!!)
        } else {
            downloadEpisodesExternally(listOf(item))
        }
        episodesAdapter?.updateItem(item)
    }

    override fun deleteEpisode(position: Int) {
        val item = episodesAdapter?.getItem(position) ?: return
        deleteEpisodes(listOf(item))
        episodesAdapter?.updateItem(item)
    }

    // SELECTION MODE ACTIONS

    private fun selectAll() {
        val adapter = episodesAdapter ?: return
        adapter.selectAll()
        selectedEpisodes.addAll(adapter.items)
        actionMode?.invalidate()
    }

    private fun selectInverse() {
        val adapter = episodesAdapter ?: return

        selectedEpisodes.clear()
        for (i in 0..adapter.itemCount) {
            adapter.toggleSelection(i)
            adapter.notifyItemChanged(i, i)
        }
        selectedEpisodes.addAll(adapter.selectedPositions.mapNotNull { adapter.getItem(it) })

        actionMode?.invalidate()
    }

    private fun markAsRead(episodes: List<EpisodeItem>) {
        presenter.markEpisodesRead(episodes, true)
        destroyActionModeIfNeeded()
    }

    private fun markAsUnread(episodes: List<EpisodeItem>) {
        presenter.markEpisodesRead(episodes, false)
        destroyActionModeIfNeeded()
    }

    private fun downloadEpisodes(episodes: List<EpisodeItem>) {
        if (source is AnimeSourceManager.StubSource) {
            activity?.toast(R.string.loader_not_implemented_error)
            return
        }

        val view = view
        val anime = presenter.anime
        presenter.downloadEpisodes(episodes)
        if (view != null && !anime.favorite) {
            addSnackbar = (activity as? MainActivity)?.binding?.rootCoordinator?.snack(view.context.getString(R.string.snack_add_to_animelib)) {
                setAction(R.string.action_add) {
                    if (!anime.favorite) {
                        addToAnimelib(anime)
                    }
                }
            }
        }
        destroyActionModeIfNeeded()
    }

    private fun downloadEpisodesExternally(episodes: List<EpisodeItem>) {
        if (source is AnimeSourceManager.StubSource) {
            activity?.toast(R.string.loader_not_implemented_error)
            return
        }

        val view = view
        val anime = presenter.anime
        presenter.downloadEpisodesExternally(episodes)
        if (view != null && !anime.favorite) {
            addSnackbar = (activity as? MainActivity)?.binding?.rootCoordinator?.snack(view.context.getString(R.string.snack_add_to_animelib)) {
                setAction(R.string.action_add) {
                    if (!anime.favorite) {
                        addToAnimelib(anime)
                    }
                }
            }
        }
        destroyActionModeIfNeeded()
    }

    private fun showDeleteEpisodesConfirmationDialog() {
        DeleteEpisodesDialog(this).showDialog(router)
    }

    override fun deleteEpisodes() {
        deleteEpisodes(getSelectedEpisodes())
    }

    private fun markPreviousAsRead(episodes: List<EpisodeItem>) {
        val adapter = episodesAdapter ?: return
        val prevEpisodes = if (presenter.sortDescending()) adapter.items.reversed() else adapter.items
        val episodePos = prevEpisodes.indexOf(episodes.lastOrNull())
        if (episodePos != -1) {
            markAsRead(prevEpisodes.take(episodePos))
        }
        destroyActionModeIfNeeded()
    }

    private fun bookmarkEpisodes(episodes: List<EpisodeItem>, bookmarked: Boolean) {
        presenter.bookmarkEpisodes(episodes, bookmarked)
        destroyActionModeIfNeeded()
    }

    fun deleteEpisodes(episodes: List<EpisodeItem>) {
        if (episodes.isEmpty()) return

        presenter.deleteEpisodes(episodes)
        destroyActionModeIfNeeded()
    }

    fun onEpisodesDeleted(episodes: List<EpisodeItem>) {
        // this is needed so the downloaded text gets removed from the item
        episodes.forEach {
            episodesAdapter?.updateItem(it, it)
        }
    }

    fun onEpisodesDeletedError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
    }

    override fun startDownloadNow(position: Int) {
        val episode = episodesAdapter?.getItem(position) ?: return
        presenter.startDownloadingNow(episode)
    }

    // OVERFLOW MENU DIALOGS

    private fun downloadEpisodes(choice: Int) {
        val episodesToDownload = when (choice) {
            R.id.download_next -> presenter.getUnseenEpisodesSorted().take(1)
            R.id.download_next_5 -> presenter.getUnseenEpisodesSorted().take(5)
            R.id.download_next_10 -> presenter.getUnseenEpisodesSorted().take(10)
            R.id.download_custom -> {
                showCustomDownloadDialog()
                return
            }
            R.id.download_unread -> presenter.allEpisodes.filter { !it.seen }
            R.id.download_all -> presenter.allEpisodes
            else -> emptyList()
        }
        if (episodesToDownload.isNotEmpty()) {
            downloadEpisodes(episodesToDownload)
        }
        destroyActionModeIfNeeded()
    }

    private fun showCustomDownloadDialog() {
        DownloadCustomEpisodesDialog(
            this,
            presenter.allEpisodes.size
        ).showDialog(router)
    }

    override fun downloadCustomEpisodes(amount: Int) {
        val episodesToDownload = presenter.getUnseenEpisodesSorted().take(amount)
        if (episodesToDownload.isNotEmpty()) {
            downloadEpisodes(episodesToDownload)
        }
    }

    // Episodes list - end

    // Tracker sheet - start
    fun onNextTrackers(trackers: List<TrackItem>) {
        trackSheet?.onNextTrackers(trackers)
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
        return trackSheet?.getSearchDialog()
    }

    // Tracker sheet - end

    private val episodeRecycler: RecyclerView
        get() = binding.fullRecycler ?: binding.chaptersRecycler!!

    companion object {
        const val FROM_SOURCE_EXTRA = "from_source"
        const val ANIME_EXTRA = "anime"

        /**
         * Key to change the cover of a anime in [onActivityResult].
         */
        const val REQUEST_IMAGE_OPEN = 101
        const val REQUEST_INTERNAL = 102
        const val REQUEST_EXTERNAL = 103
    }
}
