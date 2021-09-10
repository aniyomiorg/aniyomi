package eu.kanade.tachiyomi.data.track.job

import android.content.Context
import androidx.core.content.edit
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Track
import timber.log.Timber

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)
    private val animePreferences = context.getSharedPreferences("tracking_queue_anime", Context.MODE_PRIVATE)

    fun addItem(track: Track) {
        val trackId = track.id.toString()
        val (_, lastChapterRead) = preferences.getString(trackId, "0:0.0")!!.split(":")
        if (track.last_chapter_read > lastChapterRead.toFloat()) {
            val value = "${track.manga_id}:${track.last_chapter_read}"
            Timber.i("Queuing track item: $trackId, $value")
            preferences.edit {
                putString(trackId, value)
            }
        }
    }

    fun addItem(track: AnimeTrack) {
        val trackId = track.id.toString()
        val (_, lastEpisodeSeen) = animePreferences.getString(trackId, "0:0.0")!!.split(":")
        if (track.last_episode_seen > lastEpisodeSeen.toFloat()) {
            val value = "${track.anime_id}:${track.last_episode_seen}"
            Timber.i("Queuing track item: $trackId, $value")
            animePreferences.edit {
                putString(trackId, value)
            }
        }
    }

    fun clear() {
        preferences.edit {
            clear()
        }
        animePreferences.edit {
            clear()
        }
    }

    fun getItems(): List<DelayedTrackingItem> {
        return (preferences.all as Map<String, String>).entries
            .map {
                val (mangaId, lastChapterRead) = it.value.split(":")
                DelayedTrackingItem(
                    trackId = it.key.toLong(),
                    mangaId = mangaId.toLong(),
                    lastChapterRead = lastChapterRead.toFloat(),
                )
            }
    }

    fun getAnimeItems(): List<DelayedAnimeTrackingItem> {
        return (animePreferences.all as Map<String, String>).entries
            .map {
                val (animeId, lastEpisodeSeen) = it.value.split(":")
                DelayedAnimeTrackingItem(
                    trackId = it.key.toLong(),
                    animeId = animeId.toLong(),
                    lastEpisodeSeen = lastEpisodeSeen.toFloat(),
                )
            }
    }

    data class DelayedTrackingItem(
        val trackId: Long,
        val mangaId: Long,
        val lastChapterRead: Float,
    )

    data class DelayedAnimeTrackingItem(
        val trackId: Long,
        val animeId: Long,
        val lastEpisodeSeen: Float,
    )
}
