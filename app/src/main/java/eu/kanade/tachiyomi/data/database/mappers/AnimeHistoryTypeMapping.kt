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
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.AnimeHistoryImpl
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable.COL_EPISODE_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable.COL_LAST_SEEN
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable.COL_TIME_SEEN
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable.TABLE

class AnimeHistoryTypeMapping : SQLiteTypeMapping<AnimeHistory>(
    AnimeHistoryPutResolver(),
    AnimeHistoryGetResolver(),
    AnimeHistoryDeleteResolver()
)

open class AnimeHistoryPutResolver : DefaultPutResolver<AnimeHistory>() {

    override fun mapToInsertQuery(obj: AnimeHistory) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: AnimeHistory) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: AnimeHistory) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_EPISODE_ID to obj.episode_id,
            COL_LAST_SEEN to obj.last_seen,
            COL_TIME_SEEN to obj.time_seen
        )
}

class AnimeHistoryGetResolver : DefaultGetResolver<AnimeHistory>() {

    override fun mapFromCursor(cursor: Cursor): AnimeHistory = AnimeHistoryImpl().apply {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        episode_id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EPISODE_ID))
        last_seen = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_SEEN))
        time_seen = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_SEEN))
    }
}

class AnimeHistoryDeleteResolver : DefaultDeleteResolver<AnimeHistory>() {

    override fun mapToDeleteQuery(obj: AnimeHistory) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
