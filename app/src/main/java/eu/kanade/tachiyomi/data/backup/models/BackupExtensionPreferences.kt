package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupExtensionPreferences(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val prefs: List<BackupPreference>,
)
