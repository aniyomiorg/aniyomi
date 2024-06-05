package eu.kanade.tachiyomi.ui.download.manga

import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen
import kotlin.math.roundToInt

@Composable
fun DownloadQueueScreen(
    contentPadding: PaddingValues,
    scope: CoroutineScope,
    screenModel: MangaDownloadQueueScreenModel,
    downloadList: List<MangaDownloadHeaderItem>,
    nestedScrollConnection: NestedScrollConnection,
) {
    Scaffold {
        if (downloadList.isEmpty()) {
            EmptyScreen(
                stringRes = MR.strings.information_no_downloads,
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
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    screenModel.controllerBinding = DownloadListBinding.inflate(
                        LayoutInflater.from(context),
                    )
                    screenModel.adapter = MangaDownloadAdapter(screenModel.listener)
                    screenModel.controllerBinding.root.adapter = screenModel.adapter
                    screenModel.adapter?.isHandleDragEnabled = true
                    screenModel.controllerBinding.root.layoutManager = LinearLayoutManager(
                        context,
                    )

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
                    screenModel.controllerBinding.root
                        .updatePadding(
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom,
                        )

                    screenModel.adapter?.updateDataSet(downloadList)
                },
            )
        }
    }
}
