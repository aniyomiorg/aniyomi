package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.database.models.anime.Anime
import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.data.database.models.manga.Chapter
import eu.kanade.tachiyomi.data.database.models.manga.Manga
import eu.kanade.tachiyomi.data.database.models.manga.MangaTrack
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.Job
import kotlinx.serialization.SerializationException
import okio.buffer
import okio.gzip
import okio.source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer as FullBackupSerializer
import eu.kanade.tachiyomi.data.backup.full.models.BooleanPreferenceValue as FullBooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.FloatPreferenceValue as FullFloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.IntPreferenceValue as FullIntPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.LongPreferenceValue as FullLongPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringPreferenceValue as FullStringPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringSetPreferenceValue as FullStringSetPreferenceValue

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
) {

    var job: Job? = null

    private var backupManager = BackupManager(context)

    private var restoreAmount = 0
    private var restoreProgress = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()
    private var animeSourceMapping: Map<Long, String> = emptyMap()

    private val errors = mutableListOf<Pair<Date, String>>()

    suspend fun restoreBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()
        restoreProgress = 0
        errors.clear()

        if (!performRestore(uri)) {
            return false
        }

        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(time, errors.size, logFile.parent, logFile.name)
        return true
    }

    fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("aniyomi_restore.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun performRestore(uri: Uri): Boolean {
        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().use { it.readByteArray() }

        // Sadly, this is necessary because of old "full" backups.
        val backup = try {
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

        restoreAmount = backup.backupManga.size + backup.backupAnime.size + 2 // +2 for categories

        // Restore categories
        if (backup.backupCategories.isNotEmpty()) {
            restoreCategories(backup.backupCategories)
        }

        if (backup.backupAnimeCategories.isNotEmpty()) {
            restoreAnimeCategories(backup.backupAnimeCategories)
        }

        // Store source mapping for error messages
        val backupMaps = backup.backupBrokenSources.map { BackupSource(it.name, it.sourceId) } + backup.backupSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name }

        val backupAnimeMaps = backup.backupBrokenAnimeSources.map { BackupAnimeSource(it.name, it.sourceId) } + backup.backupAnimeSources
        animeSourceMapping = backupAnimeMaps.associate { it.sourceId to it.name }

        // Restore individual manga
        backup.backupManga.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreManga(it, backup.backupCategories)
        }

        backup.backupAnime.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreAnime(it, backup.backupAnimeCategories)
        }

        // TODO: optionally trigger online library + tracker update

        if (backup.backupPreferences.isNotEmpty()) {
            restorePreferences(
                backup.backupPreferences,
                PreferenceManager.getDefaultSharedPreferences(context),
            )
        }

        if (backup.backupExtensionPreferences.isNotEmpty()) {
            restoreExtensionPreferences(backup.backupExtensionPreferences)
        }

        return true
    }

    private suspend fun restoreCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreCategories(backupCategories)

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.manga_categories))
    }

    private suspend fun restoreAnimeCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreAnimeCategories(backupCategories)

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.anime_categories))
    }

    private suspend fun restoreManga(backupManga: BackupManga, backupCategories: List<BackupCategory>) {
        val manga = backupManga.getMangaImpl()
        val chapters = backupManga.getChaptersImpl()
        val categories = backupManga.categories.map { it.toInt() }
        val history =
            backupManga.brokenHistory.map { BackupHistory(it.url, it.lastRead, it.readDuration) } + backupManga.history
        val tracks = backupManga.getTrackingImpl()

        try {
            val dbManga = backupManager.getMangaFromDatabase(manga.url, manga.source)
            if (dbManga == null) {
                // Manga not in database
                restoreExistingManga(manga, chapters, categories, history, tracks, backupCategories)
            } else {
                // Manga in database
                // Copy information from manga already in database
                backupManager.restoreExistingManga(manga, dbManga)
                // Fetch rest of manga information
                restoreNewManga(manga, chapters, categories, history, tracks, backupCategories)
            }
        } catch (e: Exception) {
            val sourceName = sourceMapping[manga.source] ?: manga.source.toString()
            errors.add(Date() to "${manga.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, manga.title)
    }

    /**
     * Fetches manga information
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private suspend fun restoreExistingManga(
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<MangaTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        val fetchedManga = backupManager.restoreNewManga(manga)
        fetchedManga.id ?: return

        backupManager.restoreChapters(fetchedManga, chapters)
        restoreExtras(fetchedManga, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreNewManga(
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<MangaTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        backupManager.restoreChapters(backupManga, chapters)
        restoreExtras(backupManga, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreExtras(manga: Manga, categories: List<Int>, history: List<BackupHistory>, tracks: List<MangaTrack>, backupCategories: List<BackupCategory>) {
        backupManager.restoreCategories(manga, categories, backupCategories)
        backupManager.restoreHistory(history)
        backupManager.restoreTracking(manga, tracks)
    }

    private suspend fun restoreAnime(backupAnime: BackupAnime, backupCategories: List<BackupCategory>) {
        val anime = backupAnime.getAnimeImpl()
        val episodes = backupAnime.getEpisodesImpl()
        val categories = backupAnime.categories.map { it.toInt() }
        val history =
            backupAnime.brokenHistory.map { BackupAnimeHistory(it.url, it.lastSeen) } + backupAnime.history
        val tracks = backupAnime.getTrackingImpl()

        try {
            val dbAnime = backupManager.getAnimeFromDatabase(anime.url, anime.source)
            if (dbAnime == null) {
                // Anime not in database
                restoreExistingAnime(anime, episodes, categories, history, tracks, backupCategories)
            } else {
                // Anime in database
                // Copy information from anime already in database
                backupManager.restoreExistingAnime(anime, dbAnime)
                // Fetch rest of anime information
                restoreNewAnime(anime, episodes, categories, history, tracks, backupCategories)
            }
        } catch (e: Exception) {
            val sourceName = sourceMapping[anime.source] ?: anime.source.toString()
            errors.add(Date() to "${anime.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, anime.title)
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @param episodes episodes of anime that needs updating
     * @param categories categories that need updating
     */
    private suspend fun restoreExistingAnime(
        anime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        val fetchedAnime = backupManager.restoreNewAnime(anime)
        fetchedAnime.id ?: return

        backupManager.restoreEpisodes(fetchedAnime, episodes)
        restoreExtras(fetchedAnime, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreNewAnime(
        backupAnime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        backupManager.restoreEpisodes(backupAnime, episodes)
        restoreExtras(backupAnime, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreExtras(anime: Anime, categories: List<Int>, history: List<BackupAnimeHistory>, tracks: List<AnimeTrack>, backupCategories: List<BackupCategory>) {
        backupManager.restoreAnimeCategories(anime, categories, backupCategories)
        backupManager.restoreAnimeHistory(history)
        backupManager.restoreAnimeTracking(anime, tracks)
    }

    private fun restorePreferences(preferences: List<BackupPreference>, sharedPrefs: SharedPreferences) {
        preferences.forEach { pref ->
            when (pref.value) {
                is IntPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Int?) {
                        sharedPrefs.edit().putInt(pref.key, pref.value.value).apply()
                    }
                }
                is LongPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Long?) {
                        sharedPrefs.edit().putLong(pref.key, pref.value.value).apply()
                    }
                }
                is FloatPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Float?) {
                        sharedPrefs.edit().putFloat(pref.key, pref.value.value).apply()
                    }
                }
                is StringPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is String?) {
                        sharedPrefs.edit().putString(pref.key, pref.value.value).apply()
                    }
                }
                is BooleanPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Boolean?) {
                        sharedPrefs.edit().putBoolean(pref.key, pref.value.value).apply()
                    }
                }
                is StringSetPreferenceValue -> {
                    if (sharedPrefs.all[pref.key] is Set<*>?) {
                        sharedPrefs.edit().putStringSet(pref.key, pref.value.value).apply()
                    }
                }
            }
        }
    }

    private fun restoreExtensionPreferences(prefs: List<BackupExtensionPreferences>) {
        prefs.forEach {
            val sharedPrefs = context.getSharedPreferences(it.name, 0x0)
            restorePreferences(it.prefs, sharedPrefs)
        }
    }

    /**
     * Called to update dialog in [BackupConst]
     *
     * @param progress restore progress
     * @param amount total restoreAmount of anime and manga
     * @param title title of restored anime and manga
     */
    private fun showRestoreProgress(progress: Int, amount: Int, title: String) {
        notifier.showRestoreProgress(title, progress, amount)
    }
}
