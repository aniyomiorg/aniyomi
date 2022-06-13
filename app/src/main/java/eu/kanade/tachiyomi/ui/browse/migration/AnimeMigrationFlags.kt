package eu.kanade.tachiyomi.ui.browse.migration

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.util.hasCustomCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object AnimeMigrationFlags {

    private const val EPISODES = 0b0001
    private const val CATEGORIES = 0b0010
    private const val TRACK = 0b0100
    private const val CUSTOM_COVER = 0b1000

    private val coverCache: AnimeCoverCache by injectLazy()
    private val db: AnimeDatabaseHelper = Injekt.get()

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
            db.inTransaction {
                if (db.getTracks(anime.id).executeAsBlocking().isNotEmpty()) {
                    titles.add(R.string.track)
                }

                if (anime.hasCustomCover(coverCache)) {
                    titles.add(R.string.custom_cover)
                }
            }
        }
        return titles.toTypedArray()
    }
}
