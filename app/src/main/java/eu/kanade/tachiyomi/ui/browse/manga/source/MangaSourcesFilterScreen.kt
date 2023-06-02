package eu.kanade.tachiyomi.ui.browse.manga.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.manga.MangaSourcesFilterScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.presentation.core.screens.LoadingScreen

class MangaSourcesFilterScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SourcesFilterScreenModel() }
        val state by screenModel.state.collectAsState()

        if (state is MangaSourcesFilterState.Loading) {
            LoadingScreen()
            return
        }

        if (state is MangaSourcesFilterState.Error) {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                context.toast(R.string.internal_error)
                navigator.pop()
            }
            return
        }

        val successState = state as MangaSourcesFilterState.Success

        MangaSourcesFilterScreen(
            navigateUp = navigator::pop,
            state = successState,
            onClickLanguage = screenModel::toggleLanguage,
            onClickSource = screenModel::toggleSource,
        )
    }
}
