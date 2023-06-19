package eu.kanade.domain.track.anime.store

import android.content.Context
import androidx.core.content.edit
import logcat.LogPriority
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.track.anime.model.AnimeTrack

class DelayedAnimeTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

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

    fun removeAnimeItem(trackId: Long) {
        preferences.edit {
            remove(trackId.toString())
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

    data class DelayedAnimeTrackingItem(
        val trackId: Long,
        val lastEpisodeSeen: Float,
    )
}
