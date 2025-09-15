package eu.kanade.domain.items.episode.interactor

import eu.kanade.domain.entries.anime.interactor.UpdateAnime
import eu.kanade.domain.entries.anime.model.toSAnime
import eu.kanade.domain.items.episode.model.copyFromSEpisode
import eu.kanade.domain.items.episode.model.toSEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadProvider
import tachiyomi.data.items.episode.EpisodeSanitizer
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.items.episode.interactor.ShouldUpdateDbEpisode
import tachiyomi.domain.items.episode.interactor.UpdateEpisode
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.model.NoEpisodesException
import tachiyomi.domain.items.episode.model.toEpisodeUpdate
import tachiyomi.domain.items.episode.repository.EpisodeRepository
import tachiyomi.domain.items.episode.service.EpisodeRecognition
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.entries.anime.isLocal
import java.lang.Long.max
import java.time.ZonedDateTime
import java.util.TreeSet

class SyncEpisodesWithSource(
    private val downloadManager: AnimeDownloadManager,
    private val downloadProvider: AnimeDownloadProvider,
    private val episodeRepository: EpisodeRepository,
    private val shouldUpdateDbEpisode: ShouldUpdateDbEpisode,
    private val updateAnime: UpdateAnime,
    private val updateEpisode: UpdateEpisode,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    private val libraryPreferences: LibraryPreferences,
) {

    /**
     * Method to synchronize db episodes with source ones
     *
     * @param rawSourceEpisodes the episodes from the source.
     * @param anime the anime the episodes belong to.
     * @param source the source the anime belongs to.
     * @return Newly added episodes
     */
    suspend fun await(
        rawSourceEpisodes: List<SEpisode>,
        anime: Anime,
        source: AnimeSource,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): List<Episode> {
        if (rawSourceEpisodes.isEmpty() && !source.isLocal()) {
            throw NoEpisodesException()
        }

        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()

        val sourceEpisodes = rawSourceEpisodes
            .distinctBy { it.url }
            .mapIndexed { i, sEpisode ->
                Episode.create()
                    .copyFromSEpisode(sEpisode)
                    .copy(name = with(EpisodeSanitizer) { sEpisode.name.sanitize(anime.title) })
                    .copy(animeId = anime.id, sourceOrder = i.toLong())
            }

        val dbEpisodes = getEpisodesByAnimeId.await(anime.id)

        val newEpisodes = mutableListOf<Episode>()
        val updatedEpisodes = mutableListOf<Episode>()
        val removedEpisodes = dbEpisodes.filterNot { dbEpisode ->
            sourceEpisodes.any { sourceEpisode ->
                dbEpisode.url == sourceEpisode.url
            }
        }

        // Used to not set upload date of older episodes
        // to a higher value than newer episodes
        var maxSeenUploadDate = 0L

        for (sourceEpisode in sourceEpisodes) {
            var episode = sourceEpisode

            // Update metadata from source if necessary.
            if (source is AnimeHttpSource) {
                val sEpisode = episode.toSEpisode()
                source.prepareNewEpisode(sEpisode, anime.toSAnime())
                episode = episode.copyFromSEpisode(sEpisode)
            }

            // Recognize episode number for the episode.
            val episodeNumber = EpisodeRecognition.parseEpisodeNumber(
                anime.title,
                episode.name,
                episode.episodeNumber,
            )
            episode = episode.copy(episodeNumber = episodeNumber)

            val dbEpisode = dbEpisodes.find { it.url == episode.url }

            if (dbEpisode == null) {
                val toAddEpisode = if (episode.dateUpload == 0L) {
                    val altDateUpload = if (maxSeenUploadDate == 0L) nowMillis else maxSeenUploadDate
                    episode.copy(dateUpload = altDateUpload)
                } else {
                    maxSeenUploadDate = max(maxSeenUploadDate, sourceEpisode.dateUpload)
                    episode
                }
                newEpisodes.add(toAddEpisode)
            } else {
                if (shouldUpdateDbEpisode.await(dbEpisode, episode)) {
                    val shouldRenameEpisode = downloadProvider.isEpisodeDirNameChanged(
                        dbEpisode,
                        episode,
                    ) &&
                        downloadManager.isEpisodeDownloaded(
                            dbEpisode.name,
                            dbEpisode.scanlator,
                            anime.title,
                            anime.source,
                        )

                    if (shouldRenameEpisode) {
                        downloadManager.renameEpisode(source, anime, dbEpisode, episode)
                    }
                    var toChangeEpisode = dbEpisode.copy(
                        name = episode.name,
                        episodeNumber = episode.episodeNumber,
                        scanlator = episode.scanlator,
                        summary = episode.summary,
                        sourceOrder = episode.sourceOrder,
                    )
                    if (episode.dateUpload != 0L) {
                        toChangeEpisode = toChangeEpisode.copy(
                            dateUpload = sourceEpisode.dateUpload,
                        )
                    }
                    if (!toChangeEpisode.fillermark) {
                        toChangeEpisode = toChangeEpisode.copy(
                            fillermark = sourceEpisode.fillermark,
                        )
                    }
                    if (toChangeEpisode.previewUrl.isNullOrBlank()) {
                        toChangeEpisode = toChangeEpisode.copy(
                            previewUrl = sourceEpisode.previewUrl,
                        )
                    }
                    updatedEpisodes.add(toChangeEpisode)
                }
            }
        }

        // Return if there's nothing to add, delete, or update to avoid unnecessary db transactions.
        if (newEpisodes.isEmpty() && removedEpisodes.isEmpty() && updatedEpisodes.isEmpty()) {
            if (manualFetch || anime.fetchInterval == 0 || anime.nextUpdate < fetchWindow.first) {
                updateAnime.awaitUpdateFetchInterval(
                    anime,
                    now,
                    fetchWindow,
                )
            }
            return emptyList()
        }

        val changedOrDuplicateReadUrls = mutableSetOf<String>()

        val deletedEpisodeNumbers = TreeSet<Double>()
        val deletedSeenEpisodeNumbers = TreeSet<Double>()
        val deletedBookmarkedEpisodeNumbers = TreeSet<Double>()

        val readEpisodeNumbers = dbEpisodes
            .asSequence()
            .filter { it.seen && it.isRecognizedNumber }
            .map { it.episodeNumber }
            .toSet()

        removedEpisodes.forEach { episode ->
            if (episode.seen) deletedSeenEpisodeNumbers.add(episode.episodeNumber)
            if (episode.bookmark) deletedBookmarkedEpisodeNumbers.add(episode.episodeNumber)
            deletedEpisodeNumbers.add(episode.episodeNumber)
        }

        val deletedEpisodeNumberDateFetchMap = removedEpisodes.sortedByDescending { it.dateFetch }
            .associate { it.episodeNumber to it.dateFetch }

        val markDuplicateAsRead = libraryPreferences.markDuplicateSeenEpisodeAsSeen().get()
            .contains(LibraryPreferences.MARK_DUPLICATE_EPISODE_SEEN_NEW)

        // Date fetch is set in such a way that the upper ones will have bigger value than the lower ones
        // Sources MUST return the episodes from most to less recent, which is common.
        var itemCount = newEpisodes.size
        var updatedToAdd = newEpisodes.map { toAddItem ->
            var episode = toAddItem.copy(dateFetch = nowMillis + itemCount--)

            if (episode.episodeNumber in readEpisodeNumbers && markDuplicateAsRead) {
                changedOrDuplicateReadUrls.add(episode.url)
                episode = episode.copy(seen = true)
            }

            if (!episode.isRecognizedNumber || episode.episodeNumber !in deletedEpisodeNumbers) return@map episode

            episode = episode.copy(
                seen = episode.episodeNumber in deletedSeenEpisodeNumbers,
                bookmark = episode.episodeNumber in deletedBookmarkedEpisodeNumbers,
            )

            // Try to to use the fetch date of the original entry to not pollute 'Updates' tab
            deletedEpisodeNumberDateFetchMap[episode.episodeNumber]?.let {
                episode = episode.copy(dateFetch = it)
            }

            changedOrDuplicateReadUrls.add(episode.url)

            episode
        }

        if (removedEpisodes.isNotEmpty()) {
            val toDeleteIds = removedEpisodes.map { it.id }
            episodeRepository.removeEpisodesWithIds(toDeleteIds)
        }

        if (updatedToAdd.isNotEmpty()) {
            updatedToAdd = episodeRepository.addAllEpisodes(updatedToAdd)
        }

        if (updatedEpisodes.isNotEmpty()) {
            val episodeUpdates = updatedEpisodes.map { it.toEpisodeUpdate() }
            updateEpisode.awaitAll(episodeUpdates)
        }
        updateAnime.awaitUpdateFetchInterval(anime, now, fetchWindow)

        // Set this anime as updated since episodes were changed
        // Note that last_update actually represents last time the episode list changed at all
        updateAnime.awaitUpdateLastUpdate(anime.id)

        return updatedToAdd.filterNot { it.url in changedOrDuplicateReadUrls }
    }
}
