package tachiyomi.domain.source.anime.model

data class SavedSearch(
    // Tag identifier, unique
    val id: Long,

    // The source the saved search is for
    val source: Long,

    // If false the manga will not grab chapter updates
    val name: String,

    // The query if there is any
    val query: String?,

    // The filter list
    val filtersJson: String?,
)
