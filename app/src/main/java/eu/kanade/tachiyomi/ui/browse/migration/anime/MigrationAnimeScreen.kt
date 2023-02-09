package eu.kanade.tachiyomi.ui.browse.migration.anime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.animebrowse.MigrateAnimeScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateAnimeSearchScreen
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

data class MigrationAnimeScreen(
    private val sourceId: Long,
) : Screen {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MigrationAnimeScreenModel(sourceId) }

        val state by screenModel.state.collectAsState()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        MigrateAnimeScreen(
            navigateUp = navigator::pop,
            title = state.source!!.name,
            state = state,
            onClickItem = { navigator.push(MigrateSearchScreen(it.id)) },
            onClickCover = { navigator.push(AnimeScreen(it.id)) },
        )

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                when (event) {
                    MigrationAnimeEvent.FailedFetchingFavorites -> {
                        context.toast(R.string.internal_error)
                    }
                }
            }
        }
    }
}
