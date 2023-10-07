package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupExtension(
    @ProtoNumber(1) val pkgName: String,
    @ProtoNumber(2) val apk: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupExtension

        if (pkgName != other.pkgName) return false
        if (!apk.contentEquals(other.apk)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pkgName.hashCode()
        result = 31 * result + apk.contentHashCode()
        return result
    }
}
