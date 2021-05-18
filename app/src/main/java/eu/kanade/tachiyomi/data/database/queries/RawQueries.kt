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
 * Query to get the manga from the library, with their categories and unread count.
 */
val libraryQuery =
    """
    SELECT M.*, COALESCE(MC.${MangaCategory.COL_CATEGORY_ID}, 0) AS ${Manga.COL_CATEGORY}
    FROM (
        SELECT ${Manga.TABLE}.*, COALESCE(C.unread, 0) AS ${Manga.COL_UNREAD}
        FROM ${Manga.TABLE}
        LEFT JOIN (
            SELECT ${Chapter.COL_MANGA_ID}, COUNT(*) AS unread
            FROM ${Chapter.TABLE}
            WHERE ${Chapter.COL_READ} = 0
            GROUP BY ${Chapter.COL_MANGA_ID}
        ) AS C
        ON ${Manga.COL_ID} = C.${Chapter.COL_MANGA_ID}
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
        SELECT ${Anime.TABLE}.*, COALESCE(C.unread, 0) AS ${Anime.COL_UNREAD}
        FROM ${Anime.TABLE}
        LEFT JOIN (
            SELECT ${Episode.COL_ANIME_ID}, COUNT(*) AS unread
            FROM ${Episode.TABLE}
            WHERE ${Episode.COL_SEEN} = 0
            GROUP BY ${Episode.COL_ANIME_ID}
        ) AS C
        ON ${Anime.COL_ID} = C.${Episode.COL_ANIME_ID}
        WHERE ${Anime.COL_FAVORITE} = 1
        GROUP BY ${Anime.COL_ID}
        ORDER BY ${Anime.COL_TITLE}
    ) AS M
    LEFT JOIN (
        SELECT * FROM ${AnimeCategory.TABLE}) AS MC
        ON MC.${AnimeCategory.COL_ANIME_ID} = M.${Anime.COL_ID}
"""

/**
 * Query to get the recent chapters of manga from the library up to a date.
 */
fun getRecentsQuery() =
    """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, * FROM ${Manga.TABLE} JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    WHERE ${Manga.COL_FAVORITE} = 1 
    AND ${Chapter.COL_DATE_UPLOAD} > ?
    AND ${Chapter.COL_DATE_FETCH} > ${Manga.COL_DATE_ADDED}
    ORDER BY ${Chapter.COL_DATE_UPLOAD} DESC
"""

/**
 * Query to get the recent chapters of manga from the library up to a date.
 */
fun getRecentsQueryAnime() =
    """
    SELECT ${Anime.TABLE}.${Anime.COL_URL} as animeUrl, * FROM ${Anime.TABLE} JOIN ${Episode.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    WHERE ${Anime.COL_FAVORITE} = 1 
    AND ${Episode.COL_DATE_UPLOAD} > ?
    AND ${Episode.COL_DATE_FETCH} > ${Anime.COL_DATE_ADDED}
    ORDER BY ${Episode.COL_DATE_UPLOAD} DESC
"""

/**
 * Query to get the recently read chapters of manga from the library up to a date.
 * The max_last_read table contains the most recent chapters grouped by manga
 * The select statement returns all information of chapters that have the same id as the chapter in max_last_read
 * and are read after the given time period
 */
fun getRecentMangasQuery(search: String = "") =
    """
    SELECT ${Manga.TABLE}.${Manga.COL_URL} as mangaUrl, ${Manga.TABLE}.*, ${Chapter.TABLE}.*, ${History.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    JOIN (
    SELECT ${Chapter.TABLE}.${Chapter.COL_MANGA_ID},${Chapter.TABLE}.${Chapter.COL_ID} as ${History.COL_CHAPTER_ID}, MAX(${History.TABLE}.${History.COL_LAST_READ}) as ${History.COL_LAST_READ}
    FROM ${Chapter.TABLE} JOIN ${History.TABLE}
    ON ${Chapter.TABLE}.${Chapter.COL_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    GROUP BY ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}) AS max_last_read
    ON ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} = max_last_read.${Chapter.COL_MANGA_ID}
    WHERE ${History.TABLE}.${History.COL_LAST_READ} > ?
    AND max_last_read.${History.COL_CHAPTER_ID} = ${History.TABLE}.${History.COL_CHAPTER_ID}
    AND lower(${Manga.TABLE}.${Manga.COL_TITLE}) LIKE '%$search%'
    ORDER BY max_last_read.${History.COL_LAST_READ} DESC
    LIMIT ? OFFSET ?
"""

fun getRecentAnimesQuery(search: String = "") =
    """
    SELECT ${Anime.TABLE}.${Anime.COL_URL} as animeUrl, ${Anime.TABLE}.*, ${Episode.TABLE}.*, ${AnimeHistory.TABLE}.*
    FROM ${Anime.TABLE}
    JOIN ${Episode.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    JOIN ${AnimeHistory.TABLE}
    ON ${Episode.TABLE}.${Episode.COL_ID} = ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID}
    JOIN (
    SELECT ${Episode.TABLE}.${Episode.COL_ANIME_ID},${Episode.TABLE}.${Episode.COL_ID} as ${AnimeHistory.COL_EPISODE_ID}, MAX(${AnimeHistory.TABLE}.${AnimeHistory.COL_LAST_SEEN}) as ${AnimeHistory.COL_LAST_SEEN}
    FROM ${Episode.TABLE} JOIN ${AnimeHistory.TABLE}
    ON ${Episode.TABLE}.${Episode.COL_ID} = ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID}
    GROUP BY ${Episode.TABLE}.${Episode.COL_ANIME_ID}) AS max_last_read
    ON ${Episode.TABLE}.${Episode.COL_ANIME_ID} = max_last_read.${Episode.COL_ANIME_ID}
    WHERE ${AnimeHistory.TABLE}.${AnimeHistory.COL_LAST_SEEN} > ?
    AND max_last_read.${AnimeHistory.COL_EPISODE_ID} = ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID}
    AND lower(${Anime.TABLE}.${Anime.COL_TITLE}) LIKE '%$search%'
    ORDER BY max_last_read.${AnimeHistory.COL_LAST_SEEN} DESC
    LIMIT ? OFFSET ?
"""

fun getHistoryByMangaId() =
    """
    SELECT ${History.TABLE}.*
    FROM ${History.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    WHERE ${Chapter.TABLE}.${Chapter.COL_MANGA_ID} = ? AND ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
"""

fun getHistoryByAnimeId() =
    """
    SELECT ${AnimeHistory.TABLE}.*
    FROM ${AnimeHistory.TABLE}
    JOIN ${Episode.TABLE}
    ON ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
    WHERE ${Episode.TABLE}.${Episode.COL_ANIME_ID} = ? AND ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
"""

fun getHistoryByChapterUrl() =
    """
    SELECT ${History.TABLE}.*
    FROM ${History.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
    WHERE ${Chapter.TABLE}.${Chapter.COL_URL} = ? AND ${History.TABLE}.${History.COL_CHAPTER_ID} = ${Chapter.TABLE}.${Chapter.COL_ID}
"""

fun getHistoryByEpisodeUrl() =
    """
    SELECT ${AnimeHistory.TABLE}.*
    FROM ${AnimeHistory.TABLE}
    JOIN ${Episode.TABLE}
    ON ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
    WHERE ${Episode.TABLE}.${Episode.COL_URL} = ? AND ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
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

fun getTotalChapterMangaQuery() =
    """
    SELECT ${Manga.TABLE}.*
    FROM ${Manga.TABLE}
    JOIN ${Chapter.TABLE}
    ON ${Manga.TABLE}.${Manga.COL_ID} = ${Chapter.TABLE}.${Chapter.COL_MANGA_ID}
    GROUP BY ${Manga.TABLE}.${Manga.COL_ID}
    ORDER by COUNT(*)
"""

fun getTotalEpisodeAnimeQuery() =
    """
    SELECT ${Anime.TABLE}.*
    FROM ${Anime.TABLE}
    JOIN ${Anime.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    GROUP BY ${Anime.TABLE}.${Anime.COL_ID}
    ORDER by COUNT(*)
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
    JOIN ${Chapter.TABLE}
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
