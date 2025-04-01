package eu.kanade.tachiyomi.data.track.kitsu.dto

import kotlinx.serialization.Serializable

@Serializable
data class KitsuMangaMetadata(
    val data: KitsuMangaMetadataData,
)

@Serializable
data class KitsuMangaMetadataData(
    val findLibraryEntryById: KitsuMangaMetadataById,
)

@Serializable
data class KitsuMangaMetadataById(
    val media: KitsuMangaMetadataMedia,
)

@Serializable
data class KitsuMangaMetadataMedia(
    val id: String,
    val titles: KitsuMangaTitle,
    val posterImage: KitsuMangaCover,
    val description: KitsuMangaDescription,
    val staff: KitsuMangaStaff,
)

@Serializable
data class KitsuMangaTitle(
    val preferred: String,
)

@Serializable
data class KitsuMangaCover(
    val original: KitsuMangaCoverUrl,
)

@Serializable
data class KitsuMangaCoverUrl(
    val url: String,
)

@Serializable
data class KitsuMangaDescription(
    val en: String?,
)

@Serializable
data class KitsuMangaStaff(
    val nodes: List<KitsuMangaStaffNode>,
)

@Serializable
data class KitsuMangaStaffNode(
    val role: String,
    val person: KitsuMangaStaffPerson,
)

@Serializable
data class KitsuMangaStaffPerson(
    val name: String,
)
