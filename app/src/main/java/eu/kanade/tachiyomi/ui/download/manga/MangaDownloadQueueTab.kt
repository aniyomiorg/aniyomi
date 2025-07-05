package eu.kanade.tachiyomi.ui.download.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.TabContent
import tachiyomi.i18n.aniyomi.AYMR

@Composable
fun Screen.mangaDownloadTab(
    nestedScrollConnection: NestedScrollConnection,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()
    val screenModel = rememberScreenModel { MangaDownloadQueueScreenModel() }
    val downloadList by screenModel.state.collectAsState()
    val downloadCount by remember {
        derivedStateOf { downloadList.sumOf { it.subItems.size } }
    }

    return TabContent(
        titleRes = AYMR.strings.label_manga,
        searchEnabled = false,
        content = { contentPadding, _ ->
            DownloadQueueScreen(
                contentPadding = contentPadding,
                scope = scope,
                screenModel = screenModel,
                downloadList = downloadList,
                nestedScrollConnection = nestedScrollConnection,
            )
        },
        numberTitle = downloadCount,
        navigateUp = navigator::pop,
    )
}
