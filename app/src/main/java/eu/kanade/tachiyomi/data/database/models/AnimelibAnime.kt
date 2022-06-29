package eu.kanade.tachiyomi.data.database.models

class AnimelibAnime : AnimeImpl() {

    var unseenCount: Int = 0
    var seenCount: Int = 0

    val totalEpisodes
        get() = seenCount + seenCount

    val hasStarted
        get() = seenCount > 0

    var category: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimelibAnime) return false
        if (!super.equals(other)) return false

        if (unseenCount != other.unseenCount) return false
        if (seenCount != other.seenCount) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + unseenCount
        result = 31 * result + seenCount
        result = 31 * result + category
        return result
    }
}
