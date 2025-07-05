package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.components.AnimeSourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.source.anime.interactor.GetAnimeSourcesWithNonLibraryAnime
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.AnimeSourceWithCount
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.mi.data.AnimeDatabase
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ClearAnimeDatabaseScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { ClearAnimeDatabaseScreenModel() }
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()

        when (val s = state) {
            is ClearAnimeDatabaseScreenModel.State.Loading -> LoadingScreen()
            is ClearAnimeDatabaseScreenModel.State.Ready -> {
                if (s.showConfirmation) {
                    AlertDialog(
                        onDismissRequest = model::hideConfirmation,
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launchUI {
                                        model.removeAnimeBySourceId()
                                        model.clearSelection()
                                        model.hideConfirmation()
                                        context.toast(MR.strings.clear_database_completed)
                                    }
                                },
                            ) {
                                Text(text = stringResource(MR.strings.action_ok))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = model::hideConfirmation) {
                                Text(text = stringResource(MR.strings.action_cancel))
                            }
                        },
                        text = {
                            Text(text = stringResource(AYMR.strings.clear_database_confirmation))
                        },
                    )
                }

                Scaffold(
                    topBar = { scrollBehavior ->
                        AppBar(
                            title = stringResource(AYMR.strings.pref_clear_anime_database),
                            navigateUp = navigator::pop,
                            actions = {
                                if (s.items.isNotEmpty()) {
                                    AppBarActions(
                                        actions = persistentListOf(
                                            AppBar.Action(
                                                title = stringResource(MR.strings.action_select_all),
                                                icon = Icons.Outlined.SelectAll,
                                                onClick = model::selectAll,
                                            ),
                                            AppBar.Action(
                                                title = stringResource(MR.strings.action_select_all),
                                                icon = Icons.Outlined.FlipToBack,
                                                onClick = model::invertSelection,
                                            ),
                                        ),
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    },
                ) { contentPadding ->
                    if (s.items.isEmpty()) {
                        EmptyScreen(
                            message = stringResource(MR.strings.database_clean),
                            modifier = Modifier.padding(contentPadding),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize(),
                        ) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                            ) {
                                items(s.items) { sourceWithCount ->
                                    ClearDatabaseItem(
                                        source = sourceWithCount.source,
                                        count = sourceWithCount.count,
                                        isSelected = s.selection.contains(sourceWithCount.id),
                                        onClickSelect = {
                                            model.toggleSelection(
                                                sourceWithCount.source,
                                            )
                                        },
                                    )
                                }
                            }

                            HorizontalDivider()

                            Button(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                onClick = model::showConfirmation,
                                enabled = s.selection.isNotEmpty(),
                            ) {
                                Text(
                                    text = stringResource(MR.strings.action_delete),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearDatabaseItem(
        source: AnimeSource,
        count: Long,
        isSelected: Boolean,
        onClickSelect: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .selectedBackground(isSelected)
                .clickable(onClick = onClickSelect)
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimeSourceIcon(source = source)
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            ) {
                Text(
                    text = source.visualName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(text = stringResource(MR.strings.clear_database_source_item_count, count))
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClickSelect() },
            )
        }
    }
}

private class ClearAnimeDatabaseScreenModel : StateScreenModel<ClearAnimeDatabaseScreenModel.State>(
    State.Loading,
) {
    private val getSourcesWithNonLibraryAnime: GetAnimeSourcesWithNonLibraryAnime = Injekt.get()
    private val database: AnimeDatabase = Injekt.get()

    init {
        screenModelScope.launchIO {
            getSourcesWithNonLibraryAnime.subscribe()
                .collectLatest { list ->
                    mutableState.update { old ->
                        val items = list.sortedBy { it.name }
                        when (old) {
                            State.Loading -> State.Ready(items)
                            is State.Ready -> old.copy(items = items)
                        }
                    }
                }
        }
    }

    suspend fun removeAnimeBySourceId() = withNonCancellableContext {
        val state = state.value as? State.Ready ?: return@withNonCancellableContext
        database.animesQueries.deleteAnimesNotInLibraryBySourceIds(state.selection)
        database.animehistoryQueries.removeResettedHistory()
    }

    fun toggleSelection(source: AnimeSource) = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        val mutableList = state.selection.toMutableList()
        if (mutableList.contains(source.id)) {
            mutableList.remove(source.id)
        } else {
            mutableList.add(source.id)
        }
        state.copy(selection = mutableList)
    }

    fun clearSelection() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = emptyList())
    }

    fun selectAll() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = state.items.fastMap { it.id })
    }

    fun invertSelection() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(
            selection = state.items
                .fastMap { it.id }
                .filterNot { it in state.selection },
        )
    }

    fun showConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = true)
    }

    fun hideConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = false)
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Ready(
            val items: List<AnimeSourceWithCount>,
            val selection: List<Long> = emptyList(),
            val showConfirmation: Boolean = false,
        ) : State
    }
}
