package eu.kanade.tachiyomi.data.cache

import android.content.Context
import coil.imageLoader
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Makes use of Glide (which can avoid repeating requests) to download covers.
 * Names of files are created with the md5 of the anime_thumbnail URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class AnimeCoverCache(private val context: Context) {

    companion object {
        private const val COVERS_DIR = "animecovers"
        private const val CUSTOM_COVERS_DIR = "animecovers/custom"
    }

    /**
     * Cache directory used for cache management.
     */
    private val cacheDir = getCacheDir(COVERS_DIR)

    private val customCoverCacheDir = getCacheDir(CUSTOM_COVERS_DIR)

    /**
     * Returns the cover from cache.
     *
     * @param anime the anime.
     * @return cover image.
     */
    fun getCoverFile(anime: Anime): File? {
        return anime.thumbnail_url?.let {
            File(cacheDir, DiskUtil.hashKeyForDisk(it))
        }
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param anime the anime.
     * @return cover image.
     */
    fun getCustomCoverFile(anime: Anime): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(anime.id.toString()))
    }

    /**
     * Saves the given stream as the anime's custom cover to cache.
     *
     * @param anime the anime.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(anime: Anime, inputStream: InputStream) {
        getCustomCoverFile(anime).outputStream().use {
            inputStream.copyTo(it)
        }
    }

    /**
     * Delete the cover files of the anime from the cache.
     *
     * @param anime the anime.
     * @param deleteCustomCover whether the custom cover should be deleted.
     * @return number of files that were deleted.
     */
    fun deleteFromCache(anime: Anime, deleteCustomCover: Boolean = false): Int {
        var deleted = 0

        getCoverFile(anime)?.let {
            if (it.exists() && it.delete()) ++deleted
        }

        if (deleteCustomCover) {
            if (deleteCustomCover(anime)) ++deleted
        }

        return deleted
    }

    /**
     * Delete custom cover of the anime from the cache
     *
     * @param anime the anime.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(anime: Anime): Boolean {
        return getCustomCoverFile(anime).let {
            it.exists() && it.delete()
        }
    }

    /**
     * Clear coil's memory cache.
     */
    fun clearMemoryCache() {
        context.imageLoader.memoryCache?.clear()
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}
