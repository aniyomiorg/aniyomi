package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.source.manga.MangaSourceManager
import kotlinx.serialization.SerializationException
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer as FullBackupSerializer
import eu.kanade.tachiyomi.data.backup.full.models.BooleanPreferenceValue as FullBooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.FloatPreferenceValue as FullFloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.IntPreferenceValue as FullIntPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.LongPreferenceValue as FullLongPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringPreferenceValue as FullStringPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringSetPreferenceValue as FullStringSetPreferenceValue

class BackupFileValidator(
    private val mangaSourceManager: MangaSourceManager = Injekt.get(),
    private val animeSourceManager: AnimeSourceManager = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if manga cannot be found.
     * @return List of missing sources or missing trackers.
     */
    fun validate(context: Context, uri: Uri): Results {
        val backupManager = BackupManager(context)

        val backup = try {
            val backupString =
                context.contentResolver.openInputStream(uri)!!.source().gzip().buffer()
                    .use { it.readByteArray() }
            // Sadly, this is necessary because of old "full" backups.
            try {
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
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        if (backup.backupManga.isEmpty() && backup.backupAnime.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.invalid_backup_file_missing_manga))
        }

        val sources = backup.backupSources.associate { it.sourceId to it.name }
        val animesources = backup.backupAnimeSources.associate { it.sourceId to it.name }
        val missingSources = sources
            .filter { mangaSourceManager.get(it.key) == null }
            .values.map {
                val id = it.toLongOrNull()
                if (id == null) {
                    it
                } else {
                    mangaSourceManager.getOrStub(id).toString()
                }
            }
            .distinct()
            .sorted() +
            animesources
                .filter { animeSourceManager.get(it.key) == null }
                .values.map {
                    val id = it.toLongOrNull()
                    if (id == null) {
                        it
                    } else {
                        animeSourceManager.getOrStub(id).toString()
                    }
                }
                .distinct()
                .sorted()

        val trackers = backup.backupManga
            .flatMap { it.tracking }
            .map { it.syncId }
            .distinct() + backup.backupAnime
            .flatMap { it.tracking }
            .map { it.syncId }
            .distinct()
        val missingTrackers = trackers
            .mapNotNull { trackManager.getService(it.toLong()) }
            .filter { !it.isLogged }
            .map { context.getString(it.nameRes()) }
            .sorted()

        return Results(missingSources, missingTrackers)
    }

    data class Results(val missingSources: List<String>, val missingTrackers: List<String>)
}
