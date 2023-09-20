package eu.kanade.tachiyomi.ui.download.manga

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.util.lang.launchUI
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import kotlin.math.roundToInt

@Composable
fun DownloadQueueScreen(
    contentPadding: PaddingValues,
    scope: CoroutineScope,
    screenModel: MangaDownloadQueueScreenModel,
    downloadList: List<MangaDownloadHeaderItem>,
) {
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
        floatingActionButton = {
            AnimatedVisibility(
                visible = downloadList.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val isRunning by screenModel.isDownloaderRunning.collectAsState()
                ExtendedFloatingActionButton(
                    text = {
                        val id = if (isRunning) {
                            R.string.action_pause
                        } else {
                            R.string.action_resume
                        }
                        Text(text = stringResource(id))
                    },
                    icon = {
                        val icon = if (isRunning) {
                            Icons.Outlined.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        }
                        Icon(imageVector = icon, contentDescription = null)
                    },
                    onClick = {
                        if (isRunning) {
                            screenModel.pauseDownloads()
                        } else {
                            screenModel.startDownloads()
                        }
                    },
                    expanded = fabExpanded,
                )
            }
        },
    ) {
        if (downloadList.isEmpty()) {
            EmptyScreen(
                textResource = R.string.information_no_downloads,
                modifier = Modifier.padding(contentPadding),
            )
            return@Scaffold
        }
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val left = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx().roundToInt() }
        val top = with(density) { contentPadding.calculateTopPadding().toPx().roundToInt() }
        val right = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx().roundToInt() }
        val bottom = with(density) { contentPadding.calculateBottomPadding().toPx().roundToInt() }

        Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
            AndroidView(
                factory = { context ->
                    screenModel.controllerBinding = DownloadListBinding.inflate(LayoutInflater.from(context))
                    screenModel.adapter = MangaDownloadAdapter(screenModel.listener)
                    screenModel.controllerBinding.recycler.adapter = screenModel.adapter
                    screenModel.adapter?.isHandleDragEnabled = true
                    screenModel.adapter?.fastScroller = screenModel.controllerBinding.fastScroller
                    screenModel.controllerBinding.recycler.layoutManager = LinearLayoutManager(context)

                    ViewCompat.setNestedScrollingEnabled(screenModel.controllerBinding.root, true)

                    scope.launchUI {
                        screenModel.getDownloadStatusFlow()
                            .collect(screenModel::onStatusChange)
                    }
                    scope.launchUI {
                        screenModel.getDownloadProgressFlow()
                            .collect(screenModel::onUpdateDownloadedPages)
                    }

                    screenModel.controllerBinding.root
                },
                update = {
                    screenModel.controllerBinding.recycler
                        .updatePadding(
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom,
                        )

                    screenModel.controllerBinding.fastScroller
                        .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            leftMargin = left
                            topMargin = top
                            rightMargin = right
                            bottomMargin = bottom
                        }

                    screenModel.adapter?.updateDataSet(downloadList)
                },
            )
        }
    }
}
