@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models.anime

import java.io.Serializable

interface AnimeTrack : Serializable {

    var id: Long?

    var anime_id: Long

    var tracker_id: Long

    var remote_id: Long

    var library_id: Long?

    var title: String

    var last_episode_seen: Double

    var total_episodes: Long

    var score: Double

    var status: Long

    var started_watching_date: Long

    var finished_watching_date: Long

    var tracking_url: String

    var private: Boolean

    fun copyPersonalFrom(other: AnimeTrack, copyRemotePrivate: Boolean = true) {
        last_episode_seen = other.last_episode_seen
        score = other.score
        status = other.status
        started_watching_date = other.started_watching_date
        finished_watching_date = other.finished_watching_date
        if (copyRemotePrivate) private = other.private
    }

    companion object {
        fun create(serviceId: Long): AnimeTrack = AnimeTrackImpl().apply {
            tracker_id = serviceId
        }
    }
}
