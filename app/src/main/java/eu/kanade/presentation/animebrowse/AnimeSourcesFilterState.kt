package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.browse.animesource.AnimeFilterUiModel

interface AnimeSourcesFilterState {
    val isLoading: Boolean
    val items: List<AnimeFilterUiModel>
    val isEmpty: Boolean
}

fun AnimeSourcesFilterState(): AnimeSourcesFilterState {
    return AnimeSourcesFilterStateImpl()
}

class AnimeSourcesFilterStateImpl : AnimeSourcesFilterState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var items: List<AnimeFilterUiModel> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
}
