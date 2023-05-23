package eu.kanade.domain.category.manga.interactor

import eu.kanade.domain.library.service.LibraryPreferences
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.library.model.plus

class ResetMangaCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await() {
        val display = preferences.libraryDisplayMode().get()
        val sort = preferences.librarySortingMode().get()
        categoryRepository.updateAllMangaCategoryFlags(display + sort.type + sort.direction)
    }
}
