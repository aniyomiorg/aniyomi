package eu.kanade.tachiyomi.ui.browse.manga.migration

import eu.kanade.domain.entries.manga.model.hasCustomCover
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.MangaCoverCache
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

object MangaMigrationFlags {

    private const val CHAPTERS = 0b0001
    private const val CATEGORIES = 0b0010
    private const val TRACK = 0b0100
    private const val CUSTOM_COVER = 0b1000

    private val coverCache: MangaCoverCache by injectLazy()
    private val getTracks: GetMangaTracks = Injekt.get()

    val flags get() = arrayOf(CHAPTERS, CATEGORIES, TRACK, CUSTOM_COVER)

    fun hasChapters(value: Int): Boolean {
        return value and CHAPTERS != 0
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

    fun titles(manga: Manga?): Array<Int> {
        val titles = arrayOf(R.string.chapters, R.string.manga_categories).toMutableList()
        if (manga != null) {
            if (runBlocking { getTracks.await(manga.id) }.isNotEmpty()) {
                titles.add(R.string.track)
            }

            if (manga.hasCustomCover(coverCache)) {
                titles.add(R.string.custom_cover)
            }
        }
        return titles.toTypedArray()
    }
}
