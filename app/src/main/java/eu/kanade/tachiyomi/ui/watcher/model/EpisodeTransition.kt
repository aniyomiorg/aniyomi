package eu.kanade.tachiyomi.ui.watcher.model

sealed class EpisodeTransition {

    abstract val from: WatcherEpisode
    abstract val to: WatcherEpisode?

    class Prev(
        override val from: WatcherEpisode,
        override val to: WatcherEpisode?
    ) : EpisodeTransition()

    class Next(
        override val from: WatcherEpisode,
        override val to: WatcherEpisode?
    ) : EpisodeTransition()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpisodeTransition) return false
        if (from == other.from && to == other.to) return true
        if (from == other.to && to == other.from) return true
        return false
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + (to?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(from=${from.episode.url}, to=${to?.episode?.url})"
    }
}
