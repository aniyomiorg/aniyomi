package eu.kanade.tachiyomi.widget

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.source.model.Filter
import tachiyomi.domain.entries.TriStateFilter

fun Int.toTriStateFilter(): TriStateFilter {
    return when (this) {
        Filter.TriState.STATE_IGNORE -> TriStateFilter.DISABLED
        Filter.TriState.STATE_INCLUDE -> TriStateFilter.ENABLED_IS
        Filter.TriState.STATE_EXCLUDE -> TriStateFilter.ENABLED_NOT

        AnimeFilter.TriState.STATE_IGNORE -> TriStateFilter.DISABLED
        AnimeFilter.TriState.STATE_INCLUDE -> TriStateFilter.ENABLED_IS
        AnimeFilter.TriState.STATE_EXCLUDE -> TriStateFilter.ENABLED_NOT

        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}
