package eu.kanade.domain.category.manga.interactor

import eu.kanade.domain.library.service.LibraryPreferences
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.plus

class SetDisplayModeForMangaCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: MangaCategoryRepository,
) {

    suspend fun await(categoryId: Long, display: LibraryDisplayMode) {
        val category = categoryRepository.getMangaCategory(categoryId) ?: return
        val flags = category.flags + display
        if (preferences.categorizedDisplaySettings().get()) {
            categoryRepository.updatePartialMangaCategory(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.libraryDisplayMode().set(display)
            categoryRepository.updateAllMangaCategoryFlags(flags)
        }
    }

    suspend fun await(category: Category, display: LibraryDisplayMode) {
        await(category.id, display)
    }
}
