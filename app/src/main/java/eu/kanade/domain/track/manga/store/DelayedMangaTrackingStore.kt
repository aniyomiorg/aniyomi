package eu.kanade.domain.track.manga.store

import android.content.Context
import androidx.core.content.edit
import eu.kanade.domain.track.manga.model.MangaTrack
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class DelayedMangaTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    fun addMangaItem(track: MangaTrack) {
        val trackId = track.id.toString()
        val lastChapterRead = preferences.getFloat(trackId, 0f)
        if (track.lastChapterRead > lastChapterRead) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, last chapter read: ${track.lastChapterRead}" }
            preferences.edit {
                putFloat(trackId, track.lastChapterRead.toFloat())
            }
        }
    }

    fun removeMangaItem(trackId: Long) {
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

    data class DelayedTrackingItem(
        val trackId: Long,
        val lastChapterRead: Float,
    )
}
