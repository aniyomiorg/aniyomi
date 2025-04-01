package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.track.anime.model.AnimeTrack

@Serializable
data class BackupAnimeTracking(
    // in 1.x some of these values have different types or names
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
    @ProtoNumber(12) var private: Boolean = false,
    @ProtoNumber(100) var mediaId: Long = 0,
) {

    @Suppress("DEPRECATION")
    fun getTrackImpl(): AnimeTrack {
        return AnimeTrack(
            id = -1,
            animeId = -1,
            trackerId = this@BackupAnimeTracking.syncId.toLong(),
            remoteId = if (this@BackupAnimeTracking.mediaIdInt != 0) {
                this@BackupAnimeTracking.mediaIdInt.toLong()
            } else {
                this@BackupAnimeTracking.mediaId
            },
            libraryId = this@BackupAnimeTracking.libraryId,
            title = this@BackupAnimeTracking.title,
            lastEpisodeSeen = this@BackupAnimeTracking.lastEpisodeSeen.toDouble(),
            totalEpisodes = this@BackupAnimeTracking.totalEpisodes.toLong(),
            score = this@BackupAnimeTracking.score.toDouble(),
            status = this@BackupAnimeTracking.status.toLong(),
            startDate = this@BackupAnimeTracking.startedWatchingDate,
            finishDate = this@BackupAnimeTracking.finishedWatchingDate,
            remoteUrl = this@BackupAnimeTracking.trackingUrl,
            private = this@BackupAnimeTracking.private,
        )
    }
}

val backupAnimeTrackMapper = {
        _id: Long,
        anime_id: Long,
        syncId: Long,
        mediaId: Long,
        libraryId: Long?,
        title: String,
        lastEpisodeSeen: Double,
        totalEpisodes: Long,
        status: Long,
        score: Double,
        remoteUrl: String,
        startDate: Long,
        finishDate: Long,
        private: Boolean,
    ->
    BackupAnimeTracking(
        syncId = syncId.toInt(),
        mediaId = mediaId,
        // forced not null so its compatible with 1.x backup system
        libraryId = libraryId ?: 0,
        title = title,
        lastEpisodeSeen = lastEpisodeSeen.toFloat(),
        totalEpisodes = totalEpisodes.toInt(),
        score = score.toFloat(),
        status = status.toInt(),
        startedWatchingDate = startDate,
        finishedWatchingDate = finishDate,
        trackingUrl = remoteUrl,
        private = private,
    )
}
