package eu.kanade.tachiyomi.ui.anime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.databinding.AnimeControllerBinding
import eu.kanade.tachiyomi.source.AnimeSource
import eu.kanade.tachiyomi.source.AnimeSourceManager
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.anime.episode.AnimeEpisodesHeaderAdapter
import eu.kanade.tachiyomi.ui.anime.episode.DeleteEpisodesDialog
import eu.kanade.tachiyomi.ui.anime.episode.DownloadCustomEpisodesDialog
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
import eu.kanade.tachiyomi.ui.anime.episode.EpisodesAdapter
import eu.kanade.tachiyomi.ui.anime.episode.EpisodesSettingsSheet
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.ui.anime.info.AnimeInfoHeaderAdapter
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.anime.track.TrackSearchDialog
import eu.kanade.tachiyomi.ui.anime.track.TrackSheet
import eu.kanade.tachiyomi.ui.animelib.AnimelibController
import eu.kanade.tachiyomi.ui.animelib.ChangeAnimeCategoriesDialog
import eu.kanade.tachiyomi.ui.animelib.ChangeAnimeCoverDialog
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.ToolbarLiftOnScrollController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.migration.search.AnimeSearchController
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.ui.browse.source.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.recent.history.HistoryController
import eu.kanade.tachiyomi.ui.recent.updates.UpdatesController
import eu.kanade.tachiyomi.ui.watcher.WatcherActivity
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.episode.NoEpisodesException
import eu.kanade.tachiyomi.util.hasCustomCover
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.getCoordinates
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import eu.kanade.tachiyomi.util.view.snack
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.recyclerview.scrollEvents
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.ArrayDeque
import kotlin.math.min

class AnimeController :
    NucleusController<AnimeControllerBinding, AnimePresenter>,
    ToolbarLiftOnScrollController,
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

    var source: AnimeSource? = null
        private set

    val fromSource = args.getBoolean(FROM_SOURCE_EXTRA, false)

    private val preferences: PreferencesHelper by injectLazy()
    private val coverCache: AnimeCoverCache by injectLazy()

    private val toolbarTextColor by lazy { view!!.context.getResourceColor(R.attr.colorOnPrimary) }

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

    private val isLocalSource by lazy { presenter.source.id == LocalSource.ID }

    private var lastClickPositionStack = ArrayDeque(listOf(-1))

    private var isRefreshingInfo = false
    private var isRefreshingEpisodes = false

    private var trackSheet: TrackSheet? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return anime?.title
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)

        // Hide toolbar title on enter
        if (type.isEnter) {
            updateToolbarTitleAlpha()
        }
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (anime == null || source == null) {
            activity?.toast(R.string.manga_not_in_db)
            router.popController(this)
        }
    }

    override fun createPresenter(): AnimePresenter {
        return AnimePresenter(
            anime!!,
            source!!
        )
    }

    override fun createBinding(inflater: LayoutInflater) = AnimeControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (anime == null || source == null) return

        // Init RecyclerView and adapter
        animeInfoAdapter = AnimeInfoHeaderAdapter(this, fromSource)
        episodesHeaderAdapter = AnimeEpisodesHeaderAdapter(this)
        episodesAdapter = EpisodesAdapter(this, view.context)

        binding.recycler.adapter = ConcatAdapter(animeInfoAdapter, episodesHeaderAdapter, episodesAdapter)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)
        episodesAdapter?.fastScroller = binding.fastScroller

        actionFabScrollListener = actionFab?.shrinkOnScroll(binding.recycler)

        // Skips directly to episodes list if navigated to from the animelib
        binding.recycler.post {
            if (!fromSource && preferences.jumpToEpisodes()) {
                (binding.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(1, 0)
            }

            // Delayed in case we need to jump to episodes
            binding.recycler.post {
                updateToolbarTitleAlpha()
            }
        }

        binding.recycler.scrollEvents()
            .onEach { updateToolbarTitleAlpha() }
            .launchIn(viewScope)

        binding.swipeRefresh.refreshes()
            .onEach {
                fetchAnimeInfoFromSource(manualFetch = true)
                fetchEpisodesFromSource(manualFetch = true)
            }
            .launchIn(viewScope)

        (activity as? MainActivity)?.fixViewToBottom(binding.actionToolbar)

        settingsSheet = EpisodesSettingsSheet(router, presenter) { group ->
            if (group is EpisodesSettingsSheet.Filter.FilterGroup) {
                updateFilterIconState()
                episodesAdapter?.notifyDataSetChanged()
            }
        }

        trackSheet = TrackSheet(this, anime!!)

        updateFilterIconState()
    }

    private fun updateToolbarTitleAlpha(alpha: Int? = null) {
        val calculatedAlpha = when {
            // Specific alpha provided
            alpha != null -> alpha

            // First item isn't in view, full opacity
            ((binding.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > 0) -> 255

            // Based on scroll amount when first item is in view
            else -> min(binding.recycler.computeVerticalScrollOffset(), 255)
        }

        (activity as? MainActivity)?.binding?.toolbar?.setTitleTextColor(
            Color.argb(
                calculatedAlpha,
                toolbarTextColor.red,
                toolbarTextColor.green,
                toolbarTextColor.blue
            )
        )
    }

    private fun updateFilterIconState() {
        episodesHeaderAdapter?.setHasActiveFilters(settingsSheet?.filters?.hasActiveFilters() == true)
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        actionFab = fab
        fab.setText(R.string.action_start)
        fab.setIconResource(R.drawable.ic_play_arrow_24dp)
        fab.setOnClickListener {
            val item = presenter.getNextUnreadEpisode()
            if (item != null) {
                // Create animation listener
                val revealAnimationListener: Animator.AnimatorListener = object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        openEpisode(item.episode, true)
                    }
                }

                // Get coordinates and start animation
                actionFab?.getCoordinates()?.let { coordinates ->
                    binding.revealView.showRevealEffect(
                        coordinates.x,
                        coordinates.y,
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator?) {
                                openEpisode(item.episode, true)
                            }
                        }
                    )
                }
            } else {
                view?.context?.toast(R.string.no_next_chapter)
            }
        }
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener(null)
        actionFabScrollListener?.let { binding.recycler.removeOnScrollListener(it) }
        actionFab = null
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        (activity as? MainActivity)?.clearFixViewToBottom(binding.actionToolbar)
        binding.actionToolbar.destroy()
        animeInfoAdapter = null
        episodesHeaderAdapter = null
        episodesAdapter = null
        settingsSheet = null
        addSnackbar?.dismiss()
        updateToolbarTitleAlpha(255)
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
        menu.findItem(R.id.action_edit_cover).isVisible = presenter.anime.favorite
        menu.findItem(R.id.action_migrate).isVisible = presenter.anime.favorite
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareAnime()
            R.id.download_next, R.id.download_next_5, R.id.download_next_10,
            R.id.download_custom, R.id.download_unread, R.id.download_all
            -> downloadEpisodes(item.itemId)

            R.id.action_edit_categories -> onCategoriesClick()
            R.id.action_edit_cover -> handleChangeCover()
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
                val preselected = ids.mapNotNull { id ->
                    categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                }.toTypedArray()

                ChangeAnimeCategoriesDialog(this, listOf(anime), categories, preselected)
                    .showDialog(router)
            }
        }
    }

    /**
     * Toggles the favorite status and asks for confirmation to delete downloaded episodes.
     */
    private fun toggleFavorite() {
        val isNowFavorite = presenter.toggleFavorite()
        if (activity != null && !isNowFavorite && presenter.hasDownloads()) {
            (activity as? MainActivity)?.binding?.rootCoordinator?.snack(activity!!.getString(R.string.delete_downloads_for_manga)) {
                setAction(R.string.action_delete) {
                    presenter.deleteDownloads()
                }
            }
        }

        animeInfoAdapter?.notifyDataSetChanged()
    }

    fun onCategoriesClick() {
        val anime = presenter.anime
        val categories = presenter.getCategories()

        val ids = presenter.getAnimeCategoryIds(anime)
        val preselected = ids.mapNotNull { id ->
            categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
        }.toTypedArray()

        ChangeAnimeCategoriesDialog(this, listOf(anime), categories, preselected)
            .showDialog(router)
    }

    override fun updateCategoriesForAnimes(animes: List<Anime>, categories: List<Category>) {
        val anime = animes.firstOrNull() ?: return

        if (!anime.favorite) {
            toggleFavorite()
            activity?.toast(activity?.getString(R.string.manga_added_library))
            activity?.invalidateOptionsMenu()
        }

        presenter.moveAnimeToCategories(anime, categories)
    }

    /**
     * Perform a global search using the provided query.
     *
     * @param query the search query to pass to the search controller
     */
    fun performGlobalSearch(query: String) {
        router.pushController(GlobalSearchController(query).withFadeTransaction())
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

        when (val previousController = router.backstack[router.backstackSize - 2].controller()) {
            is AnimelibController -> {
                router.handleBack()
                previousController.search(query)
            }
            is UpdatesController,
            is HistoryController -> {
                // Manually navigate to AnimelibController
                router.handleBack()
                (router.activity as MainActivity).setSelectedNavItem(R.id.nav_library)
                val controller = router.getControllerWithTag(R.id.nav_library.toString()) as AnimelibController
                controller.search(query)
            }
            is LatestUpdatesController -> {
                // Search doesn't currently work in source Latest view
                return
            }
            is BrowseSourceController -> {
                router.handleBack()
                previousController.searchWithQuery(query)
            }
        }
    }

    private fun handleChangeCover() {
        val anime = anime ?: return
        if (anime.hasCustomCover(coverCache)) {
            showEditCoverDialog(anime)
        } else {
            openAnimeCoverPicker(anime)
        }
    }

    /**
     * Edit custom cover for selected anime.
     */
    private fun showEditCoverDialog(anime: Anime) {
        ChangeAnimeCoverDialog(this, anime).showDialog(router)
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
        animeInfoAdapter?.notifyDataSetChanged()
        destroyActionModeIfNeeded()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_OPEN) {
            val dataUri = data?.data
            if (dataUri == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return
            presenter.editCover(anime!!, activity, dataUri)
        }
        if (requestCode == REQUEST_SECONDS) {
            val seconds = data!!.getLongExtra("seconds_result", 0)
            val total_seconds = data.getLongExtra("total_seconds_result", 0)
            val episode: Episode = data.getSerializableExtra("episode") as Episode
            episode.last_second_seen = seconds
            episode.total_seconds = total_seconds
            presenter.setEpisodesProgress(arrayListOf(EpisodeItem(episode, anime!!)))
        }
    }

    fun onSetCoverSuccess() {
        animeInfoAdapter?.notifyDataSetChanged()
        activity?.toast(R.string.cover_updated)
    }

    fun onSetCoverError(error: Throwable) {
        activity?.toast(R.string.notification_cover_update_failed)
        Timber.e(error)
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
        if (!presenter.hasRequested && presenter.episodes.isEmpty()) {
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

        val context = view?.context
        if (context != null && episodes.any { it.seen }) {
            actionFab?.text = context.getString(R.string.action_resume)
        }
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

    fun openEpisode(episode: Episode, hasAnimation: Boolean = false) {
        val activity = activity ?: return
        val intent = WatcherActivity.newIntent(activity, presenter.anime, episode)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivityForResult(intent, REQUEST_SECONDS)
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
            lastClickPosition > position ->
                for (i in position until lastClickPosition)
                    setSelection(i)
            lastClickPosition < position ->
                for (i in lastClickPosition + 1..position)
                    setSelection(i)
            else -> setSelection(position)
        }
        if (lastClickPosition != position) {
            lastClickPositionStack.remove(position) // move to top if already exists
            lastClickPositionStack.push(position)
        }
        episodesAdapter?.notifyDataSetChanged()
    }

    fun showSettingsSheet() {
        settingsSheet?.show()
    }

    // SELECTIONS & ACTION MODE

    private fun toggleSelection(position: Int) {
        val adapter = episodesAdapter ?: return
        val item = adapter.getItem(position) ?: return
        adapter.toggleSelection(position)
        adapter.notifyDataSetChanged()
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
        actionFab?.isVisible = true
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        super.onDetach(view)
    }

    override fun downloadEpisode(position: Int) {
        val item = episodesAdapter?.getItem(position) ?: return
        if (item.status == AnimeDownload.State.ERROR) {
            DownloadService.start(activity!!)
        } else {
            downloadEpisodes(listOf(item))
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
        }
        selectedEpisodes.addAll(adapter.selectedPositions.mapNotNull { adapter.getItem(it) })

        actionMode?.invalidate()
        adapter.notifyDataSetChanged()
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
            addSnackbar = (activity as? MainActivity)?.binding?.rootCoordinator?.snack(view.context.getString(R.string.snack_add_to_animelib), Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.action_add) {
                    addToAnimelib(anime)
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
            episodesAdapter?.updateItem(it)
        }
        launchUI {
            episodesAdapter?.notifyDataSetChanged()
        }
    }

    fun onEpisodesDeletedError(error: Throwable) {
        Timber.e(error)
    }

    // OVERFLOW MENU DIALOGS

    private fun getUnseenEpisodesSorted() = presenter.episodes
        .sortedWith(presenter.getEpisodeSort())
        .filter { !it.seen && it.status == AnimeDownload.State.NOT_DOWNLOADED }
        .distinctBy { it.name }
        .reversed()

    private fun downloadEpisodes(choice: Int) {
        val episodesToDownload = when (choice) {
            R.id.download_next -> getUnseenEpisodesSorted().take(1)
            R.id.download_next_5 -> getUnseenEpisodesSorted().take(5)
            R.id.download_next_10 -> getUnseenEpisodesSorted().take(10)
            R.id.download_custom -> {
                showCustomDownloadDialog()
                return
            }
            R.id.download_unread -> presenter.episodes.filter { !it.seen }
            R.id.download_all -> presenter.episodes
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
            presenter.episodes.size
        ).showDialog(router)
    }

    override fun downloadCustomEpisodes(amount: Int) {
        val episodesToDownload = getUnseenEpisodesSorted().take(amount)
        if (episodesToDownload.isNotEmpty()) {
            downloadEpisodes(episodesToDownload)
        }
    }

    // Episodes list - end

    // Tracker sheet - start
    fun onNextTrackers(trackers: List<TrackItem>) {
        trackSheet?.onNextTrackers(trackers)
    }

    fun onTrackingRefreshDone() {
    }

    fun onTrackingRefreshError(error: Throwable) {
        Timber.e(error)
        activity?.toast(error.message)
    }

    fun onTrackingSearchResults(results: List<AnimeTrackSearch>) {
        getTrackingSearchDialog()?.onSearchResults(results)
    }

    fun onTrackingSearchResultsError(error: Throwable) {
        Timber.e(error)
        activity?.toast(error.message)
        getTrackingSearchDialog()?.onSearchResultsError()
    }

    private fun getTrackingSearchDialog(): TrackSearchDialog? {
        return trackSheet?.getSearchDialog()
    }

    // Tracker sheet - end

    companion object {
        const val FROM_SOURCE_EXTRA = "from_source"
        const val ANIME_EXTRA = "anime"

        /**
         * Key to change the cover of a anime in [onActivityResult].
         */
        const val REQUEST_IMAGE_OPEN = 101
        const val REQUEST_SECONDS = 102
    }
}
