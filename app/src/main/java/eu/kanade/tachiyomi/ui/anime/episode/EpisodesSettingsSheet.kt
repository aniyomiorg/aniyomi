package eu.kanade.tachiyomi.ui.anime.episode

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Router
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.toTriStateGroupState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimePresenter
import eu.kanade.tachiyomi.ui.anime.AnimeScreenState
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class EpisodesSettingsSheet(
    private val router: Router,
    private val presenter: AnimePresenter,
) : TabbedBottomSheetDialog(router.activity!!) {

    private lateinit var scope: CoroutineScope

    private var anime: Anime? = null

    val filters = Filter(context)
    private val sort = Sort(context)
    private val display = Display(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.menu.isVisible = true
        binding.menu.setOnClickListener { it.post { showPopupMenu(it) } }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = MainScope()
        scope.launch {
            presenter.state
                .filterIsInstance<AnimeScreenState.Success>()
                .collectLatest {
                    anime = it.anime
                    getTabViews().forEach { settings -> (settings as Settings).updateView() }
                }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    override fun getTabViews(): List<View> = listOf(
        filters,
        sort,
        display,
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.action_filter,
        R.string.action_sort,
        R.string.action_display,
    )

    private fun showPopupMenu(view: View) {
        view.popupMenu(
            menuRes = R.menu.default_chapter_filter,
            onMenuItemClick = {
                when (itemId) {
                    R.id.set_as_default -> {
                        SetEpisodeSettingsDialog(presenter.anime!!).showDialog(router)
                    }
                }
            },
        )
    }

    /**
     * Filters group (unread, downloaded, ...).
     */
    inner class Filter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val filterGroup = FilterGroup()

        init {
            setGroups(listOf(filterGroup))
        }

        /**
         * Returns true if there's at least one filter from [FilterGroup] active.
         */
        fun hasActiveFilters(): Boolean {
            return filterGroup.items.any { it.state != State.IGNORE.value }
        }

        override fun updateView() {
            filterGroup.updateModels()
        }

        inner class FilterGroup : Group {

            private val downloaded = Item.TriStateGroup(R.string.action_filter_downloaded, this)
            private val unseen = Item.TriStateGroup(R.string.action_filter_unseen, this)
            private val bookmarked = Item.TriStateGroup(R.string.action_filter_bookmarked, this)

            override val header: Item? = null
            override val items = listOf(downloaded, unseen, bookmarked)
            override val footer: Item? = null

            override fun initModels() {
                val anime = anime ?: return
                if (anime.forceDownloaded()) {
                    downloaded.state = State.INCLUDE.value
                    downloaded.enabled = false
                } else {
                    downloaded.state = anime.downloadedFilter.toTriStateGroupState().value
                }
                unseen.state = anime.unseenFilter.toTriStateGroupState().value
                bookmarked.state = anime.bookmarkedFilter.toTriStateGroupState().value
            }

            fun updateModels() {
                initModels()
                adapter.notifyItemRangeChanged(0, 3)
            }

            override fun onItemClicked(item: Item) {
                item as Item.TriStateGroup
                val newState = when (item.state) {
                    State.IGNORE.value -> State.INCLUDE
                    State.INCLUDE.value -> State.EXCLUDE
                    State.EXCLUDE.value -> State.IGNORE
                    else -> throw Exception("Unknown State")
                }
                when (item) {
                    downloaded -> presenter.setDownloadedFilter(newState)
                    unseen -> presenter.setUnseenFilter(newState)
                    bookmarked -> presenter.setBookmarkedFilter(newState)
                    else -> {}
                }
            }
        }
    }

    /**
     * Sorting group (alphabetically, by last read, ...) and ascending or descending.
     */
    inner class Sort @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val group = SortGroup()

        init {
            setGroups(listOf(group))
        }

        override fun updateView() {
            group.updateModels()
        }

        inner class SortGroup : Group {

            private val source = Item.MultiSort(R.string.sort_by_source, this)
            private val episodeNum = Item.MultiSort(R.string.sort_by_number, this)
            private val uploadDate = Item.MultiSort(R.string.sort_by_upload_date, this)

            override val header: Item? = null
            override val items = listOf(source, uploadDate, episodeNum)
            override val footer: Item? = null

            override fun initModels() {
                val anime = anime ?: return
                val sorting = anime.sorting
                val order = if (anime.sortDescending()) {
                    Item.MultiSort.SORT_DESC
                } else {
                    Item.MultiSort.SORT_ASC
                }

                source.state =
                    if (sorting == Anime.EPISODE_SORTING_SOURCE) order else Item.MultiSort.SORT_NONE
                episodeNum.state =
                    if (sorting == Anime.EPISODE_SORTING_NUMBER) order else Item.MultiSort.SORT_NONE
                uploadDate.state =
                    if (sorting == Anime.EPISODE_SORTING_UPLOAD_DATE) order else Item.MultiSort.SORT_NONE
            }

            fun updateModels() {
                initModels()
                adapter.notifyItemRangeChanged(0, 3)
            }

            override fun onItemClicked(item: Item) {
                when (item) {
                    source -> presenter.setSorting(Anime.EPISODE_SORTING_SOURCE)
                    episodeNum -> presenter.setSorting(Anime.EPISODE_SORTING_NUMBER)
                    uploadDate -> presenter.setSorting(Anime.EPISODE_SORTING_UPLOAD_DATE)
                    else -> throw Exception("Unknown sorting")
                }
            }
        }
    }

    /**
     * Display group, to show the library as a list or a grid.
     */
    inner class Display @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val group = DisplayGroup()

        init {
            setGroups(listOf(group))
        }

        override fun updateView() {
            group.updateModels()
        }

        inner class DisplayGroup : Group {

            private val displayTitle = Item.Radio(R.string.show_title, this)
            private val displayEpisodeNum = Item.Radio(R.string.show_episode_number, this)

            override val header: Item? = null
            override val items = listOf(displayTitle, displayEpisodeNum)
            override val footer: Item? = null

            override fun initModels() {
                val mode = anime?.displayMode ?: return
                displayTitle.checked = mode == Anime.EPISODE_DISPLAY_NAME
                displayEpisodeNum.checked = mode == Anime.EPISODE_DISPLAY_NUMBER
            }

            fun updateModels() {
                initModels()
                adapter.notifyItemRangeChanged(0, 2)
            }

            override fun onItemClicked(item: Item) {
                item as Item.Radio
                if (item.checked) return

                when (item) {
                    displayTitle -> presenter.setDisplayMode(Anime.EPISODE_DISPLAY_NAME)
                    displayEpisodeNum -> presenter.setDisplayMode(Anime.EPISODE_DISPLAY_NUMBER)
                    else -> throw NotImplementedError("Unknown display mode")
                }
            }
        }
    }

    open inner class Settings(context: Context, attrs: AttributeSet?) :
        ExtendedNavigationView(context, attrs) {

        lateinit var adapter: Adapter

        /**
         * Click listener to notify the parent fragment when an item from a group is clicked.
         */
        var onGroupClicked: (Group) -> Unit = {}

        fun setGroups(groups: List<Group>) {
            adapter = Adapter(groups.map { it.createItems() }.flatten())
            recycler.adapter = adapter

            groups.forEach { it.initModels() }
            addView(recycler)
        }

        open fun updateView() {
        }

        /**
         * Adapter of the recycler view.
         */
        inner class Adapter(items: List<Item>) : ExtendedNavigationView.Adapter(items) {

            override fun onItemClicked(item: Item) {
                if (item is GroupedItem) {
                    item.group.onItemClicked(item)
                    onGroupClicked(item.group)
                }
            }
        }
    }
}
