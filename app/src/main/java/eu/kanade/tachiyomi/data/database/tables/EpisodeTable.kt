package eu.kanade.tachiyomi.data.database.tables

object EpisodeTable {

    const val TABLE = "episodes"

    const val COL_ID = "_id"

    const val COL_ANIME_ID = "anime_id"

    const val COL_URL = "url"

    const val COL_NAME = "name"

    const val COL_SEEN = "seen"

    const val COL_SCANLATOR = "scanlator"

    const val COL_BOOKMARK = "bookmark"

    const val COL_DATE_FETCH = "date_fetch"

    const val COL_DATE_UPLOAD = "date_upload"

    const val COL_LAST_SECOND_SEEN = "last_second_seen"

    const val COL_TOTAL_SECONDS = "total_seconds"

    const val COL_EPISODE_NUMBER = "episode_number"

    const val COL_SOURCE_ORDER = "source_order"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_ANIME_ID INTEGER NOT NULL,
            $COL_URL TEXT NOT NULL,
            $COL_NAME TEXT NOT NULL,
            $COL_SCANLATOR TEXT,
            $COL_SEEN BOOLEAN NOT NULL,
            $COL_BOOKMARK BOOLEAN NOT NULL,
            $COL_LAST_SECOND_SEEN LONG NOT NULL,
            $COL_TOTAL_SECONDS LONG NOT NULL,
            $COL_EPISODE_NUMBER FLOAT NOT NULL,
            $COL_SOURCE_ORDER INTEGER NOT NULL,
            $COL_DATE_FETCH LONG NOT NULL,
            $COL_DATE_UPLOAD LONG NOT NULL,
            FOREIGN KEY($COL_ANIME_ID) REFERENCES ${AnimeTable.TABLE} (${AnimeTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val createAnimeIdIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_ANIME_ID}_index ON $TABLE($COL_ANIME_ID)"

    val createUnseenEpisodesIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_unseen_by_anime_index ON $TABLE($COL_ANIME_ID, $COL_SEEN) " +
            "WHERE $COL_SEEN = 0"

    val sourceOrderUpdateQuery: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_SOURCE_ORDER INTEGER DEFAULT 0"

    val bookmarkUpdateQuery: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_BOOKMARK BOOLEAN DEFAULT FALSE"

    val addScanlator: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_SCANLATOR TEXT DEFAULT NULL"

    val fixDateUploadIfNeeded: String
        get() = "UPDATE $TABLE SET $COL_DATE_UPLOAD = $COL_DATE_FETCH WHERE $COL_DATE_UPLOAD = 0"
}
