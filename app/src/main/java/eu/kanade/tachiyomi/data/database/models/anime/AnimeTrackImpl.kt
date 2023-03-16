package eu.kanade.tachiyomi.data.database.models.anime

class AnimeTrackImpl : AnimeTrack {

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

    override var tracking_url: String = ""
}
