package eu.kanade.tachiyomi.ui.browse.anime.migration.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.entries.anime.model.Anime
import eu.kanade.presentation.browse.anime.BrowseAnimeSourceContent
import eu.kanade.presentation.components.ExtendedFloatingActionButton
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreenModel
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.Constants
import kotlinx.coroutines.launch

data class AnimeSourceSearchScreen(
    private val oldAnime: Anime,
    private val sourceId: Long,
    private val query: String?,
) : Screen {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val screenModel = rememberScreenModel { BrowseAnimeSourceScreenModel(sourceId, query) }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.toolbarQuery ?: "",
                    onChangeSearchQuery = screenModel::setToolbarQuery,
                    onClickCloseSearch = navigator::pop,
                    onSearch = { screenModel.search(it) },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                AnimatedVisibility(visible = state.filters.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        text = { Text(text = stringResource(R.string.action_filter)) },
                        icon = { Icon(Icons.Outlined.FilterList, contentDescription = "") },
                        onClick = screenModel::openFilterSheet,
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            val pagingFlow by screenModel.animePagerFlowFlow.collectAsState()
            val openMigrateDialog: (Anime) -> Unit = {
                screenModel.setDialog(BrowseAnimeSourceScreenModel.Dialog.Migrate(it))
            }
            BrowseAnimeSourceContent(
                source = screenModel.source,
                animeList = pagingFlow.collectAsLazyPagingItems(),
                columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                displayMode = screenModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = {
                    val source = screenModel.source as? AnimeHttpSource ?: return@BrowseAnimeSourceContent
                    val intent = WebViewActivity.newIntent(context, source.baseUrl, source.id, source.name)
                    context.startActivity(intent)
                },
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalAnimeSourceHelpClick = { uriHandler.openUri(LocalAnimeSource.HELP_URL) },
                onAnimeClick = openMigrateDialog,
                onAnimeLongClick = { navigator.push(AnimeScreen(it.id, true)) },
            )
        }

        when (val dialog = state.dialog) {
            is BrowseAnimeSourceScreenModel.Dialog.Migrate -> {
                MigrateAnimeDialog(
                    oldAnime = oldAnime,
                    newAnime = dialog.newAnime,
                    screenModel = rememberScreenModel { MigrateAnimeDialogScreenModel() },
                    onDismissRequest = { screenModel.setDialog(null) },
                    onClickTitle = { navigator.push(AnimeScreen(dialog.newAnime.id)) },
                    onPopScreen = {
                        scope.launch {
                            navigator.popUntilRoot()
                            HomeScreen.openTab(HomeScreen.Tab.Browse())
                            navigator.push(AnimeScreen(dialog.newAnime.id))
                        }
                    },
                )
            }
            else -> {}
        }

        LaunchedEffect(state.filters) {
            screenModel.initFilterSheet(context)
        }
    }
}
