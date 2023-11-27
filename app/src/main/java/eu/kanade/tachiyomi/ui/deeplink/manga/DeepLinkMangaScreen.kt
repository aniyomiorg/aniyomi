package eu.kanade.tachiyomi.ui.deeplink.manga

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.LoadingScreen

class DeepLinkMangaScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel {
            DeepLinkMangaScreenModel(query = query)
        }
        val state by screenModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(R.string.action_search_hint),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (state) {
                is DeepLinkMangaScreenModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkMangaScreenModel.State.NoResults -> {
                    navigator.replace(GlobalMangaSearchScreen(query))
                }
                is DeepLinkMangaScreenModel.State.Result -> {
                    val resultState = state as DeepLinkMangaScreenModel.State.Result
                    if (resultState.chapterId == null) {
                        navigator.replace(
                            MangaScreen(
                                resultState.manga.id,
                                true,
                            ),
                        )
                    } else {
                        navigator.pop()
                        ReaderActivity.newIntent(
                            context,
                            resultState.manga.id,
                            resultState.chapterId,
                        ).also(context::startActivity)
                    }
                }
            }
        }
    }
}
