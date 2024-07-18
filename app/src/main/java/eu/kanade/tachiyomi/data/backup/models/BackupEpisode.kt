package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.items.episode.model.Episode

@Suppress("MagicNumber")
@Serializable
data class BackupEpisode(
    // in 1.x some of these values have different names
    // url is called key in 1.x
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var scanlator: String? = null,
    @ProtoNumber(4) var seen: Boolean = false,
    @ProtoNumber(5) var bookmark: Boolean = false,
    // lastPageRead is called progress in 1.x
    @ProtoNumber(6) var lastSecondSeen: Long = 0,
    @ProtoNumber(16) var totalSeconds: Long = 0,
    @ProtoNumber(7) var dateFetch: Long = 0,
    @ProtoNumber(8) var dateUpload: Long = 0,
    // episodeNumber is called number is 1.x
    @ProtoNumber(9) var episodeNumber: Float = 0F,
    @ProtoNumber(10) var sourceOrder: Long = 0,
    @ProtoNumber(11) var lastModifiedAt: Long = 0,
    @ProtoNumber(12) var version: Long = 0,
) {
    fun toEpisodeImpl(): Episode {
        return Episode.create().copy(
            url = this@BackupEpisode.url,
            name = this@BackupEpisode.name,
            episodeNumber = this@BackupEpisode.episodeNumber.toDouble(),
            scanlator = this@BackupEpisode.scanlator,
            seen = this@BackupEpisode.seen,
            bookmark = this@BackupEpisode.bookmark,
            lastSecondSeen = this@BackupEpisode.lastSecondSeen,
            totalSeconds = this@BackupEpisode.totalSeconds,
            dateFetch = this@BackupEpisode.dateFetch,
            dateUpload = this@BackupEpisode.dateUpload,
            sourceOrder = this@BackupEpisode.sourceOrder,
            lastModifiedAt = this@BackupEpisode.lastModifiedAt,
            version = this@BackupEpisode.version,
        )
    }
}

val backupEpisodeMapper = {
        _: Long,
        _: Long,
        url: String,
        name: String,
        scanlator: String?,
        seen: Boolean,
        bookmark: Boolean,
        lastSecondSeen: Long,
        totalSeconds: Long,
        episodeNumber: Double,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        lastModifiedAt: Long,
        version: Long,
        _: Long,
    ->
    BackupEpisode(
        url = url,
        name = name,
        episodeNumber = episodeNumber.toFloat(),
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        lastSecondSeen = lastSecondSeen,
        totalSeconds = totalSeconds,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
    )
}
