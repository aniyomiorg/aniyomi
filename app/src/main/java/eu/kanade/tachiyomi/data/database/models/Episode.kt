package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.animesource.model.SEpisode
import java.io.Serializable
import eu.kanade.domain.episode.model.Episode as DomainEpisode

interface Episode : SEpisode, Serializable {

    var id: Long?

    var anime_id: Long?

    var seen: Boolean

    var bookmark: Boolean

    var last_second_seen: Long

    var total_seconds: Long

    var date_fetch: Long

    var source_order: Int

    companion object {
        fun create(): Episode = EpisodeImpl().apply {
            episode_number = -1f
        }
    }
}

fun Episode.toDomainEpisode(): DomainEpisode? {
    if (id == null || anime_id == null) return null
    return DomainEpisode(
        id = id!!,
        animeId = anime_id!!,
        seen = seen,
        bookmark = bookmark,
        lastSecondSeen = last_second_seen,
        totalSeconds = total_seconds,
        dateFetch = date_fetch,
        sourceOrder = source_order.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        episodeNumber = episode_number,
        scanlator = scanlator,
    )
}
