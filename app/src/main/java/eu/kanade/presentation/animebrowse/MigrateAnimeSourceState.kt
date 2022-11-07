package eu.kanade.presentation.animebrowse

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.source.interactor.SetMigrateSorting

interface MigrateAnimeSourceState {
    val isLoading: Boolean
    val items: List<Pair<AnimeSource, Long>>
    val isEmpty: Boolean
    val sortingMode: SetMigrateSorting.Mode
    val sortingDirection: SetMigrateSorting.Direction
}

fun MigrateAnimeSourceState(): MigrateAnimeSourceState {
    return MigrateAnimeSourceStateImpl()
}

class MigrateAnimeSourceStateImpl : MigrateAnimeSourceState {
    override var isLoading: Boolean by mutableStateOf(true)
    override var items: List<Pair<AnimeSource, Long>> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
    override var sortingMode: SetMigrateSorting.Mode by mutableStateOf(SetMigrateSorting.Mode.ALPHABETICAL)
    override var sortingDirection: SetMigrateSorting.Direction by mutableStateOf(SetMigrateSorting.Direction.ASCENDING)
}
