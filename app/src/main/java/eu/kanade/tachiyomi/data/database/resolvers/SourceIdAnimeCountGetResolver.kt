package eu.kanade.tachiyomi.data.database.resolvers

import android.annotation.SuppressLint
import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.models.SourceIdAnimeCount
import eu.kanade.tachiyomi.data.database.tables.AnimeTable

class SourceIdAnimeCountGetResolver : DefaultGetResolver<SourceIdAnimeCount>() {

    companion object {
        val INSTANCE = SourceIdAnimeCountGetResolver()
        const val COL_COUNT = "anime_count"
    }

    @SuppressLint("Range")
    override fun mapFromCursor(cursor: Cursor): SourceIdAnimeCount {
        val sourceID = cursor.getLong(cursor.getColumnIndexOrThrow(AnimeTable.COL_SOURCE))
        val count = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COUNT))

        return SourceIdAnimeCount(sourceID, count)
    }
}
