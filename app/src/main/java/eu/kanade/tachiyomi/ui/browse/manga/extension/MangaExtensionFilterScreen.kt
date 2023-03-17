package eu.kanade.tachiyomi.ui.browse.manga.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.manga.MangaExtensionFilterScreen
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest

class MangaExtensionFilterScreen : Screen {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MangaExtensionFilterScreenModel() }
        val state by screenModel.state.collectAsState()

        if (state is MangaExtensionFilterState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as MangaExtensionFilterState.Success

        MangaExtensionFilterScreen(
            navigateUp = navigator::pop,
            state = successState,
            onClickToggle = { screenModel.toggle(it) },
        )

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest {
                when (it) {
                    MangaExtensionFilterEvent.FailedFetchingLanguages -> {
                        context.toast(R.string.internal_error)
                    }
                }
            }
        }
    }
}
