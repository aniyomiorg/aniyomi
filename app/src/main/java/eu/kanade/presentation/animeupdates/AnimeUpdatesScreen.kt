package eu.kanade.presentation.animeupdates

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.components.AnimeBottomActionMenu
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.components.FastScrollLazyColumn
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.PullRefresh
import eu.kanade.presentation.components.Scaffold
import eu.kanade.presentation.updates.updatesLastUpdatedItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload
import eu.kanade.tachiyomi.ui.animeupdates.AnimeUpdatesItem
import eu.kanade.tachiyomi.ui.animeupdates.AnimeUpdatesState
import eu.kanade.tachiyomi.ui.player.setting.PlayerPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimeUpdateScreen(
    state: AnimeUpdatesState,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    relativeTime: Int,
    onClickCover: (AnimeUpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<AnimeUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<AnimeUpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<AnimeUpdatesItem>) -> Unit,
    onUpdateSelected: (AnimeUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onOpenEpisode: (List<AnimeUpdatesItem>, context: Context, altPlayer: Boolean) -> Unit,
) {
    BackHandler(enabled = state.selectionMode, onBack = { onSelectAll(false) })

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            AnimeUpdatesBottomBar(
                selected = state.selected,
                onDownloadEpisode = onDownloadEpisode,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsSeenClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
                onOpenEpisode = onOpenEpisode,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(contentPadding))
            state.items.isEmpty() -> EmptyScreen(
                textResource = R.string.information_no_recent,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                val scope = rememberCoroutineScope()
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        if (!started) return@PullRefresh
                        scope.launch {
                            // Fake refresh status but hide it after a second as it's a long running task
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    FastScrollLazyColumn(
                        contentPadding = contentPadding,
                    ) {
                        if (lastUpdated > 0L) {
                            updatesLastUpdatedItem(lastUpdated)
                        }
                        animeupdatesUiItems(
                            uiModels = state.getUiModel(context, relativeTime),
                            selectionMode = state.selectionMode,
                            onUpdateSelected = onUpdateSelected,
                            onClickCover = onClickCover,
                            onClickUpdate = { onOpenEpisode },
                            onDownloadEpisode = onDownloadEpisode,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeUpdatesBottomBar(
    selected: List<AnimeUpdatesItem>,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<AnimeUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<AnimeUpdatesItem>, seen: Boolean) -> Unit,
    onMultiDeleteClicked: (List<AnimeUpdatesItem>) -> Unit,
    onOpenEpisode: (List<AnimeUpdatesItem>, context: Context, altPlayer: Boolean) -> Unit,
) {
    val playerPreferences: PlayerPreferences = Injekt.get()
    val context = LocalContext.current
    AnimeBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onMarkAsSeenClicked = {
            onMultiMarkAsSeenClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.seen } },
        onMarkAsUnseenClicked = {
            onMultiMarkAsSeenClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.seen } },
        onDownloadClicked = {
            onDownloadEpisode(selected, EpisodeDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != AnimeDownload.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == AnimeDownload.State.DOWNLOADED } },
        onExternalClicked = {
            onOpenEpisode(selected, context, true)
        }.takeIf { !playerPreferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
        onInternalClicked = {
            onOpenEpisode(selected, context,false)
        }.takeIf { playerPreferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
    )
}

sealed class AnimeUpdatesUiModel {
    data class Header(val date: String) : AnimeUpdatesUiModel()
    data class Item(val item: AnimeUpdatesItem) : AnimeUpdatesUiModel()
}
