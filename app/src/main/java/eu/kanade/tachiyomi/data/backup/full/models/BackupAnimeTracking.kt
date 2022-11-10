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
    @Deprecated("Use mediaId instead", level = DeprecationLevel.WARNING)
    @ProtoNumber(3)
    var mediaIdInt: Int = 0,
    // trackingUrl is called mediaUrl in 1.x
    @ProtoNumber(4) var trackingUrl: String = "",
    @ProtoNumber(5) var title: String = "",
    // lastEpisodeSeen is called last seen, and it has been changed to a float in 1.x
    @ProtoNumber(6) var lastEpisodeSeen: Float = 0F,
    @ProtoNumber(7) var totalEpisodes: Int = 0,
    @ProtoNumber(8) var score: Float = 0F,
    @ProtoNumber(9) var status: Int = 0,
    // startedReadingDate is called startReadTime in 1.x
    @ProtoNumber(10) var startedWatchingDate: Long = 0,
    // finishedReadingDate is called endReadTime in 1.x
    @ProtoNumber(11) var finishedWatchingDate: Long = 0,
    @ProtoNumber(100) var mediaId: Long = 0,
) {
    fun getTrackingImpl(): AnimeTrackImpl {
        return AnimeTrackImpl().apply {
            sync_id = this@BackupAnimeTracking.syncId
            @Suppress("DEPRECATION")
            media_id = if (this@BackupAnimeTracking.mediaIdInt != 0) {
                this@BackupAnimeTracking.mediaIdInt.toLong()
            } else {
                this@BackupAnimeTracking.mediaId
            }
            library_id = this@BackupAnimeTracking.libraryId
            title = this@BackupAnimeTracking.title
            last_episode_seen = this@BackupAnimeTracking.lastEpisodeSeen
            total_episodes = this@BackupAnimeTracking.totalEpisodes
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
                lastEpisodeSeen = track.last_episode_seen,
                totalEpisodes = track.total_episodes,
                score = track.score,
                status = track.status,
                startedWatchingDate = track.started_watching_date,
                finishedWatchingDate = track.finished_watching_date,
                trackingUrl = track.tracking_url,
            )
        }
    }
}

val backupAnimeTrackMapper = { _id: Long, anime_id: Long, syncId: Long, mediaId: Long, libraryId: Long?, title: String, lastEpisodeSeen: Double, totalEpisodes: Long, status: Long, score: Float, remoteUrl: String, startDate: Long, finishDate: Long ->
    BackupAnimeTracking(
        syncId = syncId.toInt(),
        mediaId = mediaId,
        // forced not null so its compatible with 1.x backup system
        libraryId = libraryId ?: 0,
        title = title,
        lastEpisodeSeen = lastEpisodeSeen.toFloat(),
        totalEpisodes = totalEpisodes.toInt(),
        score = score,
        status = status.toInt(),
        startedWatchingDate = startDate,
        finishedWatchingDate = finishDate,
        trackingUrl = remoteUrl,
    )
}
