package eu.kanade.domain.category.manga.interactor

import eu.kanade.domain.library.service.LibraryPreferences
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.manga.model.MangaLibrarySort
import tachiyomi.domain.library.model.plus

class SetSortModeForMangaCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await(categoryId: Long, type: MangaLibrarySort.Type, direction: MangaLibrarySort.Direction) {
        val category = categoryRepository.getMangaCategory(categoryId) ?: return
        val flags = category.flags + type + direction
        if (preferences.categorizedDisplaySettings().get()) {
            categoryRepository.updatePartialMangaCategory(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.libraryMangaSortingMode().set(MangaLibrarySort(type, direction))
            categoryRepository.updateAllMangaCategoryFlags(flags)
        }
    }

    suspend fun await(category: Category, type: MangaLibrarySort.Type, direction: MangaLibrarySort.Direction) {
        await(category.id, type, direction)
    }
}
