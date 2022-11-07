package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.browse.animeextension.AnimeFilterUiModel

@Stable
interface AnimeExtensionFilterState {
    val isLoading: Boolean
    val items: List<AnimeFilterUiModel>
    val isEmpty: Boolean
}

fun AnimeExtensionFilterState(): AnimeExtensionFilterState {
    return AnimeExtensionFilterStateImpl()
}

class AnimeExtensionFilterStateImpl : AnimeExtensionFilterState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var items: List<AnimeFilterUiModel> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
}
