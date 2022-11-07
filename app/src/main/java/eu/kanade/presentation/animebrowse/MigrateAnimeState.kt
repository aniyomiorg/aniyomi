package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.domain.anime.model.Anime

interface MigrateAnimeState {
    val isLoading: Boolean
    val items: List<Anime>
    val isEmpty: Boolean
}

fun MigrationAnimeState(): MigrateAnimeState {
    return MigrateAnimeStateImpl()
}

class MigrateAnimeStateImpl : MigrateAnimeState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var items: List<Anime> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
}
