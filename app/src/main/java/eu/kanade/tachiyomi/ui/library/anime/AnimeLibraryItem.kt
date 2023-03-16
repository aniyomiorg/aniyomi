package eu.kanade.tachiyomi.ui.library.anime

import eu.kanade.domain.library.anime.LibraryAnime
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.source.anime.getNameForAnimeInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeLibraryItem(
    val libraryAnime: LibraryAnime,
    private val sourceManager: AnimeSourceManager = Injekt.get(),
) {

    var displayMode: Long = -1
    var downloadCount: Long = -1
    var unseenCount: Long = -1
    var isLocal = false
    var sourceLanguage = ""

    /**
     * Checks if a query matches the anime
     *
     * @param constraint the query to check.
     * @return true if the anime matches the query, false otherwise.
     */
    fun matches(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(libraryAnime.anime.source).getNameForAnimeInfo() }
        val genres by lazy { libraryAnime.anime.genre }
        return libraryAnime.anime.title.contains(constraint, true) ||
            (libraryAnime.anime.author?.contains(constraint, true) ?: false) ||
            (libraryAnime.anime.artist?.contains(constraint, true) ?: false) ||
            (libraryAnime.anime.description?.contains(constraint, true) ?: false) ||
            if (constraint.contains(",")) {
                constraint.split(",").all { containsSourceOrGenre(it.trim(), sourceName, genres) }
            } else {
                containsSourceOrGenre(constraint, sourceName, genres)
            }
    }

    /**
     * Filters an anime by checking whether the query is the anime's source OR part of
     * the genres of the anime
     * Checking for genre is done only if the query isn't part of the source name.
     *
     * @param query the query to check
     * @param sourceName name of the anime's source
     * @param genres list containing anime's genres
     */
    private fun containsSourceOrGenre(query: String, sourceName: String, genres: List<String>?): Boolean {
        val minus = query.startsWith("-")
        val tag = if (minus) { query.substringAfter("-") } else query
        return when (sourceName.contains(tag, true)) {
            false -> containsGenre(query, genres)
            else -> !minus
        }
    }

    private fun containsGenre(tag: String, genres: List<String>?): Boolean {
        return if (tag.startsWith("-")) {
            genres?.find {
                it.trim().equals(tag.substringAfter("-"), ignoreCase = true)
            } == null
        } else {
            genres?.find {
                it.trim().equals(tag, ignoreCase = true)
            } != null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnimeLibraryItem

        if (libraryAnime != other.libraryAnime) return false
        if (sourceManager != other.sourceManager) return false
        if (displayMode != other.displayMode) return false
        if (downloadCount != other.downloadCount) return false
        if (unseenCount != other.unseenCount) return false
        if (isLocal != other.isLocal) return false
        if (sourceLanguage != other.sourceLanguage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = libraryAnime.hashCode()
        result = 31 * result + sourceManager.hashCode()
        result = 31 * result + displayMode.hashCode()
        result = 31 * result + downloadCount.toInt()
        result = 31 * result + unseenCount.toInt()
        result = 31 * result + isLocal.hashCode()
        result = 31 * result + sourceLanguage.hashCode()
        return result
    }
}
