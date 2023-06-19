package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.saveTo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Response
import okio.buffer
import okio.sink
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

/**
 * Class used to create episode cache
 * For each image in an episode a file is created
 * For each episode a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 *
 * @param context the application context.
 * @constructor creates an instance of the episode cache.
 */
class EpisodeCache(private val context: Context) {

    companion object {
        /** Name of cache directory.  */
        const val PARAMETER_CACHE_DIRECTORY = "episode_disk_cache"

        /** Application cache version.  */
        const val PARAMETER_APP_VERSION = 1

        /** The number of values per cache entry. Must be positive.  */
        const val PARAMETER_VALUE_COUNT = 1

        /** The maximum number of bytes this cache should use to store.  */
        const val PARAMETER_CACHE_SIZE = 1000L * 1024 * 1024
    }

    private val json: Json by injectLazy()

    /** Cache class used for cache management.  */
    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, PARAMETER_CACHE_DIRECTORY),
        PARAMETER_APP_VERSION,
        PARAMETER_VALUE_COUNT,
        PARAMETER_CACHE_SIZE,
    )

    /**
     * Returns directory of cache.
     */
    val cacheDir: File
        get() = diskCache.directory

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir)

    /**
     * Returns real size of directory in human readable format.
     */
    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    fun clear(): Int {
        var deletedFiles = 0
        cacheDir.listFiles()?.forEach {
            if (removeFileFromCache(it.name)) {
                deletedFiles++
            }
        }
        return deletedFiles
    }

    /**
     * Remove file from cache.
     *
     * @param file name of file "md5.0".
     * @return status of deletion for the file.
     */
    fun removeFileFromCache(file: String): Boolean {
        // Make sure we don't delete the journal file (keeps track of cache).
        if (file == "journal" || file.startsWith("journal.")) {
            return false
        }

        return try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substringBeforeLast(".")
            // Remove file from cache.
            diskCache.remove(key)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Add page list to disk cache.
     *
     * @param episode the episode.
     * @param video the video.
     */
    fun putPageListToCache(episode: Episode, video: Video) {
        // Convert list of pages to json string.
        val cachedValue = json.encodeToString(video)

        // Initialize the editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(getKey(episode))
            editor = diskCache.edit(key) ?: return

            // Write episode urls to cache.
            editor.newOutputStream(0).sink().buffer().use {
                it.write(cachedValue.toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()
        } catch (e: Exception) {
            // Ignore.
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    /**
     * Returns true if image is in cache.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    fun isImageInCache(imageUrl: String): Boolean {
        return try {
            diskCache.get(DiskUtil.hashKeyForDisk(imageUrl)) != null
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Get image file from url.
     *
     * @param imageUrl url of image.
     * @return path of image.
     */
    fun getVideoFile(imageUrl: String): File {
        // Get file from md5 key.
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
    }

    /**
     * Add image to cache.
     *
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    @Throws(IOException::class)
    fun putImageToCache(imageUrl: String, response: Response) {
        // Initialize editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: throw IOException("Unable to edit key")

            // Get OutputStream and write image with Okio.
            response.body.source().saveTo(editor.newOutputStream(0))

            diskCache.flush()
            editor.commit()
        } finally {
            response.body.close()
            editor?.abortUnlessCommitted()
        }
    }

    private fun getKey(episode: Episode): String {
        return "${episode.animeId}${episode.url}"
    }
}
