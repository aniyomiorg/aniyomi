package eu.kanade.tachiyomi.animesource.model

/**
 * A named group of anime, that are related to a specific anime entry
 *
 * @since extensions-lib 17
 */
class AnimeRelation(
    val name: String,
    val animes: List<SAnime>,
)
