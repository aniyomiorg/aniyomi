package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALAddEntryResult(
    val data: ALAddEntryData,
)

@Serializable
data class ALAddEntryData(
    @SerialName("SaveMediaListEntry")
    val entry: ALAddEntryEntry,
)

@Serializable
data class ALAddEntryEntry(
    val id: Long,
)
