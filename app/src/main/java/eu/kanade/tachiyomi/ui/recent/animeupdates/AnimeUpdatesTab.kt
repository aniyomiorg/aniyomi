package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.app.Activity
import androidx.compose.runtime.Composable
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animeupdates.AnimeUpdateScreen
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.main.MainActivity

@Composable
fun animeUpdatesTab(
    router: Router?,
    presenter: AnimeUpdatesPresenter,
    activity: Activity?,
): TabContent {
    return TabContent(
        titleRes = R.string.label_animeupdates,
        content = {
            AnimeUpdateScreen(
                presenter = presenter,
                onClickCover = { item ->
                    router!!.pushController(AnimeController(item.update.animeId))
                },
                onBackClicked = {
                    (activity as? MainActivity)?.moveToStartScreen()
                },
            )
        },
    )
}
