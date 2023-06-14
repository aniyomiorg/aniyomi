package eu.kanade.tachiyomi.data.download.anime

import android.content.Context
import androidx.core.content.edit
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.domain.entries.anime.interactor.GetAnime
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.interactor.GetEpisode
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to persist active downloads across application restarts.
 */
class AnimeDownloadStore(
    context: Context,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    private val json: Json = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getEpisode: GetEpisode = Injekt.get(),
) {

    /**
     * Preference file where active downloads are stored.
     */
    private val preferences = context.getSharedPreferences("active_downloads", Context.MODE_PRIVATE)

    /**
     * Counter used to keep the queue order.
     */
    private var counter = 0

    /**
     * Adds a list of downloads to the store.
     *
     * @param downloads the list of downloads to add.
     */
    fun addAll(downloads: List<AnimeDownload>) {
        preferences.edit {
            downloads.forEach { putString(getKey(it), serialize(it)) }
        }
    }

    /**
     * Removes a download from the store.
     *
     * @param download the download to remove.
     */
    fun remove(download: AnimeDownload) {
        preferences.edit {
            remove(getKey(download))
        }
    }

    /**
     * Removes all the downloads from the store.
     */
    fun clear() {
        preferences.edit {
            clear()
        }
    }

    /**
     * Returns the preference's key for the given download.
     *
     * @param download the download.
     */
    private fun getKey(download: AnimeDownload): String {
        return download.episode.id.toString()
    }

    /**
     * Returns the list of downloads to restore. It should be called in a background thread.
     */
    fun restore(): List<AnimeDownload> {
        val objs = preferences.all
            .mapNotNull { it.value as? String }
            .mapNotNull { deserialize(it) }
            .sortedBy { it.order }

        val downloads = mutableListOf<AnimeDownload>()
        if (objs.isNotEmpty()) {
            val cachedAnime = mutableMapOf<Long, Anime?>()
            for ((animeId, episodeId) in objs) {
                val anime = cachedAnime.getOrPut(animeId) {
                    runBlocking { getAnime.await(animeId) }
                } ?: continue
                val source = sourceManager.get(anime.source) as? AnimeHttpSource ?: continue
                val episode = runBlocking { getEpisode.await(episodeId) } ?: continue
                downloads.add(AnimeDownload(source, anime, episode))
            }
        }

        // Clear the store, downloads will be added again immediately.
        clear()
        return downloads
    }

    /**
     * Converts a download to a string.
     *
     * @param download the download to serialize.
     */
    private fun serialize(download: AnimeDownload): String {
        val obj = AnimeDownloadObject(download.anime.id, download.episode.id!!, counter++)
        return json.encodeToString(obj)
    }

    /**
     * Restore a download from a string.
     *
     * @param string the download as string.
     */
    private fun deserialize(string: String): AnimeDownloadObject? {
        return try {
            json.decodeFromString<AnimeDownloadObject>(string)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Class used for download serialization
 *
 * @param animeId the id of the anime.
 * @param episodeId the id of the episode.
 * @param order the order of the download in the queue.
 */
@Serializable
private data class AnimeDownloadObject(val animeId: Long, val episodeId: Long, val order: Int)
