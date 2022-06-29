package eu.kanade.domain.animetrack.model

import eu.kanade.tachiyomi.data.database.models.AnimeTrack as DbAnimeTrack

data class AnimeTrack(
    val id: Long,
    val animeId: Long,
    val syncId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val lastEpisodeSeen: Double,
    val totalEpisodes: Long,
    val status: Long,
    val score: Float,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
) {
    fun copyPersonalFrom(other: AnimeTrack): AnimeTrack {
        return this.copy(
            lastEpisodeSeen = other.lastEpisodeSeen,
            score = other.score,
            status = other.status,
            startDate = other.startDate,
            finishDate = other.finishDate,
        )
    }
}

fun AnimeTrack.toDbTrack(): DbAnimeTrack = DbAnimeTrack.create(syncId).also {
    it.id = id
    it.anime_id = animeId
    it.media_id = remoteId
    it.library_id = libraryId
    it.title = title
    it.last_episode_seen = lastEpisodeSeen.toFloat()
    it.total_episodes = totalEpisodes.toInt()
    it.status = status.toInt()
    it.score = score
    it.tracking_url = remoteUrl
    it.started_watching_date = startDate
    it.finished_watching_date = finishDate
}

fun DbAnimeTrack.toDomainTrack(idRequired: Boolean = true): AnimeTrack? {
    val trackId = id ?: if (idRequired.not()) -1 else return null
    return AnimeTrack(
        id = trackId,
        animeId = anime_id,
        syncId = sync_id.toLong(),
        remoteId = media_id,
        libraryId = library_id,
        title = title,
        lastEpisodeSeen = last_episode_seen.toDouble(),
        totalEpisodes = total_episodes.toLong(),
        status = status.toLong(),
        score = score,
        remoteUrl = tracking_url,
        startDate = started_watching_date,
        finishDate = finished_watching_date,
    )
}
