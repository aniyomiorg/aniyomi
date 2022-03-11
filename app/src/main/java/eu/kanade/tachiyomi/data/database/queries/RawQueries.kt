package eu.kanade.tachiyomi.data.database.queries

import eu.kanade.tachiyomi.data.database.resolvers.SourceIdAnimeCountGetResolver
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable as AnimeCategory
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable as AnimeHistory
import eu.kanade.tachiyomi.data.database.tables.AnimeTable as Anime
import eu.kanade.tachiyomi.data.database.tables.CategoryTable as Category
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable as Episode

/**
 * Query to get the anime from the library, with their categories and unwatched count.
 */

const val animelibQuery =
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
 * Query to get the recent episodes of anime from the library up to a date.
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

fun getHistoryByAnimeId() =
    """
    SELECT ${AnimeHistory.TABLE}.*
    FROM ${AnimeHistory.TABLE}
    JOIN ${Episode.TABLE}
    ON ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
    WHERE ${Episode.TABLE}.${Episode.COL_ANIME_ID} = ? AND ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
"""

fun getHistoryByEpisodeUrl() =
    """
    SELECT ${AnimeHistory.TABLE}.*
    FROM ${AnimeHistory.TABLE}
    JOIN ${Episode.TABLE}
    ON ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
    WHERE ${Episode.TABLE}.${Episode.COL_URL} = ? AND ${AnimeHistory.TABLE}.${AnimeHistory.COL_EPISODE_ID} = ${Episode.TABLE}.${Episode.COL_ID}
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

fun getTotalEpisodeAnimeQuery() =
    """
    SELECT ${Anime.TABLE}.*
    FROM ${Anime.TABLE}
    JOIN ${Episode.TABLE}
    ON ${Anime.TABLE}.${Anime.COL_ID} = ${Episode.TABLE}.${Episode.COL_ANIME_ID}
    GROUP BY ${Anime.TABLE}.${Anime.COL_ID}
    ORDER by COUNT(*)
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
 * Query to get the categories for an anime.
 */
fun getCategoriesForAnimeQuery() =
    """
    SELECT ${Category.TABLE}.* FROM ${Category.TABLE}
    JOIN ${AnimeCategory.TABLE} ON ${Category.TABLE}.${Category.COL_ID} =
    ${AnimeCategory.TABLE}.${AnimeCategory.COL_CATEGORY_ID}
    WHERE ${AnimeCategory.COL_ANIME_ID} = ?
"""

/** Query to get the list of sources in the database that have
 * non-library manga, and how many
 */
fun getSourceIdsWithNonLibraryAnimeQuery() =
    """
    SELECT ${Anime.COL_SOURCE}, COUNT(*) as ${SourceIdAnimeCountGetResolver.COL_COUNT}
    FROM ${Anime.TABLE}
    WHERE ${Anime.COL_FAVORITE} = 0
    GROUP BY ${Anime.COL_SOURCE}
    """
