package eu.kanade.domain.category.manga.interactor

import eu.kanade.domain.library.service.LibraryPreferences
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.plus

class SetSortModeForMangaCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await(categoryId: Long, type: LibrarySort.Type, direction: LibrarySort.Direction) {
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
            preferences.librarySortingMode().set(LibrarySort(type, direction))
            categoryRepository.updateAllMangaCategoryFlags(flags)
        }
    }

    suspend fun await(category: Category, type: LibrarySort.Type, direction: LibrarySort.Direction) {
        await(category.id, type, direction)
    }
}
