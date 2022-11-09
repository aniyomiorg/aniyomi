package eu.kanade.tachiyomi.ui.animeupdates

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    fromMore: Boolean = false,
): TabContent {
    val navigateUp: (() -> Unit)? = if (fromMore && router != null) {
        { router.popCurrentController() }
    } else {
        null
    }

    return TabContent(
        titleRes = R.string.label_animeupdates,
        content = {
            AnimeUpdateScreen(
                presenter = presenter,
                onClickCover = { item ->
                    router?.pushController(AnimeController(item.update.animeId))
                },
                onBackClicked = {
                    (activity as? MainActivity)?.moveToStartScreen()
                },
                navigateUp = navigateUp,
            )

            LaunchedEffect(presenter.selectionMode) {
                (activity as? MainActivity)?.showBottomNav(presenter.selectionMode.not())
            }
            LaunchedEffect(presenter.isLoading) {
                if (!presenter.isLoading) {
                    (activity as? MainActivity)?.ready = true
                }
            }
        },
    )
}
