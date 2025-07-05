@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models.anime

import eu.kanade.tachiyomi.animesource.model.SEpisode
import java.io.Serializable
import tachiyomi.domain.items.episode.model.Episode as DomainEpisode

interface Episode : SEpisode, Serializable {

    var id: Long?

    var anime_id: Long?

    var seen: Boolean

    var bookmark: Boolean

    var last_second_seen: Long

    var total_seconds: Long

    var date_fetch: Long

    var source_order: Int

    var last_modified: Long

    var version: Long
}

val Episode.isRecognizedNumber: Boolean
    get() = episode_number >= 0f

fun Episode.toDomainEpisode(): DomainEpisode? {
    if (id == null || anime_id == null) return null
    return DomainEpisode(
        id = id!!,
        animeId = anime_id!!,
        seen = seen,
        bookmark = bookmark,
        fillermark = fillermark,
        lastSecondSeen = last_second_seen,
        totalSeconds = total_seconds,
        dateFetch = date_fetch,
        sourceOrder = source_order.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        episodeNumber = episode_number.toDouble(),
        scanlator = scanlator,
        summary = summary,
        previewUrl = preview_url,
        lastModifiedAt = last_modified,
        version = version,
    )
}
