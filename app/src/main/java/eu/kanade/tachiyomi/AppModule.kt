package eu.kanade.tachiyomi

import android.app.Application
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import data.History
import data.Mangas
import dataanime.Animehistory
import dataanime.Animes
import eu.kanade.data.AndroidAnimeDatabaseHandler
import eu.kanade.data.AndroidDatabaseHandler
import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.DatabaseHandler
import eu.kanade.data.dateAdapter
import eu.kanade.data.listOfStringsAdapter
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.AnimeDbOpenCallback
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.DbOpenCallback
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingStore
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.mi.AnimeDatabase
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        // This is used to allow incremental migration from Storio
        val openHelperMangaConfig = SupportSQLiteOpenHelper.Configuration.builder(app)
            .callback(DbOpenCallback())
            .name(DbOpenCallback.DATABASE_NAME)
            .noBackupDirectory(false)
            .build()

        val openHelperManga = if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Support database inspector in Android Studio
            FrameworkSQLiteOpenHelperFactory().create(openHelperMangaConfig)
        } else {
            RequerySQLiteOpenHelperFactory().create(openHelperMangaConfig)
        }

        val openHelperAnimeConfig = SupportSQLiteOpenHelper.Configuration.builder(app)
            .callback(AnimeDbOpenCallback())
            .name(AnimeDbOpenCallback.DATABASE_NAME)
            .noBackupDirectory(false)
            .build()

        val openHelperAnime = if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Support database inspector in Android Studio
            FrameworkSQLiteOpenHelperFactory().create(openHelperAnimeConfig)
        } else {
            RequerySQLiteOpenHelperFactory().create(openHelperAnimeConfig)
        }

        val sqlDriverManga = AndroidSqliteDriver(openHelper = openHelperManga)

        val sqlDriverAnime = AndroidSqliteDriver(openHelper = openHelperAnime)

        addSingletonFactory {
            Database(
                driver = sqlDriverManga,
                historyAdapter = History.Adapter(
                    last_readAdapter = dateAdapter,
                ),
                mangasAdapter = Mangas.Adapter(
                    genreAdapter = listOfStringsAdapter,
                ),
            )
        }

        addSingletonFactory {
            AnimeDatabase(
                driver = sqlDriverAnime,
                animehistoryAdapter = Animehistory.Adapter(
                    last_seenAdapter = dateAdapter,
                ),
                animesAdapter = Animes.Adapter(
                    genreAdapter = listOfStringsAdapter,
                ),
            )
        }

        addSingletonFactory<DatabaseHandler> { AndroidDatabaseHandler(get(), sqlDriverManga) }

        addSingletonFactory<AnimeDatabaseHandler> { AndroidAnimeDatabaseHandler(get(), sqlDriverAnime) }

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { PreferencesHelper(app) }

        addSingletonFactory { DatabaseHelper(openHelperManga) }

        addSingletonFactory { AnimeDatabaseHelper(openHelperAnime) }

        addSingletonFactory { ChapterCache(app) }

        addSingletonFactory { EpisodeCache(app) }

        addSingletonFactory { CoverCache(app) }

        addSingletonFactory { AnimeCoverCache(app) }

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory { SourceManager(app).also { get<ExtensionManager>().init(it) } }

        addSingletonFactory { AnimeSourceManager(app).also { get<AnimeExtensionManager>().init(it) } }

        addSingletonFactory { ExtensionManager(app) }

        addSingletonFactory { AnimeExtensionManager(app) }

        addSingletonFactory { DownloadManager(app) }

        addSingletonFactory { AnimeDownloadManager(app) }

        addSingletonFactory { TrackManager(app) }

        addSingletonFactory { DelayedTrackingStore(app) }

        addSingletonFactory { ImageSaver(app) }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<PreferencesHelper>()

            get<NetworkHelper>()

            get<SourceManager>()
            get<AnimeSourceManager>()

            get<Database>()
            get<AnimeDatabase>()

            get<DatabaseHelper>()
            get<AnimeDatabaseHelper>()

            get<DownloadManager>()
            get<AnimeDownloadManager>()
        }
    }
}
