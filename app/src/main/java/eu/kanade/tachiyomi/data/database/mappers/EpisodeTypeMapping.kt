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
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.EpisodeImpl
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_ANIME_ID
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_BOOKMARK
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_DATE_FETCH
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_DATE_UPLOAD
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_EPISODE_NUMBER
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_LAST_SECOND_SEEN
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_NAME
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_SCANLATOR
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_SEEN
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_SOURCE_ORDER
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_TOTAL_SECONDS
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.COL_URL
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable.TABLE

class EpisodeTypeMapping : SQLiteTypeMapping<Episode>(
    EpisodePutResolver(),
    EpisodeGetResolver(),
    EpisodeDeleteResolver()
)

class EpisodePutResolver : DefaultPutResolver<Episode>() {

    override fun mapToInsertQuery(obj: Episode) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: Episode) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: Episode) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_ANIME_ID to obj.anime_id,
            COL_URL to obj.url,
            COL_NAME to obj.name,
            COL_SEEN to obj.seen,
            COL_SCANLATOR to obj.scanlator,
            COL_BOOKMARK to obj.bookmark,
            COL_DATE_FETCH to obj.date_fetch,
            COL_DATE_UPLOAD to obj.date_upload,
            COL_LAST_SECOND_SEEN to obj.last_second_seen,
            COL_TOTAL_SECONDS to obj.total_seconds,
            COL_EPISODE_NUMBER to obj.episode_number,
            COL_SOURCE_ORDER to obj.source_order
        )
}

class EpisodeGetResolver : DefaultGetResolver<Episode>() {

    override fun mapFromCursor(cursor: Cursor): Episode = EpisodeImpl().apply {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        anime_id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ANIME_ID))
        url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL))
        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME))
        scanlator = cursor.getString(cursor.getColumnIndexOrThrow(COL_SCANLATOR))
        seen = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SEEN)) == 1
        bookmark = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOKMARK)) == 1
        date_fetch = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE_FETCH))
        date_upload = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE_UPLOAD))
        last_second_seen = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_SECOND_SEEN))
        total_seconds = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TOTAL_SECONDS))
        episode_number = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_EPISODE_NUMBER))
        source_order = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SOURCE_ORDER))
    }
}

class EpisodeDeleteResolver : DefaultDeleteResolver<Episode>() {

    override fun mapToDeleteQuery(obj: Episode) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
