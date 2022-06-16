package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchCardItem
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchItem
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchPresenter
import eu.kanade.tachiyomi.ui.browse.migration.AnimeMigrationFlags
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithSource
import eu.kanade.tachiyomi.util.hasCustomCover
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Date

class AnimeSearchPresenter(
    initialQuery: String? = "",
    private val anime: Anime,
) : GlobalAnimeSearchPresenter(initialQuery) {

    private val replacingAnimeRelay = BehaviorRelay.create<Pair<Boolean, Anime?>>()

    private val coverCache: AnimeCoverCache by injectLazy()
    private val enhancedServices by lazy { Injekt.get<TrackManager>().services.filterIsInstance<EnhancedTrackService>() }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        replacingAnimeRelay.subscribeLatestCache(
            { controller, (isReplacingAnime, newAnime) ->
                (controller as? AnimeSearchController)?.renderIsReplacingAnime(isReplacingAnime, newAnime)
            },
        )
    }

    override fun getEnabledSources(): List<AnimeCatalogueSource> {
        // Put the source of the selected anime at the top
        return super.getEnabledSources()
            .sortedByDescending { it.id == anime.source }
    }

    override fun createCatalogueSearchItem(source: AnimeCatalogueSource, results: List<GlobalAnimeSearchCardItem>?): GlobalAnimeSearchItem {
        // Set the catalogue search item as highlighted if the source matches that of the selected anime
        return GlobalAnimeSearchItem(source, results, source.id == anime.source)
    }

    override fun networkToLocalAnime(sAnime: SAnime, sourceId: Long): Anime {
        val localAnime = super.networkToLocalAnime(sAnime, sourceId)
        // For migration, displayed title should always match source rather than local DB
        localAnime.title = sAnime.title
        return localAnime
    }

    fun migrateAnime(prevAnime: Anime, anime: Anime, replace: Boolean) {
        val source = sourceManager.get(anime.source) ?: return
        val prevSource = sourceManager.get(prevAnime.source)

        replacingAnimeRelay.call(Pair(true, null))

        presenterScope.launchIO {
            try {
                val episodes = source.getEpisodeList(anime.toAnimeInfo())
                    .map { it.toSEpisode() }

                migrateAnimeInternal(prevSource, source, episodes, prevAnime, anime, replace)
            } catch (e: Throwable) {
                withUIContext { view?.applicationContext?.toast(e.message) }
            }

            presenterScope.launchUI { replacingAnimeRelay.call(Pair(false, anime)) }
        }
    }

    private suspend fun migrateAnimeInternal(
        prevSource: AnimeSource?,
        source: AnimeSource,
        sourceEpisodes: List<SEpisode>,
        prevAnime: Anime,
        anime: Anime,
        replace: Boolean,
    ) {
        val flags = preferences.migrateFlags().get()
        val migrateEpisodes =
            AnimeMigrationFlags.hasEpisodes(
                flags,
            )
        val migrateCategories =
            AnimeMigrationFlags.hasCategories(
                flags,
            )
        val migrateTracks =
            AnimeMigrationFlags.hasTracks(
                flags,
            )
        val migrateCustomCover =
            AnimeMigrationFlags.hasCustomCover(
                flags,
            )

        db.inTransaction {
            // Update episodes read
            if (migrateEpisodes) {
                try {
                    syncEpisodesWithSource(sourceEpisodes, anime, source)
                } catch (e: Exception) {
                    // Worst case, episodes won't be synced
                }

                val prevAnimeEpisodes = db.getEpisodes(prevAnime).executeAsBlocking()
                val maxEpisodeRead = prevAnimeEpisodes
                    .filter { it.seen }
                    .maxOfOrNull { it.episode_number } ?: 0f
                val dbEpisodes = db.getEpisodes(anime).executeAsBlocking()
                for (episode in dbEpisodes) {
                    if (episode.isRecognizedNumber) {
                        val prevEpisode = prevAnimeEpisodes
                            .find { it.isRecognizedNumber && it.episode_number == episode.episode_number }
                        if (prevEpisode != null) {
                            episode.date_fetch = prevEpisode.date_fetch
                            episode.bookmark = prevEpisode.bookmark
                        }
                        if (episode.episode_number <= maxEpisodeRead) {
                            episode.seen = true
                        }
                    }
                    db.insertEpisodes(dbEpisodes).executeAsBlocking()
                }
            }

            // Update categories
            if (migrateCategories) {
                val categories = db.getCategoriesForAnime(prevAnime).executeAsBlocking()
                val animeCategories = categories.map { AnimeCategory.create(anime, it) }
                db.setAnimeCategories(animeCategories, listOf(anime))
            }

            // Update track
            if (migrateTracks) {
                val tracksToUpdate = db.getTracks(prevAnime.id).executeAsBlocking().mapNotNull { track ->
                    track.id = null
                    track.anime_id = anime.id!!

                    val service = enhancedServices
                        .firstOrNull { it.isTrackFrom(track, prevAnime, prevSource) }
                    if (service != null) service.migrateTrack(track, anime, source)
                    else track
                }
                db.insertTracks(tracksToUpdate).executeAsBlocking()
            }

            // Update favorite status
            if (replace) {
                prevAnime.favorite = false
                db.updateAnimeFavorite(prevAnime).executeAsBlocking()
            }
            anime.favorite = true

            // Update reading preferences
            anime.episode_flags = prevAnime.episode_flags
            anime.viewer_flags = prevAnime.viewer_flags

            // Update date added
            if (replace) {
                anime.date_added = prevAnime.date_added
                prevAnime.date_added = 0
            } else {
                anime.date_added = Date().time
            }

            // Update custom cover
            if (migrateCustomCover) {
                coverCache.setCustomCoverToCache(anime, coverCache.getCustomCoverFile(prevAnime.id).inputStream())
            }

            // SearchPresenter#networkToLocalManga may have updated the manga title,
            // so ensure db gets updated title too
            db.insertAnime(anime).executeAsBlocking()
        }
    }
}
