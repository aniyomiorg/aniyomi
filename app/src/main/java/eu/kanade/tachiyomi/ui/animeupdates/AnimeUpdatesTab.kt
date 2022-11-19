package eu.kanade.tachiyomi.ui.animeupdates

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bluelinelabs.conductor.Router
import eu.kanade.presentation.animeupdates.AnimeUpdateScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.toast

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

    val context = LocalContext.current
    val onUpdateLibrary = {
        val started = AnimelibUpdateService.start(context)
        context.toast(if (started) R.string.updating_library else R.string.update_already_running)
        started
    }

    return TabContent(
        titleRes = R.string.label_animeupdates,
        actions =
        if (presenter.selected.isNotEmpty()) {
            listOf(
                AppBar.Action(
                    title = stringResource(R.string.action_select_all),
                    icon = Icons.Outlined.SelectAll,
                    onClick = { presenter.toggleAllSelection(true) },
                ),
                AppBar.Action(
                    title = stringResource(R.string.action_select_inverse),
                    icon = Icons.Outlined.FlipToBack,
                    onClick = { presenter.invertSelection() },
                ),
            )
        } else {
            listOf(
                AppBar.Action(
                    title = stringResource(R.string.action_update_library),
                    icon = Icons.Outlined.Refresh,
                    onClick = { onUpdateLibrary() },
                ),
            )
        },
        content = { contentPadding ->
            AnimeUpdateScreen(
                presenter = presenter,
                contentPadding = contentPadding,
                onClickCover = { item ->
                    router?.pushController(AnimeController(item.update.animeId))
                },
                onBackClicked = {
                    (activity as? MainActivity)?.moveToStartScreen()
                },
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
        numberTitle = presenter.selected.size,
        cancelAction = { presenter.toggleAllSelection(false) },
        navigateUp = navigateUp,
    )
}
