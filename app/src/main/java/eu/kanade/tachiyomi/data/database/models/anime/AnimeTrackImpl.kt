@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models.anime

class AnimeTrackImpl : AnimeTrack {

    override var id: Long? = null

    override var anime_id: Long = 0

    override var tracker_id: Long = 0

    override var remote_id: Long = 0

    override var library_id: Long? = null

    override lateinit var title: String

    override var last_episode_seen: Double = 0.0

    override var total_episodes: Long = 0

    override var score: Double = 0.0

    override var status: Long = 0

    override var started_watching_date: Long = 0

    override var finished_watching_date: Long = 0

    override var tracking_url: String = ""

    override var private: Boolean = false
}
