package eu.kanade.tachiyomi.data.database.resolvers

import android.content.ContentValues
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.tables.AnimeTable

class AnimeNextUpdatedPutResolver : PutResolver<Anime>() {

    override fun performPut(db: StorIOSQLite, anime: Anime) = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(anime)
        val contentValues = mapToContentValues(anime)

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    fun mapToUpdateQuery(anime: Anime) = UpdateQuery.builder()
        .table(AnimeTable.TABLE)
        .where("${AnimeTable.COL_ID} = ?")
        .whereArgs(anime.id)
        .build()

    fun mapToContentValues(anime: Anime) = ContentValues(1).apply {
        put(AnimeTable.COL_NEXT_UPDATE, anime.next_update)
    }
}
