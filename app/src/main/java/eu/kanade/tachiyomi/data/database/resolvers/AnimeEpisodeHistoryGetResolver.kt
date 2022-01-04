package eu.kanade.tachiyomi.data.database.resolvers

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import eu.kanade.tachiyomi.data.database.mappers.AnimeGetResolver
import eu.kanade.tachiyomi.data.database.mappers.AnimeHistoryGetResolver
import eu.kanade.tachiyomi.data.database.mappers.EpisodeGetResolver
import eu.kanade.tachiyomi.data.database.models.AnimeEpisodeHistory

class AnimeEpisodeHistoryGetResolver : DefaultGetResolver<AnimeEpisodeHistory>() {
    companion object {
        val INSTANCE = AnimeEpisodeHistoryGetResolver()
    }

    /**
     * Anime get resolver
     */
    private val animeGetResolver = AnimeGetResolver()

    /**
     * Episode get resolver
     */
    private val episodeResolver = EpisodeGetResolver()

    /**
     * History get resolver
     */
    private val animehistoryGetResolver = AnimeHistoryGetResolver()

    /**
     * Map correct objects from cursor result
     */
    override fun mapFromCursor(cursor: Cursor): AnimeEpisodeHistory {
        // Get anime object
        val anime = animeGetResolver.mapFromCursor(cursor)

        // Get episode object
        val episode = episodeResolver.mapFromCursor(cursor)

        // Get history object
        val history = animehistoryGetResolver.mapFromCursor(cursor)

        // Make certain column conflicts are dealt with
        anime.id = episode.anime_id
        anime.url = cursor.getString(cursor.getColumnIndexOrThrow("animeUrl"))
        episode.id = history.episode_id

        // Return result
        return AnimeEpisodeHistory(anime, episode, history)
    }
}
