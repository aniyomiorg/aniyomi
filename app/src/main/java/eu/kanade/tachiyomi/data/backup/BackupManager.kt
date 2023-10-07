package eu.kanade.tachiyomi.data.backup

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.preference.PreferenceManager
import com.hippo.unifile.UniFile
import data.Manga_sync
import data.Mangas
import dataanime.Anime_sync
import dataanime.Animes
import eu.kanade.domain.items.chapter.model.copyFrom
import eu.kanade.domain.items.episode.model.copyFrom
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CATEGORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_CHAPTER_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_EXTENSIONS
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_EXTENSIONS_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_EXT_PREFS
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_EXT_PREFS_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_HISTORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_PREFS
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_PREFS_MASK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.BackupConst.BACKUP_TRACK_MASK
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
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
import eu.kanade.tachiyomi.data.backup.models.backupAnimeTrackMapper
import eu.kanade.tachiyomi.data.backup.models.backupCategoryMapper
import eu.kanade.tachiyomi.data.backup.models.backupChapterMapper
import eu.kanade.tachiyomi.data.backup.models.backupEpisodeMapper
import eu.kanade.tachiyomi.data.backup.models.backupTrackMapper
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.source.anime.getPreferenceKey
import eu.kanade.tachiyomi.source.anime.model.copyFrom
import eu.kanade.tachiyomi.source.manga.getPreferenceKey
import eu.kanade.tachiyomi.source.manga.model.copyFrom
import eu.kanade.tachiyomi.util.system.hasPermission
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import okio.buffer
import okio.gzip
import okio.sink
import tachiyomi.core.util.lang.toLong
import tachiyomi.core.util.system.logcat
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.data.updateStrategyAdapter
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entries.anime.interactor.GetAnimeFavorites
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.manga.interactor.GetMangaFavorites
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.history.anime.model.AnimeHistoryUpdate
import tachiyomi.domain.history.manga.model.MangaHistoryUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.max

class BackupManager(
    private val context: Context,
) {

    private val mangaHandler: MangaDatabaseHandler = Injekt.get()
    private val animeHandler: AnimeDatabaseHandler = Injekt.get()
    private val mangaSourceManager: MangaSourceManager = Injekt.get()
    private val animeSourceManager: AnimeSourceManager = Injekt.get()
    private val backupPreferences: BackupPreferences = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val getMangaCategories: GetMangaCategories = Injekt.get()
    private val getAnimeCategories: GetAnimeCategories = Injekt.get()
    private val getMangaFavorites: GetMangaFavorites = Injekt.get()
    private val getAnimeFavorites: GetAnimeFavorites = Injekt.get()

    internal val parser = ProtoBuf

    /**
     * Create backup file from database
     *
     * @param uri path of Uri
     * @param isAutoBackup backup called from scheduled backup job
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun createBackup(uri: Uri, flags: Int, isAutoBackup: Boolean): String {
        if (!context.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            throw IllegalStateException(context.getString(R.string.missing_storage_permission))
        }

        val databaseAnime = getAnimeFavorites.await()
        val databaseManga = getMangaFavorites.await()

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val backup = Backup(
            backupMangas(databaseManga, flags),
            backupCategories(flags),
            backupAnimes(databaseAnime, flags),
            backupAnimeCategories(flags),
            emptyList(),
            backupExtensionInfo(databaseManga),
            emptyList(),
            backupAnimeExtensionInfo(databaseAnime),
            backupPreferences(prefs, flags),
            backupExtensionPreferences(flags),
            backupExtensions(flags),
        )

        var file: UniFile? = null
        try {
            file = (
                if (isAutoBackup) {
                    // Get dir of file and create
                    var dir = UniFile.fromUri(context, uri)
                    dir = dir.createDirectory("automatic")

                    // Delete older backups
                    val numberOfBackups = backupPreferences.numberOfBackups().get()
                    val backupRegex = Regex("""aniyomi_\d+-\d+-\d+_\d+-\d+.proto.gz""")
                    dir.listFiles { _, filename -> backupRegex.matches(filename) }
                        .orEmpty()
                        .sortedByDescending { it.name }
                        .drop(numberOfBackups - 1)
                        .forEach { it.delete() }

                    // Create new file to place backup
                    dir.createFile(Backup.getBackupFilename())
                } else {
                    UniFile.fromUri(context, uri)
                }
                )
                ?: throw Exception("Couldn't create backup file")

            if (!file.isFile) {
                throw IllegalStateException("Failed to get handle on file")
            }

            val byteArray = parser.encodeToByteArray(BackupSerializer, backup)
            if (byteArray.isEmpty()) {
                throw IllegalStateException(context.getString(R.string.empty_backup_error))
            }

            file.openOutputStream().also {
                // Force overwrite old file
                (it as? FileOutputStream)?.channel?.truncate(0)
            }.sink().gzip().buffer().use { it.write(byteArray) }
            val fileUri = file.uri

            // Make sure it's a valid backup file
            BackupFileValidator().validate(context, fileUri)

            return fileUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            file?.delete()
            throw e
        }
    }

    private fun backupExtensionInfo(mangas: List<Manga>): List<BackupSource> {
        return mangas
            .asSequence()
            .map(Manga::source)
            .distinct()
            .map(mangaSourceManager::getOrStub)
            .map(BackupSource::copyFrom)
            .toList()
    }

    private fun backupAnimeExtensionInfo(animes: List<Anime>): List<BackupAnimeSource> {
        return animes
            .asSequence()
            .map(Anime::source)
            .distinct()
            .map(animeSourceManager::getOrStub)
            .map(BackupAnimeSource::copyFrom)
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
            getMangaCategories.await()
                .filterNot(Category::isSystemCategory)
                .map(backupCategoryMapper)
        } else {
            emptyList()
        }
    }

    /**
     * Backup the categories of anime library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private suspend fun backupAnimeCategories(options: Int): List<BackupCategory> {
        // Check if user wants category information in backup
        return if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            getAnimeCategories.await()
                .filterNot(Category::isSystemCategory)
                .map(backupCategoryMapper)
        } else {
            emptyList()
        }
    }

    private suspend fun backupMangas(mangas: List<Manga>, flags: Int): List<BackupManga> {
        return mangas.map {
            backupManga(it, flags)
        }
    }

    private suspend fun backupAnimes(animes: List<Anime>, flags: Int): List<BackupAnime> {
        return animes.map {
            backupAnime(it, flags)
        }
    }

    /**
     * Convert a manga to Json
     *
     * @param manga manga that gets converted
     * @param options options for the backup
     * @return [BackupManga] containing manga in a serializable form
     */
    private suspend fun backupManga(manga: Manga, options: Int): BackupManga {
        // Entry for this manga
        val mangaObject = BackupManga.copyFrom(manga)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val chapters = mangaHandler.awaitList { chaptersQueries.getChaptersByMangaId(manga.id, backupChapterMapper) }
            if (chapters.isNotEmpty()) {
                mangaObject.chapters = chapters
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForManga = getMangaCategories.await(manga.id)
            if (categoriesForManga.isNotEmpty()) {
                mangaObject.categories = categoriesForManga.map { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = mangaHandler.awaitList { manga_syncQueries.getTracksByMangaId(manga.id, backupTrackMapper) }
            if (tracks.isNotEmpty()) {
                mangaObject.tracking = tracks
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyByMangaId = mangaHandler.awaitList(true) { historyQueries.getHistoryByMangaId(manga.id) }
            if (historyByMangaId.isNotEmpty()) {
                val history = historyByMangaId.map { history ->
                    val chapter = mangaHandler.awaitOne { chaptersQueries.getChapterById(history.chapter_id) }
                    BackupHistory(chapter.url, history.last_read?.time ?: 0L, history.time_read)
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
    private suspend fun backupAnime(anime: Anime, options: Int): BackupAnime {
        // Entry for this anime
        val animeObject = BackupAnime.copyFrom(anime)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val episodes = animeHandler.awaitList { episodesQueries.getEpisodesByAnimeId(anime.id, backupEpisodeMapper) }
            if (episodes.isNotEmpty()) {
                animeObject.episodes = episodes
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForAnime = getAnimeCategories.await(anime.id)
            if (categoriesForAnime.isNotEmpty()) {
                animeObject.categories = categoriesForAnime.map { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = animeHandler.awaitList { anime_syncQueries.getTracksByAnimeId(anime.id, backupAnimeTrackMapper) }
            if (tracks.isNotEmpty()) {
                animeObject.tracking = tracks
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyByAnimeId = animeHandler.awaitList(true) { animehistoryQueries.getHistoryByAnimeId(anime.id) }
            if (historyByAnimeId.isNotEmpty()) {
                val history = historyByAnimeId.map { history ->
                    val episode = animeHandler.awaitOne { episodesQueries.getEpisodeById(history.episode_id) }
                    BackupAnimeHistory(episode.url, history.last_seen?.time ?: 0L)
                }
                if (history.isNotEmpty()) {
                    animeObject.history = history
                }
            }
        }

        return animeObject
    }

    private fun backupExtensionPreferences(flags: Int): List<BackupExtensionPreferences> {
        if (flags and BACKUP_EXT_PREFS_MASK != BACKUP_EXT_PREFS) return emptyList()
        val prefs = mutableListOf<Pair<String, SharedPreferences>>()
        Injekt.get<AnimeSourceManager>().getOnlineSources().forEach {
            val name = it.getPreferenceKey()
            prefs += Pair(name, context.getSharedPreferences(name, 0x0))
        }
        Injekt.get<MangaSourceManager>().getOnlineSources().forEach {
            val name = it.getPreferenceKey()
            prefs += Pair(name, context.getSharedPreferences(name, 0x0))
        }
        return prefs.map {
            BackupExtensionPreferences(
                it.first,
                backupPreferences(it.second, BACKUP_PREFS),
            )
        }
    }

    private fun backupPreferences(prefs: SharedPreferences, flags: Int): List<BackupPreference> {
        if (flags and BACKUP_PREFS_MASK != BACKUP_PREFS) return emptyList()
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

    @Suppress("DEPRECATION")
    private fun backupExtensions(flags: Int): List<BackupExtension> {
        if (flags and BACKUP_EXTENSIONS_MASK != BACKUP_EXTENSIONS) return emptyList()
        val installedExtensions = mutableListOf<BackupExtension>()
        Injekt.get<AnimeExtensionManager>().installedExtensionsFlow.value.forEach {
            val packageName = it.pkgName
            val apk = File(
                context.packageManager
                    .getApplicationInfo(
                        packageName,
                        PackageManager.GET_META_DATA,
                    ).publicSourceDir,
            ).readBytes()
            installedExtensions.add(
                BackupExtension(packageName, apk),
            )
        }
        Injekt.get<MangaExtensionManager>().installedExtensionsFlow.value.forEach {
            val packageName = it.pkgName
            val apk = File(
                context.packageManager
                    .getApplicationInfo(
                        packageName,
                        PackageManager.GET_META_DATA,
                    ).publicSourceDir,
            ).readBytes()
            installedExtensions.add(
                BackupExtension(packageName, apk),
            )
        }
        return installedExtensions
    }

    internal suspend fun restoreExistingManga(manga: Manga, dbManga: Mangas): Manga {
        var manga = manga.copy(id = dbManga._id)
        manga = manga.copyFrom(dbManga)
        updateManga(manga)
        return manga
    }

    /**
     * Fetches manga information
     *
     * @param manga manga that needs updating
     * @return Updated manga info.
     */
    internal suspend fun restoreNewManga(manga: Manga): Manga {
        return manga.copy(
            initialized = manga.description != null,
            id = insertManga(manga),
        )
    }

    internal suspend fun restoreExistingAnime(anime: Anime, dbAnime: Animes): Anime {
        var anime = anime.copy(id = dbAnime._id)
        anime = anime.copyFrom(dbAnime)
        updateAnime(anime)
        return anime
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @return Updated anime info.
     */
    internal suspend fun restoreNewAnime(anime: Anime): Anime {
        return anime.copy(
            initialized = anime.description != null,
            id = insertAnime(anime),
        )
    }

    /**
     * Restore the categories from Json
     *
     * @param backupCategories list containing categories
     */
    internal suspend fun restoreCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = getMangaCategories.await()

        val categories = backupCategories.map {
            var category = it.getCategory()
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.name == dbCategory.name) {
                    category = category.copy(id = dbCategory.id)
                    found = true
                    break
                }
            }
            if (!found) {
                // Let the db assign the id
                val id = mangaHandler.awaitOne {
                    categoriesQueries.insert(category.name, category.order, category.flags)
                    categoriesQueries.selectLastInsertedRowId()
                }
                category = category.copy(id = id)
            }

            category
        }

        libraryPreferences.categorizedDisplaySettings().set(
            (dbCategories + categories)
                .distinctBy { it.flags }
                .size > 1,
        )
    }

    /**
     * Restore the categories from Json
     *
     * @param backupCategories list containing categories
     */
    internal suspend fun restoreAnimeCategories(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = getAnimeCategories.await()

        val categories = backupCategories.map {
            var category = it.getCategory()
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.name == dbCategory.name) {
                    category = category.copy(id = dbCategory.id)
                    found = true
                    break
                }
            }
            if (!found) {
                // Let the db assign the id
                val id = animeHandler.awaitOne {
                    categoriesQueries.insert(category.name, category.order, category.flags)
                    categoriesQueries.selectLastInsertedRowId()
                }
                category = category.copy(id = id)
            }

            category
        }

        libraryPreferences.categorizedDisplaySettings().set(
            (dbCategories + categories)
                .distinctBy { it.flags }
                .size > 1,
        )
    }

    /**
     * Restores the categories a manga is in.
     *
     * @param manga the manga whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal suspend fun restoreCategories(manga: Manga, categories: List<Int>, backupCategories: List<BackupCategory>) {
        val dbCategories = getMangaCategories.await()
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
            mangaHandler.await(true) {
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
    internal suspend fun restoreAnimeCategories(anime: Anime, categories: List<Int>, backupCategories: List<BackupCategory>) {
        val dbCategories = getAnimeCategories.await()
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
            animeHandler.await(true) {
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
    internal suspend fun restoreHistory(history: List<BackupHistory>) {
        // List containing history to be updated
        val toUpdate = mutableListOf<MangaHistoryUpdate>()
        for ((url, lastRead, readDuration) in history) {
            var dbHistory = mangaHandler.awaitOneOrNull { historyQueries.getHistoryByChapterUrl(url) }
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory = dbHistory.copy(
                    last_read = Date(max(lastRead, dbHistory.last_read?.time ?: 0L)),
                    time_read = max(readDuration, dbHistory.time_read) - dbHistory.time_read,
                )
                toUpdate.add(
                    MangaHistoryUpdate(
                        chapterId = dbHistory.chapter_id,
                        readAt = dbHistory.last_read!!,
                        sessionReadDuration = dbHistory.time_read,
                    ),
                )
            } else {
                // If not in database create
                mangaHandler
                    .awaitOneOrNull { chaptersQueries.getChapterByUrl(url) }
                    ?.let {
                        toUpdate.add(
                            MangaHistoryUpdate(
                                chapterId = it._id,
                                readAt = Date(lastRead),
                                sessionReadDuration = readDuration,
                            ),
                        )
                    }
            }
        }
        mangaHandler.await(true) {
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
    internal suspend fun restoreAnimeHistory(history: List<BackupAnimeHistory>) {
        // List containing history to be updated
        val toUpdate = mutableListOf<AnimeHistoryUpdate>()
        for ((url, lastSeen) in history) {
            var dbHistory = animeHandler.awaitOneOrNull { animehistoryQueries.getHistoryByEpisodeUrl(url) }
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
                animeHandler
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
        animeHandler.await(true) {
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
    internal suspend fun restoreTracking(manga: Manga, tracks: List<tachiyomi.domain.track.manga.model.MangaTrack>) {
        // Fix foreign keys with the current manga id
        val tracks = tracks.map { it.copy(mangaId = manga.id!!) }

        // Get tracks from database
        val dbTracks = mangaHandler.awaitList { manga_syncQueries.getTracksByMangaId(manga.id!!) }
        val toUpdate = mutableListOf<Manga_sync>()
        val toInsert = mutableListOf<tachiyomi.domain.track.manga.model.MangaTrack>()

        tracks.forEach { track ->
            var isInDatabase = false
            for (dbTrack in dbTracks) {
                if (track.syncId == dbTrack.sync_id) {
                    // The sync is already in the db, only update its fields
                    var temp = dbTrack
                    if (track.remoteId != dbTrack.remote_id) {
                        temp = temp.copy(remote_id = track.remoteId)
                    }
                    if (track.libraryId != dbTrack.library_id) {
                        temp = temp.copy(library_id = track.libraryId)
                    }
                    temp = temp.copy(last_chapter_read = max(dbTrack.last_chapter_read, track.lastChapterRead))
                    isInDatabase = true
                    toUpdate.add(temp)
                    break
                }
            }
            if (!isInDatabase) {
                // Insert new sync. Let the db assign the id
                toInsert.add(track.copy(id = 0))
            }
        }
        // Update database
        if (toUpdate.isNotEmpty()) {
            mangaHandler.await(true) {
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
            mangaHandler.await(true) {
                toInsert.forEach { track ->
                    manga_syncQueries.insert(
                        track.mangaId,
                        track.syncId,
                        track.remoteId,
                        track.libraryId,
                        track.title,
                        track.lastChapterRead,
                        track.totalChapters,
                        track.status.toLong(),
                        track.score,
                        track.remoteUrl,
                        track.startDate,
                        track.finishDate,
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
    internal suspend fun restoreAnimeTracking(anime: Anime, tracks: List<tachiyomi.domain.track.anime.model.AnimeTrack>) {
        // Fix foreign keys with the current anime id
        val tracks = tracks.map { it.copy(animeId = anime.id!!) }

        // Get tracks from database
        val dbTracks = animeHandler.awaitList { anime_syncQueries.getTracksByAnimeId(anime.id!!) }
        val toUpdate = mutableListOf<Anime_sync>()
        val toInsert = mutableListOf<tachiyomi.domain.track.anime.model.AnimeTrack>()

        tracks.forEach { track ->
            var isInDatabase = false
            for (dbTrack in dbTracks) {
                if (track.syncId == dbTrack.sync_id) {
                    // The sync is already in the db, only update its fields
                    var temp = dbTrack
                    if (track.remoteId != dbTrack.remote_id) {
                        temp = temp.copy(remote_id = track.remoteId)
                    }
                    if (track.libraryId != dbTrack.library_id) {
                        temp = temp.copy(library_id = track.libraryId)
                    }
                    temp = temp.copy(last_episode_seen = max(dbTrack.last_episode_seen, track.lastEpisodeSeen))
                    isInDatabase = true
                    toUpdate.add(temp)
                    break
                }
            }
            if (!isInDatabase) {
                // Insert new sync. Let the db assign the id
                toInsert.add(track.copy(id = 0))
            }
        }
        // Update database
        if (toUpdate.isNotEmpty()) {
            animeHandler.await(true) {
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
            animeHandler.await(true) {
                toInsert.forEach { track ->
                    anime_syncQueries.insert(
                        track.animeId,
                        track.syncId,
                        track.remoteId,
                        track.libraryId,
                        track.title,
                        track.lastEpisodeSeen,
                        track.totalEpisodes,
                        track.status.toLong(),
                        track.score,
                        track.remoteUrl,
                        track.startDate,
                        track.finishDate,
                    )
                }
            }
        }
    }

    internal suspend fun restoreChapters(manga: Manga, chapters: List<tachiyomi.domain.items.chapter.model.Chapter>) {
        val dbChapters = mangaHandler.awaitList { chaptersQueries.getChaptersByMangaId(manga.id!!) }

        val processed = chapters.map { chapter ->
            var chapter = chapter
            val dbChapter = dbChapters.find { it.url == chapter.url }
            if (dbChapter != null) {
                chapter = chapter.copy(id = dbChapter._id)
                chapter = chapter.copyFrom(dbChapter)
                if (dbChapter.read && !chapter.read) {
                    chapter = chapter.copy(read = dbChapter.read, lastPageRead = dbChapter.last_page_read)
                } else if (chapter.lastPageRead == 0L && dbChapter.last_page_read != 0L) {
                    chapter = chapter.copy(lastPageRead = dbChapter.last_page_read)
                }
                if (!chapter.bookmark && dbChapter.bookmark) {
                    chapter = chapter.copy(bookmark = dbChapter.bookmark)
                }
            }

            chapter.copy(mangaId = manga.id ?: -1)
        }

        val newChapters = processed.groupBy { it.id > 0 }
        newChapters[true]?.let { updateKnownChapters(it) }
        newChapters[false]?.let { insertChapters(it) }
    }

    internal suspend fun restoreEpisodes(anime: Anime, episodes: List<tachiyomi.domain.items.episode.model.Episode>) {
        val dbEpisodes = animeHandler.awaitList { episodesQueries.getEpisodesByAnimeId(anime.id!!) }

        val processed = episodes.map { episode ->
            var episode = episode
            val dbEpisode = dbEpisodes.find { it.url == episode.url }
            if (dbEpisode != null) {
                episode = episode.copy(id = dbEpisode._id)
                episode = episode.copyFrom(dbEpisode)
                if (dbEpisode.seen && !episode.seen) {
                    episode = episode.copy(seen = dbEpisode.seen, lastSecondSeen = dbEpisode.last_second_seen)
                } else if (episode.lastSecondSeen == 0L && dbEpisode.last_second_seen != 0L) {
                    episode = episode.copy(lastSecondSeen = dbEpisode.last_second_seen)
                }
                if (!episode.bookmark && dbEpisode.bookmark) {
                    episode = episode.copy(bookmark = dbEpisode.bookmark)
                }
            }

            episode.copy(animeId = anime.id ?: -1)
        }

        val newEpisodes = processed.groupBy { it.id > 0 }
        newEpisodes[true]?.let { updateKnownEpisodes(it) }
        newEpisodes[false]?.let { insertEpisodes(it) }
    }

    /**
     * Returns manga
     *
     * @return [Manga], null if not found
     */
    internal suspend fun getMangaFromDatabase(url: String, source: Long): Mangas? {
        return mangaHandler.awaitOneOrNull { mangasQueries.getMangaByUrlAndSource(url, source) }
    }

    /**
     * Returns anime
     *
     * @return [Anime], null if not found
     */
    internal suspend fun getAnimeFromDatabase(url: String, source: Long): Animes? {
        return animeHandler.awaitOneOrNull { animesQueries.getAnimeByUrlAndSource(url, source) }
    }

    /**
     * Inserts manga and returns id
     *
     * @return id of [Manga], null if not found
     */
    private suspend fun insertManga(manga: Manga): Long {
        return mangaHandler.awaitOne(true) {
            mangasQueries.insert(
                source = manga.source,
                url = manga.url,
                artist = manga.artist,
                author = manga.author,
                description = manga.description,
                genre = manga.genre,
                title = manga.title,
                status = manga.status.toLong(),
                thumbnailUrl = manga.thumbnailUrl,
                favorite = manga.favorite,
                lastUpdate = manga.lastUpdate,
                nextUpdate = 0L,
                calculateInterval = 0L,
                initialized = manga.initialized,
                viewerFlags = manga.viewerFlags,
                chapterFlags = manga.chapterFlags,
                coverLastModified = manga.coverLastModified,
                dateAdded = manga.dateAdded,
                updateStrategy = manga.updateStrategy,
            )
            mangasQueries.selectLastInsertedRowId()
        }
    }

    /**
     * Inserts anime and returns id
     *
     * @return id of [Anime], null if not found
     */
    private suspend fun insertAnime(anime: Anime): Long {
        return animeHandler.awaitOne(true) {
            animesQueries.insert(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre,
                title = anime.title,
                status = anime.status.toLong(),
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                lastUpdate = anime.lastUpdate,
                nextUpdate = 0L,
                calculateInterval = 0L,
                initialized = anime.initialized,
                viewerFlags = anime.viewerFlags,
                episodeFlags = anime.episodeFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                updateStrategy = anime.updateStrategy,
            )
            animesQueries.selectLastInsertedRowId()
        }
    }

    private suspend fun updateManga(manga: Manga): Long {
        mangaHandler.await(true) {
            mangasQueries.update(
                source = manga.source,
                url = manga.url,
                artist = manga.artist,
                author = manga.author,
                description = manga.description,
                genre = manga.genre?.joinToString(separator = ", "),
                title = manga.title,
                status = manga.status,
                thumbnailUrl = manga.thumbnailUrl,
                favorite = manga.favorite.toLong(),
                lastUpdate = manga.lastUpdate,
                nextUpdate = null,
                calculateInterval = null,
                initialized = manga.initialized.toLong(),
                viewer = manga.viewerFlags,
                chapterFlags = manga.chapterFlags,
                coverLastModified = manga.coverLastModified,
                dateAdded = manga.dateAdded,
                mangaId = manga.id!!,
                updateStrategy = manga.updateStrategy.let(updateStrategyAdapter::encode),
            )
        }
        return manga.id
    }

    private suspend fun updateAnime(anime: Anime): Long {
        animeHandler.await(true) {
            animesQueries.update(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre?.joinToString(separator = ", "),
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite.toLong(),
                lastUpdate = anime.lastUpdate,
                nextUpdate = null,
                calculateInterval = null,
                initialized = anime.initialized.toLong(),
                viewer = anime.viewerFlags,
                episodeFlags = anime.episodeFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                animeId = anime.id!!,
                updateStrategy = anime.updateStrategy.let(updateStrategyAdapter::encode),
            )
        }
        return anime.id
    }

    /**
     * Inserts list of chapters
     */
    private suspend fun insertChapters(chapters: List<tachiyomi.domain.items.chapter.model.Chapter>) {
        mangaHandler.await(true) {
            chapters.forEach { chapter ->
                chaptersQueries.insert(
                    chapter.mangaId,
                    chapter.url,
                    chapter.name,
                    chapter.scanlator,
                    chapter.read,
                    chapter.bookmark,
                    chapter.lastPageRead,
                    chapter.chapterNumber,
                    chapter.sourceOrder,
                    chapter.dateFetch,
                    chapter.dateUpload,
                )
            }
        }
    }

    /**
     * Inserts list of episodes
     */
    private suspend fun insertEpisodes(episodes: List<tachiyomi.domain.items.episode.model.Episode>) {
        animeHandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.insert(
                    episode.animeId,
                    episode.url,
                    episode.name,
                    episode.scanlator,
                    episode.seen,
                    episode.bookmark,
                    episode.lastSecondSeen,
                    episode.totalSeconds,
                    episode.episodeNumber,
                    episode.sourceOrder,
                    episode.dateFetch,
                    episode.dateUpload,
                )
            }
        }
    }

    /**
     * Updates a list of chapters
     */
    private suspend fun updateChapters(chapters: List<tachiyomi.domain.items.chapter.model.Chapter>) {
        mangaHandler.await(true) {
            chapters.forEach { chapter ->
                chaptersQueries.update(
                    chapter.mangaId,
                    chapter.url,
                    chapter.name,
                    chapter.scanlator,
                    chapter.read.toLong(),
                    chapter.bookmark.toLong(),
                    chapter.lastPageRead,
                    chapter.chapterNumber.toDouble(),
                    chapter.sourceOrder,
                    chapter.dateFetch,
                    chapter.dateUpload,
                    chapter.id,
                )
            }
        }
    }

    /**
     * Updates a list of episodes
     */
    private suspend fun updateEpisodes(episodes: List<tachiyomi.domain.items.episode.model.Episode>) {
        animeHandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.update(
                    episode.animeId,
                    episode.url,
                    episode.name,
                    episode.scanlator,
                    episode.seen.toLong(),
                    episode.bookmark.toLong(),
                    episode.lastSecondSeen,
                    episode.totalSeconds,
                    episode.episodeNumber.toDouble(),
                    episode.sourceOrder,
                    episode.dateFetch,
                    episode.dateUpload,
                    episode.id,
                )
            }
        }
    }

    /**
     * Updates a list of chapters with known database ids
     */
    private suspend fun updateKnownChapters(chapters: List<tachiyomi.domain.items.chapter.model.Chapter>) {
        mangaHandler.await(true) {
            chapters.forEach { chapter ->
                chaptersQueries.update(
                    mangaId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    read = chapter.read.toLong(),
                    bookmark = chapter.bookmark.toLong(),
                    lastPageRead = chapter.lastPageRead,
                    chapterNumber = null,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    chapterId = chapter.id,
                )
            }
        }
    }

    /**
     * Updates a list of episodes with known database ids
     */
    private suspend fun updateKnownEpisodes(episodes: List<tachiyomi.domain.items.episode.model.Episode>) {
        animeHandler.await(true) {
            episodes.forEach { episode ->
                episodesQueries.update(
                    animeId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    seen = episode.seen.toLong(),
                    bookmark = episode.bookmark.toLong(),
                    lastSecondSeen = episode.lastSecondSeen,
                    totalSeconds = episode.totalSeconds,
                    episodeNumber = null,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    episodeId = episode.id,
                )
            }
        }
    }
}
