package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.animeextension.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.animeextension.details.AnimeExtensionSourceItem

@Stable
interface AnimeExtensionDetailsState {
    val isLoading: Boolean
    val extension: AnimeExtension.Installed?
    val sources: List<AnimeExtensionSourceItem>
}

fun AnimeExtensionDetailsState(): AnimeExtensionDetailsState {
    return AnimeExtensionDetailsStateImpl()
}

class AnimeExtensionDetailsStateImpl : AnimeExtensionDetailsState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var extension: AnimeExtension.Installed? by mutableStateOf(null)
    override var sources: List<AnimeExtensionSourceItem> by mutableStateOf(emptyList())
}
