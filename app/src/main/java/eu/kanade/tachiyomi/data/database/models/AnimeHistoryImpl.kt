package eu.kanade.tachiyomi.data.database.models

/**
 * Object containing the history statistics of a chapter
 */
class AnimeHistoryImpl : AnimeHistory {

    /**
     * Id of history object.
     */
    override var id: Long? = null

    /**
     * Chapter id of history object.
     */
    override var episode_id: Long = 0

    /**
     * Last time chapter was read in time long format
     */
    override var last_seen: Long = 0

    /**
     * Total time chapter was read - todo not yet implemented
     */
    override var time_seen: Long = 0
}
