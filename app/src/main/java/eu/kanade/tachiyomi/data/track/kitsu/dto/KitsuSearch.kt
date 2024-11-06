package eu.kanade.tachiyomi.data.track.kitsu.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class KitsuSearchResult(
    val media: KitsuSearchResultData,
)

@Serializable
data class KitsuSearchResultData(
    val key: String,
)

@Serializable
data class KitsuAlgoliaSearchResult(
    val hits: List<KitsuAlgoliaSearchItem>,
)

@Serializable
data class KitsuAlgoliaSearchItem(
    val id: Long,
    val canonicalTitle: String,
    val chapterCount: Long?,
    val episodeCount: Long?,
    val subtype: String?,
    val posterImage: KitsuSearchItemCover?,
    val synopsis: String?,
    val averageRating: Double?,
    val startDate: Long?,
    val endDate: Long?,
) {
    fun toMangaTrack(): MangaTrackSearch {
        return MangaTrackSearch.create(TrackerManager.KITSU).apply {
            remote_id = this@KitsuAlgoliaSearchItem.id
            title = canonicalTitle
            total_chapters = chapterCount ?: 0
            cover_url = posterImage?.original ?: ""
            summary = synopsis ?: ""
            tracking_url = KitsuApi.mangaUrl(remote_id)
            score = averageRating ?: -1.0
            publishing_status = if (endDate == null) "Publishing" else "Finished"
            publishing_type = subtype ?: ""
            start_date = startDate?.let {
                val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                outputDf.format(Date(it * 1000))
            } ?: ""
        }
    }

    fun toAnimeTrack(): AnimeTrackSearch {
        return AnimeTrackSearch.create(TrackerManager.KITSU).apply {
            remote_id = this@KitsuAlgoliaSearchItem.id
            title = canonicalTitle
            total_episodes = episodeCount ?: 0
            cover_url = posterImage?.original ?: ""
            summary = synopsis ?: ""
            tracking_url = KitsuApi.animeUrl(remote_id)
            score = averageRating ?: -1.0
            publishing_status = if (endDate == null) "Publishing" else "Finished"
            publishing_type = subtype ?: ""
            start_date = startDate?.let {
                val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                outputDf.format(Date(it * 1000))
            } ?: ""
        }
    }
}
