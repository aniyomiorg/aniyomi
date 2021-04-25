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

    /**
     * query to create animehistory table
     */
    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_EPISODE_ID INTEGER NOT NULL UNIQUE,
            $COL_LAST_SEEN LONG,
            $COL_TIME_SEEN LONG,
            FOREIGN KEY($COL_EPISODE_ID) REFERENCES ${EpisodeTable.TABLE} (${EpisodeTable.COL_ID})
            ON DELETE CASCADE
            )"""

    /**
     * query to index animehistory episode id
     */
    val createEpisodeIdIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_EPISODE_ID}_index ON $TABLE($COL_EPISODE_ID)"
}
