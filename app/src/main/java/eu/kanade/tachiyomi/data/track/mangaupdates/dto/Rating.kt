package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    val rating: Float? = null,
)

fun Rating.copyTo(track: MangaTrack): MangaTrack {
    return track.apply {
        this.score = rating ?: 0f
    }
}
