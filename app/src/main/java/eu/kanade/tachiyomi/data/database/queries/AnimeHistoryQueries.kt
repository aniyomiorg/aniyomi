package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.AnimeDbProvider
import eu.kanade.tachiyomi.data.database.models.AnimeEpisodeHistory
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.resolvers.AnimeEpisodeHistoryGetResolver
import eu.kanade.tachiyomi.data.database.resolvers.AnimeHistoryLastSeenPutResolver
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable
import java.util.Date

interface AnimeHistoryQueries : AnimeDbProvider {

    /**
     * Insert history into database
     * @param history object containing history information
     */
    fun insertHistory(history: AnimeHistory) = db.put().`object`(history).prepare()

    /**
     * Returns history of recent anime containing last read chapter
     * @param date recent date range
     * @param limit the limit of anime to grab
     * @param offset offset the db by
     * @param search what to search in the db history
     */
    fun getRecentAnime(date: Date, limit: Int = 25, offset: Int = 0, search: String = "") = db.get()
        .listOfObjects(AnimeEpisodeHistory::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getRecentAnimesQuery(search))
                .args(date.time, limit, offset)
                .observesTables(AnimeHistoryTable.TABLE)
                .build(),
        )
        .withGetResolver(AnimeEpisodeHistoryGetResolver.INSTANCE)
        .prepare()

    fun getHistoryByAnimeId(animeId: Long) = db.get()
        .listOfObjects(AnimeHistory::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getHistoryByAnimeId())
                .args(animeId)
                .observesTables(AnimeHistoryTable.TABLE)
                .build(),
        )
        .prepare()

    fun getHistoryByEpisodeUrl(chapterUrl: String) = db.get()
        .`object`(AnimeHistory::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getHistoryByEpisodeUrl())
                .args(chapterUrl)
                .observesTables(AnimeHistoryTable.TABLE)
                .build(),
        )
        .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param historyList history object list
     */
    fun updateAnimeHistoryLastSeen(historyList: List<AnimeHistory>) = db.put()
        .objects(historyList)
        .withPutResolver(AnimeHistoryLastSeenPutResolver())
        .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param history history object
     */
    fun updateAnimeHistoryLastSeen(history: AnimeHistory) = db.put()
        .`object`(history)
        .withPutResolver(AnimeHistoryLastSeenPutResolver())
        .prepare()

    fun deleteHistory() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeHistoryTable.TABLE)
                .build(),
        )
        .prepare()

    fun deleteHistoryNoLastSeen() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeHistoryTable.TABLE)
                .where("${AnimeHistoryTable.COL_LAST_SEEN} = ?")
                .whereArgs(0)
                .build(),
        )
        .prepare()
}
