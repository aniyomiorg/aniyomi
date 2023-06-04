package eu.kanade.domain.entries

enum class TriStateFilter {
    DISABLED, // Disable filter
    ENABLED_IS, // Enabled with "is" filter
    ENABLED_NOT, // Enabled with "not" filter
}

fun TriStateFilter.toTriStateGroupState(): TriStateFilter {
    return when (this) {
        TriStateFilter.DISABLED -> TriStateFilter.DISABLED
        TriStateFilter.ENABLED_IS -> TriStateFilter.ENABLED_IS
        TriStateFilter.ENABLED_NOT -> TriStateFilter.ENABLED_NOT
    }
}
