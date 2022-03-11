package eu.kanade.tachiyomi.data.backup.full.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Backup(
    @ProtoNumber(3) val backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupCategoriesAnime: List<BackupCategory> = emptyList(),
    // Bump by 100 to specify this is a 0.x value
    @ProtoNumber(102) var backupBrokenAnimeSources: List<BrokenBackupAnimeSource> = emptyList(),
    @ProtoNumber(103) var backupAnimeSources: List<BackupAnimeSource> = emptyList()
)
