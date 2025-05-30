package eu.kanade.tachiyomi.ui.library.manga

import eu.kanade.tachiyomi.source.manga.getNameForMangaInfo
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaLibraryItem(
    val libraryManga: LibraryManga,
    var downloadCount: Long = -1,
    var unreadCount: Long = -1,
    var isLocal: Boolean = false,
    var sourceLanguage: String = "",
    private val sourceManager: MangaSourceManager = Injekt.get(),
) {
    /**
     * Checks if a query matches the manga
     *
     * @param constraint the query to check.
     * @return true if the manga matches the query, false otherwise.
     */
    fun matches(constraint: String): Boolean {
        val sourceName by lazy { sourceManager.getOrStub(libraryManga.manga.source).getNameForMangaInfo() }
        if (constraint.startsWith("id:", true)) {
            val id = constraint.substringAfter("id:").toLongOrNull()
            return libraryManga.id == id
        }
        return libraryManga.manga.title.contains(constraint, true) ||
            (libraryManga.manga.author?.contains(constraint, true) ?: false) ||
            (libraryManga.manga.artist?.contains(constraint, true) ?: false) ||
            (libraryManga.manga.description?.contains(constraint, true) ?: false) ||
            constraint.split(",").map { it.trim() }.all { subconstraint ->
                checkNegatableConstraint(subconstraint) {
                    sourceName.contains(it, true) ||
                        (libraryManga.manga.genre?.any { genre -> genre.equals(it, true) } ?: false)
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
