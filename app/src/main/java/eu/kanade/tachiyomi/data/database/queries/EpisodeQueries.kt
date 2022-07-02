package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.resolvers.EpisodeProgressPutResolver
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable

interface EpisodeQueries : DbProvider {

    fun getEpisodes(animeId: Long) = db.get()
        .listOfObjects(Episode::class.java)
        .withQuery(
            Query.builder()
                .table(EpisodeTable.TABLE)
                .where("${EpisodeTable.COL_ANIME_ID} = ?")
                .whereArgs(animeId)
                .build(),
        )
        .prepare()

    fun getEpisode(id: Long) = db.get()
        .`object`(Episode::class.java)
        .withQuery(
            Query.builder()
                .table(EpisodeTable.TABLE)
                .where("${EpisodeTable.COL_ID} = ?")
                .whereArgs(id)
                .build(),
        )
        .prepare()

    fun getEpisode(url: String) = db.get()
        .`object`(Episode::class.java)
        .withQuery(
            Query.builder()
                .table(EpisodeTable.TABLE)
                .where("${EpisodeTable.COL_URL} = ?")
                .whereArgs(url)
                .build(),
        )
        .prepare()

    fun getEpisode(url: String, animeId: Long) = db.get()
        .`object`(Episode::class.java)
        .withQuery(
            Query.builder()
                .table(EpisodeTable.TABLE)
                .where("${EpisodeTable.COL_URL} = ? AND ${EpisodeTable.COL_ANIME_ID} = ?")
                .whereArgs(url, animeId)
                .build(),
        )
        .prepare()

    fun updateEpisodeProgress(episode: Episode) = db.put()
        .`object`(episode)
        .withPutResolver(EpisodeProgressPutResolver())
        .prepare()
}
