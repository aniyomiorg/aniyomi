package eu.kanade.tachiyomi.ui.browse.animesource.globalsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.GlobalSearchControllerBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.SearchableNucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceController
import uy.kohesive.injekt.injectLazy

/**
 * This controller shows and manages the different search result in global search.
 * This controller should only handle UI actions, IO actions should be done by [GlobalSearchPresenter]
 * [GlobalAnimeSearchCardAdapter.OnAnimeClickListener] called when anime is clicked in global search
 */
open class GlobalAnimeSearchController(
    protected val initialQuery: String? = null,
    protected val extensionFilter: String? = null
) : SearchableNucleusController<GlobalSearchControllerBinding, GlobalAnimeSearchPresenter>(),
    GlobalAnimeSearchCardAdapter.OnAnimeClickListener,
    GlobalAnimeSearchAdapter.OnTitleClickListener {

    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Adapter containing search results grouped by lang.
     */
    protected var adapter: GlobalAnimeSearchAdapter? = null

    /**
     * Ref to the OptionsMenu.SearchItem created in onCreateOptionsMenu
     */
    private var optionsMenuSearchItem: MenuItem? = null

    init {
        setHasOptionsMenu(true)
    }

    /**
     * Initiate the view with [R.layout.global_search_controller].
     *
     * @param inflater used to load the layout xml.
     * @param container containing parent views.
     * @return inflated view
     */
    override fun createBinding(inflater: LayoutInflater) = GlobalSearchControllerBinding.inflate(inflater)

    override fun getTitle(): String? {
        return presenter.query
    }

    /**
     * Create the [GlobalSearchPresenter] used in controller.
     *
     * @return instance of [GlobalSearchPresenter]
     */
    override fun createPresenter(): GlobalAnimeSearchPresenter {
        return GlobalAnimeSearchPresenter(initialQuery, extensionFilter)
    }

    /**
     * Called when anime in global search is clicked, opens anime.
     *
     * @param anime clicked item containing anime information.
     */
    override fun onAnimeClick(anime: Anime) {
        router.pushController(AnimeController(anime, true).withFadeTransaction())
    }

    /**
     * Called when anime in global search is long clicked.
     *
     * @param anime clicked item containing anime information.
     */
    override fun onAnimeLongClick(anime: Anime) {
        // Delegate to single click by default.
        onAnimeClick(anime)
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        createOptionsMenu(
            menu,
            inflater,
            R.menu.global_search,
            R.id.action_search,
            null,
            false // the onMenuItemActionExpand will handle this
        )

        optionsMenuSearchItem = menu.findItem(R.id.action_search)
    }

    override fun onSearchMenuItemActionExpand(item: MenuItem?) {
        super.onSearchMenuItemActionExpand(item)
        val searchView = optionsMenuSearchItem?.actionView as SearchView
        searchView.onActionViewExpanded() // Required to show the query in the view

        if (nonSubmittedQuery.isBlank()) {
            searchView.setQuery(presenter.query, false)
        }
    }

    override fun onSearchViewQueryTextSubmit(query: String?) {
        presenter.search(query ?: "")
        optionsMenuSearchItem?.collapseActionView()
        setTitle() // Update toolbar title
    }

    /**
     * Called when the view is created
     *
     * @param view view of controller
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = GlobalAnimeSearchAdapter(this)

        // Create recycler and set adapter.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        adapter?.onSaveInstanceState(outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        adapter?.onRestoreInstanceState(savedViewState)
    }

    /**
     * Returns the view holder for the given anime.
     *
     * @param source used to find holder containing source
     * @return the holder of the anime or null if it's not bound.
     */
    private fun getHolder(source: AnimeCatalogueSource): GlobalAnimeSearchHolder? {
        val adapter = adapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.bindingAdapterPosition)
            if (item != null && source.id == item.source.id) {
                return holder as GlobalAnimeSearchHolder
            }
        }

        return null
    }

    /**
     * Add search result to adapter.
     *
     * @param searchResult result of search.
     */
    fun setItems(searchResult: List<GlobalAnimeSearchItem>) {
        if (searchResult.isEmpty() && preferences.searchPinnedSourcesOnly()) {
            binding.emptyView.show(R.string.no_pinned_sources)
        } else {
            binding.emptyView.hide()
        }

        adapter?.updateDataSet(searchResult)

        val progress = searchResult.mapNotNull { it.results }.size.toDouble() / searchResult.size
        if (progress < 1) {
            binding.progressBar.isVisible = true
            binding.progressBar.progress = (progress * 100).toInt()
        } else {
            binding.progressBar.isVisible = false
        }
    }

    /**
     * Called from the presenter when a anime is initialized.
     *
     * @param anime the initialized anime.
     */
    fun onAnimeInitialized(source: AnimeCatalogueSource, anime: Anime) {
        getHolder(source)?.setImage(anime)
    }

    /**
     * Opens a catalogue with the given search.
     */
    override fun onTitleClick(source: AnimeCatalogueSource) {
        if (!preferences.incognitoMode().get()) {
            preferences.lastUsedSource().set(source.id)
        }
        router.pushController(BrowseAnimeSourceController(source, presenter.query).withFadeTransaction())
    }
}
