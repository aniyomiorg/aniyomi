package tachiyomi.domain.category.anime.interactor

import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.service.LibraryPreferences

class SetDisplayModeForAnimeCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: AnimeCategoryRepository,
) {

    suspend fun await(categoryId: Long, display: LibraryDisplayMode) {
        val category = categoryRepository.getAnimeCategory(categoryId) ?: return
        val flags = category.flags + display
        if (preferences.categorizedDisplaySettings().get()) {
            categoryRepository.updatePartialAnimeCategory(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.libraryDisplayMode().set(display)
            categoryRepository.updateAllAnimeCategoryFlags(flags)
        }
    }

    suspend fun await(category: Category, display: LibraryDisplayMode) {
        await(category.id, display)
    }
}
