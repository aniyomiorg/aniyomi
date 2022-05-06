package eu.kanade.tachiyomi.ui.recent.animehistory

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.domain.episode.model.Episode
import eu.kanade.presentation.animehistory.AnimeHistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.queryTextChanges

class AnimeHistoryController : ComposeController<AnimeHistoryPresenter>(), RootController {

    private var query = ""

    override fun getTitle() = resources?.getString(R.string.label_recent_manga)

    override fun createPresenter() = AnimeHistoryPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeHistoryScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickCover = { history ->
                router.pushController(AnimeController(history))
            },
            onClickResume = { history ->
                presenter.getNextEpisodeForAnime(history.animeId, history.episodeId)
            },
            onClickDelete = { history, all ->
                if (all) {
                    // Reset last read of chapter to 0L
                    presenter.removeAllFromHistory(history.animeId)
                } else {
                    // Remove all chapters belonging to manga from library
                    presenter.removeFromHistory(history)
                }
            },
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.history, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }
        searchView.queryTextChanges()
            .filter { router.backstack.lastOrNull()?.controller == this }
            .onEach {
                query = it.toString()
                presenter.search(query)
            }
            .launchIn(viewScope)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_history -> {
                val dialog = ClearAnimeHistoryDialogController()
                dialog.targetController = this@AnimeHistoryController
                dialog.showDialog(router)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun openEpisode(episode: Episode?) {
        val activity = activity ?: return
        if (episode != null) {
            val intent = PlayerActivity.newIntent(activity, episode.animeId, episode.id)
            startActivity(intent)
        } else {
            activity.toast(R.string.no_next_chapter)
        }
    }
}
