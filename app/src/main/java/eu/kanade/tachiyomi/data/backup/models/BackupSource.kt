package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.MangaSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BrokenBackupSource(
    @ProtoNumber(0) var name: String = "",
    @ProtoNumber(1) var sourceId: Long,
)

@Serializable
data class BackupSource(
    @ProtoNumber(1) var name: String = "",
    @ProtoNumber(2) var sourceId: Long,
) {
    companion object {
        fun copyFrom(source: MangaSource): BackupSource {
            return BackupSource(
                name = source.name,
                sourceId = source.id,
            )
        }
    }
}
