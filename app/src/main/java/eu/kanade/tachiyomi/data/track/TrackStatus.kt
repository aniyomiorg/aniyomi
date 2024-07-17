package eu.kanade.tachiyomi.data.track

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.komga.Komga
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.simkl.Simkl

@Suppress("MagicNumber")
enum class TrackStatus(val int: Long, @StringRes val res: Int) {
    READING(1, R.string.reading),
    WATCHING(11, R.string.watching),
    REPEATING(2, R.string.repeating),
    REWATCHING(17, R.string.repeating_anime),
    PLAN_TO_READ(3, R.string.plan_to_read),
    PLAN_TO_WATCH(16, R.string.plan_to_watch),
    PAUSED(4, R.string.on_hold),
    COMPLETED(5, R.string.completed),
    DROPPED(6, R.string.dropped),
    OTHER(7, R.string.not_tracked),
    ;

    companion object {
        @Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod")
        fun parseTrackerStatus(tracker: Long, statusLong: Long): TrackStatus? {
            return when (tracker) {
                (1L) -> {
                    when (statusLong) {
                        MyAnimeList.READING -> READING
                        MyAnimeList.WATCHING -> WATCHING
                        MyAnimeList.COMPLETED -> COMPLETED
                        MyAnimeList.ON_HOLD -> PAUSED
                        MyAnimeList.PLAN_TO_READ -> PLAN_TO_READ
                        MyAnimeList.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        MyAnimeList.DROPPED -> DROPPED
                        MyAnimeList.REREADING -> REPEATING
                        MyAnimeList.REWATCHING -> REWATCHING
                        else -> null
                    }
                }
                TrackerManager.ANILIST -> {
                    when (statusLong) {
                        Anilist.READING -> READING
                        Anilist.WATCHING -> WATCHING
                        Anilist.REPEATING_ANIME -> REWATCHING
                        Anilist.PLANNING -> PLAN_TO_READ
                        Anilist.PLANNING_ANIME -> PLAN_TO_WATCH
                        Anilist.REPEATING -> REPEATING
                        Anilist.PAUSED -> PAUSED
                        Anilist.COMPLETED -> COMPLETED
                        Anilist.DROPPED -> DROPPED
                        else -> null
                    }
                }
                TrackerManager.KITSU -> {
                    when (statusLong) {
                        Kitsu.READING -> READING
                        Kitsu.WATCHING -> WATCHING
                        Kitsu.COMPLETED -> COMPLETED
                        Kitsu.ON_HOLD -> PAUSED
                        Kitsu.PLAN_TO_READ -> PLAN_TO_READ
                        Kitsu.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Kitsu.DROPPED -> DROPPED
                        else -> null
                    }
                }
                (4L) -> {
                    when (statusLong) {
                        Shikimori.READING -> READING
                        Shikimori.COMPLETED -> COMPLETED
                        Shikimori.ON_HOLD -> PAUSED
                        Shikimori.PLAN_TO_READ -> PLAN_TO_READ
                        Shikimori.DROPPED -> DROPPED
                        Shikimori.REREADING -> REPEATING
                        else -> null
                    }
                }
                (5L) -> {
                    when (statusLong) {
                        Bangumi.READING -> READING
                        Bangumi.COMPLETED -> COMPLETED
                        Bangumi.ON_HOLD -> PAUSED
                        Bangumi.PLAN_TO_READ -> PLAN_TO_READ
                        Bangumi.DROPPED -> DROPPED
                        else -> null
                    }
                }
                (6L) -> {
                    when (statusLong) {
                        Komga.READING -> READING
                        Komga.COMPLETED -> COMPLETED
                        Komga.UNREAD -> null
                        else -> null
                    }
                }
                (7L) -> {
                    when (statusLong) {
                        MangaUpdates.READING_LIST -> READING
                        MangaUpdates.COMPLETE_LIST -> COMPLETED
                        MangaUpdates.ON_HOLD_LIST -> PAUSED
                        MangaUpdates.WISH_LIST -> PLAN_TO_READ
                        MangaUpdates.UNFINISHED_LIST -> DROPPED
                        else -> null
                    }
                }
                TrackerManager.SIMKL -> {
                    when (statusLong) {
                        Simkl.WATCHING -> WATCHING
                        Simkl.COMPLETED -> COMPLETED
                        Simkl.ON_HOLD -> PAUSED
                        Simkl.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Simkl.NOT_INTERESTING -> DROPPED
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}
