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
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable.COL_ANIME_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable.COL_CATEGORY_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable.TABLE

class AnimeCategoryTypeMapping : SQLiteTypeMapping<AnimeCategory>(
    AnimeCategoryPutResolver(),
    AnimeCategoryGetResolver(),
    AnimeCategoryDeleteResolver()
)

class AnimeCategoryPutResolver : DefaultPutResolver<AnimeCategory>() {

    override fun mapToInsertQuery(obj: AnimeCategory) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: AnimeCategory) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: AnimeCategory) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_ANIME_ID to obj.anime_id,
            COL_CATEGORY_ID to obj.category_id
        )
}

class AnimeCategoryGetResolver : DefaultGetResolver<AnimeCategory>() {

    override fun mapFromCursor(cursor: Cursor): AnimeCategory = AnimeCategory().apply {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        anime_id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ANIME_ID))
        category_id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_ID))
    }
}

class AnimeCategoryDeleteResolver : DefaultDeleteResolver<AnimeCategory>() {

    override fun mapToDeleteQuery(obj: AnimeCategory) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
