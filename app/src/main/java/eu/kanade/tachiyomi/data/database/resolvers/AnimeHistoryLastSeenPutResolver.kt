package eu.kanade.tachiyomi.data.database.resolvers

import androidx.annotation.NonNull
import androidx.core.content.contentValuesOf
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.Query
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.mappers.AnimeHistoryPutResolver
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable

class AnimeHistoryLastSeenPutResolver : AnimeHistoryPutResolver() {

    /**
     * Updates last_read time of chapter
     */
    override fun performPut(@NonNull db: StorIOSQLite, @NonNull history: AnimeHistory): PutResult = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(history)

        val cursor = db.lowLevel().query(
            Query.builder()
                .table(updateQuery.table())
                .where(updateQuery.where())
                .whereArgs(updateQuery.whereArgs())
                .build()
        )

        cursor.use { putCursor ->
            if (putCursor.count == 0) {
                val insertQuery = mapToInsertQuery(history)
                val insertedId = db.lowLevel().insert(insertQuery, mapToContentValues(history))
                PutResult.newInsertResult(insertedId, insertQuery.table())
            } else {
                val numberOfRowsUpdated = db.lowLevel().update(updateQuery, mapToUpdateContentValues(history))
                PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
            }
        }
    }

    override fun mapToUpdateQuery(obj: AnimeHistory) = UpdateQuery.builder()
        .table(AnimeHistoryTable.TABLE)
        .where("${AnimeHistoryTable.COL_EPISODE_ID} = ?")
        .whereArgs(obj.episode_id)
        .build()

    private fun mapToUpdateContentValues(history: AnimeHistory) =
        contentValuesOf(
            AnimeHistoryTable.COL_LAST_SEEN to history.last_seen
        )
}
