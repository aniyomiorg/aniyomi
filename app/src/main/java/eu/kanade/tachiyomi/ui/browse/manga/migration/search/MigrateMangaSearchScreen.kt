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

class MigrateSearchScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { MigrateSearchScreenModel(mangaId = mangaId) }
        val state by screenModel.state.collectAsState()

        MigrateMangaSearchScreen(
            navigateUp = navigator::pop,
            state = state,
            getManga = { screenModel.getManga(it) },
            onChangeSearchQuery = screenModel::updateSearchQuery,
            onSearch = screenModel::search,
            onClickSource = {
                if (!screenModel.incognitoMode.get()) {
                    screenModel.lastUsedSourceId.set(it.id)
                }
                navigator.push(MangaSourceSearchScreen(state.manga!!, it.id, state.searchQuery))
            },
            onClickItem = { screenModel.setDialog(MigrateMangaSearchDialog.Migrate(it)) },
            onLongClickItem = { navigator.push(MangaScreen(it.id, true)) },
        )

        when (val dialog = state.dialog) {
            is MigrateMangaSearchDialog.Migrate -> {
                MigrateMangaDialog(
                    oldManga = state.manga!!,
                    newManga = dialog.manga,
                    screenModel = rememberScreenModel { MigrateMangaDialogScreenModel() },
                    onDismissRequest = { screenModel.setDialog(null) },
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
