@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.track.model

import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack

class MangaTrackSearch : MangaTrack {

    override var id: Long? = null

    override var manga_id: Long = 0

    override var tracker_id: Long = 0

    override var remote_id: Long = 0

    override var library_id: Long? = null

    override lateinit var title: String

    override var last_chapter_read: Double = 0.0

    override var total_chapters: Long = 0

    override var score: Double = -1.0

    override var status: Long = 0

    override var started_reading_date: Long = 0

    override var finished_reading_date: Long = 0

    override var private: Boolean = false

    override lateinit var tracking_url: String

    var authors: List<String> = emptyList()

    var artists: List<String> = emptyList()

    var cover_url: String = ""

    var summary: String = ""

    var publishing_status: String = ""

    var publishing_type: String = ""

    var start_date: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MangaTrackSearch

        if (manga_id != other.manga_id) return false
        if (tracker_id != other.tracker_id) return false
        if (remote_id != other.remote_id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manga_id.hashCode()
        result = 31 * result + tracker_id.hashCode()
        result = 31 * result + remote_id.hashCode()
        return result
    }

    companion object {
        fun create(serviceId: Long): MangaTrackSearch = MangaTrackSearch().apply {
            tracker_id = serviceId
        }
    }
}
