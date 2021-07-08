package eu.kanade.tachiyomi.ui.animelib

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import com.tfcporciuncula.flow.Preference
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.databinding.LibraryControllerBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.SearchableNucleusController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.view.clicks
import reactivecircus.flowbinding.viewpager.pageSelections
import rx.Subscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimelibController(
    bundle: Bundle? = null,
    private val preferences: PreferencesHelper = Injekt.get()
) : SearchableNucleusController<LibraryControllerBinding, AnimelibPresenter>(bundle),
    RootController,
    TabbedController,
    ActionMode.Callback,
    ChangeAnimeCategoriesDialog.Listener,
    DeleteAnimelibAnimesDialog.Listener {

    /**
     * Position of the active category.
     */
    private var activeCategory: Int = preferences.lastUsedAnimeCategory().get()

    /**
     * Action mode for selections.
     */
    private var actionMode: ActionMode? = null

    /**
     * Currently selected animes.
     */
    val selectedAnimes = mutableSetOf<Anime>()

    /**
     * Relay to notify the UI of selection updates.
     */
    val selectionRelay: PublishRelay<AnimelibSelectionEvent> = PublishRelay.create()

    /**
     * Relay to notify search query changes.
     */
    val searchRelay: BehaviorRelay<String> = BehaviorRelay.create()

    /**
     * Relay to notify the animelib's viewpager for updates.
     */
    val animelibAnimeRelay: BehaviorRelay<AnimelibAnimeEvent> = BehaviorRelay.create()

    /**
     * Relay to notify the animelib's viewpager to select all anime
     */
    val selectAllRelay: PublishRelay<Int> = PublishRelay.create()

    /**
     * Relay to notify the animelib's viewpager to select the inverse
     */
    val selectInverseRelay: PublishRelay<Int> = PublishRelay.create()

    /**
     * Number of anime per row in grid mode.
     */
    var animePerRow = 0
        private set

    /**
     * Adapter of the view pager.
     */
    private var adapter: AnimelibAdapter? = null

    /**
     * Sheet containing filter/sort/display items.
     */
    private var settingsSheet: AnimelibSettingsSheet? = null

    private var tabsVisibilityRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private var animeCountVisibilityRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private var tabsVisibilitySubscription: Subscription? = null

    private var animeCountVisibilitySubscription: Subscription? = null

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    private var currentTitle: String? = null
        set(value) {
            if (field != value) {
                field = value
                setTitle()
            }
        }

    override fun getTitle(): String? {
        return currentTitle ?: resources?.getString(R.string.label_animelib)
    }

    private fun updateTitle() {
        val showCategoryTabs = preferences.animeCategoryTabs().get()
        val currentCategory = adapter?.categories?.getOrNull(binding.libraryPager.currentItem)

        var title = if (showCategoryTabs) {
            resources?.getString(R.string.label_animelib)
        } else {
            currentCategory?.name
        }

        if (preferences.animeCategoryNumberOfItems().get() && animelibAnimeRelay.hasValue()) {
            animelibAnimeRelay.value.animes.let { animeMap ->
                if (!showCategoryTabs || adapter?.categories?.size == 1) {
                    title += " (${animeMap[currentCategory?.id]?.size ?: 0})"
                }
            }
        }

        currentTitle = title
    }

    override fun createPresenter(): AnimelibPresenter {
        return AnimelibPresenter()
    }

    override fun createBinding(inflater: LayoutInflater) = LibraryControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.actionToolbar.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        adapter = AnimelibAdapter(this)
        binding.libraryPager.adapter = adapter
        binding.libraryPager.pageSelections()
            .drop(1)
            .onEach {
                preferences.lastUsedAnimeCategory().set(it)
                activeCategory = it
                updateTitle()
            }
            .launchIn(viewScope)

        getColumnsPreferenceForCurrentOrientation().asImmediateFlow { animePerRow = it }
            .drop(1)
            // Set again the adapter to recalculate the covers height
            .onEach { reattachAdapter() }
            .launchIn(viewScope)

        if (selectedAnimes.isNotEmpty()) {
            createActionModeIfNeeded()
        }

        settingsSheet = AnimelibSettingsSheet(router) { group ->
            when (group) {
                is AnimelibSettingsSheet.Filter.FilterGroup -> onFilterChanged()
                is AnimelibSettingsSheet.Sort.SortGroup -> onSortChanged()
                is AnimelibSettingsSheet.Display.DisplayGroup -> reattachAdapter()
                is AnimelibSettingsSheet.Display.BadgeGroup -> onBadgeSettingChanged()
                is AnimelibSettingsSheet.Display.TabsGroup -> onTabsSettingsChanged()
            }
        }

        binding.btnGlobalSearch.clicks()
            .onEach {
                router.pushController(
                    GlobalAnimeSearchController(presenter.query).withFadeTransaction()
                )
            }
            .launchIn(viewScope)

        (activity as? MainActivity)?.fixViewToBottom(binding.actionToolbar)
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            (activity as? MainActivity)?.binding?.tabs?.setupWithViewPager(binding.libraryPager)
            presenter.subscribeAnimelib()
        }
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        (activity as? MainActivity)?.clearFixViewToBottom(binding.actionToolbar)
        binding.actionToolbar.destroy()
        adapter?.onDestroy()
        adapter = null
        settingsSheet = null
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
        super.onDestroyView(view)
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_START
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = tabsVisibilityRelay.subscribe { visible ->
            val tabAnimator = (activity as? MainActivity)?.tabAnimator
            if (visible) {
                tabAnimator?.expand()
            } else {
                tabAnimator?.collapse()
            }
        }
        animeCountVisibilitySubscription?.unsubscribe()
        animeCountVisibilitySubscription = animeCountVisibilityRelay.subscribe {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
    }

    fun showSettingsSheet() {
        if (adapter?.categories?.isNotEmpty() == true) {
            adapter?.categories?.get(binding.libraryPager.currentItem)?.let { category ->
                settingsSheet?.show(category)
            }
        } else {
            settingsSheet?.show()
        }
    }

    fun onNextAnimelibUpdate(categories: List<Category>, animeMap: Map<Int, List<AnimelibItem>>) {
        val view = view ?: return
        val adapter = adapter ?: return

        // Show empty view if needed
        if (animeMap.isNotEmpty()) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(R.string.information_empty_library)
        }

        // Get the current active category.
        val activeCat = if (adapter.categories.isNotEmpty()) {
            binding.libraryPager.currentItem
        } else {
            activeCategory
        }

        // Set the categories
        adapter.categories = categories
        adapter.itemsPerCategory = adapter.categories
            .map { (it.id ?: -1) to (animeMap[it.id]?.size ?: 0) }
            .toMap()

        if (preferences.categorisedDisplaySettings().get()) {
            // Reattach adapter so it doesn't get de-synced
            reattachAdapter()
        }

        // Restore active category.
        binding.libraryPager.setCurrentItem(activeCat, false)

        // Trigger display of tabs
        onTabsSettingsChanged()

        // Delay the scroll position to allow the view to be properly measured.
        view.post {
            if (isAttached) {
                (activity as? MainActivity)?.binding?.tabs?.setScrollPosition(binding.libraryPager.currentItem, 0f, true)
            }
        }

        // Send the anime map to child fragments after the adapter is updated.
        animelibAnimeRelay.call(AnimelibAnimeEvent(animeMap))

        // Finally update the title
        updateTitle()
    }

    /**
     * Returns a preference for the number of anime per row based on the current orientation.
     *
     * @return the preference.
     */
    private fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
            preferences.portraitColumns()
        } else {
            preferences.landscapeColumns()
        }
    }

    private fun onFilterChanged() {
        presenter.requestFilterUpdate()
        activity?.invalidateOptionsMenu()
    }

    private fun onBadgeSettingChanged() {
        presenter.requestBadgesUpdate()
    }

    private fun onTabsSettingsChanged() {
        tabsVisibilityRelay.call(preferences.animeCategoryTabs().get() && adapter?.categories?.size ?: 0 > 1)
        animeCountVisibilityRelay.call(preferences.animeCategoryNumberOfItems().get())
        updateTitle()
    }

    /**
     * Called when the sorting mode is changed.
     */
    private fun onSortChanged() {
        presenter.requestSortUpdate()
    }

    /**
     * Reattaches the adapter to the view pager to recreate fragments
     */
    private fun reattachAdapter() {
        val adapter = adapter ?: return

        val position = binding.libraryPager.currentItem

        adapter.recycle = false
        binding.libraryPager.adapter = adapter
        binding.libraryPager.currentItem = position
        adapter.recycle = true
    }

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            binding.actionToolbar.show(
                actionMode!!,
                R.menu.library_selection
            ) { onActionItemClicked(it!!) }
            (activity as? MainActivity)?.showBottomNav(visible = false, collapse = true)
        }
    }

    /**
     * Destroys the action mode.
     */
    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        createOptionsMenu(menu, inflater, R.menu.library, R.id.action_search)
        // Mutate the filter icon because it needs to be tinted and the resource is shared.
        menu.findItem(R.id.action_filter).icon.mutate()
    }

    fun search(query: String) {
        presenter.query = query
    }

    private fun performSearch() {
        searchRelay.call(presenter.query)
        if (presenter.query.isNotEmpty()) {
            binding.btnGlobalSearch.isVisible = true
            binding.btnGlobalSearch.text =
                resources?.getString(R.string.action_global_search_query, presenter.query)
        } else {
            binding.btnGlobalSearch.isVisible = false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val settingsSheet = settingsSheet ?: return

        val filterItem = menu.findItem(R.id.action_filter)

        // Tint icon if there's a filter active
        if (settingsSheet.filters.hasActiveFilters()) {
            val filterColor = activity!!.getResourceColor(R.attr.colorFilterActive)
            DrawableCompat.setTint(filterItem.icon, filterColor)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_filter -> showSettingsSheet()
            R.id.action_update_library -> {
                activity?.let {
                    if (AnimelibUpdateService.start(it)) {
                        it.toast(R.string.updating_library)
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = selectedAnimes.size
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()

            binding.actionToolbar.findItem(R.id.action_download_unread)?.isVisible = selectedAnimes.any { it.source != LocalAnimeSource.ID }
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return onActionItemClicked(item)
    }

    private fun onActionItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_move_to_category -> showChangeAnimeCategoriesDialog()
            R.id.action_download_unseen -> downloadUnseenEpisodes()
            R.id.action_mark_as_read -> markReadStatus(true)
            R.id.action_mark_as_unread -> markReadStatus(false)
            R.id.action_delete -> showDeleteAnimeDialog()
            R.id.action_select_all -> selectAllCategoryAnime()
            R.id.action_select_inverse -> selectInverseCategoryAnime()
            else -> return false
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // Clear all the anime selections and notify child views.
        selectedAnimes.clear()
        selectionRelay.call(AnimelibSelectionEvent.Cleared())

        binding.actionToolbar.hide()
        (activity as? MainActivity)?.showBottomNav(visible = true, collapse = true)

        actionMode = null
    }

    fun openAnime(anime: Anime) {
        // Notify the presenter a anime is being opened.
        presenter.onOpenAnime()

        router.pushController(AnimeController(anime).withFadeTransaction())
    }

    /**
     * Sets the selection for a given anime.
     *
     * @param anime the anime whose selection has changed.
     * @param selected whether it's now selected or not.
     */
    fun setSelection(anime: Anime, selected: Boolean) {
        if (selected) {
            if (selectedAnimes.add(anime)) {
                selectionRelay.call(AnimelibSelectionEvent.Selected(anime))
            }
        } else {
            if (selectedAnimes.remove(anime)) {
                selectionRelay.call(AnimelibSelectionEvent.Unselected(anime))
            }
        }
    }

    /**
     * Toggles the current selection state for a given anime.
     *
     * @param anime the anime whose selection to change.
     */
    fun toggleSelection(anime: Anime) {
        if (selectedAnimes.add(anime)) {
            selectionRelay.call(AnimelibSelectionEvent.Selected(anime))
        } else if (selectedAnimes.remove(anime)) {
            selectionRelay.call(AnimelibSelectionEvent.Unselected(anime))
        }
    }

    /**
     * Clear all of the anime currently selected, and
     * invalidate the action mode to revert the top toolbar
     */
    fun clearSelection() {
        selectedAnimes.clear()
        selectionRelay.call(AnimelibSelectionEvent.Cleared())
        invalidateActionMode()
    }

    /**
     * Move the selected anime to a list of categories.
     */
    private fun showChangeAnimeCategoriesDialog() {
        // Create a copy of selected anime
        val animes = selectedAnimes.toList()

        // Hide the default category because it has a different behavior than the ones from db.
        val categories = presenter.categories.filter { it.id != 0 }

        // Get indexes of the common categories to preselect.
        val commonCategoriesIndexes = presenter.getCommonCategories(animes)
            .map { categories.indexOf(it) }
            .toTypedArray()

        ChangeAnimeCategoriesDialog(this, animes, categories, commonCategoriesIndexes)
            .showDialog(router)
    }

    private fun downloadUnseenEpisodes() {
        val animes = selectedAnimes.toList()
        presenter.downloadUnseenEpisodes(animes)
        destroyActionModeIfNeeded()
    }

    private fun markReadStatus(read: Boolean) {
        val animes = selectedAnimes.toList()
        presenter.markReadStatus(animes, read)
        destroyActionModeIfNeeded()
    }

    private fun showDeleteAnimeDialog() {
        DeleteAnimelibAnimesDialog(this, selectedAnimes.toList()).showDialog(router)
    }

    override fun updateCategoriesForAnimes(animes: List<Anime>, categories: List<Category>) {
        presenter.moveAnimesToCategories(categories, animes)
        destroyActionModeIfNeeded()
    }

    override fun deleteAnimes(animes: List<Anime>, deleteFromAnimelib: Boolean, deleteEpisodes: Boolean) {
        presenter.removeAnimes(animes, deleteFromAnimelib, deleteEpisodes)
        destroyActionModeIfNeeded()
    }

    private fun selectAllCategoryAnime() {
        adapter?.categories?.getOrNull(binding.libraryPager.currentItem)?.id?.let {
            selectAllRelay.call(it)
        }
    }

    private fun selectInverseCategoryAnime() {
        adapter?.categories?.getOrNull(binding.libraryPager.currentItem)?.id?.let {
            selectInverseRelay.call(it)
        }
    }

    override fun onSearchViewQueryTextChange(newText: String?) {
        // Ignore events if this controller isn't at the top to avoid query being reset
        if (router.backstack.lastOrNull()?.controller == this) {
            presenter.query = newText ?: ""
            performSearch()
        }
    }
}
