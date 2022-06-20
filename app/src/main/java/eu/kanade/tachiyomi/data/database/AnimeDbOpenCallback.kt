package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.squareup.sqldelight.android.AndroidSqliteDriver
import eu.kanade.tachiyomi.mi.AnimeDatabase
import logcat.logcat

class AnimeDbOpenCallback : SupportSQLiteOpenHelper.Callback(AnimeDatabase.Schema.version) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_FILENAME = "tachiyomi.animedb"
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        logcat { "Creating new database" }
        AnimeDatabase.Schema.create(AndroidSqliteDriver(database = db, cacheSize = 1))
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < newVersion) {
            logcat { "Upgrading database from $oldVersion to $newVersion" }
            AnimeDatabase.Schema.migrate(
                driver = AndroidSqliteDriver(database = db, cacheSize = 1),
                oldVersion = oldVersion,
                newVersion = newVersion,
            )
        }
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}
