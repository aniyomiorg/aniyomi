package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime

/**
 * A source that may handle opening an SAnime for a given URI.
 *
 * @since extensions-lib 1.5
 */
interface ResolvableAnimeSource : AnimeSource {

    /**
     * Whether this source may potentially handle the given URI.
     *
     * @since extensions-lib 1.5
     */
    fun canResolveUri(uri: String): Boolean

    /**
     * Called if canHandleUri is true. Returns the corresponding SAnime, if possible.
     *
     * @since extensions-lib 1.5
     */
    suspend fun getAnime(uri: String): SAnime?
}
