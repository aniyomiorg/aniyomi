package eu.kanade.tachiyomi.data.backup.full

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.backup.AbstractBackupManager
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CATEGORY
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CATEGORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CHAPTER
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_CHAPTER_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_HISTORY
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_HISTORY_MASK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_TRACK
import eu.kanade.tachiyomi.data.backup.BackupCreateService.Companion.BACKUP_TRACK_MASK
import eu.kanade.tachiyomi.data.backup.full.models.Backup
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeHistory
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeSource
import eu.kanade.tachiyomi.data.backup.full.models.BackupAnimeTracking
import eu.kanade.tachiyomi.data.backup.full.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.full.models.BackupEpisode
import eu.kanade.tachiyomi.data.backup.full.models.BackupFull
import eu.kanade.tachiyomi.data.backup.full.models.BackupSerializer
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeCategory
import eu.kanade.tachiyomi.data.database.models.AnimeHistory
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import okio.buffer
import okio.gzip
import okio.sink
import kotlin.math.max

class FullBackupManager(context: Context) : AbstractBackupManager(context) {

    val parser = ProtoBuf

    /**
     * Create backup Json file from database
     *
     * @param uri path of Uri
     * @param isJob backup called from job
     */
    override fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String? {
        // Create root object
        var backup: Backup? = null

        animedatabaseHelper.inTransaction {
            val databaseAnime = getFavoriteAnime()

            backup = Backup(
                backupAnime(databaseAnime, flags),
                backupCategoriesAnime(),
                emptyList(),
                backupAnimeExtensionInfo(databaseAnime)
            )
        }

        try {
            val file: UniFile = (
                if (isJob) {
                    // Get dir of file and create
                    var dir = UniFile.fromUri(context, uri)
                    dir = dir.createDirectory("automatic")

                    // Delete older backups
                    val numberOfBackups = numberOfBackups()
                    val backupRegex = Regex("""animite_\d+-\d+-\d+_\d+-\d+.proto.gz""")
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

            val byteArray = parser.encodeToByteArray(BackupSerializer, backup!!)
            file.openOutputStream().sink().gzip().buffer().use { it.write(byteArray) }
            val fileUri = file.uri

            // Validate it to make sure it works
            FullBackupRestoreValidator().validate(context, fileUri)

            return fileUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            throw e
        }
    }

    private fun backupAnime(animes: List<Anime>, flags: Int): List<BackupAnime> {
        return animes.map {
            backupAnimeObject(it, flags)
        }
    }

    private fun backupAnimeExtensionInfo(animes: List<Anime>): List<BackupAnimeSource> {
        return animes
            .asSequence()
            .map { it.source }
            .distinct()
            .map { animesourceManager.getOrStub(it) }
            .map { BackupAnimeSource.copyFrom(it) }
            .toList()
    }

    /**
     * Backup the categories of library
     *
     * @return list of [BackupCategory] to be backed up
     */
    private fun backupCategoriesAnime(): List<BackupCategory> {
        return animedatabaseHelper.getCategories()
            .executeAsBlocking()
            .map { BackupCategory.copyFrom(it) }
    }

    /**
     * Convert an anime to Json
     *
     * @param anime manga that gets converted
     * @param options options for the backup
     * @return [BackupAnime] containing anime in a serializable form
     */
    private fun backupAnimeObject(anime: Anime, options: Int): BackupAnime {
        // Entry for this manga
        val animeObject = BackupAnime.copyFrom(anime)

        // Check if user wants chapter information in backup
        if (options and BACKUP_CHAPTER_MASK == BACKUP_CHAPTER) {
            // Backup all the chapters
            val episodes = animedatabaseHelper.getEpisodes(anime).executeAsBlocking()
            if (episodes.isNotEmpty()) {
                animeObject.episodes = episodes.map { BackupEpisode.copyFrom(it) }
            }
        }

        // Check if user wants category information in backup
        if (options and BACKUP_CATEGORY_MASK == BACKUP_CATEGORY) {
            // Backup categories for this manga
            val categoriesForAnime = animedatabaseHelper.getCategoriesForAnime(anime).executeAsBlocking()
            if (categoriesForAnime.isNotEmpty()) {
                animeObject.categories = categoriesForAnime.mapNotNull { it.order }
            }
        }

        // Check if user wants track information in backup
        if (options and BACKUP_TRACK_MASK == BACKUP_TRACK) {
            val tracks = animedatabaseHelper.getTracks(anime).executeAsBlocking()
            if (tracks.isNotEmpty()) {
                animeObject.tracking = tracks.map { BackupAnimeTracking.copyFrom(it) }
            }
        }

        // Check if user wants history information in backup
        if (options and BACKUP_HISTORY_MASK == BACKUP_HISTORY) {
            val historyForAnime = animedatabaseHelper.getHistoryByAnimeId(anime.id!!).executeAsBlocking()
            if (historyForAnime.isNotEmpty()) {
                val history = historyForAnime.mapNotNull { history ->
                    val url = animedatabaseHelper.getEpisode(history.episode_id).executeAsBlocking()?.url
                    url?.let { BackupAnimeHistory(url, history.last_seen) }
                }
                if (history.isNotEmpty()) {
                    animeObject.history = history
                }
            }
        }

        return animeObject
    }

    fun restoreAnimeNoFetch(anime: Anime, dbAnime: Anime) {
        anime.id = dbAnime.id
        anime.copyFrom(dbAnime)
        insertAnime(anime)
    }

    /**
     * Fetches anime information
     *
     * @param anime anime that needs updating
     * @return Updated anime info.
     */
    fun restoreAnime(anime: Anime): Anime {
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
    internal fun restoreCategoriesAnime(backupCategories: List<BackupCategory>) {
        // Get categories from file and from db
        val dbCategories = animedatabaseHelper.getCategories().executeAsBlocking()

        // Iterate over them
        backupCategories.map { it.getCategoryImpl() }.forEach { category ->
            // Used to know if the category is already in the db
            var found = false
            for (dbCategory in dbCategories) {
                // If the category is already in the db, assign the id to the file's category
                // and do nothing
                if (category.name == dbCategory.name) {
                    category.id = dbCategory.id
                    found = true
                    break
                }
            }
            // If the category isn't in the db, remove the id and insert a new category
            // Store the inserted id in the category
            if (!found) {
                // Let the db assign the id
                category.id = null
                val result = animedatabaseHelper.insertCategory(category).executeAsBlocking()
                category.id = result.insertedId()?.toInt()
            }
        }
    }

    /**
     * Restores the categories an anime is in.
     *
     * @param anime the anime whose categories have to be restored.
     * @param categories the categories to restore.
     */
    internal fun restoreCategoriesForAnime(anime: Anime, categories: List<Int>, backupCategories: List<BackupCategory>) {
        val dbCategories = animedatabaseHelper.getCategories().executeAsBlocking()
        val animeCategoriesToUpdate = ArrayList<AnimeCategory>(categories.size)
        categories.forEach { backupCategoryOrder ->
            backupCategories.firstOrNull {
                it.order == backupCategoryOrder
            }?.let { backupCategory ->
                dbCategories.firstOrNull { dbCategory ->
                    dbCategory.name == backupCategory.name
                }?.let { dbCategory ->
                    animeCategoriesToUpdate += AnimeCategory.create(anime, dbCategory)
                }
            }
        }

        // Update database
        if (animeCategoriesToUpdate.isNotEmpty()) {
            animedatabaseHelper.deleteOldAnimesCategories(listOf(anime)).executeAsBlocking()
            animedatabaseHelper.insertAnimesCategories(animeCategoriesToUpdate).executeAsBlocking()
        }
    }

    /**
     * Restore history from Json
     *
     * @param history list containing history to be restored
     */
    internal fun restoreHistoryForAnime(history: List<BackupAnimeHistory>) {
        // List containing history to be updated
        val historyToBeUpdated = ArrayList<AnimeHistory>(history.size)
        for ((url, lastSeen) in history) {
            val dbHistory = animedatabaseHelper.getHistoryByEpisodeUrl(url).executeAsBlocking()
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory.apply {
                    last_seen = max(lastSeen, dbHistory.last_seen)
                }
                historyToBeUpdated.add(dbHistory)
            } else {
                // If not in database create
                animedatabaseHelper.getEpisode(url).executeAsBlocking()?.let {
                    val historyToAdd = AnimeHistory.create(it).apply {
                        last_seen = lastSeen
                    }
                    historyToBeUpdated.add(historyToAdd)
                }
            }
        }
        animedatabaseHelper.updateAnimeHistoryLastSeen(historyToBeUpdated).executeAsBlocking()
    }

    /**
     * Restores the sync of an anime.
     *
     * @param anime the anime whose sync have to be restored.
     * @param tracks the track list to restore.
     */
    internal fun restoreTrackForAnime(anime: Anime, tracks: List<AnimeTrack>) {
        // Fix foreign keys with the current anime id
        tracks.map { it.anime_id = anime.id!! }

        // Get tracks from database
        val dbTracks = animedatabaseHelper.getTracks(anime).executeAsBlocking()
        val trackToUpdate = mutableListOf<AnimeTrack>()

        tracks.forEach { track ->
            var isInDatabase = false
            for (dbTrack in dbTracks) {
                if (track.sync_id == dbTrack.sync_id) {
                    // The sync is already in the db, only update its fields
                    if (track.media_id != dbTrack.media_id) {
                        dbTrack.media_id = track.media_id
                    }
                    if (track.library_id != dbTrack.library_id) {
                        dbTrack.library_id = track.library_id
                    }
                    dbTrack.last_episode_seen = max(dbTrack.last_episode_seen, track.last_episode_seen)
                    isInDatabase = true
                    trackToUpdate.add(dbTrack)
                    break
                }
            }
            if (!isInDatabase) {
                // Insert new sync. Let the db assign the id
                track.id = null
                trackToUpdate.add(track)
            }
        }
        // Update database
        if (trackToUpdate.isNotEmpty()) {
            animedatabaseHelper.insertTracks(trackToUpdate).executeAsBlocking()
        }
    }

    internal fun restoreEpisodesForAnime(anime: Anime, episodes: List<Episode>) {
        val dbEpisodes = animedatabaseHelper.getEpisodes(anime).executeAsBlocking()

        episodes.forEach { episode ->
            val dbEpisode = dbEpisodes.find { it.url == episode.url }
            if (dbEpisode != null) {
                episode.id = dbEpisode.id
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
