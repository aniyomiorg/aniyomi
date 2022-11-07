package eu.kanade.tachiyomi.ui.animecategory

import android.os.Bundle
import eu.kanade.domain.category.interactor.CreateAnimeCategoryWithName
import eu.kanade.domain.category.interactor.DeleteAnimeCategory
import eu.kanade.domain.category.interactor.GetAnimeCategories
import eu.kanade.domain.category.interactor.RenameAnimeCategory
import eu.kanade.domain.category.interactor.ReorderAnimeCategory
import eu.kanade.domain.category.model.Category
import eu.kanade.presentation.category.AnimeCategoryState
import eu.kanade.presentation.category.AnimeCategoryStateImpl
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeCategoryPresenter(
    private val state: AnimeCategoryStateImpl = AnimeCategoryState() as AnimeCategoryStateImpl,
    private val getCategories: GetAnimeCategories = Injekt.get(),
    private val createCategoryWithName: CreateAnimeCategoryWithName = Injekt.get(),
    private val renameCategory: RenameAnimeCategory = Injekt.get(),
    private val reorderCategory: ReorderAnimeCategory = Injekt.get(),
    private val deleteCategory: DeleteAnimeCategory = Injekt.get(),
) : BasePresenter<AnimeCategoryController>(), AnimeCategoryState by state {

    private val _events: Channel<Event> = Channel(Int.MAX_VALUE)
    val events = _events.consumeAsFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        presenterScope.launchIO {
            getCategories.subscribe()
                .collectLatest {
                    state.isLoading = false
                    state.categories = it.filterNot(Category::isSystemCategory)
                }
        }
    }

    fun createCategory(name: String) {
        presenterScope.launchIO {
            when (createCategoryWithName.await(name)) {
                is CreateAnimeCategoryWithName.Result.NameAlreadyExistsError -> _events.send(Event.CategoryWithNameAlreadyExists)
                is CreateAnimeCategoryWithName.Result.InternalError -> _events.send(Event.InternalError)
                else -> {}
            }
        }
    }

    fun deleteCategory(category: Category) {
        presenterScope.launchIO {
            when (deleteCategory.await(category.id)) {
                is DeleteAnimeCategory.Result.InternalError -> _events.send(Event.InternalError)
                else -> {}
            }
        }
    }

    fun moveUp(category: Category) {
        presenterScope.launchIO {
            when (reorderCategory.await(category, category.order - 1)) {
                is ReorderAnimeCategory.Result.InternalError -> _events.send(Event.InternalError)
                else -> {}
            }
        }
    }

    fun moveDown(category: Category) {
        presenterScope.launchIO {
            when (reorderCategory.await(category, category.order + 1)) {
                is ReorderAnimeCategory.Result.InternalError -> _events.send(Event.InternalError)
                else -> {}
            }
        }
    }

    fun renameCategory(category: Category, name: String) {
        presenterScope.launchIO {
            when (renameCategory.await(category, name)) {
                RenameAnimeCategory.Result.NameAlreadyExistsError -> _events.send(Event.CategoryWithNameAlreadyExists)
                is RenameAnimeCategory.Result.InternalError -> _events.send(Event.InternalError)
                else -> {}
            }
        }
    }

    sealed class Dialog {
        object Create : Dialog()
        data class Rename(val category: Category) : Dialog()
        data class Delete(val category: Category) : Dialog()
    }

    sealed class Event {
        object CategoryWithNameAlreadyExists : Event()
        object InternalError : Event()
    }
}
