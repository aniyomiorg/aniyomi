package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupManga
import eu.kanade.tachiyomi.data.backup.full.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.full.models.BackupSource
import eu.kanade.tachiyomi.data.backup.full.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.full.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import kotlinx.coroutines.Job
import okio.buffer
import okio.gzip
import okio.source
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val backup = backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

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
            restorePreferences(backup.backupPreferences)
        }

        return true
    }

    private suspend fun restoreCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreCategories(backupCategories)

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
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
        tracks: List<Track>,
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
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
    ) {
        backupManager.restoreChapters(backupManga, chapters)
        restoreExtras(backupManga, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreExtras(manga: Manga, categories: List<Int>, history: List<BackupHistory>, tracks: List<Track>, backupCategories: List<BackupCategory>) {
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

    private fun restorePreferences(preferences: List<BackupPreference>) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.forEach { pref ->
            when (pref.value) {
                is IntPreferenceValue -> {
                    if (prefs.all[pref.key] is Int?) {
                        prefs.edit().putInt(pref.key, pref.value.value).apply()
                    }
                }
                is LongPreferenceValue -> {
                    if (prefs.all[pref.key] is Long?) {
                        prefs.edit().putLong(pref.key, pref.value.value).apply()
                    }
                }
                is FloatPreferenceValue -> {
                    if (prefs.all[pref.key] is Float?) {
                        prefs.edit().putFloat(pref.key, pref.value.value).apply()
                    }
                }
                is StringPreferenceValue -> {
                    if (prefs.all[pref.key] is String?) {
                        prefs.edit().putString(pref.key, pref.value.value).apply()
                    }
                }
                is BooleanPreferenceValue -> {
                    if (prefs.all[pref.key] is Boolean?) {
                        prefs.edit().putBoolean(pref.key, pref.value.value).apply()
                    }
                }
                is StringSetPreferenceValue -> {
                    if (prefs.all[pref.key] is Set<*>?) {
                        prefs.edit().putStringSet(pref.key, pref.value.value).apply()
                    }
                }
            }
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
