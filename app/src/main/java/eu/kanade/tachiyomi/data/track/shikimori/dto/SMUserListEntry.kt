package eu.kanade.tachiyomi.data.track.shikimori.dto

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi
import eu.kanade.tachiyomi.data.track.shikimori.toTrackStatus
import kotlinx.serialization.Serializable

@Serializable
data class SMUserListEntry(
    val id: Long,
    val chapters: Double,
    val episodes: Double,
    val score: Int,
    val status: String,
) {
    fun toMangaTrack(trackId: Long, manga: SMEntry): MangaTrack {
        return MangaTrack.create(trackId).apply {
            title = manga.name
            remote_id = this@SMUserListEntry.id
            total_chapters = manga.chapters!!
            library_id = this@SMUserListEntry.id
            last_chapter_read = this@SMUserListEntry.chapters
            score = this@SMUserListEntry.score.toDouble()
            status = toTrackStatus(this@SMUserListEntry.status)
            tracking_url = ShikimoriApi.BASE_URL + manga.url
        }
    }

    fun toAnimeTrack(trackId: Long, anime: SMEntry): AnimeTrack {
        return AnimeTrack.create(trackId).apply {
            title = anime.name
            remote_id = this@SMUserListEntry.id
            total_episodes = anime.episodes!!
            library_id = this@SMUserListEntry.id
            last_episode_seen = this@SMUserListEntry.episodes
            score = this@SMUserListEntry.score.toDouble()
            status = toTrackStatus(this@SMUserListEntry.status)
            tracking_url = ShikimoriApi.BASE_URL + anime.url
        }
    }
}
