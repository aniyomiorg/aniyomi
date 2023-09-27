package tachiyomi.domain.category.anime.interactor

import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.service.LibraryPreferences

class ResetAnimeCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await() {
        val display = preferences.libraryDisplayMode().get()
        val sort = preferences.libraryAnimeSortingMode().get()
        categoryRepository.updateAllAnimeCategoryFlags(display + sort.type + sort.direction)
    }
}
