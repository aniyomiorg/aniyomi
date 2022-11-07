package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.browse.animeextension.AnimeExtensionUiModel

interface AnimeExtensionsState {
    val isLoading: Boolean
    val isRefreshing: Boolean
    val items: List<AnimeExtensionUiModel>
    val updates: Int
    val isEmpty: Boolean
}

fun AnimeExtensionState(): AnimeExtensionsState {
    return AnimeExtensionsStateImpl()
}

class AnimeExtensionsStateImpl : AnimeExtensionsState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var isRefreshing: Boolean by mutableStateOf(false)
    override var items: List<AnimeExtensionUiModel> by mutableStateOf(emptyList())
    override var updates: Int by mutableStateOf(0)
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
}
