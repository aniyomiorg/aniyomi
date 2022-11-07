package eu.kanade.domain.episode.model

import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.data.database.models.EpisodeImpl
import eu.kanade.tachiyomi.data.database.models.Episode as DbEpisode

data class Episode(
    val id: Long,
    val animeId: Long,
    val seen: Boolean,
    val bookmark: Boolean,
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val dateFetch: Long,
    val sourceOrder: Long,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val episodeNumber: Float,
    val scanlator: String?,
) {
    val isRecognizedNumber: Boolean
        get() = episodeNumber >= 0f

    fun toSEpisode(): SEpisode {
        return SEpisode.create().also {
            it.url = url
            it.name = name
            it.date_upload = dateUpload
            it.episode_number = episodeNumber
            it.scanlator = scanlator
        }
    }

    fun copyFromSEpisode(sEpisode: SEpisode): Episode {
        return this.copy(
            name = sEpisode.name,
            url = sEpisode.url,
            dateUpload = sEpisode.date_upload,
            episodeNumber = sEpisode.episode_number,
            scanlator = sEpisode.scanlator?.ifBlank { null },
        )
    }

    companion object {
        fun create() = Episode(
            id = -1,
            animeId = -1,
            seen = false,
            bookmark = false,
            lastSecondSeen = 0,
            totalSeconds = 0,
            dateFetch = 0,
            sourceOrder = 0,
            url = "",
            name = "",
            dateUpload = -1,
            episodeNumber = -1f,
            scanlator = null,
        )
    }
}

// TODO: Remove when all deps are migrated
fun Episode.toDbEpisode(): DbEpisode = EpisodeImpl().also {
    it.id = id
    it.anime_id = animeId
    it.url = url
    it.name = name
    it.scanlator = scanlator
    it.seen = seen
    it.bookmark = bookmark
    it.last_second_seen = lastSecondSeen
    it.total_seconds = totalSeconds
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.episode_number = episodeNumber
    it.source_order = sourceOrder.toInt()
}
