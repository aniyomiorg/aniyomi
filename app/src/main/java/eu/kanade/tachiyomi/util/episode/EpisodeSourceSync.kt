package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import java.util.TreeSet

/**
 * Helper method for syncing the list of episodes from the source with the ones from the database.
 *
 * @param db the database.
 * @param rawSourceEpisodes a list of episodes from the source.
 * @param anime the anime of the episodes.
 * @param source the source of the episodes.
 * @return a pair of new insertions and deletions.
 */
fun syncEpisodesWithSource(
    db: AnimeDatabaseHelper,
    rawSourceEpisodes: List<SEpisode>,
    anime: Anime,
    source: AnimeSource
): Pair<List<Episode>, List<Episode>> {
    if (rawSourceEpisodes.isEmpty()) {
        throw NoEpisodesException()
    }

    val downloadManager: AnimeDownloadManager = Injekt.get()

    // Episodes from db.
    val dbEpisodes = db.getEpisodes(anime).executeAsBlocking()

    val sourceEpisodes = rawSourceEpisodes
        .distinctBy { it.url }
        .mapIndexed { i, sEpisode ->
            Episode.create().apply {
                copyFrom(sEpisode)
                anime_id = anime.id
                source_order = i
            }
        }

    // Episodes from the source not in db.
    val toAdd = mutableListOf<Episode>()

    // Episodes whose metadata have changed.
    val toChange = mutableListOf<Episode>()

    for (sourceEpisode in sourceEpisodes) {
        val dbEpisode = dbEpisodes.find { it.url == sourceEpisode.url }

        // Add the episode if not in db already, or update if the metadata changed.
        if (dbEpisode == null) {
            toAdd.add(sourceEpisode)
        } else {
            // this forces metadata update for the main viewable things in the episode list
            if (source is AnimeHttpSource) {
                source.prepareNewEpisode(sourceEpisode, anime)
            }

            EpisodeRecognition.parseEpisodeNumber(sourceEpisode, anime)

            if (shouldUpdateDbEpisode(dbEpisode, sourceEpisode)) {
                if (dbEpisode.name != sourceEpisode.name && downloadManager.isEpisodeDownloaded(dbEpisode, anime)) {
                    downloadManager.renameEpisode(source, anime, dbEpisode, sourceEpisode)
                }
                dbEpisode.scanlator = sourceEpisode.scanlator
                dbEpisode.name = sourceEpisode.name
                dbEpisode.date_upload = sourceEpisode.date_upload
                dbEpisode.episode_number = sourceEpisode.episode_number
                toChange.add(dbEpisode)
            }
        }
    }

    // Recognize number for new episodes.
    toAdd.forEach {
        if (source is AnimeHttpSource) {
            source.prepareNewEpisode(it, anime)
        }
        EpisodeRecognition.parseEpisodeNumber(it, anime)
    }

    // Episodes from the db not in the source.
    val toDelete = dbEpisodes.filterNot { dbEpisode ->
        sourceEpisodes.any { sourceEpisode ->
            dbEpisode.url == sourceEpisode.url
        }
    }

    // Return if there's nothing to add, delete or change, avoiding unnecessary db transactions.
    if (toAdd.isEmpty() && toDelete.isEmpty() && toChange.isEmpty()) {
        return Pair(emptyList(), emptyList())
    }

    val readded = mutableListOf<Episode>()

    db.inTransaction {
        val deletedEpisodeNumbers = TreeSet<Float>()
        val deletedReadEpisodeNumbers = TreeSet<Float>()
        if (toDelete.isNotEmpty()) {
            for (c in toDelete) {
                if (c.seen) {
                    deletedReadEpisodeNumbers.add(c.episode_number)
                }
                deletedEpisodeNumbers.add(c.episode_number)
            }
            db.deleteEpisodes(toDelete).executeAsBlocking()
        }

        if (toAdd.isNotEmpty()) {
            // Set the date fetch for new items in reverse order to allow another sorting method.
            // Sources MUST return the episodes from most to less recent, which is common.
            var now = Date().time

            for (i in toAdd.indices.reversed()) {
                val c = toAdd[i]
                c.date_fetch = now++
                // Try to mark already read episodes as read when the source deletes them
                if (c.isRecognizedNumber && c.episode_number in deletedReadEpisodeNumbers) {
                    c.seen = true
                }
                if (c.isRecognizedNumber && c.episode_number in deletedEpisodeNumbers) {
                    readded.add(c)
                }
            }
            val episodes = db.insertEpisodes(toAdd).executeAsBlocking()
            toAdd.forEach { episode ->
                episode.id = episodes.results().getValue(episode).insertedId()
            }
        }

        if (toChange.isNotEmpty()) {
            db.insertEpisodes(toChange).executeAsBlocking()
        }

        // Fix order in source.
        db.fixEpisodesSourceOrder(sourceEpisodes).executeAsBlocking()

        // Set this anime as updated since episodes were changed
        anime.last_update = Date().time
        db.updateLastUpdated(anime).executeAsBlocking()
    }

    return Pair(toAdd.subtract(readded).toList(), toDelete.subtract(readded).toList())
}

// checks if the episode in db needs updated
private fun shouldUpdateDbEpisode(dbEpisode: Episode, sourceEpisode: SEpisode): Boolean {
    return dbEpisode.scanlator != sourceEpisode.scanlator || dbEpisode.name != sourceEpisode.name ||
        dbEpisode.date_upload != sourceEpisode.date_upload ||
        dbEpisode.episode_number != sourceEpisode.episode_number
}

class NoEpisodesException : Exception()
