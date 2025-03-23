package eu.kanade.tachiyomi.data.track.myanimelist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MALManga(
    val id: Long,
    val title: String,
    val synopsis: String = "",
    @SerialName("num_chapters")
    val numChapters: Long,
    val mean: Double = -1.0,
    @SerialName("main_picture")
    val covers: MALMangaCovers,
    val status: String,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("start_date")
    val startDate: String?,
)

@Serializable
data class MALMangaCovers(
    val large: String = "",
    val medium: String,
)

@Serializable
data class MALMangaMetadata(
    val id: Long,
    val title: String,
    val synopsis: String?,
    @SerialName("main_picture")
    val covers: MALMangaCovers,
    val authors: List<MALAuthor>,
)

@Serializable
data class MALAuthor(
    val node: MALAuthorNode,
    val role: String,
)

@Serializable
data class MALAuthorNode(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
)
