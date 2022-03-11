package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.AbstractBackupRestore
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.full.models.*
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Episode
import okio.buffer
import okio.gzip
import okio.source
import java.util.*

class FullBackupRestore(context: Context, notifier: BackupNotifier) : AbstractBackupRestore<FullBackupManager>(context, notifier) {

    override suspend fun performRestore(uri: Uri): Boolean {
        backupManager = FullBackupManager(context)

        val backupString = context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().use { it.readByteArray() }
        val backup = backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

        restoreAmount = backup.backupAnime.size + 2 // +2 for categories

        // Restore categories
        if (backup.backupCategoriesAnime.isNotEmpty()) {
            restoreCategoriesAnime(backup.backupCategoriesAnime)
        }

        // Store source mapping for error messages
        val backupMapsAnime = backup.backupBrokenAnimeSources.map { BackupAnimeSource(it.name, it.sourceId) } + backup.backupAnimeSources
        sourceMapping = backupMapsAnime.map { it.sourceId to it.name }.toMap()

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

    private fun restoreCategoriesAnime(backupCategories: List<BackupCategory>) {
        animedb.inTransaction {
            backupManager.restoreCategoriesAnime(backupCategories)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.anime_categories))
    }

    private fun restoreAnime(backupAnime: BackupAnime, backupCategories: List<BackupCategory>) {
        val anime = backupAnime.getAnimeImpl()
        val episodes = backupAnime.getEpisodesImpl()
        val categories = backupAnime.categories
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
     * Returns an anime restore observable
     *
     * @param anime anime data from json
     * @param episodes episodes data from json
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

    private fun restoreExtraForAnime(anime: Anime, categories: List<Int>, history: List<BackupAnimeHistory>, tracks: List<AnimeTrack>, backupCategories: List<BackupCategory>) {
        // Restore categories
        backupManager.restoreCategoriesForAnime(anime, categories, backupCategories)

        // Restore history
        backupManager.restoreHistoryForAnime(history)

        // Restore tracking
        backupManager.restoreTrackForAnime(anime, tracks)
    }
}
