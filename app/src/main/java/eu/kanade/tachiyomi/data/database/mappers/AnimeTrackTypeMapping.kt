package eu.kanade.tachiyomi.data.database.mappers

import android.database.Cursor
import androidx.core.content.contentValuesOf
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.AnimeTrackImpl
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_ANIME_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_FINISH_DATE
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_LAST_EPISODE_SEEN
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_LIBRARY_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_MEDIA_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_SCORE
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_START_DATE
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_STATUS
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_SYNC_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_TITLE
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_TOTAL_EPISODES
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.COL_TRACKING_URL
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable.TABLE

class AnimeTrackTypeMapping : SQLiteTypeMapping<AnimeTrack>(
    AnimeTrackPutResolver(),
    AnimeTrackGetResolver(),
    AnimeTrackDeleteResolver()
)

class AnimeTrackPutResolver : DefaultPutResolver<AnimeTrack>() {

    override fun mapToInsertQuery(obj: AnimeTrack) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: AnimeTrack) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: AnimeTrack) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_ANIME_ID to obj.anime_id,
            COL_SYNC_ID to obj.sync_id,
            COL_MEDIA_ID to obj.media_id,
            COL_LIBRARY_ID to obj.library_id,
            COL_TITLE to obj.title,
            COL_LAST_EPISODE_SEEN to obj.last_episode_seen,
            COL_TOTAL_EPISODES to obj.total_episodes,
            COL_STATUS to obj.status,
            COL_TRACKING_URL to obj.tracking_url,
            COL_SCORE to obj.score,
            COL_START_DATE to obj.started_watching_date,
            COL_FINISH_DATE to obj.finished_watching_date
        )
}

class AnimeTrackGetResolver : DefaultGetResolver<AnimeTrack>() {

    override fun mapFromCursor(cursor: Cursor): AnimeTrack = AnimeTrackImpl().apply {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        anime_id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ANIME_ID))
        sync_id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNC_ID))
        media_id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MEDIA_ID))
        library_id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LIBRARY_ID))
        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
        last_episode_seen = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_LAST_EPISODE_SEEN))
        total_episodes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_EPISODES))
        status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATUS))
        score = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_SCORE))
        tracking_url = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRACKING_URL))
        started_watching_date = cursor.getLong(cursor.getColumnIndexOrThrow(COL_START_DATE))
        finished_watching_date = cursor.getLong(cursor.getColumnIndexOrThrow(COL_FINISH_DATE))
    }
}

class AnimeTrackDeleteResolver : DefaultDeleteResolver<AnimeTrack>() {

    override fun mapToDeleteQuery(obj: AnimeTrack) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
