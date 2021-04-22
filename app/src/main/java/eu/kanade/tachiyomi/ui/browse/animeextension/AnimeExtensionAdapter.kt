package eu.kanade.tachiyomi.ui.browse.animeextension

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter that holds the catalogue cards.
 *
 * @param controller instance of [AnimeExtensionController].
 */
class AnimeExtensionAdapter(controller: AnimeExtensionController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val buttonClickListener: OnButtonClickListener = controller

    interface OnButtonClickListener {
        fun onButtonClick(position: Int)
    }
}
