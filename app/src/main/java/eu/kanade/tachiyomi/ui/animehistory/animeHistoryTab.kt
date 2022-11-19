package eu.kanade.tachiyomi.ui.animehistory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animehistory.AnimeHistoryScreen
import eu.kanade.presentation.components.AppBar
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
        searchEnabled = true,
        content = { contentPadding ->
            AnimeHistoryScreen(
                presenter = presenter,
                contentPadding = contentPadding,
                onClickCover = { history ->
                    router?.pushController(AnimeController(history.animeId))
                },
                onClickResume = { history ->
                    presenter.getNextEpisodeForAnime(history.animeId, history.episodeId)
                },
            )
        },
        actions =
        listOf(
            AppBar.Action(
                title = stringResource(R.string.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { presenter.dialog = AnimeHistoryPresenter.Dialog.DeleteAll },
            ),
        ),
        navigateUp = navigateUp,
    )
}
