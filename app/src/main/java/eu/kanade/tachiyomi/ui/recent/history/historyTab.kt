package eu.kanade.tachiyomi.ui.recent.history

import androidx.compose.runtime.Composable
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.history.HistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.manga.MangaController

@Composable
fun historyTab(
    router: Router?,
    presenter: HistoryPresenter,
): TabContent {
    return TabContent(
        titleRes = R.string.label_history,
        content = {
            HistoryScreen(
                presenter = presenter,
                onClickCover = { history ->
                    router!!.pushController(MangaController(history.mangaId))
                },
                onClickResume = { history ->
                    presenter.getNextChapterForManga(history.mangaId, history.chapterId)
                },
            )
        },
    )
}
