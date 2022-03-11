package eu.kanade.tachiyomi.data.track.job

import android.content.Context
import androidx.core.content.edit
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val animePreferences = context.getSharedPreferences("tracking_queue_anime", Context.MODE_PRIVATE)

    fun addItem(track: AnimeTrack) {
        val trackId = track.id.toString()
        val (_, lastEpisodeSeen) = animePreferences.getString(trackId, "0:0.0")!!.split(":")
        if (track.last_episode_seen > lastEpisodeSeen.toFloat()) {
            val value = "${track.anime_id}:${track.last_episode_seen}"
            logcat(LogPriority.INFO) { ("Queuing track item: $trackId, $value") }
            animePreferences.edit {
                putString(trackId, value)
            }
        }
    }

    fun clear() {
        animePreferences.edit {
            clear()
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

    data class DelayedAnimeTrackingItem(
        val trackId: Long,
        val animeId: Long,
        val lastEpisodeSeen: Float,
    )
}
