package eu.kanade.tachiyomi.ui.browse.anime.migration.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.MigrateAnimeSearchScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen

class MigrateAnimeSearchScreen(private val animeId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { MigrateAnimeSearchScreenModel(animeId = animeId) }
        val state by screenModel.state.collectAsState()

        MigrateAnimeSearchScreen(
            navigateUp = navigator::pop,
            state = state,
            getAnime = { screenModel.getAnime(it) },
            onChangeSearchQuery = screenModel::updateSearchQuery,
            onSearch = screenModel::search,
            onClickSource = {
                if (!screenModel.incognitoMode.get()) {
                    screenModel.lastUsedSourceId.set(it.id)
                }
                navigator.push(AnimeSourceSearchScreen(state.anime!!, it.id, state.searchQuery))
            },
            onClickItem = { screenModel.setDialog(MigrateAnimeSearchDialog.Migrate(it)) },
            onLongClickItem = { navigator.push(AnimeScreen(it.id, true)) },
        )

        when (val dialog = state.dialog) {
            is MigrateAnimeSearchDialog.Migrate -> {
                MigrateAnimeDialog(
                    oldAnime = state.anime!!,
                    newAnime = dialog.anime,
                    screenModel = rememberScreenModel { MigrateAnimeDialogScreenModel() },
                    onDismissRequest = { screenModel.setDialog(null) },
                    onClickTitle = {
                        navigator.push(AnimeScreen(dialog.anime.id, true))
                    },
                    onPopScreen = {
                        if (navigator.lastItem is AnimeScreen) {
                            val lastItem = navigator.lastItem
                            navigator.popUntil { navigator.items.contains(lastItem) }
                            navigator.push(AnimeScreen(dialog.anime.id))
                        } else {
                            navigator.replace(AnimeScreen(dialog.anime.id))
                        }
                    },
                )
            }
            else -> {}
        }
    }
}
