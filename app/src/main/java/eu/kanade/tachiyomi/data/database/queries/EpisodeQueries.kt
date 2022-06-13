package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeEpisode
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.resolvers.AnimeEpisodeGetResolver
import eu.kanade.tachiyomi.data.database.resolvers.EpisodeBackupPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.EpisodeKnownBackupPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.EpisodeProgressPutResolver
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable
import java.util.Date

interface EpisodeQueries : DbProvider {

    fun getEpisodes(anime: Anime) = db.get()
        .listOfObjects(Episode::class.java)
        .withQuery(
            Query.builder()
                .table(EpisodeTable.TABLE)
                .where("${EpisodeTable.COL_ANIME_ID} = ?")
                .whereArgs(anime.id)
                .build(),
        )
        .prepare()

    fun getRecentEpisodes(date: Date) = db.get()
        .listOfObjects(AnimeEpisode::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getRecentsQueryAnime())
                .args(date.time)
                .observesTables(EpisodeTable.TABLE)
                .build(),
        )
        .withGetResolver(AnimeEpisodeGetResolver.INSTANCE)
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

    fun insertEpisodes(episodes: List<Episode>) = db.put().objects(episodes).prepare()

    fun deleteEpisodes(episodes: List<Episode>) = db.delete().objects(episodes).prepare()

    fun updateEpisodesBackup(episodes: List<Episode>) = db.put()
        .objects(episodes)
        .withPutResolver(EpisodeBackupPutResolver())
        .prepare()

    fun updateKnownEpisodesBackup(episodes: List<Episode>) = db.put()
        .objects(episodes)
        .withPutResolver(EpisodeKnownBackupPutResolver())
        .prepare()

    fun updateEpisodeProgress(episode: Episode) = db.put()
        .`object`(episode)
        .withPutResolver(EpisodeProgressPutResolver())
        .prepare()

    fun updateEpisodesProgress(episodes: List<Episode>) = db.put()
        .objects(episodes)
        .withPutResolver(EpisodeProgressPutResolver())
        .prepare()
}
