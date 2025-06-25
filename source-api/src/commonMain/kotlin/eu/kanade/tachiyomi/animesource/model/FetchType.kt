package eu.kanade.tachiyomi.animesource.model

/**
 * Define what type of content the anime should fetch.
 * The fetch type for a [SAnime] will not change after it's been initialized
 * to either Seasons or Episodes
 *
 * @since extensions-lib 16
 */
enum class FetchType {
    /**
     * Unknown, [SAnime] will call both `getEpisodeList` and `getSeasonList`
     * and use the first non-empty one, prioritizing episodes.
     */
    Unknown,

    /**
     * [SAnime] will only call `getSeasonList`.
     */
    Seasons,

    /**
     * [SAnime] will only call `getEpisodeList`.
     */
    Episodes,
}
