package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALAnimeMetadata(
    val data: ALAnimeMetadataData,
)

@Serializable
data class ALAnimeMetadataData(
    @SerialName("Media")
    val media: ALAnimeMetadataMedia,
)

@Serializable
data class ALAnimeMetadataMedia(
    val id: Long,
    val title: ALItemTitle,
    val coverImage: ItemCover,
    val description: String?,
    val staff: ALAnimeStaff,
    val studios: ALStudios,
)

@Serializable
data class ALAnimeStaff(
    val edges: List<ALAnimeStaffEdge>,
)

@Serializable
data class ALAnimeStaffEdge(
    val role: String,
    val node: ALAnimeStaffNode,
)

@Serializable
data class ALAnimeStaffNode(
    val name: ALItemTitle,
)
