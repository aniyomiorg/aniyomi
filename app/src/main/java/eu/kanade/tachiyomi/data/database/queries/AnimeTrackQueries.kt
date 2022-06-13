package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable
import eu.kanade.tachiyomi.data.track.TrackService

interface AnimeTrackQueries : DbProvider {

    fun getTracks() = db.get()
        .listOfObjects(AnimeTrack::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTrackTable.TABLE)
                .build(),
        )
        .prepare()

    fun getTracks(animeId: Long?) = db.get()
        .listOfObjects(AnimeTrack::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTrackTable.TABLE)
                .where("${AnimeTrackTable.COL_ANIME_ID} = ?")
                .whereArgs(animeId)
                .build(),
        )
        .prepare()

    fun insertTrack(track: AnimeTrack) = db.put().`object`(track).prepare()

    fun insertTracks(tracks: List<AnimeTrack>) = db.put().objects(tracks).prepare()

    fun deleteTrackForAnime(anime: Anime, sync: TrackService) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(AnimeTrackTable.TABLE)
                .where("${AnimeTrackTable.COL_ANIME_ID} = ? AND ${AnimeTrackTable.COL_SYNC_ID} = ?")
                .whereArgs(anime.id, sync.id)
                .build(),
        )
        .prepare()
}
