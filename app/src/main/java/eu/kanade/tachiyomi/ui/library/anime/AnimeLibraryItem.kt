package eu.kanade.tachiyomi.ui.library.anime

import eu.kanade.tachiyomi.source.anime.getNameForAnimeInfo
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class AnimeLibraryItem(
    val libraryAnime: LibraryAnime,
    var downloadCount: Long = -1,
    var unseenCount: Long = -1,
    var isLocal: Boolean = false,
    var sourceLanguage: String = "",
    private val sourceManager: AnimeSourceManager = Injekt.get(),
) {
    /**
     * Checks if a query matches the anime
     *
     * @param constraint the query to check.
     * @return true if the anime matches the query, false otherwise.
     */
    fun matches(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(libraryAnime.anime.source).getNameForAnimeInfo() }
        if (constraint.startsWith("id:", true)) {
            val id = constraint.substringAfter("id:").toLongOrNull()
            return libraryAnime.id == id
        }
        return libraryAnime.anime.title.contains(constraint, true) ||
            (libraryAnime.anime.author?.contains(constraint, true) ?: false) ||
            (libraryAnime.anime.artist?.contains(constraint, true) ?: false) ||
            (libraryAnime.anime.description?.contains(constraint, true) ?: false) ||
            constraint.split(",").map { it.trim() }.all { subconstraint ->
                checkNegatableConstraint(subconstraint) {
                    sourceName.contains(it, true) ||
                        (libraryAnime.anime.genre?.any { genre -> genre.equals(it, true) } ?: false)
                }
            }
    }

    /**
     * Checks a predicate on a negatable constraint. If the constraint starts with a minus character,
     * the minus is stripped and the result of the predicate is inverted.
     *
     * @param constraint the argument to the predicate. Inverts the predicate if it starts with '-'.
     * @param predicate the check to be run against the constraint.
     * @return !predicate(x) if constraint = "-x", otherwise predicate(constraint)
     */
    private fun checkNegatableConstraint(
        constraint: String,
        predicate: (String) -> Boolean,
    ): Boolean {
        return if (constraint.startsWith("-")) {
            !predicate(constraint.substringAfter("-").trimStart())
        } else {
            predicate(constraint)
        }
    }
}
