package eu.kanade.presentation.category

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.domain.category.model.Category
import eu.kanade.tachiyomi.ui.animecategory.AnimeCategoryPresenter

@Stable
interface AnimeCategoryState {
    val isLoading: Boolean
    var dialog: AnimeCategoryPresenter.Dialog?
    val categories: List<Category>
    val isEmpty: Boolean
}

fun AnimeCategoryState(): AnimeCategoryState {
    return AnimeCategoryStateImpl()
}

class AnimeCategoryStateImpl : AnimeCategoryState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var dialog: AnimeCategoryPresenter.Dialog? by mutableStateOf(null)
    override var categories: List<Category> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { categories.isEmpty() }
}
