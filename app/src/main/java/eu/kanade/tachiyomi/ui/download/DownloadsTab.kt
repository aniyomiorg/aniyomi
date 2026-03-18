package eu.kanade.tachiyomi.ui.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NestedMenuItem
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadHeaderItem
import eu.kanade.tachiyomi.ui.download.anime.AnimeDownloadQueueScreenModel
import eu.kanade.tachiyomi.ui.download.anime.animeDownloadTab
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

data object DownloadsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_history_enter)
            return TabOptions(
                index = 6u,
                title = stringResource(MR.strings.label_download_queue),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val animeScreenModel = rememberScreenModel { AnimeDownloadQueueScreenModel() }
        val animeDownloadList by animeScreenModel.state.collectAsState()
        val animeDownloadCount by remember {
            derivedStateOf { animeDownloadList.sumOf { it.subItems.size } }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        var fabExpanded by remember { mutableStateOf(true) }
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    fabExpanded = available.y >= 0
                    return scrollBehavior.nestedScrollConnection.onPreScroll(available, source)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    return scrollBehavior.nestedScrollConnection.onPostScroll(consumed, available, source)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return scrollBehavior.nestedScrollConnection.onPreFling(available)
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    return scrollBehavior.nestedScrollConnection.onPostFling(consumed, available)
                }
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    titleContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(MR.strings.label_download_queue),
                                maxLines = 1,
                                modifier = Modifier.weight(1f, false),
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (animeDownloadCount > 0) {
                                val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
                                Pill(
                                    text = "$animeDownloadCount",
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                        .copy(alpha = pillAlpha),
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    },
                    navigateUp = navigator::pop,
                    actions = {
                        AnimeActions(animeScreenModel, animeDownloadList)
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = animeDownloadList.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val animeIsRunning by animeScreenModel.isDownloaderRunning.collectAsState()
                    ExtendedFloatingActionButton(
                        text = {
                            val id = if (animeIsRunning) {
                                AYMR.strings.action_stop
                            } else {
                                AYMR.strings.action_continue
                            }
                            Text(text = stringResource(id))
                        },
                        icon = {
                            val icon = if (animeIsRunning) {
                                Icons.Outlined.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            }
                            Icon(imageVector = icon, contentDescription = null)
                        },
                        onClick = {
                            if (animeIsRunning) {
                                animeScreenModel.pauseDownloads()
                            } else {
                                animeScreenModel.startDownloads()
                            }
                        },
                        expanded = fabExpanded,
                    )
                }
            },
        ) { contentPadding ->
            animeDownloadTab(
                nestedScrollConnection,
            ).content(
                PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                snackbarHostState,
            )
        }
    }

    @Composable
    private fun AnimeActions(
        animeScreenModel: AnimeDownloadQueueScreenModel,
        animeDownloadList: List<AnimeDownloadHeaderItem>,
    ) {
        if (animeDownloadList.isNotEmpty()) {
            var sortExpanded by remember { mutableStateOf(false) }
            val onDismissRequest = { sortExpanded = false }
            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = onDismissRequest,
            ) {
                NestedMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_newest)) },
                            onClick = {
                                animeScreenModel.reorderQueue(
                                    { it.download.episode.dateUpload },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                            onClick = {
                                animeScreenModel.reorderQueue(
                                    { it.download.episode.dateUpload },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
                NestedMenuItem(
                    text = {
                        Text(
                            text = stringResource(AYMR.strings.action_order_by_episode_number),
                        )
                    },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                            onClick = {
                                animeScreenModel.reorderQueue(
                                    { it.download.episode.episodeNumber },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                            onClick = {
                                animeScreenModel.reorderQueue(
                                    { it.download.episode.episodeNumber },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                    },
                )
            }

            AppBarActions(
                persistentListOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_sort),
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        onClick = { sortExpanded = true },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_cancel_all),
                        onClick = { animeScreenModel.clearQueue() },
                    ),
                ),
            )
        }
    }
}
