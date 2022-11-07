package eu.kanade.tachiyomi.ui.recent.updates

import android.app.Activity
import androidx.compose.runtime.Composable
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.updates.UpdateScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController

@Composable
fun updatesTab(
    router: Router?,
    presenter: UpdatesPresenter,
    activity: Activity?,
): TabContent {
    return TabContent(
        titleRes = R.string.label_updates,
        content = {
            UpdateScreen(
                presenter = presenter,
                onClickCover = { item ->
                    router!!.pushController(MangaController(item.update.mangaId))
                },
                onBackClicked = {
                    (activity as? MainActivity)?.moveToStartScreen()
                },
            )
        },
    )
}
