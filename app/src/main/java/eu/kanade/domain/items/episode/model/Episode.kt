package eu.kanade.domain.items.episode.model

import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.data.database.models.anime.EpisodeImpl
import tachiyomi.domain.items.episode.model.Episode
import eu.kanade.tachiyomi.data.database.models.anime.Episode as DbEpisode

// TODO: Remove when all deps are migrated
fun Episode.toSEpisode(): SEpisode {
    return SEpisode.create().also {
        it.url = url
        it.name = name
        it.date_upload = dateUpload
        it.episode_number = episodeNumber.toFloat()
        it.fillermark = fillermark
        it.scanlator = scanlator
        it.summary = summary
        it.preview_url = previewUrl
    }
}

fun Episode.copyFromSEpisode(sEpisode: SEpisode): Episode {
    return this.copy(
        name = sEpisode.name,
        url = sEpisode.url,
        dateUpload = sEpisode.date_upload,
        episodeNumber = sEpisode.episode_number.toDouble(),
        fillermark = sEpisode.fillermark,
        scanlator = sEpisode.scanlator?.ifBlank { null },
        summary = sEpisode.summary?.ifBlank { null },
        previewUrl = sEpisode.preview_url?.ifBlank { null },
    )
}

fun Episode.toDbEpisode(): DbEpisode = EpisodeImpl().also {
    it.id = id
    it.anime_id = animeId
    it.url = url
    it.name = name
    it.scanlator = scanlator
    it.summary = summary
    it.preview_url = previewUrl
    it.seen = seen
    it.bookmark = bookmark
    it.fillermark = fillermark
    it.last_second_seen = lastSecondSeen
    it.total_seconds = totalSeconds
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.episode_number = episodeNumber.toFloat()
    it.source_order = sourceOrder.toInt()
}
