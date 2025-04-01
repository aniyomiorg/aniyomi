package tachiyomi.domain.track.anime.model

import java.io.Serializable

data class AnimeTrack(
    val id: Long,
    val animeId: Long,
    val trackerId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val lastEpisodeSeen: Double,
    val totalEpisodes: Long,
    val status: Long,
    val score: Double,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,
) : Serializable
