package tachiyomi.domain.library.manga.model

import tachiyomi.domain.R

object MangaLibraryGroup {

    const val BY_DEFAULT = 0
    const val BY_SOURCE = 1
    const val BY_STATUS = 2
    const val BY_TRACK_STATUS = 3
    const val UNGROUPED = 4

    fun groupTypeStringRes(type: Int, hasCategories: Boolean = true): Int {
        return when (type) {
            BY_STATUS -> R.string.status
            BY_SOURCE -> R.string.label_sources
            BY_TRACK_STATUS -> R.string.tracking_status
            UNGROUPED -> R.string.ungrouped
            else -> if (hasCategories) R.string.categories else R.string.ungrouped
        }
    }
}
