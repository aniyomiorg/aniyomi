package eu.kanade.tachiyomi.data.track.jellyfin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDto(
    @SerialName("Name") val name: String,
    @SerialName("Id") val id: String,
    @SerialName("UserData") val userData: UserDataDto,
    @SerialName("IndexNumber") val indexNumber: Long? = null,
)

@Serializable
data class UserDataDto(
    @SerialName("Played") val played: Boolean,
)

@Serializable
data class ItemsDto(
    @SerialName("Items") val items: List<ItemDto>,
)
