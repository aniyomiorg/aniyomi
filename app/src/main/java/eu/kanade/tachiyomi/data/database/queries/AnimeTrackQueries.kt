package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.Query
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable

interface AnimeTrackQueries : DbProvider {

    fun getTracks() = db.get()
        .listOfObjects(AnimeTrack::class.java)
        .withQuery(
            Query.builder()
                .table(AnimeTrackTable.TABLE)
                .build(),
        )
        .prepare()
}
