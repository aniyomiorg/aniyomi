package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.mappers.AnimeCategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.AnimeTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.CategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.EpisodeTypeMapping
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.queries.AnimeCategoryQueries
import eu.kanade.tachiyomi.data.database.queries.AnimeQueries
import eu.kanade.tachiyomi.data.database.queries.CategoryQueries
import eu.kanade.tachiyomi.data.database.queries.EpisodeQueries

/**
 * This class provides operations to manage the database through its interfaces.
 */
class AnimeDatabaseHelper(
    openHelper: SupportSQLiteOpenHelper,
) :
    AnimeQueries, EpisodeQueries, CategoryQueries, AnimeCategoryQueries {

    override val db = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(openHelper)
        .addTypeMapping(Anime::class.java, AnimeTypeMapping())
        .addTypeMapping(Episode::class.java, EpisodeTypeMapping())
        .addTypeMapping(Category::class.java, CategoryTypeMapping())
        .addTypeMapping(AnimeCategory::class.java, AnimeCategoryTypeMapping())
        .build()
}
