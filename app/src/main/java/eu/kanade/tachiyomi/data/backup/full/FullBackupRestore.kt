package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.AbstractBackupRestore
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupManga
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
import okio.buffer
import okio.gzip
import okio.source
import java.util.Date

class FullBackupRestore(context: Context, notifier: BackupNotifier) : AbstractBackupRestore<FullBackupManager>(context, notifier) {

    override suspend fun performRestore(uri: Uri): Boolean {
        backupManager = FullBackupManager(context)

        @Suppress("BlockingMethodInNonBlockingContext")
        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().use { it.readByteArray() }
        val backup = backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

        restoreAmount = backup.backupManga.size + backup.backupAnime.size + 2 // +2 for categories

        // Restore categories
        if (backup.backupCategories.isNotEmpty()) {
            restoreCategories(backup.backupCategories)
        }

        // Restore categories
        if (backup.backupCategoriesAnime.isNotEmpty()) {
            restoreCategoriesAnime(backup.backupCategoriesAnime)
        }

        // Store source mapping for error messages
        val backupMaps = backup.backupBrokenSources.map { BackupSource(it.name, it.sourceId) } + backup.backupSources
        val backupMapsAnime = backup.backupBrokenAnimeSources.map { BackupAnimeSource(it.name, it.sourceId) } + backup.backupAnimeSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name } +
            backupMapsAnime.associate { it.sourceId to it.name }

        // Restore individual manga
        backup.backupManga.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreManga(it, backup.backupCategories)
        }

        // Restore individual anime
        backup.backupAnime.forEach {
            if (job?.isActive != true) {
                return false
            }

            restoreAnime(it, backup.backupCategoriesAnime)
        }

        // Restore preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val flowPrefs = FlowSharedPreferences(prefs)
        backup.backupPreferences.forEach { pref ->
            when (pref.value) {
                is IntPreferenceValue -> {
                    prefs.edit().putInt(pref.key, pref.value.value).apply()
                }
                is LongPreferenceValue -> {
                    prefs.edit().putLong(pref.key, pref.value.value).apply()
                }
                is FloatPreferenceValue -> {
                    prefs.edit().putFloat(pref.key, pref.value.value).apply()
                }
                is StringPreferenceValue -> {
                    prefs.edit().putString(pref.key, pref.value.value).apply()
                }
                is BooleanPreferenceValue -> {
                    prefs.edit().putBoolean(pref.key, pref.value.value).apply()
                }
                is StringSetPreferenceValue -> {
                    prefs.edit().putStringSet(pref.key, pref.value.value).apply()
                }
            }
        }

        // TODO: optionally trigger online library + tracker update

        return true
    }

    private suspend fun restoreCategories(backupCategories: List<BackupCategory>) {
        backupManager.restoreCategories(backupCategories)

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private suspend fun restoreCategoriesAnime(backupCategories: List<BackupCategory>) {
        backupManager.restoreCategoriesAnime(backupCategories)

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.anime_categories))
    }

    private suspend fun restoreManga(backupManga: BackupManga, backupCategories: List<BackupCategory>) {
        val manga = backupManga.getMangaImpl()
        val chapters = backupManga.getChaptersImpl()
        val categories = backupManga.categories.map { it.toInt() }
        val history = backupManga.brokenHistory.map { BackupHistory(it.url, it.lastRead) } + backupManga.history
        val tracks = backupManga.getTrackingImpl()

        try {
            restoreMangaData(manga, chapters, categories, history, tracks, backupCategories)
        } catch (e: Exception) {
            val sourceName = sourceMapping[manga.source] ?: manga.source.toString()
            errors.add(Date() to "${manga.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, manga.title)
    }

    private suspend fun restoreAnime(backupAnime: BackupAnime, backupCategories: List<BackupCategory>) {
        val anime = backupAnime.getAnimeImpl()
        val episodes = backupAnime.getEpisodesImpl()
        val categories = backupAnime.categories.map { it.toInt() }
        val history = backupAnime.brokenHistory.map { BackupAnimeHistory(it.url, it.lastSeen) } + backupAnime.history
        val tracks = backupAnime.getTrackingImpl()

        try {
            restoreAnimeData(anime, episodes, categories, history, tracks, backupCategories)
        } catch (e: Exception) {
            val sourceName = sourceMapping[anime.source] ?: anime.source.toString()
            errors.add(Date() to "${anime.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, anime.title)
    }

    /**
     * Returns a manga restore observable
     *
     * @param manga manga data from json
     * @param chapters chapters data from json
     * @param categories categories data from json
     * @param history history data from json
     * @param tracks tracking data from json
     */
    private suspend fun restoreMangaData(
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbManga = backupManager.getMangaFromDatabase(manga.url, manga.source)
        if (dbManga == null) {
            // Manga not in database
            restoreMangaFetch(manga, chapters, categories, history, tracks, backupCategories)
        } else {
            // Manga in database
            // Copy information from manga already in database
            backupManager.restoreMangaNoFetch(manga, dbManga)
            // Fetch rest of manga information
            restoreMangaNoFetch(manga, chapters, categories, history, tracks, backupCategories)
        }
    }

    /**
     * Returns a manga restore observable
     *
     * @param manga manga data from json
     * @param chapters chapters data from json
     * @param categories categories data from json
     * @param history history data from json
     * @param tracks tracking data from json
     */
    private suspend fun restoreAnimeData(
        anime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbAnime = backupManager.getAnimeFromDatabase(anime.url, anime.source)
        if (dbAnime == null) {
            // Anime not in database
            restoreAnimeFetch(anime, episodes, categories, history, tracks, backupCategories)
        } else {
            // Anime in database
            // Copy information from anime already in database
            backupManager.restoreAnimeNoFetch(anime, dbAnime)
            // Fetch rest of anime information
            restoreAnimeNoFetch(anime, episodes, categories, history, tracks, backupCategories)
        }
    }

    /**
     * Fetches manga information
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private suspend fun restoreMangaFetch(
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
    ) {
        try {
            val fetchedManga = backupManager.restoreManga(manga)
            fetchedManga.id ?: return

            backupManager.restoreChaptersForManga(fetchedManga, chapters)

            restoreExtraForManga(fetchedManga, categories, history, tracks, backupCategories)
        } catch (e: Exception) {
            errors.add(Date() to "${manga.title} - ${e.message}")
        }
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @param episodes episodes of anime that needs updating
     * @param categories categories that need updating
     */
    private suspend fun restoreAnimeFetch(
        anime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        try {
            val fetchedAnime = backupManager.restoreAnime(anime)
            fetchedAnime.id ?: return

            backupManager.restoreEpisodesForAnime(fetchedAnime, episodes)

            restoreExtraForAnime(fetchedAnime, categories, history, tracks, backupCategories)
        } catch (e: Exception) {
            errors.add(Date() to "${anime.title} - ${e.message}")
        }
    }

    private suspend fun restoreMangaNoFetch(
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
    ) {
        backupManager.restoreChaptersForManga(backupManga, chapters)

        restoreExtraForManga(backupManga, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreAnimeNoFetch(
        backupAnime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>,
    ) {
        backupManager.restoreEpisodesForAnime(backupAnime, episodes)

        restoreExtraForAnime(backupAnime, categories, history, tracks, backupCategories)
    }

    private suspend fun restoreExtraForManga(manga: Manga, categories: List<Int>, history: List<BackupHistory>, tracks: List<Track>, backupCategories: List<BackupCategory>) {
        // Restore categories
        backupManager.restoreCategoriesForManga(manga, categories, backupCategories)

        // Restore history
        backupManager.restoreHistoryForManga(history)

        // Restore tracking
        backupManager.restoreTrackForManga(manga, tracks)
    }

    private suspend fun restoreExtraForAnime(anime: Anime, categories: List<Int>, history: List<BackupAnimeHistory>, tracks: List<AnimeTrack>, backupCategories: List<BackupCategory>) {
        // Restore categories
        backupManager.restoreCategoriesForAnime(anime, categories, backupCategories)

        // Restore history
        backupManager.restoreHistoryForAnime(history)

        // Restore tracking
        backupManager.restoreTrackForAnime(anime, tracks)
    }
}
