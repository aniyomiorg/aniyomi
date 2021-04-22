package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.load.Key
import eu.kanade.tachiyomi.data.database.models.Anime
import java.security.MessageDigest

data class AnimeThumbnail(val anime: Anime, val coverLastModified: Long) : Key {
    val key = anime.url + coverLastModified

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(key.toByteArray(Key.CHARSET))
    }
}

fun Anime.toAnimeThumbnail() = AnimeThumbnail(this, cover_last_modified)
