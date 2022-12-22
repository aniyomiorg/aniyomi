package eu.kanade.tachiyomi.ui.animelib

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.core.prefs.CheckboxState
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.animelib.AnimelibScreen
import eu.kanade.presentation.components.ChangeCategoryDialog
import eu.kanade.presentation.components.DeleteAnimelibAnimeDialog
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.DownloadCustomAmountDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.FullComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.cancel

class AnimelibController(
    bundle: Bundle? = null,
) : FullComposeController<AnimelibPresenter>(bundle), RootController {

    /**
     * Sheet containing filter/sort/display items.
     */
    private var settingsSheet: AnimelibSettingsSheet? = null

    override fun createPresenter(): AnimelibPresenter = AnimelibPresenter()

    @Composable
    override fun ComposeContent() {
        val context = LocalContext.current
        val getAnimeForCategory = presenter.getAnimeForCategory(page = presenter.activeCategory)

        AnimelibScreen(
            presenter = presenter,
            onAnimeClicked = ::openAnime,
            onContinueWatchingClicked = ::continueWatching,
            onGlobalSearchClicked = {
                router.pushController(GlobalAnimeSearchController(presenter.searchQuery))
            },
            onChangeCategoryClicked = ::showAnimeCategoriesDialog,
            onMarkAsSeenClicked = { markSeenStatus(true) },
            onMarkAsUnseenClicked = { markSeenStatus(false) },
            onDownloadClicked = ::runDownloadEpisodeAction,
            onDeleteClicked = ::showDeleteAnimeDialog,
            onClickFilter = ::showSettingsSheet,
            onClickRefresh = {
                val started = AnimelibUpdateService.start(context, it)
                context.toast(if (started) R.string.updating_category else R.string.update_already_running)
                started
            },
            onClickOpenRandomAnime = {
                val items = getAnimeForCategory.map { it.animelibAnime.anime.id }
                if (getAnimeForCategory.isNotEmpty()) {
                    openAnime(items.random())
                } else {
                    context.toast(R.string.information_no_entries_found)
                }
            },
            onClickInvertSelection = { presenter.invertSelection(presenter.activeCategory) },
            onClickSelectAll = { presenter.selectAll(presenter.activeCategory) },
            onClickUnselectAll = ::clearSelection,
        )

        val onDismissRequest = { presenter.dialog = null }
        when (val dialog = presenter.dialog) {
            is AnimelibPresenter.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        presenter.clearSelection()
                        router.pushController(CategoryController())
                    },
                    onConfirm = { include, exclude ->
                        presenter.clearSelection()
                        presenter.setAnimeCategories(dialog.anime, include, exclude)
                    },
                )
            }
            is AnimelibPresenter.Dialog.DeleteAnime -> {
                DeleteAnimelibAnimeDialog(
                    containsLocalAnime = dialog.anime.any(Anime::isLocal),
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteAnime, deleteEpisode ->
                        presenter.removeAnimes(dialog.anime.map { it.toDbAnime() }, deleteAnime, deleteEpisode)
                        presenter.clearSelection()
                    },
                )
            }
            is AnimelibPresenter.Dialog.DownloadCustomAmount -> {
                DownloadCustomAmountDialog(
                    maxAmount = dialog.max,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { amount ->
                        presenter.downloadUnseenEpisodes(dialog.anime, amount)
                        presenter.clearSelection()
                    },
                )
            }
            null -> {}
        }

        LaunchedEffect(presenter.selectionMode) {
            // Could perhaps be removed when navigation is in a Compose world
            if (router.backstackSize == 1) {
                (activity as? MainActivity)?.showBottomNav(presenter.selectionMode.not())
            }
        }
        LaunchedEffect(presenter.isLoading) {
            if (!presenter.isLoading) {
                (activity as? MainActivity)?.ready = true
            }
        }
    }

    override fun handleBack(): Boolean {
        return when {
            presenter.selection.isNotEmpty() -> {
                presenter.clearSelection()
                true
            }
            presenter.searchQuery != null -> {
                presenter.searchQuery = null
                true
            }
            else -> false
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        settingsSheet = AnimelibSettingsSheet(router) { group ->
            when (group) {
                is AnimelibSettingsSheet.Filter.FilterGroup -> onFilterChanged()
                else -> {} // Handled via different mechanisms
            }
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            presenter.subscribeAnimelib()
        }
    }

    override fun onDestroyView(view: View) {
        settingsSheet?.sheetScope?.cancel()
        settingsSheet = null
        super.onDestroyView(view)
    }

    fun showSettingsSheet() {
        presenter.categories.getOrNull(presenter.activeCategory)?.let { category ->
            settingsSheet?.show(category)
        }
    }

    private fun onFilterChanged() {
        viewScope.launchUI {
            presenter.requestFilterUpdate()
            activity?.invalidateOptionsMenu()
        }
    }

    fun search(query: String) {
        presenter.searchQuery = query
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val settingsSheet = settingsSheet ?: return
        presenter.hasActiveFilters = settingsSheet.filters.hasActiveFilters()
    }

    private fun openAnime(animeId: Long) {
        // Notify the presenter an anime is being opened.
        presenter.onOpenAnime()

        router.pushController(AnimeController(animeId))
    }

    private fun continueWatching(animelibAnime: AnimelibAnime) {
        viewScope.launchIO {
            val episode = presenter.getNextUnseenEpisode(animelibAnime.anime)
            if (episode != null) openEpisode(episode)
        }
    }

    private fun openEpisode(episode: Episode) {
        activity?.run {
            startActivity(PlayerActivity.newIntent(this, episode.animeId, episode.id))
        }
    }

    /**
     * Clear all of the anime currently selected, and
     * invalidate the action mode to revert the top toolbar
     */
    private fun clearSelection() {
        presenter.clearSelection()
    }

    /**
     * Move the selected anime to a list of categories.
     */
    private fun showAnimeCategoriesDialog() {
        viewScope.launchIO {
            // Create a copy of selected anime
            val animeList = presenter.selection.map { it.anime }

            // Hide the default category because it has a different behavior than the ones from db.
            val categories = presenter.categories.filter { it.id != 0L }

            // Get indexes of the common categories to preselect.
            val common = presenter.getCommonCategories(animeList)
            // Get indexes of the mix categories to preselect.
            val mix = presenter.getMixCategories(animeList)
            val preselected = categories.map {
                when (it) {
                    in common -> CheckboxState.State.Checked(it)
                    in mix -> CheckboxState.TriState.Exclude(it)
                    else -> CheckboxState.State.None(it)
                }
            }
            presenter.dialog = AnimelibPresenter.Dialog.ChangeCategory(animeList, preselected)
        }
    }

    private fun runDownloadEpisodeAction(action: DownloadAction) {
        val mangas = presenter.selection.map { it.anime }.toList()
        when (action) {
            DownloadAction.NEXT_1_CHAPTER -> presenter.downloadUnseenEpisodes(mangas, 1)
            DownloadAction.NEXT_5_CHAPTERS -> presenter.downloadUnseenEpisodes(mangas, 5)
            DownloadAction.NEXT_10_CHAPTERS -> presenter.downloadUnseenEpisodes(mangas, 10)
            DownloadAction.UNREAD_CHAPTERS -> presenter.downloadUnseenEpisodes(mangas, null)
            DownloadAction.CUSTOM -> {
                presenter.dialog = AnimelibPresenter.Dialog.DownloadCustomAmount(
                    mangas,
                    presenter.selection.maxOf { it.unseenCount }.toInt(),
                )
                return
            }
            else -> {}
        }
        presenter.clearSelection()
    }

    private fun markSeenStatus(seen: Boolean) {
        val animeList = presenter.selection.toList()
        presenter.markSeenStatus(animeList.map { it.anime }, seen)
        presenter.clearSelection()
    }

    private fun showDeleteAnimeDialog() {
        val animeList = presenter.selection.map { it.anime }
        presenter.dialog = AnimelibPresenter.Dialog.DeleteAnime(animeList)
    }
}
