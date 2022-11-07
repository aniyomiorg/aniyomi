package eu.kanade.presentation.animelib

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.category.model.Category
import eu.kanade.tachiyomi.ui.animelib.AnimelibPresenter

@Stable
interface AnimelibState {
    val isLoading: Boolean
    val categories: List<Category>
    var searchQuery: String?
    val selection: List<AnimelibAnime>
    val selectionMode: Boolean
    var hasActiveFilters: Boolean
    var dialog: AnimelibPresenter.Dialog?
}

fun AnimelibState(): AnimelibState {
    return AnimelibStateImpl()
}

class AnimelibStateImpl : AnimelibState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var categories: List<Category> by mutableStateOf(emptyList())
    override var searchQuery: String? by mutableStateOf(null)
    override var selection: List<AnimelibAnime> by mutableStateOf(emptyList())
    override val selectionMode: Boolean by derivedStateOf { selection.isNotEmpty() }
    override var hasActiveFilters: Boolean by mutableStateOf(false)
    override var dialog: AnimelibPresenter.Dialog? by mutableStateOf(null)
}
