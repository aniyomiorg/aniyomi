package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderItem
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterGap
import eu.kanade.tachiyomi.util.system.createReaderThemeContext
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.delay
import tachiyomi.core.util.lang.launchUI
import tachiyomi.core.util.system.logcat
import kotlin.math.max

/**
 * Pager adapter used by this [viewer] to where [ViewerChapters] updates are posted.
 */
class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    /**
     * Paired list of currently set items.
     */
    var joinedItems: MutableList<Pair<ReaderItem, ReaderItem?>> = mutableListOf()
        private set

    /**
     * Single list of items
     */
    private var subItems: MutableList<ReaderItem> = mutableListOf()

    /**
     * Holds preprocessed items so they don't get removed when changing chapter
     */
    private var preprocessed: MutableMap<Int, InsertPage> = mutableMapOf()

    var nextTransition: ChapterTransition.Next? = null
        private set

    var currentChapter: ReaderChapter? = null

    // SY -->
    /** Page used to start the shifted pages */
    var pageToShift: ReaderPage? = null

    /** Varibles used to check if config of the pages have changed */
    private var shifted = viewer.config.shiftDoublePage
    private var doubledUp = viewer.config.doublePages
    // SY <--

    /**
     * Context that has been wrapped to use the correct theme values based on the
     * current app theme and reader background color
     */
    private var readerThemedContext = viewer.activity.createReaderThemeContext()

    /**
     * Updates this adapter with the given [chapters]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions and inverting the pages if the viewer
     * has R2L direction.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<ReaderItem>()

        // Forces chapter transition if there is missing chapters
        val prevHasMissingChapters = calculateChapterGap(chapters.currChapter, chapters.prevChapter) > 0
        val nextHasMissingChapters = calculateChapterGap(chapters.nextChapter, chapters.currChapter) > 0

        // Add previous chapter pages and transition.
        if (chapters.prevChapter != null) {
            // We only need to add the last few pages of the previous chapter, because it'll be
            // selected as the current chapter when one of those pages is selected.
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        // Skip transition page if the chapter is loaded & current page is not a transition page
        if (prevHasMissingChapters || forceTransition || chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        var insertPageLastPage: InsertPage? = null

        // Add current chapter.
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            val pages = currPages.toMutableList()

            val lastPage = pages.last()

            // Insert preprocessed pages into current page list
            preprocessed.keys.sortedDescending()
                .forEach { key ->
                    if (lastPage.index == key) {
                        insertPageLastPage = preprocessed[key]
                    }
                    preprocessed[key]?.let { pages.add(key + 1, it) }
                }

            newItems.addAll(pages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages.
        nextTransition = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
            .also {
                if (nextHasMissingChapters || forceTransition ||
                    chapters.nextChapter?.state !is ReaderChapter.State.Loaded
                ) {
                    newItems.add(it)
                }
            }

        if (chapters.nextChapter != null) {
            // Add at most two pages, because this chapter will be selected before the user can
            // swap more pages.
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        // Resets double-page splits, else insert pages get misplaced
        subItems.filterIsInstance<InsertPage>().also { subItems.removeAll(it) }

        preprocessed = mutableMapOf()
        subItems = newItems.toMutableList()

        var useSecondPage = false
        if (shifted != viewer.config.shiftDoublePage || (doubledUp != viewer.config.doublePages && doubledUp)) {
            if (shifted && (doubledUp == viewer.config.doublePages)) {
                useSecondPage = true
            }
            shifted = viewer.config.shiftDoublePage
        }
        doubledUp = viewer.config.doublePages
        setJoinedItems(useSecondPage)

        // Will skip insert page otherwise
        if (insertPageLastPage != null) {
            viewer.moveToPage(insertPageLastPage!!)
        }
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getCount(): Int {
        return joinedItems.size
    }

    /**
     * Creates a new view for the item at the given [position].
     */
    override fun createView(container: ViewGroup, position: Int): View {
        val item = joinedItems[position].first
        val item2 = joinedItems[position].second
        return when (item) {
            is ReaderPage -> PagerPageHolder(readerThemedContext, viewer, item, item2 as? ReaderPage)
            is ChapterTransition -> PagerTransitionHolder(readerThemedContext, viewer, item)
            else -> throw NotImplementedError("Holder for ${item.javaClass} not implemented")
        }
    }

    /**
     * Returns the current position of the given [view] on the adapter.
     */
    override fun getItemPosition(view: Any): Int {
        if (view is PositionableView) {
            val position = joinedItems.indexOf(view.item)
            if (position != -1) {
                return position
            } else {
                logcat { "Position for ${view.item} not found" }
            }
        }
        return POSITION_NONE
    }

    fun onPageSplit(currentPage: Any?, newPage: InsertPage) {
        if (currentPage !is ReaderPage) return

        val currentIndex = joinedItems.indexOfFirst { it.first == currentPage }

        // Put aside preprocessed pages for next chapter so they don't get removed when changing chapter
        if (currentPage.chapter.chapter.id != currentChapter?.chapter?.id) {
            preprocessed[newPage.index] = newPage
            return
        }

        val placeAtIndex = when (viewer) {
            is L2RPagerViewer,
            is VerticalPagerViewer,
            -> currentIndex + 1
            else -> currentIndex
        }

        // It will enter a endless cycle of insert pages
        if (viewer is R2LPagerViewer && placeAtIndex - 1 >= 0 && joinedItems[placeAtIndex - 1].first is InsertPage) {
            return
        }

        // Same here it will enter a endless cycle of insert pages
        if (joinedItems[placeAtIndex].first is InsertPage) {
            return
        }

        joinedItems.add(placeAtIndex, newPage to null)

        notifyDataSetChanged()
    }

    fun cleanupPageSplit() {
        val insertPages = joinedItems.filter { it.first is InsertPage }
        joinedItems.removeAll(insertPages)
        notifyDataSetChanged()
    }

    fun refresh() {
        readerThemedContext = viewer.activity.createReaderThemeContext()
    }

    // SY -->
    private fun setJoinedItems(useSecondPage: Boolean = false) {
        val oldCurrent = joinedItems.getOrNull(viewer.pager.currentItem)
        if (!viewer.config.doublePages) {
            // If not in double mode, set up items like before
            subItems.forEach {
                (it as? ReaderPage)?.shiftedPage = false
            }
            this.joinedItems = subItems.map { Pair<ReaderItem, ReaderItem?>(it, null) }.toMutableList()
            if (viewer is R2LPagerViewer) {
                joinedItems.reverse()
            }
        } else {
            val pagedItems = mutableListOf<MutableList<ReaderPage?>>()
            val otherItems = mutableListOf<ReaderItem>()
            pagedItems.add(mutableListOf())
            // Step 1: segment the pages and transition pages
            subItems.forEach {
                when (it) {
                    is ReaderPage -> {
                        if (pagedItems.last().lastOrNull() != null &&
                            pagedItems.last().last()?.chapter?.chapter?.id != it.chapter.chapter.id
                        ) {
                            pagedItems.add(mutableListOf())
                        }
                        pagedItems.last().add(it)
                    }
                    is ChapterTransition -> {
                        otherItems.add(it)
                        pagedItems.add(mutableListOf())
                    }
                }
            }
            var pagedIndex = 0
            val subJoinedItems = mutableListOf<Pair<ReaderItem, ReaderItem?>>()
            // Step 2: run through each set of pages
            pagedItems.forEach { items ->

                items.forEach {
                    it?.shiftedPage = false
                }
                // Step 3: If pages have been shifted,
                if (viewer.config.shiftDoublePage) {
                    run loop@{
                        var index = items.indexOf(pageToShift)
                        if (pageToShift?.fullPage == true) {
                            index = max(0, index - 1)
                        }
                        // Go from the current page and work your way back to the first page,
                        // or the first page that's a full page.
                        // This is done in case user tries to shift a page after a full page
                        val fullPageBeforeIndex = max(
                            0,
                            (
                                if (index > -1) {
                                    (
                                        items.take(index).indexOfLast { it?.fullPage == true }
                                        )
                                } else {
                                    -1
                                }
                                ),
                        )
                        // Add a shifted page to the first place there isnt a full page
                        (fullPageBeforeIndex until items.size).forEach {
                            if (items[it]?.fullPage == false) {
                                items[it]?.shiftedPage = true
                                return@loop
                            }
                        }
                    }
                }

                // Step 4: Add blanks for chunking
                var itemIndex = 0
                while (itemIndex < items.size) {
                    items[itemIndex]?.isolatedPage = false
                    if (items[itemIndex]?.fullPage == true || items[itemIndex]?.shiftedPage == true) {
                        // Add a 'blank' page after each full page. It will be used when chunked to solo a page
                        items.add(itemIndex + 1, null)
                        if (items[itemIndex]?.fullPage == true && itemIndex > 0 &&
                            items[itemIndex - 1] != null && (itemIndex - 1) % 2 == 0
                        ) {
                            // If a page is a full page, check if the previous page needs to be isolated
                            // we should check if it's an even or odd page, since even pages need shifting
                            // For example if Page 1 is full, Page 0 needs to be isolated
                            // No need to take account shifted pages, because null additions should
                            // always have an odd index in the list
                            items[itemIndex - 1]?.isolatedPage = true
                            items.add(itemIndex, null)
                            itemIndex++
                        }
                        itemIndex++
                    }
                    itemIndex++
                }

                // Step 5: chunk em
                if (items.isNotEmpty()) {
                    subJoinedItems.addAll(
                        items.chunked(2).map { Pair(it.first()!!, it.getOrNull(1)) },
                    )
                }
                otherItems.getOrNull(pagedIndex)?.let {
                    subJoinedItems.add(Pair(it, null))
                    pagedIndex++
                }
            }
            if (viewer is R2LPagerViewer) {
                subJoinedItems.reverse()
            }

            this.joinedItems = subJoinedItems
        }
        notifyDataSetChanged()

        // Step 6: Move back to our previous page or transition page
        // The listener is likely off around now, but either way when shifting or doubling,
        // we need to set the page back correctly
        // We will however shift to the first page of the new chapter if the last page we were are
        // on is not in the new chapter that has loaded
        val newPage =
            when {
                (oldCurrent?.first as? ReaderPage)?.chapter != currentChapter &&
                    (oldCurrent?.first as? ChapterTransition)?.from != currentChapter -> subItems.find {
                    (it as? ReaderPage)?.chapter == currentChapter
                }
                useSecondPage -> (oldCurrent?.second ?: oldCurrent?.first)
                else -> oldCurrent?.first ?: return
            }
        var index = joinedItems.indexOfFirst { it.first == newPage || it.second == newPage }
        if (newPage is ChapterTransition && index == -1) {
            val newerPage = if (newPage is ChapterTransition.Next) {
                joinedItems.filter {
                    (it.first as? ReaderPage)?.chapter == newPage.to
                }.minByOrNull { (it.first as? ReaderPage)?.index ?: Int.MAX_VALUE }?.first
            } else {
                joinedItems.filter {
                    (it.first as? ReaderPage)?.chapter == newPage.to
                }.maxByOrNull { (it.first as? ReaderPage)?.index ?: Int.MIN_VALUE }?.first
            }
            index = joinedItems.indexOfFirst { it.first == newerPage || it.second == newerPage }
        }
        viewer.pager.setCurrentItem(index, false)
    }

    fun splitDoublePages(current: ReaderPage) {
        val oldCurrent = joinedItems.getOrNull(viewer.pager.currentItem)
        setJoinedItems(
            oldCurrent?.second == current ||
                (current.index + 1) < (
                    (
                        oldCurrent?.second
                            ?: oldCurrent?.first
                        ) as? ReaderPage
                    )?.index ?: 0,
        )

        // The listener may be removed when we split a page, so the ui may not have updated properly
        // This case usually happens when we load a new chapter and the first 2 pages need to split og
        viewer.scope.launchUI {
            delay(100)
            viewer.onPageChange(viewer.pager.currentItem)
        }
    }
    // SY <--
}
