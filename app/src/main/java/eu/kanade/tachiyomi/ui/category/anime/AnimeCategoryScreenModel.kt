package eu.kanade.tachiyomi.ui.category.anime

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.category.anime.interactor.CreateAnimeCategoryWithName
import tachiyomi.domain.category.anime.interactor.DeleteAnimeCategory
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.anime.interactor.GetVisibleAnimeCategories
import tachiyomi.domain.category.anime.interactor.HideAnimeCategory
import tachiyomi.domain.category.anime.interactor.RenameAnimeCategory
import tachiyomi.domain.category.anime.interactor.ReorderAnimeCategory
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeCategoryScreenModel(
    private val getAllCategories: GetAnimeCategories = Injekt.get(),
    private val getVisibleCategories: GetVisibleAnimeCategories = Injekt.get(),
    private val createCategoryWithName: CreateAnimeCategoryWithName = Injekt.get(),
    private val hideCategory: HideAnimeCategory = Injekt.get(),
    private val deleteCategory: DeleteAnimeCategory = Injekt.get(),
    private val reorderCategory: ReorderAnimeCategory = Injekt.get(),
    private val renameCategory: RenameAnimeCategory = Injekt.get(),
    private val categoryRepository: AnimeCategoryRepository = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) : StateScreenModel<AnimeCategoryScreenState>(AnimeCategoryScreenState.Loading) {

    private val _events: Channel<AnimeCategoryEvent> = Channel()
    val events = _events.receiveAsFlow()
    private val reorderMutex = Mutex()

    init {
        screenModelScope.launch {
            val allCategories = if (libraryPreferences.hideHiddenCategoriesSettings().get()) {
                getVisibleCategories.subscribe()
            } else {
                getAllCategories.subscribe()
            }

            allCategories.collectLatest { categories ->
                mutableState.update {
                    AnimeCategoryScreenState.Success(
                        categories = categories
                            .filterNot(Category::isSystemCategory)
                            .toImmutableList(),
                    )
                }
            }
        }
    }

    fun createCategory(name: String, parentId: Long? = null) {
        screenModelScope.launch {
            when (createCategoryWithName.await(name, parentId)) {
                is CreateAnimeCategoryWithName.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )

                else -> {}
            }
        }
    }

    fun hideCategory(category: Category) {
        screenModelScope.launch {
            when (hideCategory.await(category)) {
                is HideAnimeCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        screenModelScope.launch {
            when (deleteCategory.await(categoryId = categoryId)) {
                is DeleteAnimeCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun changeOrder(category: Category, newIndex: Int) {
        screenModelScope.launch {
            when (reorderCategory.await(category, newIndex)) {
                is ReorderAnimeCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun renameCategory(category: Category, name: String) {
        screenModelScope.launch {
            when (renameCategory.await(category, name)) {
                is RenameAnimeCategory.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )
                else -> {}
            }
        }
    }

    fun moveUp(category: Category) {
        screenModelScope.launch {
            reorderMutex.withLock {
                val currentState = mutableState.value
                if (currentState !is AnimeCategoryScreenState.Success) return@withLock

                val siblings = currentState.categories.filter { it.parentId == category.parentId }
                val sortedSiblings = siblings.sortedWith(compareBy({ it.order }, { it.id }))
                val index = sortedSiblings.indexOfFirst { it.id == category.id }
                if (index > 0) {
                    val prevCategory = sortedSiblings[index - 1]
                    val updates = listOf(
                        CategoryUpdate(id = category.id, order = prevCategory.order),
                        CategoryUpdate(id = prevCategory.id, order = category.order),
                    )
                    categoryRepository.updatePartialAnimeCategories(updates)
                }
            }
        }
    }

    fun moveDown(category: Category) {
        screenModelScope.launch {
            reorderMutex.withLock {
                val currentState = mutableState.value
                if (currentState !is AnimeCategoryScreenState.Success) return@withLock

                val siblings = currentState.categories.filter { it.parentId == category.parentId }
                val sortedSiblings = siblings.sortedWith(compareBy({ it.order }, { it.id }))
                val index = sortedSiblings.indexOfFirst { it.id == category.id }
                if (index >= 0 && index < sortedSiblings.size - 1) {
                    val nextCategory = sortedSiblings[index + 1]
                    val updates = listOf(
                        CategoryUpdate(id = category.id, order = nextCategory.order),
                        CategoryUpdate(id = nextCategory.id, order = category.order),
                    )
                    categoryRepository.updatePartialAnimeCategories(updates)
                }
            }
        }
    }

    fun moveToParent(category: Category) {
        screenModelScope.launch {
            if (category.parentId != null) {
                val currentState = mutableState.value
                if (currentState !is AnimeCategoryScreenState.Success) return@launch

                val siblings = currentState.categories.filter { it.parentId == category.parentId }
                val sortedSiblings = siblings.sortedWith(compareBy({ it.order }, { it.id }))
                val maxOrder = sortedSiblings.filterNot { it.id == category.id }
                    .maxOfOrNull { it.order } ?: 0

                categoryRepository.clearAnimeCategoryParentId(
                    categoryId = category.id,
                    order = maxOrder + 1,
                )
            }
        }
    }

    fun setThumbnail(categoryId: Long, thumbnailUrl: String?) {
        screenModelScope.launch {
            categoryRepository.updatePartialAnimeCategory(
                CategoryUpdate(
                    id = categoryId,
                    thumbnailUrl = thumbnailUrl,
                ),
            )
        }
    }

    fun showDialog(dialog: AnimeCategoryDialog) {
        mutableState.update {
            when (it) {
                AnimeCategoryScreenState.Loading -> it
                is AnimeCategoryScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                AnimeCategoryScreenState.Loading -> it
                is AnimeCategoryScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}

sealed interface AnimeCategoryDialog {
    data class Create(val parentId: Long?) : AnimeCategoryDialog
    data class Rename(val category: Category) : AnimeCategoryDialog
    data class Delete(val category: Category) : AnimeCategoryDialog
}

sealed interface AnimeCategoryEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : AnimeCategoryEvent
    data object InternalError : LocalizedMessage(MR.strings.internal_error)
}

sealed interface AnimeCategoryScreenState {

    @Immutable
    data object Loading : AnimeCategoryScreenState

    @Immutable
    data class Success(
        val categories: ImmutableList<Category>,
        val dialog: AnimeCategoryDialog? = null,
    ) : AnimeCategoryScreenState {

        val isEmpty: Boolean
            get() = categories.isEmpty()
    }
}
