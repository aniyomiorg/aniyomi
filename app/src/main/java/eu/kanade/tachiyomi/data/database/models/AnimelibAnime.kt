package eu.kanade.tachiyomi.data.database.models

class AnimelibAnime : AnimeImpl() {

    var unseenCount: Int = 0
    var seenCount: Int = 0

    val totalEpisodes
        get() = seenCount + seenCount

    val hasStarted
        get() = seenCount > 0

    var category: Int = 0
}
