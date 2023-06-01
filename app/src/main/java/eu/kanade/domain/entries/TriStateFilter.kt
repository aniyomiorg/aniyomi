package eu.kanade.domain.entries

import eu.kanade.tachiyomi.widget.TriState

enum class TriStateFilter {
    DISABLED, // Disable filter
    ENABLED_IS, // Enabled with "is" filter
    ENABLED_NOT, // Enabled with "not" filter
}

fun TriStateFilter.toTriStateGroupState(): TriState {
    return when (this) {
        TriStateFilter.DISABLED -> TriState.DISABLED
        TriStateFilter.ENABLED_IS -> TriState.ENABLED_IS
        TriStateFilter.ENABLED_NOT -> TriState.ENABLED_NOT
    }
}
