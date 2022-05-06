package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.resolvers.AnimeHistoryUpsertResolver
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable

interface AnimeHistoryQueries : DbProvider {

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

    fun getHistoryByEpisodeUrl(episodeUrl: String) = db.get()
        .`object`(AnimeHistory::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getHistoryByEpisodeUrl())
                .args(episodeUrl)
                .observesTables(AnimeHistoryTable.TABLE)
                .build(),
        )
        .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param history history object
     */
    fun upsertAnimeHistoryLastSeen(history: AnimeHistory) = db.put()
        .`object`(history)
        .withPutResolver(AnimeHistoryUpsertResolver())
        .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param historyList history object list
     */
    fun upsertAnimeHistoryLastSeen(historyList: List<AnimeHistory>) = db.put()
        .objects(historyList)
        .withPutResolver(AnimeHistoryUpsertResolver())
        .prepare()

    fun deleteAnimeHistoryNoLastSeen() = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeHistoryTable.TABLE)
                .where("${AnimeHistoryTable.COL_LAST_SEEN} = ?")
                .whereArgs(0)
                .build(),
        )
        .prepare()
}
