package eu.kanade.presentation.entries.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.EntryDownloadDropdownMenu
import eu.kanade.presentation.entries.DownloadAction
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.feed.SourceFeedScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun EntryToolbar(
    title: String,
    hasFilters: Boolean,
    navigateUp: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    onClickEditCategory: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickSettings: (() -> Unit)?,
    // Anime only
    changeAnimeSkipIntro: (() -> Unit)?,
    // SY -->
    onClickEditInfo: (() -> Unit)?,
    // KMK -->
    onClickRelatedAnimes: (() -> Unit)?,
    // KMK <--
    // SY <--
    // For action mode
    actionModeCounter: Int,
    onCancelActionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float,
    isManga: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    fun onHomeClicked() = navigator?.popUntil { screen ->
        screen is SourceFeedScreen || screen is BrowseAnimeSourceScreen
    }
    val isHomeEnabled = Injekt.get<UiPreferences>().showHomeOnRelatedAnimes().get()

    val isActionMode = actionModeCounter > 0
    AppBar(
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, modifier = Modifier.alpha(titleAlphaProvider()))
            }
        },
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme
            .surfaceColorAtElevation(3.dp)
            .copy(alpha = if (isActionMode) 1f else backgroundAlphaProvider()),
        navigateUp = {
            if (isActionMode) {
                onCancelActionMode()
            } else {
                navigateUp()

                if (isHomeEnabled && navigator != null) {
                    if (navigator.size >= 2 &&
                        navigator.items[navigator.size - 2] is AnimeScreen ||
                        navigator.size >= 5
                    ) {
                        onHomeClicked()
                    }
                }
            }
        },
        actions = {
            var downloadExpanded by remember { mutableStateOf(false) }
            if (onClickDownload != null) {
                val onDismissRequest = { downloadExpanded = false }
                EntryDownloadDropdownMenu(
                    expanded = downloadExpanded,
                    onDismissRequest = onDismissRequest,
                    onDownloadClicked = onClickDownload,
                    isManga = isManga,
                )
            }

            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            AppBarActions(
                actions = persistentListOf<AppBar.AppBarAction>().builder().apply {
                    if (isActionMode) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = onSelectAll,
                            ),
                        )
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = onInvertSelection,
                            ),
                        )
                        return@apply
                    }
                    if (onClickDownload != null) {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.manga_download),
                                icon = Icons.Outlined.Download,
                                onClick = { downloadExpanded = !downloadExpanded },
                            ),
                        )
                    }
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_filter),
                            icon = Icons.Outlined.FilterList,
                            iconTint = filterTint,
                            onClick = onClickFilter,
                        ),
                    )
                                // SY -->
                                if (onClickEditInfo != null) {
                                    add(
                                        AppBar.OverflowAction(
                                            title = stringResource(TLMR.strings.action_edit_info),
                                            onClick = onClickEditInfo,
                                        ),
                                    )
                                }
                                // SY <--
                    if (changeAnimeSkipIntro != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_change_intro_length),
                                onClick = changeAnimeSkipIntro,
                            ),
                        )
                    }
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.action_webview_refresh),
                            onClick = onClickRefresh,
                        ),
                    )

                    if (onClickEditCategory != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_edit_categories),
                                onClick = onClickEditCategory,
                            ),
                        )
                    }
                    if (onClickMigrate != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_migrate),
                                onClick = onClickMigrate,
                            ),
                        )
                    }
                    if (onClickShare != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_share),
                                onClick = onClickShare,
                            ),
                        )
                    }
                                // KMK -->
                                if (onClickRelatedAnimes != null) {
                                    add(
                                        AppBar.OverflowAction(
                                            title = stringResource(TLMR.strings.pref_source_related_mangas),
                                            onClick = onClickRelatedAnimes,
                                        ),
                                    )
                                }
                                // KMK <--
                    if (onClickSettings != null) {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.settings),
                                onClick = onClickSettings,
                            ),
                        )
                    }
                }
                    .build(),
            )
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
    )
}
