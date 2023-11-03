package eu.kanade.tachiyomi.ui.browse.anime.migration

import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object AnimeMigrationFlags {

    private const val EPISODES = 0b00001
    private const val CATEGORIES = 0b00010
    private const val TRACK = 0b00100
    private const val CUSTOM_COVER = 0b01000
    private const val DELETE_DOWNLOADED = 0b10000

    private val coverCache: AnimeCoverCache by injectLazy()
    private val getTracks: GetAnimeTracks = Injekt.get()
    private val downloadCache: AnimeDownloadCache by injectLazy()

    val flags get() = arrayOf(EPISODES, CATEGORIES, TRACK, CUSTOM_COVER, DELETE_DOWNLOADED)
    private var enableFlags = emptyList<Int>().toMutableList()

    fun hasEpisodes(value: Int): Boolean {
        return value and EPISODES != 0
    }

    fun hasCategories(value: Int): Boolean {
        return value and CATEGORIES != 0
    }

    fun hasTracks(value: Int): Boolean {
        return value and TRACK != 0
    }

    fun hasCustomCover(value: Int): Boolean {
        return value and CUSTOM_COVER != 0
    }

    fun hasDeleteDownloaded(value: Int): Boolean {
        return value and DELETE_DOWNLOADED != 0
    }

    fun getEnabledFlagsPositions(value: Int): List<Int> {
        return flags.mapIndexedNotNull { index, flag -> if (value and flag != 0) index else null }
    }

    fun getFlagsFromPositions(positions: Array<Int>): Int {
        val fold = positions.fold(0) { accumulated, position -> accumulated or enableFlags[position] }
        enableFlags.clear()
        return fold
    }

    fun titles(anime: Anime?): Array<Int> {
        enableFlags.add(EPISODES)
        enableFlags.add(CATEGORIES)
        val titles = arrayOf(R.string.episodes, R.string.anime_categories).toMutableList()
        if (anime != null) {
            if (runBlocking { getTracks.await(anime.id) }.isNotEmpty()) {
                titles.add(R.string.track)
                enableFlags.add(TRACK)
            }

            if (anime.hasCustomCover(coverCache)) {
                titles.add(R.string.custom_cover)
                enableFlags.add(CUSTOM_COVER)
            }
            if (downloadCache.getDownloadCount(anime) > 0) {
                titles.add(R.string.delete_downloaded)
                enableFlags.add(DELETE_DOWNLOADED)
            }
        }
        return titles.toTypedArray()
    }
}
