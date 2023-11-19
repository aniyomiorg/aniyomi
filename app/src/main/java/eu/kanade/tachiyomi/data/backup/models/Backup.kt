package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Backup(
    @ProtoNumber(1) val backupManga: List<BackupManga> = emptyList(),
    @ProtoNumber(2) var backupCategories: List<BackupCategory> = emptyList(),
    @ProtoNumber(3) val backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupAnimeCategories: List<BackupCategory> = emptyList(),
    // Bump by 100 to specify this is a 0.x value
    @ProtoNumber(100) var backupBrokenSources: List<BrokenBackupSource> = emptyList(),
    @ProtoNumber(101) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(102) var backupBrokenAnimeSources: List<BrokenBackupAnimeSource> = emptyList(),
    @ProtoNumber(103) var backupAnimeSources: List<BackupAnimeSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupExtensionPreferences: List<BackupExtensionPreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensions: List<BackupExtension> = emptyList(),
) {

    companion object {
        fun getBackupFilename(): String {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            return "aniyomi_$date.proto.gz"
        }
    }
}
