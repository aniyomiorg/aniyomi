package eu.kanade.tachiyomi.ui.stats.anime

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.util.fastCountNot
import eu.kanade.core.util.fastDistinctBy
import eu.kanade.core.util.fastFilter
import eu.kanade.core.util.fastFilterNot
import eu.kanade.core.util.fastMapNotNull
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.track.AnimeTrackService
import eu.kanade.tachiyomi.data.track.TrackManager
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.items.episode.interactor.GetEpisodeByAnimeId
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_HAS_UNVIEWED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ENTRY_NON_VIEWED
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.anime.model.AnimeTrack
import tachiyomi.source.local.entries.anime.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeStatsScreenModel(
    private val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val getAnimelibAnime: GetLibraryAnime = Injekt.get(),
    private val getEpisodeByAnimeId: GetEpisodeByAnimeId = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val preferences: LibraryPreferences = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    private val loggedServices by lazy { trackManager.services.fastFilter { it.isLogged && it is AnimeTrackService } }

    init {
        coroutineScope.launchIO {
            val animelibAnime = getAnimelibAnime.await()

            val distinctLibraryAnime = animelibAnime.fastDistinctBy { it.id }

            val animeTrackMap = getAnimeTrackMap(distinctLibraryAnime)
            val scoredAnimeTrackerMap = getScoredAnimeTrackMap(animeTrackMap)

            val meanScore = getTrackMeanScore(scoredAnimeTrackerMap)

            val overviewStatData = StatsData.AnimeOverview(
                libraryAnimeCount = distinctLibraryAnime.size,
                completedAnimeCount = distinctLibraryAnime.count {
                    it.anime.status.toInt() == SAnime.COMPLETED && it.unseenCount == 0L
                },
                totalSeenDuration = getWatchTime(distinctLibraryAnime),
            )

            val titlesStatData = StatsData.AnimeTitles(
                globalUpdateItemCount = getGlobalUpdateItemCount(animelibAnime),
                startedAnimeCount = distinctLibraryAnime.count { it.hasStarted },
                localAnimeCount = distinctLibraryAnime.count { it.anime.isLocal() },
            )

            val chaptersStatData = StatsData.Episodes(
                totalEpisodeCount = distinctLibraryAnime.sumOf { it.totalEpisodes }.toInt(),
                readEpisodeCount = distinctLibraryAnime.sumOf { it.seenCount }.toInt(),
                downloadCount = downloadManager.getDownloadCount(),
            )

            val trackersStatData = StatsData.Trackers(
                trackedTitleCount = animeTrackMap.count { it.value.isNotEmpty() },
                meanScore = meanScore,
                trackerCount = loggedServices.size,
            )

            mutableState.update {
                StatsScreenState.SuccessAnime(
                    overview = overviewStatData,
                    titles = titlesStatData,
                    episodes = chaptersStatData,
                    trackers = trackersStatData,
                )
            }
        }
    }

    private fun getGlobalUpdateItemCount(libraryAnime: List<LibraryAnime>): Int {
        val includedCategories = preferences.animeLibraryUpdateCategories().get().map { it.toLong() }
        val includedAnime = if (includedCategories.isNotEmpty()) {
            libraryAnime.filter { it.category in includedCategories }
        } else {
            libraryAnime
        }

        val excludedCategories = preferences.animeLibraryUpdateCategoriesExclude().get().map { it.toLong() }
        val excludedMangaIds = if (excludedCategories.isNotEmpty()) {
            libraryAnime.fastMapNotNull { anime ->
                anime.id.takeIf { anime.category in excludedCategories }
            }
        } else {
            emptyList()
        }

        val updateRestrictions = preferences.libraryUpdateItemRestriction().get()
        return includedAnime
            .fastFilterNot { it.anime.id in excludedMangaIds }
            .fastDistinctBy { it.anime.id }
            .fastCountNot {
                (ENTRY_NON_COMPLETED in updateRestrictions && it.anime.status.toInt() == SAnime.COMPLETED) ||
                    (ENTRY_HAS_UNVIEWED in updateRestrictions && it.unseenCount != 0L) ||
                    (ENTRY_NON_VIEWED in updateRestrictions && it.totalEpisodes > 0 && !it.hasStarted)
            }
    }

    private suspend fun getAnimeTrackMap(libraryAnime: List<LibraryAnime>): Map<Long, List<AnimeTrack>> {
        val loggedServicesIds = loggedServices.map { it.id }.toHashSet()
        return libraryAnime.associate { anime ->
            val tracks = getTracks.await(anime.id)
                .fastFilter { it.syncId in loggedServicesIds }

            anime.id to tracks
        }
    }

    private suspend fun getWatchTime(libraryAnimeList: List<LibraryAnime>): Long {
        var watchTime = 0L
        libraryAnimeList.forEach { libraryAnime ->
            getEpisodeByAnimeId.await(libraryAnime.anime.id).forEach { episode ->
                watchTime += if (episode.seen) {
                    episode.totalSeconds
                } else {
                    episode.lastSecondSeen
                }
            }
        }

        return watchTime
    }

    private fun getScoredAnimeTrackMap(animeTrackMap: Map<Long, List<AnimeTrack>>): Map<Long, List<AnimeTrack>> {
        return animeTrackMap.mapNotNull { (animeId, tracks) ->
            val trackList = tracks.mapNotNull { track ->
                track.takeIf { it.score > 0.0 }
            }
            if (trackList.isEmpty()) return@mapNotNull null
            animeId to trackList
        }.toMap()
    }

    private fun getTrackMeanScore(scoredAnimeTrackMap: Map<Long, List<AnimeTrack>>): Double {
        return scoredAnimeTrackMap
            .map { (_, tracks) ->
                tracks.map {
                    get10PointScore(it)
                }.average()
            }
            .fastFilter { !it.isNaN() }
            .average()
    }

    private fun get10PointScore(track: AnimeTrack): Float {
        val service = trackManager.getService(track.syncId)!!
        return service.animeService.get10PointScore(track)
    }
}
