package eu.kanade.tachiyomi.ui.browse.migration.search

import android.os.Bundle
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animetrack.interactor.GetAnimeTracks
import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.episode.interactor.GetEpisodeByAnimeId
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.model.toEpisodeUpdate
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.track.EnhancedTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchCardItem
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchItem
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchPresenter
import eu.kanade.tachiyomi.ui.browse.migration.AnimeMigrationFlags
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.Date

class AnimeSearchPresenter(
    initialQuery: String? = "",
    private val anime: Anime,
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    preferenceStore: PreferenceStore = Injekt.get(),
) : GlobalAnimeSearchPresenter(initialQuery) {

    private val replacingAnimeRelay = BehaviorRelay.create<Pair<Boolean, Anime?>>()
    private val coverCache: AnimeCoverCache by injectLazy()
    private val enhancedServices by lazy { Injekt.get<TrackManager>().services.filterIsInstance<EnhancedTrackService>() }

    val migrateFlags: Preference<Int> by lazy {
        preferenceStore.getInt("migrate_flags", Int.MAX_VALUE)
    }

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

    override suspend fun networkToLocalAnime(sAnime: SAnime, sourceId: Long): Anime {
        val localAnime = super.networkToLocalAnime(sAnime, sourceId)
        // For migration, displayed title should always match source rather than local DB
        return localAnime.copy(title = sAnime.title)
    }

    fun migrateAnime(prevAnime: Anime, anime: Anime, replace: Boolean) {
        val source = sourceManager.get(anime.source) ?: return
        val prevSource = sourceManager.get(prevAnime.source)

        replacingAnimeRelay.call(Pair(true, null))

        presenterScope.launchIO {
            try {
                val episodes = source.getEpisodeList(anime.toSAnime())

                migrateAnimeInternal(prevSource, source, episodes, prevAnime, anime, replace)
            } catch (e: Throwable) {
                withUIContext { view?.applicationContext?.toast(e.message) }
            }

            withUIContext { replacingAnimeRelay.call(Pair(false, anime)) }
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
        val flags = migrateFlags.get()

        val migrateEpisodes = AnimeMigrationFlags.hasEpisodes(flags)
        val migrateCategories = AnimeMigrationFlags.hasCategories(flags)
        val migrateTracks = AnimeMigrationFlags.hasTracks(flags)
        val migrateCustomCover = AnimeMigrationFlags.hasCustomCover(flags)

        try {
            syncEpisodesWithSource.await(sourceEpisodes, anime, source)
        } catch (e: Exception) {
            // Worst case, episodes won't be synced
        }

        // Update episodes seen, bookmark and dateFetch
        if (migrateEpisodes) {
            val prevAnimeEpisodes = getEpisodeByAnimeId.await(prevAnime.id)
            val animeEpisodes = getEpisodeByAnimeId.await(anime.id)

            val maxEpisodeSeen = prevAnimeEpisodes
                .filter { it.seen }
                .maxOfOrNull { it.episodeNumber }

            val updatedAnimeEpisodes = animeEpisodes.map { animeEpisode ->
                var updatedEpisode = animeEpisode
                if (updatedEpisode.isRecognizedNumber) {
                    val prevEpisode = prevAnimeEpisodes
                        .find { it.isRecognizedNumber && it.episodeNumber == updatedEpisode.episodeNumber }

                    if (prevEpisode != null) {
                        updatedEpisode = updatedEpisode.copy(
                            dateFetch = prevEpisode.dateFetch,
                            bookmark = prevEpisode.bookmark,
                        )
                    }

                    if (maxEpisodeSeen != null && updatedEpisode.episodeNumber <= maxEpisodeSeen) {
                        updatedEpisode = updatedEpisode.copy(seen = true)
                    }
                }

                updatedEpisode
            }

            val episodeUpdates = updatedAnimeEpisodes.map { it.toEpisodeUpdate() }
            updateEpisode.awaitAll(episodeUpdates)
        }

        // Update categories
        if (migrateCategories) {
            val categoryIds = getCategories.await(prevAnime.id).map { it.id }
            setAnimeCategories.await(anime.id, categoryIds)
        }

        // Update track
        if (migrateTracks) {
            val tracks = getTracks.await(prevAnime.id).mapNotNull { track ->
                val updatedTrack = track.copy(animeId = anime.id)

                val service = enhancedServices
                    .firstOrNull { it.isTrackFrom(updatedTrack, prevAnime, prevSource) }

                if (service != null) {
                    service.migrateTrack(updatedTrack, anime, source)
                } else {
                    updatedTrack
                }
            }
            insertTrack.awaitAll(tracks)
        }

        if (replace) {
            updateAnime.await(AnimeUpdate(prevAnime.id, favorite = false, dateAdded = 0))
        }

        // Update custom cover (recheck if custom cover exists)
        if (migrateCustomCover && prevAnime.hasCustomCover()) {
            @Suppress("BlockingMethodInNonBlockingContext")
            coverCache.setCustomCoverToCache(anime.toDbAnime(), coverCache.getCustomCoverFile(prevAnime.id).inputStream())
        }

        updateAnime.await(
            AnimeUpdate(
                id = anime.id,
                favorite = true,
                episodeFlags = prevAnime.episodeFlags,
                viewerFlags = prevAnime.viewerFlags,
                dateAdded = if (replace) prevAnime.dateAdded else Date().time,
            ),
        )
    }
}
