package eu.kanade.tachiyomi.ui.animehistory

import androidx.compose.runtime.Composable
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animehistory.AnimeHistoryScreen
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.pushController

@Composable
fun animeHistoryTab(
    router: Router?,
    presenter: AnimeHistoryPresenter,
    fromMore: Boolean = false,
): TabContent {
    val navigateUp: (() -> Unit)? = if (fromMore && router != null) {
        { router.popCurrentController() }
    } else {
        null
    }

    return TabContent(
        titleRes = R.string.label_animehistory,
        content = {
            AnimeHistoryScreen(
                presenter = presenter,
                onClickCover = { history ->
                    router?.pushController(AnimeController(history.animeId))
                },
                onClickResume = { history ->
                    presenter.getNextEpisodeForAnime(history.animeId, history.episodeId)
                },
                navigateUp = navigateUp,
            )
        },
    )
}
