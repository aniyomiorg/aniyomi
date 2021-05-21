package eu.kanade.tachiyomi.ui.browse.migration.anime

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

class MigrationAnimeAdapter(controller: MigrationAnimeController) :
    FlexibleAdapter<IFlexible<*>>(null, controller, true) {

    val coverClickListener: OnCoverClickListener = controller

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
