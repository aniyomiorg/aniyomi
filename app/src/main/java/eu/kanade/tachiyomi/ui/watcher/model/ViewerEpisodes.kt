package eu.kanade.tachiyomi.ui.watcher.model

data class ViewerEpisodes(
    val currEpisode: WatcherEpisode,
    val prevEpisode: WatcherEpisode?,
    val nextEpisode: WatcherEpisode?
) {

    fun ref() {
        currEpisode.ref()
        prevEpisode?.ref()
        nextEpisode?.ref()
    }

    fun unref() {
        currEpisode.unref()
        prevEpisode?.unref()
        nextEpisode?.unref()
    }
}
