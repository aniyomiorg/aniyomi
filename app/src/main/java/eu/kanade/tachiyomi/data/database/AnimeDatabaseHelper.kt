package eu.kanade.tachiyomi.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.mappers.AnimeCategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.AnimeHistoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.AnimeTrackTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.AnimeTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.CategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.EpisodeTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.HistoryTypeMapping
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.queries.AnimeCategoryQueries
import eu.kanade.tachiyomi.data.database.queries.AnimeHistoryQueries
import eu.kanade.tachiyomi.data.database.queries.AnimeQueries
import eu.kanade.tachiyomi.data.database.queries.AnimeTrackQueries
import eu.kanade.tachiyomi.data.database.queries.CategoryQueries
import eu.kanade.tachiyomi.data.database.queries.EpisodeQueries
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

/**
 * This class provides operations to manage the database through its interfaces.
 */
open class AnimeDatabaseHelper(context: Context) :
    AnimeQueries, EpisodeQueries, AnimeTrackQueries, CategoryQueries, AnimeCategoryQueries, AnimeHistoryQueries {

    private val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(AnimeDbOpenCallback.DATABASE_NAME)
        .callback(AnimeDbOpenCallback())
        .build()

    override val db = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(RequerySQLiteOpenHelperFactory().create(configuration))
        .addTypeMapping(Anime::class.java, AnimeTypeMapping())
        .addTypeMapping(Episode::class.java, EpisodeTypeMapping())
        .addTypeMapping(AnimeTrack::class.java, AnimeTrackTypeMapping())
        .addTypeMapping(Category::class.java, CategoryTypeMapping())
        .addTypeMapping(AnimeCategory::class.java, AnimeCategoryTypeMapping())
        .addTypeMapping(History::class.java, HistoryTypeMapping())
        .addTypeMapping(AnimeHistory::class.java, AnimeHistoryTypeMapping())
        .build()

    inline fun inTransaction(block: () -> Unit) = db.inTransaction(block)
}
