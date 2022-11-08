package eu.kanade.presentation.animeupdates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.AnimeBottomActionMenu
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.components.SwipeRefresh
import eu.kanade.presentation.updates.updatesLastUpdatedItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesItem
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesPresenter
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesPresenter.Dialog
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesPresenter.Event
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.TachiyomiBottomNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimeUpdateScreen(
    presenter: AnimeUpdatesPresenter,
    onClickCover: (AnimeUpdatesItem) -> Unit,
    onBackClicked: () -> Unit,
) {
    val internalOnBackPressed = {
        if (presenter.selectionMode) {
            presenter.toggleAllSelection(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)

    val context = LocalContext.current
    val onUpdateLibrary = {
        val started = AnimelibUpdateService.start(context)
        context.toast(if (started) R.string.updating_library else R.string.update_already_running)
        started
    }

    Scaffold(
        topBar = { scrollBehavior ->
            AnimeUpdatesAppBar(
                incognitoMode = presenter.isIncognitoMode,
                downloadedOnlyMode = presenter.isDownloadOnly,
                onUpdateLibrary = { onUpdateLibrary() },
                actionModeCounter = presenter.selected.size,
                onSelectAll = { presenter.toggleAllSelection(true) },
                onInvertSelection = { presenter.invertSelection() },
                onCancelActionMode = { presenter.toggleAllSelection(false) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            AnimeUpdatesBottomBar(
                selected = presenter.selected,
                onDownloadEpisode = presenter::downloadEpisodes,
                onMultiBookmarkClicked = presenter::bookmarkUpdates,
                onMultiMarkAsSeenClicked = presenter::markUpdatesSeen,
                onMultiDeleteClicked = {
                    presenter.dialog = Dialog.DeleteConfirmation(it)
                },
                onOpenEpisode = presenter::openEpisode,
            )
        },
    ) { contentPadding ->
        val contentPaddingWithNavBar = TachiyomiBottomNavigationView.withBottomNavPadding(contentPadding)
        when {
            presenter.isLoading -> LoadingScreen()
            presenter.uiModels.isEmpty() -> EmptyScreen(
                textResource = R.string.information_no_recent,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                AnimeUpdateScreenContent(
                    presenter = presenter,
                    contentPadding = contentPaddingWithNavBar,
                    onUpdateLibrary = onUpdateLibrary,
                    onClickCover = onClickCover,
                )
            }
        }
    }
}

@Composable
private fun AnimeUpdateScreenContent(
    presenter: AnimeUpdatesPresenter,
    contentPadding: PaddingValues,
    onUpdateLibrary: () -> Boolean,
    onClickCover: (AnimeUpdatesItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    SwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = {
            val started = onUpdateLibrary()
            if (!started) return@SwipeRefresh
            scope.launch {
                // Fake refresh status but hide it after a second as it's a long running task
                isRefreshing = true
                delay(1.seconds)
                isRefreshing = false
            }
        },
        enabled = presenter.selectionMode.not(),
        indicatorPadding = contentPadding,
    ) {
        FastScrollLazyColumn(
            contentPadding = contentPadding,
        ) {
            if (presenter.lastUpdated > 0L) {
                updatesLastUpdatedItem(presenter.lastUpdated)
            }

            animeupdatesUiItems(
                uiModels = presenter.uiModels,
                selectionMode = presenter.selectionMode,
                onUpdateSelected = presenter::toggleSelection,
                onClickCover = onClickCover,
                onClickUpdate = {
                    val intent = PlayerActivity.newIntent(context, it.update.animeId, it.update.episodeId)
                    context.startActivity(intent)
                },
                onDownloadEpisode = presenter::downloadEpisodes,
                relativeTime = presenter.relativeTime,
                dateFormat = presenter.dateFormat,
            )
        }
    }

    val onDismissDialog = { presenter.dialog = null }
    when (val dialog = presenter.dialog) {
        is Dialog.DeleteConfirmation -> {
            AnimeUpdatesDeleteConfirmationDialog(
                onDismissRequest = onDismissDialog,
                onConfirm = {
                    presenter.toggleAllSelection(false)
                    presenter.deleteEpisodes(dialog.toDelete)
                },
            )
        }
        null -> {}
    }
    LaunchedEffect(Unit) {
        presenter.events.collectLatest { event ->
            when (event) {
                Event.InternalError -> context.toast(R.string.internal_error)
            }
        }
    }
}

@Composable
private fun AnimeUpdatesAppBar(
    modifier: Modifier = Modifier,
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
    onUpdateLibrary: () -> Unit,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    AppBar(
        modifier = modifier,
        title = stringResource(R.string.label_recent_updates),
        actions = {
            IconButton(onClick = onUpdateLibrary) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_update_library),
                )
            }
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Outlined.SelectAll,
                    contentDescription = stringResource(R.string.action_select_all),
                )
            }
            IconButton(onClick = onInvertSelection) {
                Icon(
                    imageVector = Icons.Outlined.FlipToBack,
                    contentDescription = stringResource(R.string.action_select_inverse),
                )
            }
        },
        downloadedOnlyMode = downloadedOnlyMode,
        incognitoMode = incognitoMode,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun AnimeUpdatesBottomBar(
    selected: List<AnimeUpdatesItem>,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<AnimeUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<AnimeUpdatesItem>, seen: Boolean) -> Unit,
    onMultiDeleteClicked: (List<AnimeUpdatesItem>) -> Unit,
    onOpenEpisode: (List<AnimeUpdatesItem>, altPlayer: Boolean) -> Unit,
) {
    val playerPreferences: PlayerPreferences = Injekt.get()
    AnimeBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.any { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.all { it.update.bookmark } },
        onMarkAsSeenClicked = {
            onMultiMarkAsSeenClicked(selected, true)
        }.takeIf { selected.any { !it.update.seen } },
        onMarkAsUnseenClicked = {
            onMultiMarkAsSeenClicked(selected, false)
        }.takeIf { selected.any { it.update.seen } },
        onDownloadClicked = {
            onDownloadEpisode(selected, EpisodeDownloadAction.START)
        }.takeIf {
            selected.any { it.downloadStateProvider() != AnimeDownload.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.any { it.downloadStateProvider() == AnimeDownload.State.DOWNLOADED } },
        onExternalClicked = {
            onOpenEpisode(selected, true)
        }.takeIf { !playerPreferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
        onInternalClicked = {
            onOpenEpisode(selected, false)
        }.takeIf { playerPreferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
    )
}

sealed class AnimeUpdatesUiModel {
    data class Header(val date: Date) : AnimeUpdatesUiModel()
    data class Item(val item: AnimeUpdatesItem) : AnimeUpdatesUiModel()
}
