package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.tables.TrackTable
import eu.kanade.tachiyomi.data.track.TrackService

interface AnimeTrackQueries : DbProvider {

    fun getTracks() = db.get()
        .listOfObjects(Track::class.java)
        .withQuery(
            Query.builder()
                .table(TrackTable.TABLE)
                .build()
        )
        .prepare()

    fun getTracks(anime: Anime) = db.get()
        .listOfObjects(Track::class.java)
        .withQuery(
            Query.builder()
                .table(TrackTable.TABLE)
                .where("${TrackTable.COL_MANGA_ID} = ?")
                .whereArgs(anime.id)
                .build()
        )
        .prepare()

    fun insertTrack(track: Track) = db.put().`object`(track).prepare()

    fun insertTracks(tracks: List<Track>) = db.put().objects(tracks).prepare()

    fun deleteTrackForAnime(anime: Anime, sync: TrackService) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(TrackTable.TABLE)
                .where("${TrackTable.COL_MANGA_ID} = ? AND ${TrackTable.COL_SYNC_ID} = ?")
                .whereArgs(anime.id, sync.id)
                .build()
        )
        .prepare()
}
