package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.database.models.Anime as DbAnime
import eu.kanade.tachiyomi.data.database.models.Episode as DbEpisode

/**
 * Helper method for syncing the list of episodes from the source with the ones from the database.
 *
 * @param rawSourceEpisodes a list of episodes from the source.
 * @param anime the anime of the episodes.
 * @param source the source of the episodes.
 * @return a pair of new insertions and deletions.
 */
suspend fun syncEpisodesWithSource(
    rawSourceEpisodes: List<SEpisode>,
    anime: DbAnime,
    source: AnimeSource,
    syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
): Pair<List<DbEpisode>, List<DbEpisode>> {
    val domainAnime = anime.toDomainAnime() ?: return Pair(emptyList(), emptyList())
    val (added, deleted) = syncEpisodesWithSource.await(rawSourceEpisodes, domainAnime, source)

    val addedDbEpisodes = added.map { it.toDbEpisode() }
    val deletedDbEpisodes = deleted.map { it.toDbEpisode() }

    return Pair(addedDbEpisodes, deletedDbEpisodes)
}
