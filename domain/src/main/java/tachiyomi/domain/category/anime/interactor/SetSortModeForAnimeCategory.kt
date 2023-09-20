package tachiyomi.domain.category.anime.interactor

import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.anime.model.AnimeLibrarySort
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.service.LibraryPreferences

class SetSortModeForAnimeCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await(categoryId: Long, type: AnimeLibrarySort.Type, direction: AnimeLibrarySort.Direction) {
        val category = categoryRepository.getAnimeCategory(categoryId) ?: return
        val flags = category.flags + type + direction
        if (preferences.categorizedDisplaySettings().get()) {
            categoryRepository.updatePartialAnimeCategory(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.libraryAnimeSortingMode().set(AnimeLibrarySort(type, direction))
            categoryRepository.updateAllAnimeCategoryFlags(flags)
        }
    }

    suspend fun await(category: Category, type: AnimeLibrarySort.Type, direction: AnimeLibrarySort.Direction) {
        await(category.id, type, direction)
    }
}
