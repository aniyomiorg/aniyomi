package eu.kanade.tachiyomi.ui.anime.episode.base

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

abstract class BaseEpisodesAdapter<T : IFlexible<*>>(
    controller: OnEpisodeClickListener,
    items: List<T>? = null,
) : FlexibleAdapter<T>(items, controller, true) {

    /**
     * Listener for browse item clicks.
     */
    val clickListener: OnEpisodeClickListener = controller

    /**
     * Listener which should be called when user clicks the download icons.
     */
    interface OnEpisodeClickListener {
        fun downloadEpisode(position: Int)
        fun deleteEpisode(position: Int)
        fun startDownloadNow(position: Int)
        fun downloadEpisodeExternally(position: Int)
    }
}
