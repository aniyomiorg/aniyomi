package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable as AnimeCategory
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable as AnimeHistory
import eu.kanade.tachiyomi.data.database.tables.AnimeTable as Anime
import eu.kanade.tachiyomi.data.database.tables.CategoryTable as Category
import eu.kanade.tachiyomi.data.database.tables.ChapterTable as Chapter
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable as Episode
import eu.kanade.tachiyomi.data.database.tables.HistoryTable as History
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable as MangaCategory
import eu.kanade.tachiyomi.data.database.tables.MangaTable as Manga

/**
 * Query to get the manga from the library, with their categories, read and unread count.
 */
val libraryQuery =
    """
    SELECT M.*, COALESCE(MC.${MangaCategory.COL_CATEGORY_ID}, 0) AS ${Manga.COL_CATEGORY}
    FROM (
        SELECT ${Manga.TABLE}.*, COALESCE(C.unreadCount, 0) AS ${Manga.COMPUTED_COL_UNREAD_COUNT}, COALESCE(R.readCount, 0) AS ${Manga.COMPUTED_COL_READ_COUNT}
        FROM ${Manga.TABLE}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, COUNT(*) AS unreadCount
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_READ} = 0
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS C
        ON ${Manga.COL_ID} = C.${Chapter.COL_MANGA_ID}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, COUNT(*) AS readCount
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_READ} = 1
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS R
        ON ${Manga.COL_ID} = R.${Chapter.COL_MANGA_ID}
        WHERE ${Manga.COL_FAVORITE} = 1
        GROUP BY ${Manga.COL_ID}
        ORDER BY ${Manga.COL_TITLE}
    ) AS M
    LEFT JOIN (
        SELECT * FROM ${MangaCategory.TABLE}) AS MC
        ON MC.${MangaCategory.COL_MANGA_ID} = M.${Manga.COL_ID}
"""

val animelibQuery =
    """
    SELECT M.*, COALESCE(MC.${AnimeCategory.COL_CATEGORY_ID}, 0) AS ${Anime.COL_CATEGORY}
    FROM (
        SELECT ${Anime.TABLE}.*, COALESCE(C.unseenCount, 0) AS ${Anime.COMPUTED_COL_UNSEEN_COUNT}, COALESCE(R.seenCount, 0) AS ${Anime.COMPUTED_COL_SEEN_COUNT}
        FROM ${Anime.TABLE}
        LEFT JOIN (
            SELECT ${Episode.COL_ANIME_ID}, COUNT(*) AS unseenCount
            FROM ${Episode.TABLE}
            WHERE ${Episode.COL_SEEN} = 0
            GROUP BY ${Episode.COL_ANIME_ID}
        ) AS C
        ON ${Anime.COL_ID} = C.${Episode.COL_ANIME_ID}
        LEFT JOIN (
            SELECT ${Episode.COL_ANIME_ID}, COUNT(*) AS seenCount
            FROM ${Episode.TABLE}
            WHERE ${Episode.COL_SEEN} = 1
            GROUP BY ${Episode.COL_ANIME_ID}
        ) AS R
        ON ${Anime.COL_ID} = R.${Episode.COL_ANIME_ID}
        WHERE ${Anime.COL_FAVORITE} = 1
        GROUP BY ${Anime.COL_ID}
        ORDER BY ${Anime.COL_TITLE}
    ) AS M
    LEFT JOIN (
        SELECT * FROM ${AnimeCategory.TABLE}) AS MC
        ON MC.${AnimeCategory.COL_ANIME_ID} = M.${Anime.COL_ID}
"""

fun getLastReadMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*, MAX(${History.TABLE}.${History.COL_LAST_READ}) AS max
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    WHERE ${Manga.TABLE}.${Manga.COL_FAVORITE} = 1
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER BY max DESC
"""

fun getLastSeenAnimeQuery() =
    """
    SELECT ${Anime.TABLE}.*, MAX(${AnimeHistory.TABLE}.${AnimeHistory.COL_LAST_SEEN}) AS max
    FROM ${Anime.TABLE}
    JOIN ${Episode.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    JOIN ${AnimeHistory.TABLE}
    ON ${Episode.TABLE}.${Episode.COL_ID} = ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID}
    WHERE ${Anime.TABLE}.${Anime.COL_FAVORITE} = 1
    GROUP BY ${Anime.TABLE}.${Anime.COL_ID}
    ORDER BY max DESC
"""

fun getLatestChapterMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*, MAX(${Chapter.TABLE}.${Chapter.COL_DATE_UPLOAD}) AS max
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER by max DESC
"""

fun getLatestEpisodeAnimeQuery() =
    """
    SELECT ${Anime.TABLE}.*, MAX(${Episode.TABLE}.${Episode.COL_DATE_UPLOAD}) AS max
    FROM ${Anime.TABLE}
    JOIN ${Episode.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    GROUP BY ${Anime.TABLE}.${Anime.COL_ID}
    ORDER by max DESC
"""

fun getChapterFetchDateMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*, MAX(${Chapter.TABLE}.${Chapter.COL_DATE_FETCH}) AS max
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER by max DESC
"""

fun getEpisodeFetchDateAnimeQuery() =
    """
    SELECT ${Anime.TABLE}.*, MAX(${Episode.TABLE}.${Episode.COL_DATE_FETCH}) AS max
    FROM ${Anime.TABLE}
    JOIN ${Episode.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    GROUP BY ${Anime.TABLE}.${Anime.COL_ID}
    ORDER by max DESC
"""

/**
 * Query to get the categories for a manga.
 */
fun getCategoriesForMangaQuery() =
    """
    SELECT ${Category.TABLE}.* FROM ${Category.TABLE}
    JOIN ${MangaCategory.TABLE} ON ${Category.TABLE}.${Category.COL_ID} =
    ${MangaCategory.TABLE}.${MangaCategory.COL_CATEGORY_ID}
    WHERE ${MangaCategory.COL_MANGA_ID} = ?
"""

/**
 * Query to get the categories for an anime.
 */
fun getCategoriesForAnimeQuery() =
    """
    SELECT ${Category.TABLE}.* FROM ${Category.TABLE}
    JOIN ${AnimeCategory.TABLE} ON ${Category.TABLE}.${Category.COL_ID} =
    ${AnimeCategory.TABLE}.${AnimeCategory.COL_CATEGORY_ID}
    WHERE ${AnimeCategory.COL_ANIME_ID} = ?
"""
