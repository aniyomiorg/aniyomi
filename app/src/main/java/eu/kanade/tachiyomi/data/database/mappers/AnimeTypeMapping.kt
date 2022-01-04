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
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeImpl
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_ARTIST
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_AUTHOR
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_COVER_LAST_MODIFIED
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_DATE_ADDED
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_DESCRIPTION
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_EPISODE_FLAGS
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_FAVORITE
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_GENRE
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_INITIALIZED
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_LAST_UPDATE
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_SOURCE
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_STATUS
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_THUMBNAIL_URL
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_TITLE
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_URL
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.COL_VIEWER
import eu.kanade.tachiyomi.data.database.tables.AnimeTable.TABLE

class AnimeTypeMapping : SQLiteTypeMapping<Anime>(
    AnimePutResolver(),
    AnimeGetResolver(),
    AnimeDeleteResolver()
)

class AnimePutResolver : DefaultPutResolver<Anime>() {

    override fun mapToInsertQuery(obj: Anime) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: Anime) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: Anime) =
        contentValuesOf(
            COL_ID to obj.id,
            COL_SOURCE to obj.source,
            COL_URL to obj.url,
            COL_ARTIST to obj.artist,
            COL_AUTHOR to obj.author,
            COL_DESCRIPTION to obj.description,
            COL_GENRE to obj.genre,
            COL_TITLE to obj.title,
            COL_STATUS to obj.status,
            COL_THUMBNAIL_URL to obj.thumbnail_url,
            COL_FAVORITE to obj.favorite,
            COL_LAST_UPDATE to obj.last_update,
            COL_INITIALIZED to obj.initialized,
            COL_VIEWER to obj.viewer_flags,
            COL_EPISODE_FLAGS to obj.episode_flags,
            COL_COVER_LAST_MODIFIED to obj.cover_last_modified,
            COL_DATE_ADDED to obj.date_added
        )
}

interface BaseAnimeGetResolver {
    fun mapBaseFromCursor(anime: Anime, cursor: Cursor) = anime.apply {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        source = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SOURCE))
        url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL))
        artist = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTIST))
        author = cursor.getString(cursor.getColumnIndexOrThrow(COL_AUTHOR))
        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION))
        genre = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENRE))
        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
        status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATUS))
        thumbnail_url = cursor.getString(cursor.getColumnIndexOrThrow(COL_THUMBNAIL_URL))
        favorite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FAVORITE)) == 1
        last_update = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_UPDATE))
        initialized = cursor.getInt(cursor.getColumnIndexOrThrow(COL_INITIALIZED)) == 1
        viewer_flags = cursor.getInt(cursor.getColumnIndexOrThrow(COL_VIEWER))
        episode_flags = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EPISODE_FLAGS))
        cover_last_modified = cursor.getLong(cursor.getColumnIndexOrThrow(COL_COVER_LAST_MODIFIED))
        date_added = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE_ADDED))
    }
}

open class AnimeGetResolver : DefaultGetResolver<Anime>(), BaseAnimeGetResolver {

    override fun mapFromCursor(cursor: Cursor): Anime {
        return mapBaseFromCursor(AnimeImpl(), cursor)
    }
}

class AnimeDeleteResolver : DefaultDeleteResolver<Anime>() {

    override fun mapToDeleteQuery(obj: Anime) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
