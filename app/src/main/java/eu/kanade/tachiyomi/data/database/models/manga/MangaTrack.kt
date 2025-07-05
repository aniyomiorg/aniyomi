@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models.manga

import java.io.Serializable

interface MangaTrack : Serializable {

    var id: Long?

    var manga_id: Long

    var tracker_id: Long

    var remote_id: Long

    var library_id: Long?

    var title: String

    var last_chapter_read: Double

    var total_chapters: Long

    var score: Double

    var status: Long

    var started_reading_date: Long

    var finished_reading_date: Long

    var tracking_url: String

    var private: Boolean

    fun copyPersonalFrom(other: MangaTrack, copyRemotePrivate: Boolean = true) {
        last_chapter_read = other.last_chapter_read
        score = other.score
        status = other.status
        started_reading_date = other.started_reading_date
        finished_reading_date = other.finished_reading_date
        if (copyRemotePrivate) private = other.private
    }

    companion object {
        fun create(serviceId: Long): MangaTrack = MangaTrackImpl().apply {
            tracker_id = serviceId
        }
    }
}
