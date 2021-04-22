package eu.kanade.tachiyomi.ui.watcher.model

class InsertPage(val parent: WatcherPage) : WatcherPage(parent.index, parent.url, parent.imageUrl) {

    override var episode: WatcherEpisode = parent.episode

    init {
        stream = parent.stream
    }
}
