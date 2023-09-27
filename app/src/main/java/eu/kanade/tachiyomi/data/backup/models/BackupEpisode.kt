package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.items.episode.model.Episode

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
) {
    fun toEpisodeImpl(): Episode {
        return Episode.create().copy(
            url = this@BackupEpisode.url,
            name = this@BackupEpisode.name,
            episodeNumber = this@BackupEpisode.episodeNumber,
            scanlator = this@BackupEpisode.scanlator,
            seen = this@BackupEpisode.seen,
            bookmark = this@BackupEpisode.bookmark,
            lastSecondSeen = this@BackupEpisode.lastSecondSeen,
            totalSeconds = this@BackupEpisode.totalSeconds,
            dateFetch = this@BackupEpisode.dateFetch,
            dateUpload = this@BackupEpisode.dateUpload,
            sourceOrder = this@BackupEpisode.sourceOrder,
        )
    }
}

val backupEpisodeMapper = { _: Long, _: Long, url: String, name: String, scanlator: String?, seen: Boolean, bookmark: Boolean, lastSecondSeen: Long, totalSeconds: Long, episodeNumber: Float, source_order: Long, dateFetch: Long, dateUpload: Long ->
    BackupEpisode(
        url = url,
        name = name,
        episodeNumber = episodeNumber,
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        lastSecondSeen = lastSecondSeen,
        totalSeconds = totalSeconds,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = source_order,
    )
}
