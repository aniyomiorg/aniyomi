package eu.kanade.tachiyomi.data.track.job

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
    private val animePreferences = context.getSharedPreferences("tracking_queue_anime", Context.MODE_PRIVATE)

    fun addItem(track: Track) {
        val trackId = track.id.toString()
        val (_, lastChapterRead) = preferences.getString(trackId, "0:0.0")!!.split(":")
        if (track.lastChapterRead > lastChapterRead.toFloat()) {
            val value = "${track.mangaId}:${track.lastChapterRead}"
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, $value" }
            preferences.edit {
                putString(trackId, value)
            }
        }
    }

    fun addItem(track: AnimeTrack) {
        val trackId = track.id.toString()
        val (_, lastEpisodeSeen) = animePreferences.getString(trackId, "0:0.0")!!.split(":")
        if (track.lastEpisodeSeen > lastEpisodeSeen.toFloat()) {
            val value = "${track.animeId}:${track.lastEpisodeSeen}"
            logcat(LogPriority.DEBUG) { ("Queuing track item: $trackId, $value") }
            animePreferences.edit {
                putString(trackId, value)
            }
        }
    }

    fun remove(track: Track) {
        preferences.edit {
            remove(track.id.toString())
        }
    }

    fun remove(track: AnimeTrack) {
        animePreferences.edit {
            remove(track.id.toString())
        }
    }

    @Suppress("UNCHECKED_CAST")
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

    @Suppress("UNCHECKED_CAST")
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
