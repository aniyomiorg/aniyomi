package eu.kanade.domain.items.episode.model

import dataanime.Episodes
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
        it.episode_number = episodeNumber
        it.scanlator = scanlator
    }
}

fun Episode.copyFromSEpisode(sEpisode: SEpisode): Episode {
    return this.copy(
        name = sEpisode.name,
        url = sEpisode.url,
        dateUpload = sEpisode.date_upload,
        episodeNumber = sEpisode.episode_number,
        scanlator = sEpisode.scanlator?.ifBlank { null },
    )
}

fun Episode.copyFrom(other: Episodes): Episode {
    return copy(
        name = other.name,
        url = other.url,
        dateUpload = other.date_upload,
        episodeNumber = other.episode_number,
        scanlator = other.scanlator?.ifBlank { null },
    )
}

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
