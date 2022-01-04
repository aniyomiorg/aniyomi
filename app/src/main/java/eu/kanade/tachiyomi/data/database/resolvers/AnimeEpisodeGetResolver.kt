package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.AnimeGetResolver
import eu.kanade.tachiyomi.data.database.mappers.EpisodeGetResolver
import eu.kanade.tachiyomi.data.database.models.AnimeEpisode

class AnimeEpisodeGetResolver : DefaultGetResolver<AnimeEpisode>() {

    companion object {
        val INSTANCE = AnimeEpisodeGetResolver()
    }

    private val animeGetResolver = AnimeGetResolver()

    private val episodeGetResolver = EpisodeGetResolver()

    override fun mapFromCursor(cursor: Cursor): AnimeEpisode {
        val anime = animeGetResolver.mapFromCursor(cursor)
        val episode = episodeGetResolver.mapFromCursor(cursor)
        anime.id = episode.anime_id
        anime.url = cursor.getString(cursor.getColumnIndexOrThrow("animeUrl"))

        return AnimeEpisode(anime, episode)
    }
}
