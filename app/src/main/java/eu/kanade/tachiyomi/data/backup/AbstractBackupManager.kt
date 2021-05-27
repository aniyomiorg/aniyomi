package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.toAnimeInfo
import eu.kanade.tachiyomi.data.database.models.toMangaInfo
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.toSChapter
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.episode.syncEpisodesWithSource
import uy.kohesive.injekt.injectLazy

abstract class AbstractBackupManager(protected val context: Context) {

    internal val databaseHelper: DatabaseHelper by injectLazy()
    internal val animedatabaseHelper: AnimeDatabaseHelper by injectLazy()
    internal val sourceManager: SourceManager by injectLazy()
    internal val trackManager: TrackManager by injectLazy()
    protected val preferences: PreferencesHelper by injectLazy()

    abstract fun createBackup(uri: Uri, flags: Int, isJob: Boolean): String?

    /**
     * Returns manga
     *
     * @return [Manga], null if not found
     */
    internal fun getMangaFromDatabase(manga: Manga): Manga? =
        databaseHelper.getManga(manga.url, manga.source).executeAsBlocking()

    /**
     * Returns manga
     *
     * @return [Manga], null if not found
     */
    internal fun getAnimeFromDatabase(anime: Anime): Anime? =
        animedatabaseHelper.getAnime(anime.url, anime.source).executeAsBlocking()

    /**
     * Fetches chapter information.
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @param chapters list of chapters in the backup
     * @return Updated manga chapters.
     */
    internal suspend fun restoreChapters(source: Source, manga: Manga, chapters: List<Chapter>): Pair<List<Chapter>, List<Chapter>> {
        val fetchedChapters = source.getChapterList(manga.toMangaInfo())
            .map { it.toSChapter() }
        val syncedChapters = syncChaptersWithSource(databaseHelper, fetchedChapters, manga, source)
        if (syncedChapters.first.isNotEmpty()) {
            chapters.forEach { it.manga_id = manga.id }
            updateChapters(chapters)
        }
        return syncedChapters
    }

    /**
     * Fetches chapter information.
     *
     * @param source source of manga
     * @param manga manga that needs updating
     * @param chapters list of chapters in the backup
     * @return Updated manga chapters.
     */
    internal suspend fun restoreEpisodes(source: AnimeSource, anime: Anime, episodes: List<Episode>): Pair<List<Episode>, List<Episode>> {
        val fetchedEpisodes = source.getEpisodeList(anime.toAnimeInfo())
            .map { it.toSEpisode() }
        val syncedEpisodes = syncEpisodesWithSource(animedatabaseHelper, fetchedEpisodes, anime, source)
        if (syncedEpisodes.first.isNotEmpty()) {
            episodes.forEach { it.anime_id = anime.id }
            updateEpisodes(episodes)
        }
        return syncedEpisodes
    }

    /**
     * Returns list containing manga from library
     *
     * @return [Manga] from library
     */
    protected fun getFavoriteManga(): List<Manga> =
        databaseHelper.getFavoriteMangas().executeAsBlocking()

    /**
     * Returns list containing manga from library
     *
     * @return [Manga] from library
     */
    protected fun getFavoriteAnime(): List<Anime> =
        animedatabaseHelper.getFavoriteAnimes().executeAsBlocking()

    /**
     * Inserts manga and returns id
     *
     * @return id of [Manga], null if not found
     */
    internal fun insertManga(manga: Manga): Long? =
        databaseHelper.insertManga(manga).executeAsBlocking().insertedId()

    /**
     * Inserts list of chapters
     */
    protected fun insertChapters(chapters: List<Chapter>) {
        databaseHelper.insertChapters(chapters).executeAsBlocking()
    }

    /**
     * Updates a list of chapters
     */
    protected fun updateChapters(chapters: List<Chapter>) {
        databaseHelper.updateChaptersBackup(chapters).executeAsBlocking()
    }

    /**
     * Updates a list of chapters with known database ids
     */
    protected fun updateKnownChapters(chapters: List<Chapter>) {
        databaseHelper.updateKnownChaptersBackup(chapters).executeAsBlocking()
    }

    /**
     * Inserts anime and returns id
     *
     * @return id of [Anime], null if not found
     */
    internal fun insertAnime(anime: Anime): Long? =
        animedatabaseHelper.insertAnime(anime).executeAsBlocking().insertedId()

    /**
     * Inserts list of chapters
     */
    protected fun insertEpisodes(episodes: List<Episode>) {
        animedatabaseHelper.insertEpisodes(episodes).executeAsBlocking()
    }

    /**
     * Updates a list of chapters
     */
    protected fun updateEpisodes(episodes: List<Episode>) {
        animedatabaseHelper.updateEpisodesBackup(episodes).executeAsBlocking()
    }

    /**
     * Updates a list of chapters with known database ids
     */
    protected fun updateKnownEpisodes(episodes: List<Episode>) {
        animedatabaseHelper.updateKnownEpisodesBackup(episodes).executeAsBlocking()
    }

    /**
     * Return number of backups.
     *
     * @return number of backups selected by user
     */
    protected fun numberOfBackups(): Int = preferences.numberOfBackups().get()
}
