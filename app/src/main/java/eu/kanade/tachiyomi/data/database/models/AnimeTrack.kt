package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface AnimeTrack : Serializable {

    var id: Long?

    var anime_id: Long

    var sync_id: Int

    var media_id: Long

    var library_id: Long?

    var title: String

    var last_episode_seen: Float

    var total_episodes: Int

    var score: Float

    var status: Int

    var started_watching_date: Long

    var finished_watching_date: Long

    var tracking_url: String

    fun copyPersonalFrom(other: AnimeTrack) {
        last_episode_seen = other.last_episode_seen
        score = other.score
        status = other.status
        started_watching_date = other.started_watching_date
        finished_watching_date = other.finished_watching_date
    }

    companion object {
        fun create(serviceId: Long): AnimeTrack = AnimeTrackImpl().apply {
            sync_id = serviceId.toInt()
        }
    }
}
