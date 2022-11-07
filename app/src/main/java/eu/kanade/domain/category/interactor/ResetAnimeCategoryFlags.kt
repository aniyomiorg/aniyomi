package eu.kanade.domain.category.interactor

import eu.kanade.domain.category.repository.CategoryRepositoryAnime
import eu.kanade.domain.library.model.plus
import eu.kanade.domain.library.service.LibraryPreferences

class ResetAnimeCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepositoryAnime,
) {

    suspend fun await() {
        val display = preferences.libraryDisplayMode().get()
        val sort = preferences.librarySortingMode().get()
        categoryRepository.updateAllFlags(display + sort.type + sort.direction)
    }
}
