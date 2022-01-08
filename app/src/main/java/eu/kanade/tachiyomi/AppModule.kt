package eu.kanade.tachiyomi

import android.app.Application
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingStore
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { PreferencesHelper(app) }

        addSingletonFactory { DatabaseHelper(app) }

        addSingletonFactory { AnimeDatabaseHelper(app) }

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

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<PreferencesHelper>()

            get<NetworkHelper>()

            get<SourceManager>()
            get<AnimeSourceManager>()

            get<DatabaseHelper>()
            get<AnimeDatabaseHelper>()

            get<DownloadManager>()
            get<AnimeDownloadManager>()
        }
    }
}
