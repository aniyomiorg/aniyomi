package eu.kanade.tachiyomi.ui.recent.animehistory

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.domain.anime.interactor.GetAnimeById
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.presentation.animehistory.AnimeHistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.appcompat.queryTextChanges
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class AnimeHistoryController : ComposeController<AnimeHistoryPresenter>(), RootController {

    private var query = ""

    private val preferences: PreferencesHelper = Injekt.get()
    private val getAnimeById: GetAnimeById = Injekt.get()
    private val sourceManager: AnimeSourceManager by injectLazy()

    override fun getTitle() = resources?.getString(R.string.label_recent_manga)

    override fun createPresenter() = AnimeHistoryPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeHistoryScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickCover = { history ->
                parentController!!.router.pushController(AnimeController(history.animeId))
            },
            onClickResume = { history ->
                presenter.getNextEpisodeForAnime(history.animeId, history.episodeId)
            },
            onClickDelete = { history, all ->
                if (all) {
                    // Reset last read of episode to 0L
                    presenter.removeAllFromHistory(history.animeId)
                } else {
                    // Remove all episodes belonging to anime from library
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

    suspend fun openEpisode(episode: Episode) {
        val activity = activity ?: return
        val anime = getAnimeById.await(episode.animeId) ?: return
        val useExternal = preferences.alwaysUseExternalPlayer()
        if (useExternal) {
            openEpisodeExternal(episode, anime)
        } else {
            val intent = PlayerActivity.newIntent(activity, anime.id, episode.id)
            startActivity(intent)
        }
    }

    private fun openEpisodeExternal(episode: Episode, anime: Anime) {
        val context = activity ?: return
        val source = sourceManager.get(anime.source) ?: return
        val dbEpisode = episode.toDbEpisode()
        val dbAnime = anime.toDbAnime()
        launchIO {
            val video = try {
                EpisodeLoader.getLink(dbEpisode, dbAnime, source).awaitSingle()
            } catch (e: Exception) {
                launchUI { context.toast(e.message) }
                return@launchIO
            }
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(dbEpisode, dbAnime, true)
            if (video != null) {
                AnimeController.EXT_EPISODE = episode
                AnimeController.EXT_ANIME = anime

                val extIntent = ExternalIntents(anime, source).getExternalIntent(episode, video, isDownloaded, context)
                if (extIntent != null) try {
                    startActivityForResult(extIntent, AnimeController.REQUEST_EXTERNAL)
                } catch (e: Exception) {
                    launchUI { context.toast(e.message) }
                    return@launchIO
                }
            } else {
                launchUI { context.toast("Couldn't find any video links.") }
                return@launchIO
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ExternalIntents.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun resumeLastEpisodeSeen() {
        presenter.resumeLastEpisodeSeen()
    }
}
