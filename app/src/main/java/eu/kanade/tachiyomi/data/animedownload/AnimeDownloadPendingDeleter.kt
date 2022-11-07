package eu.kanade.tachiyomi.data.animedownload

import android.content.Context
import androidx.core.content.edit
import eu.kanade.domain.anime.model.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy

/**
 * Class used to keep a list of episodes for future deletion.
 *
 * @param context the application context.
 */
class AnimeDownloadPendingDeleter(context: Context) {

    private val json: Json by injectLazy()

    /**
     * Preferences used to store the list of episodes to delete.
     */
    private val preferences = context.getSharedPreferences("episodes_to_delete", Context.MODE_PRIVATE)

    /**
     * Last added episode, used to avoid decoding from the preference too often.
     */
    private var lastAddedEntry: Entry? = null

    /**
     * Adds a list of episodes for future deletion.
     *
     * @param episodes the episodes to be deleted.
     * @param anime the anime of the episodes.
     */
    @Synchronized
    fun addEpisodes(episodes: List<Episode>, anime: Anime) {
        val lastEntry = lastAddedEntry

        val newEntry = if (lastEntry != null && lastEntry.anime.id == anime.id) {
            // Append new episodes
            val newEpisodes = lastEntry.episodes.addUniqueById(episodes)

            // If no episodes were added, do nothing
            if (newEpisodes.size == lastEntry.episodes.size) return

            // Last entry matches the anime, reuse it to avoid decoding json from preferences
            lastEntry.copy(episodes = newEpisodes)
        } else {
            val existingEntry = preferences.getString(anime.id.toString(), null)
            if (existingEntry != null) {
                // Existing entry found on preferences, decode json and add the new episode
                val savedEntry = json.decodeFromString<Entry>(existingEntry)

                // Append new episodes
                val newEpisodes = savedEntry.episodes.addUniqueById(episodes)

                // If no episodes were added, do nothing
                if (newEpisodes.size == savedEntry.episodes.size) return

                savedEntry.copy(episodes = newEpisodes)
            } else {
                // No entry has been found yet, create a new one
                Entry(episodes.map { it.toEntry() }, anime.toEntry())
            }
        }

        // Save current state
        val json = json.encodeToString(newEntry)
        preferences.edit {
            putString(newEntry.anime.id.toString(), json)
        }
        lastAddedEntry = newEntry
    }

    /**
     * Returns the list of episodes to be deleted grouped by its anime.
     *
     * Note: the returned list of anime and episodes only contain basic information needed by the
     * downloader, so don't use them for anything else.
     */
    @Synchronized
    fun getPendingEpisodes(): Map<Anime, List<Episode>> {
        val entries = decodeAll()
        preferences.edit {
            clear()
        }
        lastAddedEntry = null

        return entries.associate { (episodes, anime) ->
            anime.toModel() to episodes.map { it.toModel() }
        }
    }

    /**
     * Decodes all the episodes from preferences.
     */
    private fun decodeAll(): List<Entry> {
        return preferences.all.values.mapNotNull { rawEntry ->
            try {
                (rawEntry as? String)?.let { json.decodeFromString<Entry>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Returns a copy of episode entries ensuring no duplicates by episode id.
     */
    private fun List<EpisodeEntry>.addUniqueById(episodes: List<Episode>): List<EpisodeEntry> {
        val newList = toMutableList()
        for (episode in episodes) {
            if (none { it.id == episode.id }) {
                newList.add(episode.toEntry())
            }
        }
        return newList
    }

    /**
     * Class used to save an entry of episodes with their anime into preferences.
     */
    @Serializable
    private data class Entry(
        val episodes: List<EpisodeEntry>,
        val anime: AnimeEntry,
    )

    /**
     * Class used to save an entry for an episode into preferences.
     */
    @Serializable
    private data class EpisodeEntry(
        val id: Long,
        val url: String,
        val name: String,
        val scanlator: String? = null,
    )

    /**
     * Class used to save an entry for an anime into preferences.
     */
    @Serializable
    private data class AnimeEntry(
        val id: Long,
        val url: String,
        val title: String,
        val source: Long,
    )

    /**
     * Returns an anime entry from an anime model.
     */
    private fun Anime.toEntry(): AnimeEntry {
        return AnimeEntry(id, url, title, source)
    }

    /**
     * Returns an episode entry from an episode model.
     */
    private fun Episode.toEntry(): EpisodeEntry {
        return EpisodeEntry(id!!, url, name, scanlator)
    }

    /**
     * Returns an anime model from an anime entry.
     */
    private fun AnimeEntry.toModel(): Anime {
        return Anime.create().copy(
            url = url,
            title = title,
            source = source,
            id = id,
        )
    }

    /**
     * Returns an episode model from an episode entry.
     */
    private fun EpisodeEntry.toModel(): Episode {
        return Episode.create().also {
            it.id = id
            it.url = url
            it.name = name
            it.scanlator = scanlator
        }
    }
}
