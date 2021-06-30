package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.AbstractBackupRestore
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.full.models.*
import eu.kanade.tachiyomi.data.database.models.*
import okio.buffer
import okio.gzip
import okio.source
import java.util.Date

class FullBackupRestore(context: Context, notifier: BackupNotifier) : AbstractBackupRestore<FullBackupManager>(context, notifier) {

    override suspend fun performRestore(uri: Uri): Boolean {
        backupManager = FullBackupManager(context)

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
        sourceMapping = backup.backupSources.map { it.sourceId to it.name }.toMap() +
            backup.backupAnimeSources.map { it.sourceId to it.name }.toMap()

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

        // TODO: optionally trigger online library + tracker update

        return true
    }

    private fun restoreCategories(backupCategories: List<BackupCategory>) {
        db.inTransaction {
            backupManager.restoreCategories(backupCategories)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private fun restoreCategoriesAnime(backupCategories: List<BackupCategory>) {
        animedb.inTransaction {
            backupManager.restoreCategoriesAnime(backupCategories)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private fun restoreManga(backupManga: BackupManga, backupCategories: List<BackupCategory>) {
        val manga = backupManga.getMangaImpl()
        val chapters = backupManga.getChaptersImpl()
        val categories = backupManga.categories
        val history = backupManga.history
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

    private fun restoreAnime(backupAnime: BackupAnime, backupCategories: List<BackupCategory>) {
        val anime = backupAnime.getAnimeImpl()
        val episodes = backupAnime.getEpisodesImpl()
        val categories = backupAnime.categories
        val history = backupAnime.history
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
    private fun restoreMangaData(
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>
    ) {
        db.inTransaction {
            val dbManga = backupManager.getMangaFromDatabase(manga)
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
    private fun restoreAnimeData(
        anime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>
    ) {
        animedb.inTransaction {
            val dbAnime = backupManager.getAnimeFromDatabase(anime)
            if (dbAnime == null) {
                // Manga not in database
                restoreAnimeFetch(anime, episodes, categories, history, tracks, backupCategories)
            } else {
                // Manga in database
                // Copy information from manga already in database
                backupManager.restoreAnimeNoFetch(anime, dbAnime)
                // Fetch rest of manga information
                restoreAnimeNoFetch(anime, episodes, categories, history, tracks, backupCategories)
            }
        }
    }

    /**
     * Fetches manga information
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private fun restoreMangaFetch(
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>
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
    private fun restoreAnimeFetch(
        anime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>
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

    private fun restoreMangaNoFetch(
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>
    ) {
        backupManager.restoreChaptersForManga(backupManga, chapters)

        restoreExtraForManga(backupManga, categories, history, tracks, backupCategories)
    }

    private fun restoreAnimeNoFetch(
        backupAnime: Anime,
        episodes: List<Episode>,
        categories: List<Int>,
        history: List<BackupAnimeHistory>,
        tracks: List<AnimeTrack>,
        backupCategories: List<BackupCategory>
    ) {
        backupManager.restoreEpisodesForAnime(backupAnime, episodes)

        restoreExtraForAnime(backupAnime, categories, history, tracks, backupCategories)
    }

    private fun restoreExtraForManga(manga: Manga, categories: List<Int>, history: List<BackupHistory>, tracks: List<Track>, backupCategories: List<BackupCategory>) {
        // Restore categories
        backupManager.restoreCategoriesForManga(manga, categories, backupCategories)

        // Restore history
        backupManager.restoreHistoryForManga(history)

        // Restore tracking
        backupManager.restoreTrackForManga(manga, tracks)
    }

    private fun restoreExtraForAnime(anime: Anime, categories: List<Int>, history: List<BackupAnimeHistory>, tracks: List<AnimeTrack>, backupCategories: List<BackupCategory>) {
        // Restore categories
        backupManager.restoreCategoriesForAnime(anime, categories, backupCategories)

        // Restore history
        backupManager.restoreHistoryForAnime(history)

        // Restore tracking
        backupManager.restoreTrackForAnime(anime, tracks)
    }
}
