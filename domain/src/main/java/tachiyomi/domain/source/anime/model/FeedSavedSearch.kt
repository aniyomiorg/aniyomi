package tachiyomi.domain.source.anime.model

data class FeedSavedSearch(
    // Tag identifier, unique
    val id: Long,

    // Source for the saved search
    val source: Long,

    // If null then get latest/popular, if set get the saved search
    val savedSearch: Long?,

    // If the feed is a global (FeedScreen) or source specific feed (SourceFeedScreen)
    val global: Boolean,
    val feedOrder: Long,
)
