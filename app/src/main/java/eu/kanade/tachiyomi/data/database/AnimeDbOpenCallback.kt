package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.tables.AnimeCategoryTable
import eu.kanade.tachiyomi.data.database.tables.AnimeHistoryTable
import eu.kanade.tachiyomi.data.database.tables.AnimeTable
import eu.kanade.tachiyomi.data.database.tables.AnimeTrackTable
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.EpisodeTable

class AnimeDbOpenCallback : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.animedb"

        /**
         * Version of the database.
         */
        const val DATABASE_VERSION = 114
    }

    override fun onCreate(db: SupportSQLiteDatabase) = with(db) {
        execSQL(AnimeTable.createTableQuery)
        execSQL(EpisodeTable.createTableQuery)
        execSQL(AnimeTrackTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        execSQL(AnimeCategoryTable.createTableQuery)
        execSQL(AnimeHistoryTable.createTableQuery)

        // DB indexes
        execSQL(AnimeTable.createUrlIndexQuery)
        execSQL(AnimeTable.createLibraryIndexQuery)
        execSQL(EpisodeTable.createAnimeIdIndexQuery)
        execSQL(EpisodeTable.createUnseenEpisodesIndexQuery)
        execSQL(AnimeHistoryTable.createEpisodeIdIndexQuery)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(EpisodeTable.sourceOrderUpdateQuery)

            // Fix kissmanga covers after supporting cloudflare
            db.execSQL(
                """UPDATE animes SET thumbnail_url =
                    REPLACE(thumbnail_url, '93.174.95.110', 'kissmanga.com') WHERE source = 4"""
            )
        }
        if (oldVersion < 3) {
            // Initialize history tables
            db.execSQL(AnimeHistoryTable.createTableQuery)
            db.execSQL(AnimeHistoryTable.createEpisodeIdIndexQuery)
        }
        if (oldVersion < 4) {
            db.execSQL(EpisodeTable.bookmarkUpdateQuery)
        }
        if (oldVersion < 5) {
            db.execSQL(EpisodeTable.addScanlator)
        }
        if (oldVersion < 6) {
            db.execSQL(AnimeTrackTable.addTrackingUrl)
        }
        if (oldVersion < 7) {
            db.execSQL(AnimeTrackTable.addLibraryId)
        }
        if (oldVersion < 8) {
            db.execSQL("DROP INDEX IF EXISTS animes_favorite_index")
            db.execSQL(AnimeTable.createLibraryIndexQuery)
            db.execSQL(EpisodeTable.createUnseenEpisodesIndexQuery)
        }
        if (oldVersion < 9) {
            db.execSQL(AnimeTrackTable.addStartDate)
            db.execSQL(AnimeTrackTable.addFinishDate)
        }
        if (oldVersion < 10) {
            db.execSQL(AnimeTable.addCoverLastModified)
        }
        if (oldVersion < 11) {
            db.execSQL(AnimeTable.addDateAdded)
            db.execSQL(AnimeTable.backfillDateAdded)
        }
        if (oldVersion < 112) {
            db.execSQL(AnimeTable.addNextUpdateCol)
        }
        if (oldVersion < 113) {
            db.execSQL(AnimeTrackTable.renameTableToTemp)
            db.execSQL(AnimeTrackTable.createTableQuery)
            db.execSQL(AnimeTrackTable.insertFromTempTable)
            db.execSQL(AnimeTrackTable.dropTempTable)
        }
        if (oldVersion < 14) {
            db.execSQL(EpisodeTable.fixDateUploadIfNeeded)
        }
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}
