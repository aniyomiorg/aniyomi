package eu.kanade.tachiyomi.ui.category.anime

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.category.anime.interactor.CreateAnimeCategoryWithName
import tachiyomi.domain.category.anime.interactor.DeleteAnimeCategory
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.anime.interactor.GetVisibleAnimeCategories
import tachiyomi.domain.category.anime.interactor.HideAnimeCategory
import tachiyomi.domain.category.anime.interactor.RenameAnimeCategory
import tachiyomi.domain.category.anime.interactor.ReorderAnimeCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
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
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
) : StateScreenModel<AnimeCategoryScreenState>(AnimeCategoryScreenState.Loading) {

    private val _events: Channel<AnimeCategoryEvent> = Channel()
    val events = _events.receiveAsFlow()

    init {
        coroutineScope.launch {
            val allCategories = if (libraryPreferences.hideHiddenCategoriesSettings().get()) {
                getVisibleCategories.subscribe()
            } else {
                getAllCategories.subscribe()
            }

            allCategories.collectLatest { categories ->
                mutableState.update {
                    AnimeCategoryScreenState.Success(
                        categories = categories.filterNot(Category::isSystemCategory),
                    )
                }
            }
        }
    }

    fun createCategory(name: String) {
        coroutineScope.launch {
            when (createCategoryWithName.await(name)) {
                is CreateAnimeCategoryWithName.Result.InternalError -> _events.send(
                    AnimeCategoryEvent.InternalError,
                )

                else -> {}
            }
        }
    }

    fun hideCategory(category: Category) {
        coroutineScope.launch {
            when (hideCategory.await(category)) {
                is HideAnimeCategory.Result.InternalError -> _events.send(AnimeCategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        coroutineScope.launch {
            when (deleteCategory.await(categoryId = categoryId)) {
                is DeleteAnimeCategory.Result.InternalError -> _events.send(AnimeCategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun moveUp(category: Category) {
        coroutineScope.launch {
            when (reorderCategory.moveUp(category)) {
                is ReorderAnimeCategory.Result.InternalError -> _events.send(AnimeCategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun moveDown(category: Category) {
        coroutineScope.launch {
            when (reorderCategory.moveDown(category)) {
                is ReorderAnimeCategory.Result.InternalError -> _events.send(AnimeCategoryEvent.InternalError)
                else -> {}
            }
        }
    }

    fun renameCategory(category: Category, name: String) {
        coroutineScope.launch {
            when (renameCategory.await(category, name)) {
                is RenameAnimeCategory.Result.InternalError -> _events.send(AnimeCategoryEvent.InternalError)
                else -> {}
            }
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

sealed class AnimeCategoryDialog {
    object Create : AnimeCategoryDialog()
    data class Rename(val category: Category) : AnimeCategoryDialog()
    data class Delete(val category: Category) : AnimeCategoryDialog()
}

sealed class AnimeCategoryEvent {
    sealed class LocalizedMessage(@StringRes val stringRes: Int) : AnimeCategoryEvent()
    object InternalError : LocalizedMessage(R.string.internal_error)
}

sealed class AnimeCategoryScreenState {

    @Immutable
    object Loading : AnimeCategoryScreenState()

    @Immutable
    data class Success(
        val categories: List<Category>,
        val dialog: AnimeCategoryDialog? = null,
    ) : AnimeCategoryScreenState() {

        val isEmpty: Boolean
            get() = categories.isEmpty()
    }
}
