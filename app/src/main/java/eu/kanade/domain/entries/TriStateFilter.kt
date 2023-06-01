package eu.kanade.domain.entries

import eu.kanade.tachiyomi.widget.ExtendedNavigationView

enum class TriStateFilter {
    DISABLED, // Disable filter
    ENABLED_IS, // Enabled with "is" filter
    ENABLED_NOT, // Enabled with "not" filter
}

fun TriStateFilter.toTriStateGroupState(): ExtendedNavigationView.Item.TriStateGroup.State {
    return when (this) {
        TriStateFilter.DISABLED -> ExtendedNavigationView.Item.TriStateGroup.State.DISABLED
        TriStateFilter.ENABLED_IS -> ExtendedNavigationView.Item.TriStateGroup.State.ENABLED_IS
        TriStateFilter.ENABLED_NOT -> ExtendedNavigationView.Item.TriStateGroup.State.ENABLED_NOT
    }
}
