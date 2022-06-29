package eu.kanade.tachiyomi.data.track.model

import eu.kanade.tachiyomi.data.database.models.AnimeTrack

class AnimeTrackSearch : AnimeTrack {

    override var id: Long? = null

    override var anime_id: Long = 0

    override var sync_id: Int = 0

    override var media_id: Long = 0

    override var library_id: Long? = null

    override lateinit var title: String

    override var last_episode_seen: Float = 0F

    override var total_episodes: Int = 0

    override var score: Float = 0f

    override var status: Int = 0

    override var started_watching_date: Long = 0

    override var finished_watching_date: Long = 0

    override lateinit var tracking_url: String

    var cover_url: String = ""

    var summary: String = ""

    var publishing_status: String = ""

    var publishing_type: String = ""

    var start_date: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnimeTrackSearch

        if (anime_id != other.anime_id) return false
        if (sync_id != other.sync_id) return false
        if (media_id != other.media_id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = anime_id.hashCode()
        result = 31 * result + sync_id
        result = 31 * result + media_id.hashCode()
        return result
    }

    companion object {
        fun create(serviceId: Long): AnimeTrackSearch = AnimeTrackSearch().apply {
            sync_id = serviceId.toInt()
        }
    }
}
