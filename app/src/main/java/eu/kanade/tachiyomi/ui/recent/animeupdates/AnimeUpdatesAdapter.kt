package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.content.Context
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor

class AnimeUpdatesAdapter(
    val controller: AnimeUpdatesController,
    context: Context,
    val items: List<IFlexible<*>>?,
) : BaseEpisodesAdapter<IFlexible<*>>(controller, items) {

    var seenColor = context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    var unseenColor = context.getResourceColor(R.attr.colorOnSurface)
    val unseenColorSecondary = context.getResourceColor(android.R.attr.textColorSecondary)
    var bookmarkedColor = context.getResourceColor(R.attr.colorAccent)

    val coverClickListener: OnCoverClickListener = controller

    init {
        setDisplayHeadersAtStartUp(true)
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
