package eu.kanade.tachiyomi.ui.anime

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.commandiron.wheel_picker_compose.WheelTextPicker
import eu.kanade.domain.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.anime.AnimeScreen
import eu.kanade.presentation.anime.EpisodeSettingsDialog
import eu.kanade.presentation.anime.components.AnimeCoverDialog
import eu.kanade.presentation.anime.components.DeleteEpisodesDialog
import eu.kanade.presentation.components.ChangeCategoryDialog
import eu.kanade.presentation.components.DuplicateAnimeDialog
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.manga.BaseSelector
import eu.kanade.presentation.manga.EditCoverAction
import eu.kanade.presentation.manga.components.DownloadCustomAmountDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.isLocalOrStub
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.anime.track.AnimeTrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.browse.animesource.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateAnimeSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoriesTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.injectLazy

class AnimeScreen(
    private val animeId: Long,
    val fromSource: Boolean = false,
) : Screen, AssistContentScreen {

    private var assistUrl: String? = null

    override val key = uniqueScreenKey

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { AnimeInfoScreenModel(context, animeId, fromSource) }

        val state by screenModel.state.collectAsState()

        if (state is AnimeScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as AnimeScreenState.Success
        val isAnimeHttpSource = remember { successState.source is AnimeHttpSource }

        LaunchedEffect(successState.anime, screenModel.source) {
            if (isAnimeHttpSource) {
                try {
                    withIOContext {
                        assistUrl = getAnimeUrl(screenModel.anime, screenModel.source)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to get anime URL" }
                }
            }
        }

        AnimeScreen(
            state = successState,
            snackbarHostState = screenModel.snackbarHostState,
            isTabletUi = isTabletUi(),
            onBackClicked = navigator::pop,
            onEpisodeClicked = { episode, alt ->
                scope.launchIO {
                    openEpisode(context, episode, alt)
                }
                Unit
            },
            onDownloadEpisode = screenModel::runEpisodeDownloadActions.takeIf { !successState.source.isLocalOrStub() },
            onAddToLibraryClicked = {
                screenModel.toggleFavorite()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onWebViewClicked = { openAnimeInWebView(context, screenModel.anime, screenModel.source) }.takeIf { isAnimeHttpSource },
            onWebViewLongClicked = { copyAnimeUrl(context, screenModel.anime, screenModel.source) }.takeIf { isAnimeHttpSource },
            onTrackingClicked = screenModel::showTrackDialog.takeIf { successState.trackingAvailable },
            onTagClicked = { scope.launch { performGenreSearch(navigator, it, screenModel.source!!) } },
            onFilterButtonClicked = screenModel::showSettingsDialog,
            onRefresh = screenModel::fetchAllFromSource,
            onContinueWatching = {
                scope.launchIO {
                    continueWatching(context, screenModel.getNextUnseenEpisode())
                }
                Unit
            },
            onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
            onCoverClicked = screenModel::showCoverDialog,
            onShareClicked = { shareAnime(context, screenModel.anime, screenModel.source) }.takeIf { isAnimeHttpSource },
            onDownloadActionClicked = screenModel::runDownloadAction.takeIf { !successState.source.isLocalOrStub() },
            onEditCategoryClicked = screenModel::promptChangeCategories.takeIf { successState.anime.favorite },
            onMigrateClicked = { navigator.push(MigrateAnimeSearchScreen(successState.anime.id)) }.takeIf { successState.anime.favorite },
            changeAnimeSkipIntro = { screenModel::showAnimeSkipIntroDialog.takeIf { successState.anime.favorite } },
            onMultiBookmarkClicked = screenModel::bookmarkEpisodes,
            onMultiMarkAsSeenClicked = screenModel::markEpisodesSeen,
            onMarkPreviousAsSeenClicked = screenModel::markPreviousEpisodeSeen,
            onMultiDeleteClicked = screenModel::showDeleteEpisodeDialog,
            onEpisodeSelected = screenModel::toggleSelection,
            onAllEpisodeSelected = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
        )

        val onDismissRequest = { screenModel.dismissDialog() }
        when (val dialog = (state as? AnimeScreenState.Success)?.dialog) {
            null -> {}
            is AnimeInfoScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoriesTab(false)) },
                    onConfirm = { include, _ ->
                        screenModel.moveAnimeToCategoriesAndAddToLibrary(dialog.anime, include)
                    },
                )
            }
            is AnimeInfoScreenModel.Dialog.DeleteEpisodes -> {
                DeleteEpisodesDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.toggleAllSelection(false)
                        screenModel.deleteEpisodes(dialog.episodes)
                    },
                )
            }
            is AnimeInfoScreenModel.Dialog.DownloadCustomAmount -> {
                DownloadCustomAmountDialog(
                    maxAmount = dialog.max,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { amount ->
                        val episodesToDownload = screenModel.getUnseenEpisodesSorted().take(amount)
                        if (episodesToDownload.isNotEmpty()) {
                            screenModel.startDownload(episodes = episodesToDownload, startNow = false)
                        }
                    },
                )
            }
            is AnimeInfoScreenModel.Dialog.DuplicateAnime -> DuplicateAnimeDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.toggleFavorite(onRemoved = {}, onAdded = {}, checkDuplicate = false) },
                onOpenAnime = { navigator.push(AnimeScreen(dialog.duplicate.id)) },
                duplicateFrom = screenModel.getSourceOrStub(dialog.duplicate),
            )
            AnimeInfoScreenModel.Dialog.SettingsSheet -> EpisodeSettingsDialog(
                onDismissRequest = onDismissRequest,
                anime = successState.anime,
                onDownloadFilterChanged = screenModel::setDownloadedFilter,
                onUnseenFilterChanged = screenModel::setUnseenFilter,
                onBookmarkedFilterChanged = screenModel::setBookmarkedFilter,
                onSortModeChanged = screenModel::setSorting,
                onDisplayModeChanged = screenModel::setDisplayMode,
                onSetAsDefault = screenModel::setCurrentSettingsAsDefault,
            )
            AnimeInfoScreenModel.Dialog.TrackSheet -> {
                NavigatorAdaptiveSheet(
                    screen = AnimeTrackInfoDialogHomeScreen(
                        animeId = successState.anime.id,
                        animeTitle = successState.anime.title,
                        sourceId = successState.source.id,
                    ),
                    enableSwipeDismiss = { it.lastItem is AnimeTrackInfoDialogHomeScreen },
                    onDismissRequest = onDismissRequest,
                )
            }
            AnimeInfoScreenModel.Dialog.FullCover -> {
                val sm = rememberScreenModel { AnimeCoverScreenModel(successState.anime.id) }
                val anime by sm.state.collectAsState()
                if (anime != null) {
                    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult
                        sm.editCover(context, it)
                    }
                    AnimeCoverDialog(
                        coverDataProvider = { anime!! },
                        snackbarHostState = sm.snackbarHostState,
                        isCustomCover = remember(anime) { anime!!.hasCustomCover() },
                        onShareClick = { sm.shareCover(context) },
                        onSaveClick = { sm.saveCover(context) },
                        onEditClick = {
                            when (it) {
                                EditCoverAction.EDIT -> getContent.launch("image/*")
                                EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                            }
                        },
                        onDismissRequest = onDismissRequest,
                    )
                } else {
                    LoadingScreen(Modifier.systemBarsPadding())
                }
            }

            AnimeInfoScreenModel.Dialog.ChangeAnimeSkipIntro -> {
                ChangeIntroLength(
                    anime = successState.anime,
                    onDismissRequest = onDismissRequest,
                )
            }
        }
    }

    private suspend fun continueWatching(context: Context, unseenEpisode: Episode?) {
        if (unseenEpisode != null) openEpisode(context, unseenEpisode)
    }
    private fun openEpisodeInternal(context: Context, animeId: Long, episodeId: Long) {
        context.startActivity(PlayerActivity.newIntent(context, animeId, episodeId))
    }

    private suspend fun openEpisodeExternal(context: Context, animeId: Long, episodeId: Long) {
        context.startActivity(ExternalIntents.newIntent(context, animeId, episodeId))
    }

    private suspend fun openEpisode(context: Context, episode: Episode, altPlayer: Boolean = false) {
        val playerPreferences: PlayerPreferences by injectLazy()
        if (playerPreferences.alwaysUseExternalPlayer().get() != altPlayer) {
            openEpisodeExternal(context, episode.animeId, episode.id)
        } else {
            openEpisodeInternal(context, episode.animeId, episode.id)
        }
    }

    private fun getAnimeUrl(anime_: Anime?, source_: AnimeSource?): String? {
        val anime = anime_ ?: return null
        val source = source_ as? AnimeHttpSource ?: return null

        return try {
            source.getAnimeUrl(anime.toSAnime())
        } catch (e: Exception) {
            null
        }
    }

    private fun openAnimeInWebView(context: Context, anime_: Anime?, source_: AnimeSource?) {
        getAnimeUrl(anime_, source_)?.let { url ->
            val intent = WebViewActivity.newIntent(context, url, source_?.id, anime_?.title)
            context.startActivity(intent)
        }
    }

    private fun shareAnime(context: Context, anime_: Anime?, source_: AnimeSource?) {
        try {
            getAnimeUrl(anime_, source_)?.let { url ->
                val intent = url.toUri().toShareIntent(context, type = "text/plain")
                context.startActivity(
                    Intent.createChooser(
                        intent,
                        context.getString(R.string.action_share),
                    ),
                )
            }
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Perform a search using the provided query.
     *
     * @param query the search query to the parent controller
     */
    private suspend fun performSearch(navigator: Navigator, query: String, global: Boolean) {
        if (global) {
            navigator.push(GlobalAnimeSearchScreen(query))
            return
        }

        if (navigator.size < 2) {
            return
        }

        when (val previousController = navigator.items[navigator.size - 2]) {
            is HomeScreen -> {
                navigator.pop()
                previousController.search(query)
            }
            is BrowseAnimeSourceScreen -> {
                navigator.pop()
                previousController.search(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     *
     * @param genreName the search genre to the parent controller
     */
    private suspend fun performGenreSearch(navigator: Navigator, genreName: String, source: AnimeSource) {
        if (navigator.size < 2) {
            return
        }

        val previousController = navigator.items[navigator.size - 2]
        if (previousController is BrowseAnimeSourceScreen && source is AnimeHttpSource) {
            navigator.pop()
            previousController.searchGenre(genreName)
        } else {
            performSearch(navigator, genreName, global = false)
        }
    }

    /**
     * Copy Anime URL to Clipboard
     */
    private fun copyAnimeUrl(context: Context, anime_: Anime?, source_: AnimeSource?) {
        val anime = anime_ ?: return
        val source = source_ as? AnimeHttpSource ?: return
        val url = source.getAnimeUrl(anime.toSAnime())
        context.copyToClipboard(url, url)
    }
}

@Composable
fun ChangeIntroLength(
    anime: Anime,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val setAnimeViewerFlags: SetAnimeViewerFlags by injectLazy()
    val titleText = R.string.action_change_intro_length
    var newLength = 0
    BaseSelector(
        title = stringResource(titleText),
        content = {
            WheelTextPicker(
                modifier = Modifier.align(Alignment.Center),
                texts = remember { 1..255 }.map { "$it" },
                onScrollFinished = {
                    newLength = it
                    null
                },
                startIndex = anime.viewerFlags.toInt(),
            )
        },
        onConfirm = {
            scope.launchIO {
                setAnimeViewerFlags.awaitSetSkipIntroLength(anime.id, newLength.toLong())
                onDismissRequest()
            }
            Unit
        },
        onDismissRequest = onDismissRequest,
    )
}
