package tachiyomi.domain.items.season.service

import aniyomi.domain.anime.SeasonAnime
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.entries.anime.model.Anime

val seasonSortAlphabetically: (SeasonAnime, SeasonAnime) -> Int = { i1, i2 ->
    i1.anime.title.lowercase().compareToWithCollator(i2.anime.title.lowercase())
}

fun getSeasonSortComparator(anime: Anime): Comparator<SeasonAnime> = Comparator { s1, s2 ->
    when (anime.seasonSorting) {
        Anime.SEASON_SORT_SOURCE -> {
            s1.anime.seasonSourceOrder.compareTo(s2.anime.seasonSourceOrder)
        }
        Anime.SEASON_SORT_SEASON -> {
            s1.anime.seasonNumber.compareTo(s2.anime.seasonNumber)
        }
        Anime.SEASON_SORT_UPLOAD -> {
            s1.latestUpload.compareTo(s2.latestUpload)
        }
        Anime.SEASON_SORT_ALPHABET -> {
            seasonSortAlphabetically(s1, s2)
        }
        Anime.SEASON_SORT_COUNT -> {
            s1.unseenCount.compareTo(s2.unseenCount)
        }
        Anime.SEASON_SORT_LAST_SEEN -> {
            s1.lastSeen.compareTo(s2.lastSeen)
        }
        Anime.SEASON_SORT_FETCHED -> {
            s1.fetchedAt.compareTo(s2.fetchedAt)
        }
        else -> throw NotImplementedError("Invalid season sorting method: ${anime.seasonSorting}")
    }
}
