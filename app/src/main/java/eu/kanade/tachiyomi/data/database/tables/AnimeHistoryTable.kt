package eu.kanade.tachiyomi.data.database.tables

object AnimeHistoryTable {

    /**
     * Table name
     */
    const val TABLE = "animehistory"

    /**
     * Id column name
     */
    const val COL_ID = "${TABLE}_id"

    /**
     * Episode id column name
     */
    const val COL_EPISODE_ID = "${TABLE}_episode_id"

    /**
     * Last seen column name
     */
    const val COL_LAST_SEEN = "${TABLE}_last_seen"

    /**
     * Time seen column name
     */
    const val COL_TIME_SEEN = "${TABLE}_time_seen"
}
