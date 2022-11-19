package eu.kanade.tachiyomi.ui.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.history.HistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.manga.MangaController

@Composable
fun historyTab(
    router: Router?,
    presenter: HistoryPresenter,
    fromMore: Boolean = false,
): TabContent {
    val navigateUp: (() -> Unit)? = if (fromMore && router != null) {
        { router.popCurrentController() }
    } else {
        null
    }

    return TabContent(
        titleRes = R.string.label_history,
        searchEnabled = true,
        content = { contentPadding ->
            HistoryScreen(
                presenter = presenter,
                contentPadding = contentPadding,
                onClickCover = { history ->
                    router!!.pushController(MangaController(history.mangaId))
                },
                onClickResume = { history ->
                    presenter.getNextChapterForManga(history.mangaId, history.chapterId)
                },
            )
        },
        actions =
        listOf(
            AppBar.Action(
                title = stringResource(R.string.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { presenter.dialog = HistoryPresenter.Dialog.DeleteAll },
            ),
        ),
        navigateUp = navigateUp,
    )
}
