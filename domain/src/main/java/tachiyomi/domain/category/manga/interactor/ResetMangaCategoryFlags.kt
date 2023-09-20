package tachiyomi.domain.category.manga.interactor

import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.service.LibraryPreferences

class ResetMangaCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await() {
        val display = preferences.libraryDisplayMode().get()
        val sort = preferences.libraryMangaSortingMode().get()
        categoryRepository.updateAllMangaCategoryFlags(display + sort.type + sort.direction)
    }
}
