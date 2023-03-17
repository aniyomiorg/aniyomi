package eu.kanade.domain.category.anime.interactor

import eu.kanade.domain.category.anime.repository.AnimeCategoryRepository
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.category.model.CategoryUpdate
import eu.kanade.domain.library.model.LibrarySort
import eu.kanade.domain.library.model.plus
import eu.kanade.domain.library.service.LibraryPreferences

class SetSortModeForAnimeCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await(categoryId: Long, type: LibrarySort.Type, direction: LibrarySort.Direction) {
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
            preferences.librarySortingMode().set(LibrarySort(type, direction))
            categoryRepository.updateAllAnimeCategoryFlags(flags)
        }
    }

    suspend fun await(category: Category, type: LibrarySort.Type, direction: LibrarySort.Direction) {
        await(category.id, type, direction)
    }
}
