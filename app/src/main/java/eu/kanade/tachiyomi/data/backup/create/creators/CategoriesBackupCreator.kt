package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.backupCategoryMapper
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.manga.interactor.GetMangaCategories
import tachiyomi.domain.category.model.Category
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CategoriesBackupCreator(
    private val getAnimeCategories: GetAnimeCategories = Injekt.get(),
    private val getMangaCategories: GetMangaCategories = Injekt.get(),
) {

    suspend fun backupAnimeCategories(): List<BackupCategory> {
        return getAnimeCategories.await()
            .filterNot(Category::isSystemCategory)
            .map(backupCategoryMapper)
    }

    suspend fun backupMangaCategories(): List<BackupCategory> {
        return getMangaCategories.await()
            .filterNot(Category::isSystemCategory)
            .map(backupCategoryMapper)
    }
}
