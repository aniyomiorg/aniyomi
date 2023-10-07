package eu.kanade.tachiyomi.util

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.backup.BackupManager
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import kotlinx.serialization.SerializationException
import okio.buffer
import okio.gzip
import okio.source
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer as FullBackupSerializer
import eu.kanade.tachiyomi.data.backup.full.models.BooleanPreferenceValue as FullBooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.FloatPreferenceValue as FullFloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.IntPreferenceValue as FullIntPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.LongPreferenceValue as FullLongPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringPreferenceValue as FullStringPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringSetPreferenceValue as FullStringSetPreferenceValue

object BackupUtil {
    /**
     * Decode a potentially-gzipped backup.
     */
    fun decodeBackup(context: Context, uri: Uri): Backup {
        val backupManager = BackupManager(context)

        val backupStringSource = context.contentResolver.openInputStream(uri)!!.source().buffer()

        val peeked = backupStringSource.peek()
        peeked.require(2)
        val id1id2 = peeked.readShort()
        val backupString = if (id1id2.toInt() == 0x1f8b) { // 0x1f8b is gzip magic bytes
            backupStringSource.gzip().buffer()
        } else {
            backupStringSource
        }.use { it.readByteArray() }

        return try {
            backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)
        } catch (e: SerializationException) {
            val fullBackup = backupManager.parser.decodeFromByteArray(FullBackupSerializer, backupString)
            val backupPreferences = fullBackup.backupPreferences.map {
                val value = when (it.value) {
                    is FullIntPreferenceValue -> IntPreferenceValue(it.value.value)
                    is FullLongPreferenceValue -> LongPreferenceValue(it.value.value)
                    is FullFloatPreferenceValue -> FloatPreferenceValue(it.value.value)
                    is FullBooleanPreferenceValue -> BooleanPreferenceValue(it.value.value)
                    is FullStringPreferenceValue -> StringPreferenceValue(it.value.value)
                    is FullStringSetPreferenceValue -> StringSetPreferenceValue(it.value.value)
                }
                BackupPreference(it.key, value)
            }
            Backup(
                fullBackup.backupManga,
                fullBackup.backupCategories,
                fullBackup.backupAnime,
                fullBackup.backupAnimeCategories,
                fullBackup.backupBrokenSources,
                fullBackup.backupSources,
                fullBackup.backupBrokenAnimeSources,
                fullBackup.backupAnimeSources,
                backupPreferences,
            )
        }
    }
}
