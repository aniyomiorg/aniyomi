package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.BaseAnimeGetResolver
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime
import eu.kanade.tachiyomi.data.database.tables.AnimeTable

class AnimelibAnimeGetResolver : DefaultGetResolver<AnimelibAnime>(), BaseAnimeGetResolver {

    companion object {
        val INSTANCE = AnimelibAnimeGetResolver()
    }

    override fun mapFromCursor(cursor: Cursor): AnimelibAnime {
        val anime = AnimelibAnime()

        mapBaseFromCursor(anime, cursor)
        anime.unseen = cursor.getInt(cursor.getColumnIndexOrThrow(AnimeTable.COL_UNREAD))
        anime.category = cursor.getInt(cursor.getColumnIndexOrThrow(AnimeTable.COL_CATEGORY))

        return anime
    }
}
