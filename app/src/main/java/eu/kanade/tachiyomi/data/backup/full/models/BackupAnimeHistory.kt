package eu.kanade.tachiyomi.data.backup.full.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupAnimeHistory(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var lastSeen: Long,
)

@Deprecated("Replaced with BackupHistory. This is retained for legacy reasons.")
@Serializable
data class BrokenBackupAnimeHistory(
    @ProtoNumber(0) var url: String,
    @ProtoNumber(1) var lastSeen: Long,
)
