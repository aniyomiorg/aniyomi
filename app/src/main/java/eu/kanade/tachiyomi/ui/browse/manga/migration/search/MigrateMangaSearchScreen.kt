package eu.kanade.tachiyomi.ui.browse.manga.migration.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.manga.MigrateMangaSearchScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen

class MigrateMangaSearchScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { MigrateMangaSearchScreenModel(mangaId = mangaId) }
        val state by screenModel.state.collectAsState()

        val dialogScreenModel = rememberScreenModel {
            MangaMigrateSearchScreenDialogScreenModel(
                mangaId = mangaId,
            )
        }
        val dialogState by dialogScreenModel.state.collectAsState()

        MigrateMangaSearchScreen(
            state = state,
            fromSourceId = dialogState.manga?.source,
            navigateUp = navigator::pop,
            onChangeSearchQuery = screenModel::updateSearchQuery,
            onSearch = { screenModel.search() },
            getManga = { screenModel.getManga(it) },
            onChangeSearchFilter = screenModel::setSourceFilter,
            onToggleResults = screenModel::toggleFilterResults,
            onClickSource = {
                navigator.push(
                    MangaSourceSearchScreen(dialogState.manga!!, it.id, state.searchQuery),
                )
            },
            onClickItem = {
                dialogScreenModel.setDialog(
                    MangaMigrateSearchScreenDialogScreenModel.Dialog.Migrate(it),
                )
            },
            onLongClickItem = { navigator.push(MangaScreen(it.id, true)) },
        )

        when (val dialog = dialogState.dialog) {
            is MangaMigrateSearchScreenDialogScreenModel.Dialog.Migrate -> {
                MigrateMangaDialog(
                    oldManga = dialogState.manga!!,
                    newManga = dialog.manga,
                    screenModel = rememberScreenModel { MigrateMangaDialogScreenModel() },
                    onDismissRequest = { dialogScreenModel.setDialog(null) },
                    onClickTitle = {
                        navigator.push(MangaScreen(dialog.manga.id, true))
                    },
                    onPopScreen = {
                        if (navigator.lastItem is MangaScreen) {
                            val lastItem = navigator.lastItem
                            navigator.popUntil { navigator.items.contains(lastItem) }
                            navigator.push(MangaScreen(dialog.manga.id))
                        } else {
                            navigator.replace(MangaScreen(dialog.manga.id))
                        }
                    },
                )
            }
            else -> {}
        }
    }
}
