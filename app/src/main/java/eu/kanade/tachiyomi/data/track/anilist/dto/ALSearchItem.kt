package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
data class ALSearchItem(
    val id: Long,
    val title: ALItemTitle,
    val coverImage: ItemCover,
    val description: String?,
    val format: String?,
    val status: String?,
    val startDate: ALFuzzyDate,
    val chapters: Long?,
    val episodes: Long?,
    val averageScore: Int?,
    val staff: ALStaff?,
    val studios: ALStudios?,
) {
    fun toALManga(): ALManga = ALManga(
        remoteId = id,
        title = title.userPreferred,
        imageUrl = coverImage.large,
        description = description,
        format = format?.replace("_", "-") ?: "",
        publishingStatus = status ?: "",
        startDateFuzzy = startDate.toEpochMilli(),
        totalChapters = chapters ?: 0,
        averageScore = averageScore ?: -1,
        staff = staff!!,
    )

    fun toALAnime(): ALAnime = ALAnime(
        remoteId = id,
        title = title.userPreferred,
        imageUrl = coverImage.large,
        description = description,
        format = format?.replace("_", "-") ?: "",
        publishingStatus = status ?: "",
        startDateFuzzy = startDate.toEpochMilli(),
        totalEpisodes = episodes ?: 0,
        averageScore = averageScore ?: -1,
        studios = studios!!,
    )
}

@Serializable
data class ALItemTitle(
    val userPreferred: String,
)

@Serializable
data class ItemCover(
    val large: String,
)

@Serializable
data class ALStaff(
    val edges: List<ALStaffEdge>,
)

@Serializable
data class ALStaffEdge(
    val role: String,
    val id: Int,
    val node: ALStaffNode,
)

@Serializable
data class ALStaffNode(
    val name: ALStaffName,
)

@Serializable
data class ALStaffName(
    val userPreferred: String?,
    val native: String?,
    val full: String?,
) {
    operator fun invoke(): String? {
        return userPreferred ?: full ?: native
    }
}

@Serializable
data class ALStudios(
    val edges: List<ALStudiosEdge>,
)

@Serializable
data class ALStudiosEdge(
    val isMain: Boolean,
    val node: ALStudiosNode,
)

@Serializable
data class ALStudiosNode(
    val name: String,
)
