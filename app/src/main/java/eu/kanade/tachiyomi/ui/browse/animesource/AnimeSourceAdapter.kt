package eu.kanade.tachiyomi.ui.browse.animesource

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [AnimeSourceController].
 */
class AnimeSourceAdapter(controller: AnimeSourceController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val clickListener: OnSourceClickListener = controller

    /**
     * Listener which should be called when user clicks browse.
     * Note: Should only be handled by [AnimeSourceController]
     */
    interface OnSourceClickListener {
        fun onBrowseClick(position: Int)
        fun onLatestClick(position: Int)
        fun onPinClick(position: Int)
    }
}
