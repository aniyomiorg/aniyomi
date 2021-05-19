package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.content.Context
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor

class AnimeUpdatesAdapter(
    val controller: AnimeUpdatesController,
    context: Context
) : BaseEpisodesAdapter<IFlexible<*>>(controller) {

    var seenColor = context.getResourceColor(R.attr.colorOnSurface, 0.38f)
    var unseenColor = context.getResourceColor(R.attr.colorOnSurface)

    val coverClickListener: OnCoverClickListener = controller

    init {
        setDisplayHeadersAtStartUp(true)
    }

    interface OnCoverClickListener {
        fun onCoverClick(position: Int)
    }
}
