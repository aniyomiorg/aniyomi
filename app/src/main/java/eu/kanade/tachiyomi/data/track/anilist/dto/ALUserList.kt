package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALUserListEntryQueryResult(
    val data: ALUserListEntryPage,
)

@Serializable
data class ALUserListEntryPage(
    @SerialName("Page")
    val page: ALUserListMediaList,
)

@Serializable
data class ALUserListMediaList(
    val mediaList: List<ALUserListItem>,
)

@Serializable
data class ALUserListItem(
    val id: Long,
    val status: String,
    val scoreRaw: Int,
    val progress: Int,
    val startedAt: ALFuzzyDate,
    val completedAt: ALFuzzyDate,
    val media: ALSearchItem,
) {
    fun toALUserManga(): ALUserManga {
        return ALUserManga(
            libraryId = this@ALUserListItem.id,
            listStatus = status,
            scoreRaw = scoreRaw,
            chaptersRead = progress,
            startDateFuzzy = startedAt.toEpochMilli(),
            completedDateFuzzy = completedAt.toEpochMilli(),
            manga = media.toALManga(),
        )
    }

    fun toALUserAnime(): ALUserAnime {
        return ALUserAnime(
            libraryId = this@ALUserListItem.id,
            listStatus = status,
            scoreRaw = scoreRaw,
            episodesSeen = progress,
            startDateFuzzy = startedAt.toEpochMilli(),
            completedDateFuzzy = completedAt.toEpochMilli(),
            anime = media.toALAnime(),
        )
    }
}
