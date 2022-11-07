package eu.kanade.tachiyomi.data.cache

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.File
import java.io.IOException
import java.io.InputStream
import eu.kanade.domain.anime.model.Anime as DomainAnime

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Names of files are created with the md5 of the thumbnail URL.
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
     * @param animeThumbnailUrl the anime.
     * @return cover image.
     */
    fun getCoverFile(animeThumbnailUrl: String?): File? {
        return animeThumbnailUrl?.let {
            File(cacheDir, DiskUtil.hashKeyForDisk(it))
        }
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param animeId the anime id.
     * @return cover image.
     */
    fun getCustomCoverFile(animeId: Long?): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(animeId.toString()))
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
        getCustomCoverFile(anime.id).outputStream().use {
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

        getCoverFile(anime.thumbnail_url)?.let {
            if (it.exists() && it.delete()) ++deleted
        }

        if (deleteCustomCover) {
            if (deleteCustomCover(anime.id)) ++deleted
        }

        return deleted
    }

    fun deleteFromCache(anime: DomainAnime, deleteCustomCover: Boolean = false): Int {
        var amountDeleted = 0

        getCoverFile(anime.thumbnailUrl)?.let {
            if (it.exists() && it.delete()) amountDeleted++
        }

        if (deleteCustomCover && deleteCustomCover(anime.id)) {
            amountDeleted++
        }

        return amountDeleted
    }

    /**
     * Delete custom cover of the anime from the cache
     *
     * @param animeId the anime id.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(animeId: Long?): Boolean {
        return getCustomCoverFile(animeId).let {
            it.exists() && it.delete()
        }
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}
