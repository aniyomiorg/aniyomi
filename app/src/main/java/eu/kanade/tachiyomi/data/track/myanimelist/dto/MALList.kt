package eu.kanade.tachiyomi.data.track.myanimelist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MALListAnimeItem(
    @SerialName("num_episodes")
    val numEpisodes: Long,
    @SerialName("my_list_status")
    val myListStatus: MALListAnimeItemStatus?,
)

@Serializable
data class MALListAnimeItemStatus(
    @SerialName("is_rewatching")
    val isRewatching: Boolean,
    val status: String,
    @SerialName("num_episodes_watched")
    val numEpisodesWatched: Double,
    val score: Int,
    @SerialName("start_date")
    val startDate: String?,
    @SerialName("finish_date")
    val finishDate: String?,
)

@Serializable
data class MALListMangaItem(
    @SerialName("num_chapters")
    val numChapters: Long,
    @SerialName("my_list_status")
    val myListStatus: MALListMangaItemStatus?,
)

@Serializable
data class MALListMangaItemStatus(
    @SerialName("is_rereading")
    val isRereading: Boolean,
    val status: String,
    @SerialName("num_chapters_read")
    val numChaptersRead: Double,
    val score: Int,
    @SerialName("start_date")
    val startDate: String?,
    @SerialName("finish_date")
    val finishDate: String?,
)
