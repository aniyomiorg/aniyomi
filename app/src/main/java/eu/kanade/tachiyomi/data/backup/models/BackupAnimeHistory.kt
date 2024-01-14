package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.history.anime.model.AnimeHistory
import java.util.Date

@Serializable
data class BackupAnimeHistory(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var lastRead: Long,
    @ProtoNumber(3) var readDuration: Long = 0,
) {
    fun getHistoryImpl(): AnimeHistory {
        return AnimeHistory.create().copy(
            seenAt = Date(lastRead),
        )
    }
}

@Deprecated("Replaced with BackupHistory. This is retained for legacy reasons.")
@Serializable
data class BrokenBackupAnimeHistory(
    @ProtoNumber(0) var url: String,
    @ProtoNumber(1) var lastSeen: Long,
) {
    fun toBackupHistory(): BackupAnimeHistory {
        return BackupAnimeHistory(url, lastSeen)
    }
}
