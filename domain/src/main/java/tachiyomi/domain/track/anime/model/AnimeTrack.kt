package tachiyomi.domain.track.anime.model

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
)
