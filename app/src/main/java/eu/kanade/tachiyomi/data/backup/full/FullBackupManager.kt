package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import com.hippo.unifile.UniFile
import data.Manga_sync
import data.Mangas
import dataanime.Anime_sync
import dataanime.Animes
import eu.kanade.domain.animehistory.model.AnimeHistoryUpdate
import eu.kanade.domain.history.model.HistoryUpdate
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.AbstractBackupManager
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CATEGORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CHAPTER_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_HISTORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_PREFS
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_PREFS_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_TRACK_MASK
import eu.kanade.tachiyomi.data.backup.full.models.Backup
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupFull
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
import eu.kanade.tachiyomi.data.backup.full.models.backupAnimeTrackMapper
import eu.kanade.tachiyomi.data.backup.full.models.backupCategoryMapper
import eu.kanade.tachiyomi.data.backup.full.models.backupChapterMapper
import eu.kanade.tachiyomi.data.backup.full.models.backupEpisodeMapper
import eu.kanade.tachiyomi.data.backup.full.models.backupTrackMapper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import okio.buffer
import okio.gzip
import okio.sink
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.max
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.manga.model.Manga as DomainManga

class FullBackupManager(context: Context) : AbstractBackupManager(context) {

    val parser = ProtoBuf

    /**
     * Create backup Json file from database
     *
     * @param uri path of Uri
     * @param isAutoBackup backup called from scheduled backup job
     */
    override suspend fun createBackup(uri: Uri, flags: Int, isAutoBackup: Boolean): String {
        // Create root object
        var backup: Backup? = null

        val databaseAnime = getFavoriteAnime()
        val databaseManga = getFavoriteManga()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        backup = Backup(
            backupManga(databaseManga, flags),
            backupCategories(flags),
            backupAnime(databaseAnime, flags),
            backupCategoriesAnime(flags),
            emptyList(),
            backupExtensionInfo(databaseManga),
            emptyList(),
            backupAnimeExtensionInfo(databaseAnime),
            backupPreferences(prefs, flags),
        )

        var file: UniFile? = null
        try {
            file = (
                if (isAutoBackup) {
                    // Get dir of file and create
                    var dir = UniFile.fromUri(context, uri)
                    dir = dir.createDirectory("automatic")

                    // Delete older backups
                    val numberOfBackups = numberOfBackups()
                    val backupRegex = Regex("""aniyomi_\d+-\d+-\d+_\d+-\d+.proto.gz""")
                    dir.listFiles { _, filename -> backupRegex.matches(filename) }
                        .orEmpty()
                        .sortedByDescending { it.name }
                        .drop(numberOfBackups - 1)
                        .forEach { it.delete() }

                    // Create new file to place backup
                    dir.createFile(BackupFull.getDefaultFilename())
                } else {
                    UniFile.fromUri(context, uri)
                }
                )
                ?: throw Exception("Couldn't create backup file")

            if (!file.isFile) {
                throw IllegalStateException("Failed to get handle on file")
            }

            val byteArray = parser.encodeToByteArray(BackupSerializer, backup!!)
            if (byteArray.isEmpty()) {
                throw IllegalStateException(context.getString(R.string.empty_backup_error))
            }

            file.openOutputStream().also {
                // Force overwrite old file
                (it as? FileOutputStream)?.channel?.truncate(0)
            }.sink().gzip().buffer().use { it.write(byteArray) }
            val fileUri = file.uri

            // Make sure it's a valid backup file
            FullBackupRestoreValidator().validate(context, fileUri)

            return fileUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            file?.delete()
            throw e
        }
    }

    private suspend fun backupManga(mangas: List<DomainManga>, flags: Int): List<BackupManga> {
        return mangas.map {
            backupMangaObject(it, flags)
        }
    }

    private suspend fun backupAnime(animes: List<DomainAnime>, flags: Int): List<BackupAnime> {
        return animes.map {
            backupAnimeObject(it, flags)
        }
    }

    private fun backupExtensionInfo(mangas: List<DomainManga>): List<BackupSource> {
        return mangas
            .asSequence()
            .map { it.source }
            .distinct()
            .map { sourceManager.getOrStub(it) }
            .map { BackupSource.copyFrom(it) }
            .toList()
    }

    private fun backupAnimeExtensionInfo(animes: List<DomainAnime>): List<BackupAnimeSource> {
        return animes
            .asSequence()
            .map { it.source }
            .distinct()
            .map { animesourceManager.getOrStub(it) }
            .map { BackupAnimeSource.copyFrom(it) }
            .toList()
    }

    /**
     * Backup the categories of manga library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private suspend fun backupCategories(options: Int): List<BackupCategory> {
        // Check if user wants category information in backup
        return if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            handler.awaitList { categoriesQueries.getCategories(backupCategoryMapper) }
        } else {
            emptyList()
        }
    }

    /**
     * Backup the categories of anime library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private suspend fun backupCategoriesAnime(options: Int): List<BackupCategory> {
        // Check if user wants category information in backup
        return if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            animehandler.awaitList { categoriesQueries.getCategories(backupCategoryMapper) }
        } else {
            emptyList()
        }
    }

    /**
     * Convert a manga to Json
     *
     * @param manga manga that gets converted
     * @param options options for the backup
     * @return [BackupManga] containing manga in a serializable form
     */
    private suspend fun backupMangaObject(manga: DomainManga, options: Int): BackupManga {
        // Entry for this manga
        val mangaObject = BackupManga.copyFrom(manga)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val chapters = handler.awaitList { chaptersQueries.getChaptersByMangaId(manga.id, backupChapterMapper) }
            if (chapters.isNotEmpty()) {
                mangaObject.chapters = chapters
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForManga = handler.awaitList { categoriesQueries.getCategoriesByMangaId(manga.id) }
            if (categoriesForManga.isNotEmpty()) {
                mangaObject.categories = categoriesForManga.map { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = handler.awaitList { manga_syncQueries.getTracksByMangaId(manga.id, backupTrackMapper) }
            if (tracks.isNotEmpty()) {
                mangaObject.tracking = tracks
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyByMangaId = handler.awaitList(true) { historyQueries.getHistoryByMangaId(manga.id) }
            if (historyByMangaId.isNotEmpty()) {
                val history = historyByMangaId.map { history ->
                    val chapter = handler.awaitOne { chaptersQueries.getChapterById(history.chapter_id) }
                    BackupHistory(chapter.url, history.last_read?.time ?: 0L)
                }
                if (history.isNotEmpty()) {
                    mangaObject.history = history
                }
            }
        }

        return mangaObject
    }

    /**
     * Convert an anime to Json
     *
     * @param anime anime that gets converted
     * @param options options for the backup
     * @return [BackupAnime] containing anime in a serializable form
     */
    private suspend fun backupAnimeObject(anime: DomainAnime, options: Int): BackupAnime {
        // Entry for this anime
        val animeObject = BackupAnime.copyFrom(anime)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val episodes = animehandler.awaitList { episodesQueries.getEpisodesByAnimeId(anime.id, backupEpisodeMapper) }
            if (episodes.isNotEmpty()) {
                animeObject.episodes = episodes
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForAnime = animehandler.awaitList { categoriesQueries.getCategoriesByAnimeId(anime.id) }
            if (categoriesForAnime.isNotEmpty()) {
                animeObject.categories = categoriesForAnime.map { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = animehandler.awaitList { anime_syncQueries.getTracksByAnimeId(anime.id, backupAnimeTrackMapper) }
            if (tracks.isNotEmpty()) {
                animeObject.tracking = tracks
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyByAnimeId = animehandler.awaitList(true) { animehistoryQueries.getHistoryByAnimeId(anime.id) }
            if (historyByAnimeId.isNotEmpty()) {
                val history = historyByAnimeId.map { history ->
                    val episode = animehandler.awaitOne { episodesQueries.getEpisodeById(history.episode_id) }
                    BackupAnimeHistory(episode.url, history.last_seen?.time ?: 0L)
                }
                if (history.isNotEmpty()) {
                    animeObject.history = history
                }
            }
        }

        return animeObject
    }

    private fun backupPreferences(prefs: SharedPreferences, options: Int): List<BackupPreference> {
        if (options and BACKUP_PREFS_MASK != BACKUP_PREFS) return emptyList()
        val backupPreferences = mutableListOf<BackupPreference>()
        for (pref in prefs.all) {
            val toAdd = when (pref.value) {
                is Int -> {
                    BackupPreference(pref.key, IntPreferenceValue(pref.value as Int))
                }
                is Long -> {
                    BackupPreference(pref.key, LongPreferenceValue(pref.value as Long))
                }
                is Float -> {
                    BackupPreference(pref.key, FloatPreferenceValue(pref.value as Float))
                }
                is String -> {
                    BackupPreference(pref.key, StringPreferenceValue(pref.value as String))
                }
                is Boolean -> {
                    BackupPreference(pref.key, BooleanPreferenceValue(pref.value as Boolean))
                }
                is Set<*> -> {
                    (pref.value as? Set<String>)?.let {
                        BackupPreference(pref.key, StringSetPreferenceValue(it))
                    } ?: continue
                }
                else -> {
                    continue
                }
            }
            backupPreferences.add(toAdd)
        }
        return backupPreferences
    }

    suspend fun restoreMangaNoFetch(manga: Manga, dbManga: Mangas) {
        manga.id = dbManga._id
        manga.copyFrom(dbManga)
        updateManga(manga)
    }

    /**
     * Fetches manga information
     *
     * @param manga manga that needs updating
     * @return Updated manga info.
     */
    suspend fun restoreManga(manga: Manga): Manga {
        return manga.also {
            it.initialized = it.description != null
            it.id = insertManga(it)
        }
    }

    suspend fun restoreAnimeNoFetch(anime: Anime, dbAnime: Animes) {
        anime.id = dbAnime._id
        anime.copyFrom(dbAnime)
        updateAnime(anime)
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @return Updated anime info.
     */
    suspend fun restoreAnime(anime: Anime): Anime {
        return anime.also {
            it.initialized = it.description != null
            it.id = insertAnime(it)
        }
    }

    /**
     * Restore the categories from Json
     *
     * @param backupCategories list containing categories
     */
    internal suspend fun restoreCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = handler.awaitList { categoriesQueries.getCategories() }

        // Iterate over them
        backupCategories
            .map { it.getCategoryImpl() }
            .forEach { category ->
                // Used to know if the category is already in the db
                var found = false
                for (dbCategory in dbCategories) {
                    // If the category is already in the db, assign the id to the file's category
                    // and do nothing
                    if (category.name == dbCategory.name) {
                        category.id = dbCategory.id.toInt()
                        found = true
                        break
                    }
                }
                // If the category isn't in the db, remove the id and insert a new category
                // Store the inserted id in the category
                if (!found) {
                    // Let the db assign the id
                    category.id = null
                    category.id = handler.awaitOne {
                        categoriesQueries.insert(category.name, category.order.toLong(), category.flags.toLong())
                        categoriesQueries.selectLastInsertedRowId()
                    }.toInt()
                }
            }
    }

    /**
     * Restore the categories from Json
     *
     * @param backupCategories list containing categories
     */
    internal suspend fun restoreCategoriesAnime(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = animehandler.awaitList { categoriesQueries.getCategories() }

        // Iterate over them
        backupCategories
            .map { it.getCategoryImpl() }
            .forEach { category ->
                // Used to know if the category is already in the db
                var found = false
                for (dbCategory in dbCategories) {
                    // If the category is already in the db, assign the id to the file's category
                    // and do nothing
                    if (category.name == dbCategory.name) {
                        category.id = dbCategory.id.toInt()
                        found = true
                        break
                    }
                }
                // If the category isn't in the db, remove the id and insert a new category
                // Store the inserted id in the category
                if (!found) {
                    // Let the db assign the id
                    category.id = null
                    category.id = animehandler.awaitOne {
                        categoriesQueries.insert(category.name, category.order.toLong(), category.flags.toLong())
                        categoriesQueries.selectLastInsertedRowId()
                    }.toInt()
                }
            }
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal suspend fun restoreCategoriesForManga(manga: Manga, categories: List<Int>, backupCategories: List<BackupCategory>) {
        val dbCategories = handler.awaitList { categoriesQueries.getCategories() }
        val mangaCategoriesToUpdate = mutableListOf<Pair<Long, Long>>()

        categories.forEach { backupCategoryOrder ->
            backupCategories.firstOrNull {
                it.order == backupCategoryOrder.toLong()
            }?.let { backupCategory ->
                dbCategories.firstOrNull { dbCategory ->
                    dbCategory.name == backupCategory.name
                }?.let { dbCategory ->
                    mangaCategoriesToUpdate.add(Pair(manga.id!!, dbCategory.id))
                }
            }
        }

        // Update database
        if (mangaCategoriesToUpdate.isNotEmpty()) {
            handler.await(true) {
                mangas_categoriesQueries.deleteMangaCategoryByMangaId(manga.id!!)
                mangaCategoriesToUpdate.forEach { (mangaId, categoryId) ->
                    mangas_categoriesQueries.insert(mangaId, categoryId)
                }
            }
        }
    }

    /**
     * Restores the categories an anime is in.
     *
     * @param anime the anime whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal suspend fun restoreCategoriesForAnime(anime: Anime, categories: List<Int>, backupCategories: List<BackupCategory>) {
        val dbCategories = animehandler.awaitList { categoriesQueries.getCategories() }
        val animeCategoriesToUpdate = mutableListOf<Pair<Long, Long>>()

        categories.forEach { backupCategoryOrder ->
            backupCategories.firstOrNull {
                it.order == backupCategoryOrder.toLong()
            }?.let { backupCategory ->
                dbCategories.firstOrNull { dbCategory ->
                    dbCategory.name == backupCategory.name
                }?.let { dbCategory ->
                    animeCategoriesToUpdate.add(Pair(anime.id!!, dbCategory.id))
                }
            }
        }

        // Update database
        if (animeCategoriesToUpdate.isNotEmpty()) {
            animehandler.await(true) {
                animes_categoriesQueries.deleteAnimeCategoryByAnimeId(anime.id!!)
                animeCategoriesToUpdate.forEach { (animeId, categoryId) ->
                    animes_categoriesQueries.insert(animeId, categoryId)
                }
            }
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    internal suspend fun restoreHistoryForManga(history: List<BackupHistory>) {
        // List containing history to be updated
        val toUpdate = mutableListOf<HistoryUpdate>()
        for ((url, lastRead) in history) {
            var dbHistory = handler.awaitOneOrNull { historyQueries.getHistoryByChapterUrl(url) }
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory = dbHistory.copy(last_read = Date(max(lastRead, dbHistory.last_read?.time ?: 0L)))
                toUpdate.add(
                    HistoryUpdate(
                        chapterId = dbHistory.chapter_id,
                        readAt = dbHistory.last_read!!,
                        sessionReadDuration = dbHistory.time_read,
                    ),
                )
            } else {
                // If not in database create
                handler
                    .awaitOneOrNull { chaptersQueries.getChapterByUrl(url) }
                    ?.let {
                        toUpdate.add(
                            HistoryUpdate(
                                chapterId = it._id,
                                readAt = Date(lastRead),
                                sessionReadDuration = 0,
                            ),
                        )
                    }
            }
        }
        handler.await(true) {
            toUpdate.forEach { payload ->
                historyQueries.upsert(
                    payload.chapterId,
                    payload.readAt,
                    payload.sessionReadDuration,
                )
            }
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    internal suspend fun restoreHistoryForAnime(history: List<BackupAnimeHistory>) {
        // List containing history to be updated
        val toUpdate = mutableListOf<AnimeHistoryUpdate>()
        for ((url, lastSeen) in history) {
            var dbHistory = animehandler.awaitOneOrNull { animehistoryQueries.getHistoryByEpisodeUrl(url) }
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory = dbHistory.copy(last_seen = Date(max(lastSeen, dbHistory.last_seen?.time ?: 0L)))
                toUpdate.add(
                    AnimeHistoryUpdate(
                        episodeId = dbHistory.episode_id,
                        seenAt = dbHistory.last_seen!!,
                    ),
                )
            } else {
                // If not in database create
                animehandler
                    .awaitOneOrNull { episodesQueries.getEpisodeByUrl(url) }
                    ?.let {
                        toUpdate.add(
                            AnimeHistoryUpdate(
                                episodeId = it._id,
                                seenAt = Date(lastSeen),
                            ),
                        )
                    }
            }
        }
        animehandler.await(true) {
            toUpdate.forEach { payload ->
                animehistoryQueries.upsert(
                    payload.episodeId,
                    payload.seenAt,
                )
            }
        }
    }

    /**
     * Restores the sync of a manga.
     *
     * @param manga the manga whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    internal suspend fun restoreTrackForManga(manga: Manga, tracks: List<Track>) {
        // Fix foreign keys with the current manga id
        tracks.map { it.manga_id = manga.id!! }

        // Get tracks from database

        val dbTracks = handler.awaitList { manga_syncQueries.getTracksByMangaId(manga.id!!) }
        val toUpdate = mutableListOf<Manga_sync>()
        val toInsert = mutableListOf<Track>()

        tracks.forEach { track ->
            var isInDatabase = false
            for (dbTrack in dbTracks) {
                if (track.sync_id == dbTrack.sync_id.toInt()) {
                    // The sync is already in the db, only update its fields
                    var temp = dbTrack
                    if (track.media_id != dbTrack.remote_id) {
                        temp = temp.copy(remote_id = track.media_id)
                    }
                    if (track.library_id != dbTrack.library_id) {
                        temp = temp.copy(library_id = track.library_id)
                    }
                    temp = temp.copy(last_chapter_read = max(dbTrack.last_chapter_read, track.last_chapter_read.toDouble()))
                    isInDatabase = true
                    toUpdate.add(temp)
                    break
                }
            }
            if (!isInDatabase) {
                // Insert new sync. Let the db assign the id
                track.id = null
                toInsert.add(track)
            }
        }
        // Update database
        if (toUpdate.isNotEmpty()) {
            handler.await(true) {
                toUpdate.forEach { track ->
                    manga_syncQueries.update(
                        track.manga_id,
                        track.sync_id,
                        track.remote_id,
                        track.library_id,
                        track.title,
                        track.last_chapter_read,
                        track.total_chapters,
                        track.status,
                        track.score.toDouble(),
                        track.remote_url,
                        track.start_date,
                        track.finish_date,
                        track._id,
                    )
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            handler.await(true) {
                toInsert.forEach { track ->
                    manga_syncQueries.insert(
                        track.manga_id,
                        track.sync_id.toLong(),
                        track.media_id,
                        track.library_id,
                        track.title,
                        track.last_chapter_read.toDouble(),
                        track.total_chapters.toLong(),
                        track.status.toLong(),
                        track.score,
                        track.tracking_url,
                        track.started_reading_date,
                        track.finished_reading_date,
                    )
                }
            }
        }
    }

    /**
     * Restores the sync of a manga.
     *
     * @param anime the anime whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    internal suspend fun restoreTrackForAnime(anime: Anime, tracks: List<AnimeTrack>) {
        // Fix foreign keys with the current anime id
        tracks.map { it.anime_id = anime.id!! }

        // Get tracks from database

        val dbTracks = animehandler.awaitList { anime_syncQueries.getTracksByAnimeId(anime.id!!) }
        val toUpdate = mutableListOf<Anime_sync>()
        val toInsert = mutableListOf<AnimeTrack>()

        tracks.forEach { track ->
            var isInDatabase = false
            for (dbTrack in dbTracks) {
                if (track.sync_id == dbTrack.sync_id.toInt()) {
                    // The sync is already in the db, only update its fields
                    var temp = dbTrack
                    if (track.media_id != dbTrack.remote_id) {
                        temp = temp.copy(remote_id = track.media_id)
                    }
                    if (track.library_id != dbTrack.library_id) {
                        temp = temp.copy(library_id = track.library_id)
                    }
                    temp = temp.copy(last_episode_seen = max(dbTrack.last_episode_seen, track.last_episode_seen.toDouble()))
                    isInDatabase = true
                    toUpdate.add(temp)
                    break
                }
            }
            if (!isInDatabase) {
                // Insert new sync. Let the db assign the id
                track.id = null
                toInsert.add(track)
            }
        }
        // Update database
        if (toUpdate.isNotEmpty()) {
            animehandler.await(true) {
                toUpdate.forEach { track ->
                    anime_syncQueries.update(
                        track.anime_id,
                        track.sync_id,
                        track.remote_id,
                        track.library_id,
                        track.title,
                        track.last_episode_seen,
                        track.total_episodes,
                        track.status,
                        track.score.toDouble(),
                        track.remote_url,
                        track.start_date,
                        track.finish_date,
                        track._id,
                    )
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            animehandler.await(true) {
                toInsert.forEach { track ->
                    anime_syncQueries.insert(
                        track.anime_id,
                        track.sync_id.toLong(),
                        track.media_id,
                        track.library_id,
                        track.title,
                        track.last_episode_seen.toDouble(),
                        track.total_episodes.toLong(),
                        track.status.toLong(),
                        track.score,
                        track.tracking_url,
                        track.started_watching_date,
                        track.finished_watching_date,
                    )
                }
            }
        }
    }

    internal suspend fun restoreChaptersForManga(manga: Manga, chapters: List<Chapter>) {
        val dbChapters = handler.awaitList { chaptersQueries.getChaptersByMangaId(manga.id!!) }

        chapters.forEach { chapter ->
            val dbChapter = dbChapters.find { it.url == chapter.url }
            if (dbChapter != null) {
                chapter.id = dbChapter._id
                chapter.copyFrom(dbChapter)
                if (dbChapter.read && !chapter.read) {
                    chapter.read = dbChapter.read
                    chapter.last_page_read = dbChapter.last_page_read.toInt()
                } else if (chapter.last_page_read == 0 && dbChapter.last_page_read != 0L) {
                    chapter.last_page_read = dbChapter.last_page_read.toInt()
                }
                if (!chapter.bookmark && dbChapter.bookmark) {
                    chapter.bookmark = dbChapter.bookmark
                }
            }

            chapter.manga_id = manga.id
        }

        val newChapters = chapters.groupBy { it.id != null }
        newChapters[true]?.let { updateKnownChapters(it) }
        newChapters[false]?.let { insertChapters(it) }
    }

    internal suspend fun restoreEpisodesForAnime(anime: Anime, episodes: List<Episode>) {
        val dbEpisodes = animehandler.awaitList { episodesQueries.getEpisodesByAnimeId(anime.id!!) }

        episodes.forEach { episode ->
            val dbEpisode = dbEpisodes.find { it.url == episode.url }
            if (dbEpisode != null) {
                episode.id = dbEpisode._id
                episode.copyFrom(dbEpisode)
                if (dbEpisode.seen && !episode.seen) {
                    episode.seen = dbEpisode.seen
                    episode.last_second_seen = dbEpisode.last_second_seen
                } else if (episode.last_second_seen == 0L && dbEpisode.last_second_seen != 0L) {
                    episode.last_second_seen = dbEpisode.last_second_seen
                }
                if (!episode.bookmark && dbEpisode.bookmark) {
                    episode.bookmark = dbEpisode.bookmark
                }
            }

            episode.anime_id = anime.id
        }

        val newEpisodes = episodes.groupBy { it.id != null }
        newEpisodes[true]?.let { updateKnownEpisodes(it) }
        newEpisodes[false]?.let { insertEpisodes(it) }
    }
}
