package eu.kanade.presentation.entries

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.EntryDownloadDropdownMenu
import eu.kanade.presentation.components.UpIcon
import eu.kanade.tachiyomi.R
import tachiyomi.presentation.core.theme.active

@Composable
fun EntryToolbar(
    modifier: Modifier = Modifier,
    title: String,
    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float = titleAlphaProvider,
    hasFilters: Boolean,
    onBackClicked: () -> Unit,
    onClickFilter: () -> Unit,
    onClickShare: (() -> Unit)?,
    onClickDownload: ((DownloadAction) -> Unit)?,
    onClickEditCategory: (() -> Unit)?,
    onClickRefresh: () -> Unit,
    onClickMigrate: (() -> Unit)?,
    onClickSettings: (() -> Unit)?,
    // Anime only
    changeAnimeSkipIntro: (() -> Unit)?,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    isManga: Boolean,
) {
    Column(
        modifier = modifier,
    ) {
        val isActionMode = actionModeCounter > 0
        TopAppBar(
            title = {
                Text(
                    text = if (isActionMode) actionModeCounter.toString() else title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (isActionMode) 1f else titleAlphaProvider()),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    UpIcon(Icons.Outlined.Close.takeIf { isActionMode })
                }
            },
            actions = {
                if (isActionMode) {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(R.string.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = onSelectAll,
                            ),
                            AppBar.Action(
                                title = stringResource(R.string.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = onInvertSelection,
                            ),
                        ),
                    )
                } else {
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
                        actions = buildList {
                            if (onClickDownload != null) {
                                add(
                                    AppBar.Action(
                                        title = stringResource(R.string.manga_download),
                                        icon = Icons.Outlined.Download,
                                        onClick = { downloadExpanded = !downloadExpanded },
                                    ),
                                )
                            }
                            add(
                                AppBar.Action(
                                    title = stringResource(R.string.action_filter),
                                    icon = Icons.Outlined.FilterList,
                                    iconTint = filterTint,
                                    onClick = onClickFilter,
                                ),
                            )
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(R.string.action_webview_refresh),
                                    onClick = onClickRefresh,
                                ),
                            )
                            if (onClickEditCategory != null) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(R.string.action_edit_categories),
                                        onClick = onClickEditCategory,
                                    ),
                                )
                            }
                            if (onClickMigrate != null) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(R.string.action_migrate),
                                        onClick = onClickMigrate,
                                    ),
                                )
                            }
                            if (changeAnimeSkipIntro != null) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(R.string.action_change_intro_length),
                                        onClick = changeAnimeSkipIntro,
                                    ),
                                )
                            }
                            if (onClickSettings != null) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(R.string.settings),
                                        onClick = onClickSettings,
                                    ),
                                )
                            }
                            if (onClickShare != null) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(R.string.action_share),
                                        onClick = onClickShare,
                                    ),
                                )
                            }
                        },
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme
                    .surfaceColorAtElevation(3.dp)
                    .copy(alpha = if (isActionMode) 1f else backgroundAlphaProvider()),
            ),
        )
    }
}
