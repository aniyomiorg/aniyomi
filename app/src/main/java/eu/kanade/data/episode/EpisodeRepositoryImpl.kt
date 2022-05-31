package eu.kanade.data.episode

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.toLong
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.repository.EpisodeRepository
import eu.kanade.tachiyomi.util.system.logcat
import logcat.LogPriority

class EpisodeRepositoryImpl(
    private val databaseHandler: AnimeDatabaseHandler,
) : EpisodeRepository {

    override suspend fun update(episodeUpdate: EpisodeUpdate) {
        try {
            databaseHandler.await {
                episodesQueries.update(
                    episodeUpdate.animeId,
                    episodeUpdate.url,
                    episodeUpdate.name,
                    episodeUpdate.scanlator,
                    episodeUpdate.seen?.toLong(),
                    episodeUpdate.bookmark?.toLong(),
                    episodeUpdate.lastSecondSeen,
                    episodeUpdate.episodeNumber?.toDouble(),
                    episodeUpdate.sourceOrder,
                    episodeUpdate.dateFetch,
                    episodeUpdate.dateUpload,
                    episodeId = episodeUpdate.id,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
