package eu.kanade.tachiyomi.ui.browse.anime.migration

import eu.kanade.domain.entries.anime.model.hasCustomCover
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object AnimeMigrationFlags {

    private const val EPISODES = 0b0001
    private const val CATEGORIES = 0b0010
    private const val TRACK = 0b0100
    private const val CUSTOM_COVER = 0b1000

    private val coverCache: AnimeCoverCache by injectLazy()
    private val getTracks: GetAnimeTracks = Injekt.get()

    val flags get() = arrayOf(EPISODES, CATEGORIES, TRACK, CUSTOM_COVER)

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

    fun getEnabledFlagsPositions(value: Int): List<Int> {
        return flags.mapIndexedNotNull { index, flag -> if (value and flag != 0) index else null }
    }

    fun getFlagsFromPositions(positions: Array<Int>): Int {
        return positions.fold(0) { accumulated, position -> accumulated or (1 shl position) }
    }

    fun titles(anime: Anime?): Array<Int> {
        val titles = arrayOf(R.string.episodes, R.string.anime_categories).toMutableList()
        if (anime != null) {
            if (runBlocking { getTracks.await(anime.id) }.isNotEmpty()) {
                titles.add(R.string.track)
            }

            if (anime.hasCustomCover(coverCache)) {
                titles.add(R.string.custom_cover)
            }
        }
        return titles.toTypedArray()
    }
}
