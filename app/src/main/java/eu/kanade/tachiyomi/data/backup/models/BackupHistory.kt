package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.history.manga.model.MangaHistory
import java.util.Date

@Serializable
data class BackupHistory(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var lastRead: Long,
    @ProtoNumber(3) var readDuration: Long = 0,
) {
    fun getHistoryImpl(): MangaHistory {
        return MangaHistory.create().copy(
            readAt = Date(lastRead),
            readDuration = readDuration,
        )
    }
}
