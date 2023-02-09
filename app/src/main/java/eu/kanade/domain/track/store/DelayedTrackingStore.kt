package eu.kanade.domain.track.store

import android.content.Context
import androidx.core.content.edit
import eu.kanade.domain.animetrack.model.AnimeTrack
import eu.kanade.domain.track.model.Track
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    fun addMangaItem(track: Track) {
        val trackId = track.id.toString()
        val lastChapterRead = preferences.getFloat(trackId, 0f)
        if (track.lastChapterRead > lastChapterRead) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, last chapter read: ${track.lastChapterRead}" }
            preferences.edit {
                putFloat(trackId, track.lastChapterRead.toFloat())
            }
        }
    }

    fun addAnimeItem(track: AnimeTrack) {
        val trackId = track.id.toString()
        val lastEpisodeSeen = preferences.getFloat(trackId, 0f)
        if (track.lastEpisodeSeen > lastEpisodeSeen) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, last episode seen: ${track.lastEpisodeSeen}" }
            preferences.edit {
                putFloat(trackId, track.lastEpisodeSeen.toFloat())
            }
        }
    }

    fun remove(trackId: Long) {
        preferences.edit {
            remove(trackId.toString())
        }
    }

    fun getMangaItems(): List<DelayedTrackingItem> {
        return preferences.all.mapNotNull {
            DelayedTrackingItem(
                trackId = it.key.toLong(),
                lastChapterRead = it.value.toString().toFloat(),
            )
        }
    }

    fun getAnimeItems(): List<DelayedAnimeTrackingItem> {
        return preferences.all.mapNotNull {
            DelayedAnimeTrackingItem(
                trackId = it.key.toLong(),
                lastEpisodeSeen = it.value.toString().toFloat(),
            )
        }
    }

    data class DelayedTrackingItem(
        val trackId: Long,
        val lastChapterRead: Float,
    )

    data class DelayedAnimeTrackingItem(
        val trackId: Long,
        val lastEpisodeSeen: Float,
    )
}
