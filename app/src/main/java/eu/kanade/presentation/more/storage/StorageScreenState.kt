package eu.kanade.presentation.more.storage

import androidx.compose.runtime.Immutable
import tachiyomi.domain.category.model.Category

sealed class StorageScreenState {
    @Immutable
    object Loading : StorageScreenState()

    @Immutable
    data class Success(
        val selectedCategory: Category,
        val items: List<StorageItem>,
        val categories: List<Category>,
    ) : StorageScreenState()
}
