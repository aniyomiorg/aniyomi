package eu.kanade.tachiyomi.ui.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import eu.kanade.tachiyomi.ui.download.manga.MangaDownloadHeaderItem
import eu.kanade.tachiyomi.ui.download.manga.MangaDownloadQueueScreenModel
import eu.kanade.tachiyomi.ui.download.manga.mangaDownloadTab
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.TabText
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
        val scope = rememberCoroutineScope()
        val animeScreenModel = rememberScreenModel { AnimeDownloadQueueScreenModel() }
        val mangaScreenModel = rememberScreenModel { MangaDownloadQueueScreenModel() }
        val animeDownloadList by animeScreenModel.state.collectAsState()
        val mangaDownloadList by mangaScreenModel.state.collectAsState()
        val animeDownloadCount by remember {
            derivedStateOf { animeDownloadList.sumOf { it.subItems.size } }
        }
        val mangaDownloadCount by remember {
            derivedStateOf { mangaDownloadList.sumOf { it.subItems.size } }
        }

        val state = rememberPagerState { 2 }
        val snackbarHostState = remember { SnackbarHostState() }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        var fabExpanded by remember { mutableStateOf(true) }
        val nestedScrollConnection = remember {
            // All this lines just for fab state :/
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
                        when (state.currentPage) {
                            0 -> AnimeActions(animeScreenModel, animeDownloadList)
                            1 -> MangaActions(mangaScreenModel, mangaDownloadList)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = when (state.currentPage) {
                        0 -> animeDownloadList.isNotEmpty()
                        1 -> mangaDownloadList.isNotEmpty()
                        else -> false
                    },
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val animeIsRunning by animeScreenModel.isDownloaderRunning.collectAsState()
                    val mangaIsRunning by mangaScreenModel.isDownloaderRunning.collectAsState()
                    ExtendedFloatingActionButton(
                        text = {
                            val id = when (state.currentPage) {
                                0 -> if (animeIsRunning) {
                                    AYMR.strings.action_stop
                                } else {
                                    AYMR.strings.action_continue
                                }
                                1 -> if (mangaIsRunning) {
                                    MR.strings.action_pause
                                } else {
                                    MR.strings.action_resume
                                }
                                else -> MR.strings.action_pause
                            }
                            Text(text = stringResource(id))
                        },
                        icon = {
                            val icon = when (state.currentPage) {
                                0 -> if (animeIsRunning) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                }
                                1 -> if (mangaIsRunning) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                }
                                else -> Icons.Filled.PlayArrow
                            }
                            Icon(imageVector = icon, contentDescription = null)
                        },
                        onClick = {
                            when (state.currentPage) {
                                0 -> if (animeIsRunning) {
                                    animeScreenModel.pauseDownloads()
                                } else {
                                    animeScreenModel.startDownloads()
                                }

                                1 -> if (mangaIsRunning) {
                                    mangaScreenModel.pauseDownloads()
                                } else {
                                    mangaScreenModel.startDownloads()
                                }
                            }
                        },
                        expanded = fabExpanded,
                    )
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
            ) {
                PrimaryTabRow(
                    selectedTabIndex = state.currentPage,
                    modifier = Modifier.zIndex(1f),
                ) {
                    listOf(
                        Tab(
                            selected = state.currentPage == 0,
                            onClick = { scope.launch { state.animateScrollToPage(0) } },
                            text = {
                                TabText(
                                    text = stringResource(AYMR.strings.label_anime),
                                    badgeCount = animeDownloadCount,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        Tab(
                            selected = state.currentPage == 1,
                            onClick = { scope.launch { state.animateScrollToPage(1) } },
                            text = {
                                TabText(
                                    text = stringResource(AYMR.strings.manga),
                                    badgeCount = mangaDownloadCount,
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }

                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    verticalAlignment = Alignment.Top,
                    pageNestedScrollConnection = nestedScrollConnection,
                ) { page ->
                    when (page) {
                        0 -> animeDownloadTab(
                            nestedScrollConnection,
                        ).content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )
                        1 -> mangaDownloadTab(
                            nestedScrollConnection,
                        ).content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )
                    }
                }
            }
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

    @Composable
    private fun MangaActions(
        mangaScreenModel: MangaDownloadQueueScreenModel,
        mangaDownloadList: List<MangaDownloadHeaderItem>,
    ) {
        if (mangaDownloadList.isNotEmpty()) {
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
                                mangaScreenModel.reorderQueue(
                                    { it.download.chapter.dateUpload },
                                    true,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_oldest)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    { it.download.chapter.dateUpload },
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
                            text = stringResource(MR.strings.action_order_by_chapter_number),
                        )
                    },
                    children = { closeMenu ->
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_asc)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    { it.download.chapter.chapterNumber },
                                    false,
                                )
                                closeMenu()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(MR.strings.action_desc)) },
                            onClick = {
                                mangaScreenModel.reorderQueue(
                                    { it.download.chapter.chapterNumber },
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
                        onClick = { mangaScreenModel.clearQueue() },
                    ),
                ),
            )
        }
    }
}
