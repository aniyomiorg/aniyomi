package eu.kanade.tachiyomi.data.backup.full.models

import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.AnimeTrackImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupAnimeTracking(
    // in 1.x some of these values have different types or names
    // syncId is called siteId in 1,x
    @ProtoNumber(1) var syncId: Int,
    // LibraryId is not null in 1.x
    @ProtoNumber(2) var libraryId: Long,
    @ProtoNumber(3) var mediaId: Int = 0,
    // trackingUrl is called mediaUrl in 1.x
    @ProtoNumber(4) var trackingUrl: String = "",
    @ProtoNumber(5) var title: String = "",
    // lastChapterRead is called last read, and it has been changed to a float in 1.x
    @ProtoNumber(6) var lastChapterRead: Float = 0F,
    @ProtoNumber(7) var totalChapters: Int = 0,
    @ProtoNumber(8) var score: Float = 0F,
    @ProtoNumber(9) var status: Int = 0,
    // startedReadingDate is called startReadTime in 1.x
    @ProtoNumber(10) var startedWatchingDate: Long = 0,
    // finishedReadingDate is called endReadTime in 1.x
    @ProtoNumber(11) var finishedWatchingDate: Long = 0,
) {
    fun getTrackingImpl(): AnimeTrackImpl {
        return AnimeTrackImpl().apply {
            sync_id = this@BackupAnimeTracking.syncId
            media_id = this@BackupAnimeTracking.mediaId
            library_id = this@BackupAnimeTracking.libraryId
            title = this@BackupAnimeTracking.title
            last_episode_seen = this@BackupAnimeTracking.lastChapterRead
            total_episodes = this@BackupAnimeTracking.totalChapters
            score = this@BackupAnimeTracking.score
            status = this@BackupAnimeTracking.status
            started_watching_date = this@BackupAnimeTracking.startedWatchingDate
            finished_watching_date = this@BackupAnimeTracking.finishedWatchingDate
            tracking_url = this@BackupAnimeTracking.trackingUrl
        }
    }

    companion object {
        fun copyFrom(track: AnimeTrack): BackupAnimeTracking {
            return BackupAnimeTracking(
                syncId = track.sync_id,
                mediaId = track.media_id,
                // forced not null so its compatible with 1.x backup system
                libraryId = track.library_id!!,
                title = track.title,
                // convert to float for 1.x
                lastChapterRead = track.last_episode_seen,
                totalChapters = track.total_episodes,
                score = track.score,
                status = track.status,
                startedWatchingDate = track.started_watching_date,
                finishedWatchingDate = track.finished_watching_date,
                trackingUrl = track.tracking_url,
            )
        }
    }
}
