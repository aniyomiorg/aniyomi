package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALMangaMetadata(
    val data: ALMangaMetadataData,
)

@Serializable
data class ALMangaMetadataData(
    @SerialName("Media")
    val media: ALMangaMetadataMedia,
)

@Serializable
data class ALMangaMetadataMedia(
    val id: Long,
    val title: ALItemTitle,
    val coverImage: ItemCover,
    val description: String?,
    val staff: ALStaff,
)

@Serializable
data class ALStaff(
    val edges: List<ALStaffEdge>,
)

@Serializable
data class ALStaffEdge(
    val role: String,
    val node: ALStaffNode,
)

@Serializable
data class ALStaffNode(
    val name: ALItemTitle,
)
