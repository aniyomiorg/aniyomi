package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeSourcesPresenter

@Stable
interface AnimeSourcesState {
    var dialog: AnimeSourcesPresenter.Dialog?
    val isLoading: Boolean
    val items: List<AnimeSourceUiModel>
    val isEmpty: Boolean
}

fun AnimeSourcesState(): AnimeSourcesState {
    return AnimeSourcesStateImpl()
}

class AnimeSourcesStateImpl : AnimeSourcesState {
    override var dialog: AnimeSourcesPresenter.Dialog? by mutableStateOf(null)
    override var isLoading: Boolean by mutableStateOf(true)
    override var items: List<AnimeSourceUiModel> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
}
