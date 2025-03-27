package eu.kanade.tachiyomi.data.cache

import android.content.Context
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.domain.entries.anime.model.Anime
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Class used to create background cache.
 * It is used to store the background of the library.
 * Names of files are created with the md5 of the background URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the background cache.
 */
class AnimeBackgroundCache(private val context: Context) {

    companion object {
        private const val BACKGROUNDS_DIR = "animebackgrounds"
        private const val CUSTOM_BACKGROUNDS_DIR = "animebackgrounds/custom"
    }

    /**
     * Cache directory used for cache management.
     */
    private val cacheDir = getCacheDir(BACKGROUNDS_DIR)

    private val customBackgroundCacheDir = getCacheDir(CUSTOM_BACKGROUNDS_DIR)

    /**
     * Returns the background from cache.
     *
     * @param animeBackgroundUrl the anime.
     * @return background image.
     */
    fun getBackgroundFile(animeBackgroundUrl: String?): File? {
        return animeBackgroundUrl?.let {
            File(cacheDir, DiskUtil.hashKeyForDisk(it))
        }
    }

    /**
     * Returns the custom background from cache.
     *
     * @param animeId the anime id.
     * @return background image.
     */
    fun getCustomBackgroundFile(animeId: Long?): File {
        return File(customBackgroundCacheDir, DiskUtil.hashKeyForDisk(animeId.toString()))
    }

    /**
     * Saves the given stream as the anime's custom background to cache.
     *
     * @param anime the anime.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomBackgroundToCache(anime: Anime, inputStream: InputStream) {
        getCustomBackgroundFile(anime.id).outputStream().use {
            inputStream.copyTo(it)
        }
    }

    /**
     * Delete the background files of the anime from the cache.
     *
     * @param anime the anime.
     * @param deleteCustomBackground whether the custom background should be deleted.
     * @return number of files that were deleted.
     */
    fun deleteFromCache(anime: Anime, deleteCustomBackground: Boolean = false): Int {
        var deleted = 0

        getBackgroundFile(anime.backgroundUrl)?.let {
            if (it.exists() && it.delete()) ++deleted
        }

        if (deleteCustomBackground) {
            if (deleteCustomBackground(anime.id)) ++deleted
        }

        return deleted
    }

    /**
     * Delete custom background of the anime from the cache
     *
     * @param animeId the anime id.
     * @return whether the background was deleted.
     */
    fun deleteCustomBackground(animeId: Long?): Boolean {
        return getCustomBackgroundFile(animeId).let {
            it.exists() && it.delete()
        }
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}
