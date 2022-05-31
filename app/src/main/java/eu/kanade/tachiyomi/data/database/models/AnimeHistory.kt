package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

/**
 * Object containing the history statistics of a chapter
 */
interface AnimeHistory : Serializable {

    /**
     * Id of history object.
     */
    var id: Long?

    /**
     * Chapter id of history object.
     */
    var episode_id: Long

    /**
     * Last time episode was read in time long format
     */
    var last_seen: Long

    companion object {

        /**
         * History constructor
         *
         * @param chapter chapter object
         * @return history object
         */
        fun create(episode: Episode): AnimeHistory = AnimeHistoryImpl().apply {
            this.episode_id = episode.id!!
        }
    }
}
