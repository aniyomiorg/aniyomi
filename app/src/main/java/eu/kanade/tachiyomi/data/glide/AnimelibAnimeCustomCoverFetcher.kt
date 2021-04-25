package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.data.DataFetcher
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.Anime
import java.io.File
import java.io.InputStream
import java.lang.Exception

open class AnimelibAnimeCustomCoverFetcher(
    private val anime: Anime,
    private val coverCache: AnimeCoverCache
) : FileFetcher() {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        getCustomCoverFile()?.let {
            loadFromFile(it, callback)
        } ?: callback.onLoadFailed(Exception("Custom cover file not found"))
    }

    protected fun getCustomCoverFile(): File? {
        return coverCache.getCustomCoverFile(anime).takeIf { it.exists() }
    }
}
