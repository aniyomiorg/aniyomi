package eu.kanade.domain.track.anime.model

import tachiyomi.domain.track.anime.model.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack as DbAnimeTrack

fun AnimeTrack.copyPersonalFrom(other: AnimeTrack): AnimeTrack {
    return this.copy(
        lastEpisodeSeen = other.lastEpisodeSeen,
        score = other.score,
        status = other.status,
        startDate = other.startDate,
        finishDate = other.finishDate,
        private = other.private,
    )
}

fun AnimeTrack.toDbTrack(): DbAnimeTrack = DbAnimeTrack.create(trackerId).also {
    it.id = id
    it.anime_id = animeId
    it.remote_id = remoteId
    it.library_id = libraryId
    it.title = title
    it.last_episode_seen = lastEpisodeSeen
    it.total_episodes = totalEpisodes
    it.status = status
    it.score = score
    it.tracking_url = remoteUrl
    it.started_watching_date = startDate
    it.finished_watching_date = finishDate
    it.private = private
}

fun DbAnimeTrack.toDomainTrack(idRequired: Boolean = true): AnimeTrack? {
    val trackId = id ?: if (!idRequired) -1 else return null
    return AnimeTrack(
        id = trackId,
        animeId = anime_id,
        trackerId = tracker_id.toLong(),
        remoteId = remote_id,
        libraryId = library_id,
        title = title,
        lastEpisodeSeen = last_episode_seen,
        totalEpisodes = total_episodes,
        status = status,
        score = score,
        remoteUrl = tracking_url,
        startDate = started_watching_date,
        finishDate = finished_watching_date,
        private = private,
    )
}
