package eu.kanade.tachiyomi.data.database.tables

object AnimeTrackTable {

    const val TABLE = "anime_sync"

    const val COL_ID = "_id"

    const val COL_ANIME_ID = "anime_id"

    const val COL_SYNC_ID = "sync_id"

    const val COL_MEDIA_ID = "remote_id"

    const val COL_LIBRARY_ID = "library_id"

    const val COL_TITLE = "title"

    const val COL_LAST_EPISODE_SEEN = "last_episode_seen"

    const val COL_STATUS = "status"

    const val COL_SCORE = "score"

    const val COL_TOTAL_EPISODES = "total_episodes"

    const val COL_TRACKING_URL = "remote_url"

    const val COL_START_DATE = "start_date"

    const val COL_FINISH_DATE = "finish_date"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_ANIME_ID INTEGER NOT NULL,
            $COL_SYNC_ID INTEGER NOT NULL,
            $COL_MEDIA_ID INTEGER NOT NULL,
            $COL_LIBRARY_ID INTEGER,
            $COL_TITLE TEXT NOT NULL,
            $COL_LAST_EPISODE_SEEN INTEGER NOT NULL,
            $COL_TOTAL_EPISODES INTEGER NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_SCORE FLOAT NOT NULL,
            $COL_TRACKING_URL TEXT NOT NULL,
            $COL_START_DATE LONG NOT NULL,
            $COL_FINISH_DATE LONG NOT NULL,
            UNIQUE ($COL_ANIME_ID, $COL_SYNC_ID) ON CONFLICT REPLACE,
            FOREIGN KEY($COL_ANIME_ID) REFERENCES ${AnimeTable.TABLE} (${AnimeTable.COL_ID})
            ON DELETE CASCADE
            )"""

    val addTrackingUrl: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_TRACKING_URL TEXT DEFAULT ''"

    val addLibraryId: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_LIBRARY_ID INTEGER NULL"

    val addStartDate: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_START_DATE LONG NOT NULL DEFAULT 0"

    val addFinishDate: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_FINISH_DATE LONG NOT NULL DEFAULT 0"
}
