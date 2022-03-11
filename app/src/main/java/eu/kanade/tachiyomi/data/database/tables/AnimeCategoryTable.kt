package eu.kanade.tachiyomi.data.database.tables

object AnimeCategoryTable {

    const val TABLE = "anime_categories"

    const val COL_ID = "_id"

    const val COL_ANIME_ID = "anime_id"

    const val COL_CATEGORY_ID = "category_id"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_ANIME_ID INTEGER NOT NULL,
            $COL_CATEGORY_ID INTEGER NOT NULL,
            FOREIGN KEY($COL_CATEGORY_ID) REFERENCES ${CategoryTable.TABLE} (${CategoryTable.COL_ID})
            ON DELETE CASCADE,
            FOREIGN KEY($COL_ANIME_ID) REFERENCES ${AnimeTable.TABLE} (${AnimeTable.COL_ID})
            ON DELETE CASCADE
            )"""
}
