package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.model.toSEpisode
import eu.kanade.tachiyomi.ui.browse.migration.AnimeMigrationFlags
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalAnimeSearchCardItem
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalAnimeSearchItem
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalAnimeSearchPresenter
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithSource
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.toast
import java.util.Date

class AnimeSearchPresenter(
    initialQuery: String? = "",
    private val anime: Anime
) : GlobalAnimeSearchPresenter(initialQuery) {

    private val replacingAnimeRelay = BehaviorRelay.create<Boolean>()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        replacingAnimeRelay.subscribeLatestCache({ controller, isReplacingAnime -> (controller as? AnimeSearchController)?.renderIsReplacingAnime(isReplacingAnime) })
    }

    override fun getEnabledSources(): List<CatalogueSource> {
        // Put the source of the selected anime at the top
        return super.getEnabledSources()
            .sortedByDescending { it.id == anime.source }
    }

    override fun createCatalogueSearchItem(source: CatalogueSource, results: List<GlobalAnimeSearchCardItem>?): GlobalAnimeSearchItem {
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

        replacingAnimeRelay.call(true)

        presenterScope.launchIO {
            try {
                val episodes = source.getEpisodeList(anime.toAnimeInfo())
                    .map { it.toSEpisode() }

                migrateAnimeInternal(source, episodes, prevAnime, anime, replace)
            } catch (e: Throwable) {
                withUIContext { view?.applicationContext?.toast(e.message) }
            }

            presenterScope.launchUI { replacingAnimeRelay.call(false) }
        }
    }

    private fun migrateAnimeInternal(
        source: Source,
        sourceEpisodes: List<SEpisode>,
        prevAnime: Anime,
        anime: Anime,
        replace: Boolean
    ) {
        val flags = preferences.migrateFlags().get()
        val migrateEpisodes =
            AnimeMigrationFlags.hasEpisodes(
                flags
            )
        val migrateCategories =
            AnimeMigrationFlags.hasCategories(
                flags
            )
        val migrateTracks =
            AnimeMigrationFlags.hasTracks(
                flags
            )

        db.inTransaction {
            // Update episodes read
            if (migrateEpisodes) {
                try {
                    syncEpisodesWithSource(db, sourceEpisodes, anime, source)
                } catch (e: Exception) {
                    // Worst case, episodes won't be synced
                }

                val prevAnimeEpisodes = db.getEpisodes(prevAnime).executeAsBlocking()
                val maxEpisodeRead = prevAnimeEpisodes
                    .filter { it.read }
                    .maxOfOrNull { it.episode_number }
                if (maxEpisodeRead != null) {
                    val dbEpisodes = db.getEpisodes(anime).executeAsBlocking()
                    for (episode in dbEpisodes) {
                        if (episode.isRecognizedNumber) {
                            val prevEpisode = prevAnimeEpisodes
                                .find { it.isRecognizedNumber && it.episode_number == episode.episode_number }
                            if (prevEpisode != null) {
                                episode.date_fetch = prevEpisode.date_fetch
                                episode.bookmark = prevEpisode.bookmark
                            } else if (episode.episode_number <= maxEpisodeRead) {
                                episode.read = true
                            }
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
                val tracks = db.getTracks(prevAnime).executeAsBlocking()
                for (track in tracks) {
                    track.id = null
                    track.anime_id = anime.id!!
                }
                db.insertTracks(tracks).executeAsBlocking()
            }

            // Update favorite status
            if (replace) {
                prevAnime.favorite = false
                db.updateAnimeFavorite(prevAnime).executeAsBlocking()
            }
            anime.favorite = true
            db.updateAnimeFavorite(anime).executeAsBlocking()

            // Update reading preferences
            anime.episode_flags = prevAnime.episode_flags
            db.updateFlags(anime).executeAsBlocking()
            anime.viewer = prevAnime.viewer
            db.updateAnimeViewer(anime).executeAsBlocking()

            // Update date added
            if (replace) {
                anime.date_added = prevAnime.date_added
                prevAnime.date_added = 0
            } else {
                anime.date_added = Date().time
            }

            // SearchPresenter#networkToLocalAnime may have updated the anime title, so ensure db gets updated title
            db.updateAnimeTitle(anime).executeAsBlocking()
        }
    }
}
