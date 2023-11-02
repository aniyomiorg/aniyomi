package eu.kanade.tachiyomi.ui.storage

import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.util.fastDistinctBy
import eu.kanade.presentation.more.storage.StorageItem
import eu.kanade.presentation.more.storage.StorageScreenState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import kotlin.random.Random

abstract class CommonStorageScreenModel<T>(
    private val downloadCacheChanges: SharedFlow<Unit>,
    private val downloadCacheIsInitializing: StateFlow<Boolean>,
    private val libraries: Flow<List<T>>,
    private val categories: Flow<List<Category>>,
    private val getTotalDownloadSize: () -> Long,
    private val getDownloadSize: T.() -> Long,
    private val getDownloadCount: T.() -> Int,
    private val getId: T.() -> Long,
    private val getCategoryId: T.() -> Long,
    private val getTitle: T.() -> String,
    private val getThumbnail: T.() -> String?,
) : StateScreenModel<StorageScreenState>(StorageScreenState.Loading) {

    private val selectedCategory = MutableStateFlow(AllCategory)

    init {
        coroutineScope.launchIO {
            combine(
                flow = downloadCacheChanges,
                flow2 = downloadCacheIsInitializing,
                flow3 = libraries,
                flow4 = categories,
                flow5 = selectedCategory,
                transform = { _, _, libraries, categories, selectedCategory ->
                    val distinctLibraries = libraries.fastDistinctBy {
                        it.getId()
                    }.filter {
                        selectedCategory == AllCategory || it.getCategoryId() == selectedCategory.id
                    }
                    val size = getTotalDownloadSize()
                    val random = Random(size + distinctLibraries.size)

                    mutableState.update {
                        StorageScreenState.Success(
                            selectedCategory = selectedCategory,
                            categories = listOf(AllCategory, *categories.toTypedArray()),
                            items = distinctLibraries.map {
                                StorageItem(
                                    id = it.getId(),
                                    title = it.getTitle(),
                                    size = it.getDownloadSize(),
                                    thumbnail = it.getThumbnail(),
                                    entriesCount = it.getDownloadCount(),
                                    color = Color(
                                        random.nextInt(255),
                                        random.nextInt(255),
                                        random.nextInt(255),
                                    ),
                                )
                            }.sortedByDescending { it.size },
                        )
                    }
                },
            ).collectLatest {}
        }
    }

    fun setSelectedCategory(category: Category) {
        selectedCategory.update { category }
    }

    abstract fun deleteEntry(id: Long)

    companion object {
        /**
         * A dummy category used to display all entries irrespective of the category.
         */
        private val AllCategory = Category(
            id = -1L,
            name = "All",
            order = 0L,
            flags = 0L,
            hidden = false,
        )
    }
}
