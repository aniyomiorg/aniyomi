package eu.kanade.domain.category.anime.interactor

import eu.kanade.domain.library.service.LibraryPreferences
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.library.model.plus

class ResetAnimeCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await() {
        val display = preferences.libraryDisplayMode().get()
        val sort = preferences.librarySortingMode().get()
        categoryRepository.updateAllAnimeCategoryFlags(display + sort.type + sort.direction)
    }
}
