package eu.kanade.tachiyomi.ui.animelib

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAll
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.library.model.display
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.presentation.components.ChangeCategoryDialog
import eu.kanade.presentation.components.DeleteAnimelibAnimeDialog
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.EmptyScreenAction
import eu.kanade.presentation.components.AnimelibBottomActionMenu
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.animelib.components.AnimelibContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.manga.components.DownloadCustomAmountDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

object AnimelibTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val libraryPreferences: LibraryPreferences by injectLazy()
            val title = if (libraryPreferences.bottomNavStyle().get() == 2) {
                R.string.label_library
            } else {
                R.string.label_animelib
            }
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(title),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        requestOpenSettingsSheet()
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val screenModel = rememberScreenModel { AnimelibScreenModel() }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

        val onClickRefresh: (Category?) -> Boolean = {
            val started = AnimelibUpdateService.start(context, it)
            scope.launch {
                val msgRes = if (started) R.string.updating_category else R.string.update_already_running
                snackbarHostState.showSnackbar(context.getString(msgRes))
            }
            started
        }
        val onClickFilter: () -> Unit = {
            scope.launch { sendSettingsSheetIntent(state.categories[screenModel.activeCategoryIndex]) }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                val title = state.getToolbarTitle(
                    defaultTitle = stringResource(R.string.label_library),
                    defaultCategoryTitle = stringResource(R.string.label_default),
                    page = screenModel.activeCategoryIndex,
                )
                val tabVisible = state.showCategoryTabs && state.categories.size > 1
                LibraryToolbar(
                    hasActiveFilters = state.hasActiveFilters,
                    selectedCount = state.selection.size,
                    title = title,
                    onClickUnselectAll = screenModel::clearSelection,
                    onClickSelectAll = { screenModel.selectAll(screenModel.activeCategoryIndex) },
                    onClickInvertSelection = { screenModel.invertSelection(screenModel.activeCategoryIndex) },
                    onClickFilter = onClickFilter,
                    onClickRefresh = { onClickRefresh(null) },
                    onClickOpenRandomManga = {
                        scope.launch {
                            val randomItem = screenModel.getRandomAnimelibItemForCurrentCategory()
                            if (randomItem != null) {
                                navigator.push(AnimeScreen(randomItem.animelibAnime.anime.id))
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.information_no_entries_found))
                            }
                        }
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = screenModel::search,
                    scrollBehavior = scrollBehavior.takeIf { !tabVisible }, // For scroll overlay when no tab
                )
            },
            bottomBar = {
                AnimelibBottomActionMenu(
                    visible = state.selectionMode,
                    onChangeCategoryClicked = screenModel::openChangeCategoryDialog,
                    onMarkAsSeenClicked = { screenModel.markSeenSelection(true) },
                    onMarkAsUnseenClicked = { screenModel.markSeenSelection(false) },
                    onDownloadClicked = screenModel::runDownloadActionSelection
                        .takeIf { state.selection.fastAll { !it.anime.isLocal() } },
                    onDeleteClicked = screenModel::openDeleteAnimeDialog,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            when {
                state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
                state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
                    val handler = LocalUriHandler.current
                    EmptyScreen(
                        textResource = R.string.information_empty_library,
                        modifier = Modifier.padding(contentPadding),
                        actions = listOf(
                            EmptyScreenAction(
                                stringResId = R.string.getting_started_guide,
                                icon = Icons.Outlined.HelpOutline,
                                onClick = { handler.openUri("https://tachiyomi.org/help/guides/getting-started") },
                            ),
                        ),
                    )
                }
                else -> {
                    AnimelibContent(
                        categories = state.categories,
                        searchQuery = state.searchQuery,
                        selection = state.selection,
                        contentPadding = contentPadding,
                        currentPage = { screenModel.activeCategoryIndex },
                        hasActiveFilters = state.hasActiveFilters,
                        showPageTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
                        onChangeCurrentPage = { screenModel.activeCategoryIndex = it },
                        onAnimeClicked = { navigator.push(AnimeScreen(it)) },
                        onContinueWatchingClicked = { it: AnimelibAnime ->
                            scope.launchIO {
                                val episode = screenModel.getNextUnseenEpisode(it.anime)
                                if (episode != null) {
                                    context.startActivity(PlayerActivity.newIntent(context, episode.animeId, episode.id))
                                } else {
                                    snackbarHostState.showSnackbar(context.getString(R.string.no_next_episode))
                                }
                            }
                            // TODO: External Intent
                            Unit
                        }.takeIf { state.showAnimeContinueButton },
                        onToggleSelection = { screenModel.toggleSelection(it) },
                        onToggleRangeSelection = {
                            screenModel.toggleRangeSelection(it)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = onClickRefresh,
                        onGlobalSearchClicked = {
                            navigator.push(GlobalAnimeSearchScreen(screenModel.state.value.searchQuery ?: ""))
                        },
                        getNumberOfAnimeForCategory = { state.getAnimeCountForCategory(it) },
                        getDisplayModeForPage = { state.categories[it].display },
                        getColumnsForOrientation = { screenModel.getColumnsPreferenceForCurrentOrientation(it) },
                    ) { state.getAnimelibItemsByPage(it) }
                }
            }
        }

        val onDismissRequest = screenModel::closeDialog
        when (val dialog = state.dialog) {
            is AnimelibScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        screenModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        screenModel.clearSelection()
                        screenModel.setAnimeCategories(dialog.anime, include, exclude)
                    },
                )
            }
            is AnimelibScreenModel.Dialog.DeleteAnime -> {
                DeleteAnimelibAnimeDialog(
                    containsLocalAnime = dialog.anime.any(Anime::isLocal),
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteAnime, deleteChapter ->
                        screenModel.removeAnimes(dialog.anime, deleteAnime, deleteChapter)
                        screenModel.clearSelection()
                    },
                )
            }
            is AnimelibScreenModel.Dialog.DownloadCustomAmount -> {
                DownloadCustomAmountDialog(
                    maxAmount = dialog.max,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { amount ->
                        screenModel.downloadUnseenEpisodes(dialog.anime, amount)
                        screenModel.clearSelection()
                    },
                )
            }
            null -> {}
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> screenModel.clearSelection()
                state.searchQuery != null -> screenModel.search(null)
            }
        }

        LaunchedEffect(state.selectionMode) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }

        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(screenModel::search) }
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest { onClickFilter() } }
        }
    }

    // For invoking search from other screen
    private val queryEvent = Channel<String>()
    suspend fun search(query: String) = queryEvent.send(query)

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()
    private val openSettingsSheetEvent_ = Channel<Category>()
    val openSettingsSheetEvent = openSettingsSheetEvent_.receiveAsFlow()
    private suspend fun sendSettingsSheetIntent(category: Category) = openSettingsSheetEvent_.send(category)
    suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
}
