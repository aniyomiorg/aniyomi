package eu.kanade.tachiyomi.ui.deeplink.anime

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class DeepLinkAnimeScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel {
            DeepLinkAnimeScreenModel(query = query)
        }
        val state by screenModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.action_search_hint),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (state) {
                is DeepLinkAnimeScreenModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkAnimeScreenModel.State.NoResults -> {
                    navigator.replace(GlobalAnimeSearchScreen(query))
                }
                is DeepLinkAnimeScreenModel.State.Result -> {
                    val resultState = state as DeepLinkAnimeScreenModel.State.Result
                    if (resultState.episodeId == null) {
                        navigator.replace(
                            AnimeScreen(
                                resultState.anime.id,
                                true,
                            ),
                        )
                    } else {
                        navigator.pop()
                        PlayerActivity.newIntent(
                            context,
                            resultState.anime.id,
                            resultState.episodeId,
                        ).also(context::startActivity)
                    }
                }
            }
        }
    }
}
