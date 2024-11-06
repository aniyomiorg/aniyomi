package eu.kanade.tachiyomi.data.track.simkl.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.simkl.SimklApi.Companion.POSTERS_URL
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimklSearchResult(
    val ids: SimlkSearchResultIds,
    @SerialName("title_romaji")
    val titleRomaji: String?,
    val title: String?,
    @SerialName("ep_count")
    val epCount: Long?,
    val poster: String?,
    @SerialName("all_titles")
    val allTitles: List<String>?,
    val url: String,
    val status: String?,
    val type: String?,
    val year: Int?,
) {
    fun toTrackSearch(fallbackType: String): AnimeTrackSearch {
        return AnimeTrackSearch.create(TrackerManager.SIMKL).apply {
            remote_id = ids.simklId
            title = titleRomaji ?: this@SimklSearchResult.title!!
            total_episodes = epCount ?: 1
            cover_url = poster?.let { "$POSTERS_URL${it}_m.webp" } ?: ""
            summary = allTitles?.joinToString("\n", prefix = "All titles:\n") ?: ""
            tracking_url = url
            publishing_status = this@SimklSearchResult.status ?: "ended"
            publishing_type = type ?: fallbackType
            start_date = year?.toString() ?: ""
        }
    }
}

@Serializable
data class SimlkSearchResultIds(
    @SerialName("simkl_id")
    val simklId: Long,
)
