package eu.kanade.tachiyomi.ui.recent.animehistory

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.domain.episode.model.Episode
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.presentation.animehistory.AnimeHistoryScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.anime.episode.EpisodeItem
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
    private val sourceManager: AnimeSourceManager by injectLazy()

    override fun getTitle() = resources?.getString(R.string.label_recent_manga)

    override fun createPresenter() = AnimeHistoryPresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        AnimeHistoryScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickCover = { history ->
                parentController!!.router.pushController(AnimeController(history))
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

    fun openEpisode(episode: Episode?) {
        val activity = activity ?: return
        if (episode != null) {
            val intent = PlayerActivity.newIntent(activity, episode.animeId, episode.id)

            if (preferences.alwaysUseExternalPlayer()) launchIO {
                val db: AnimeDatabaseHelper by injectLazy()
                val anime = episode.animeId?.let { db.getAnime(it).executeAsBlocking() } ?: return@launchIO
                val dbEpisode = episode.toDbEpisode()
                val video = try {
                    EpisodeLoader.getLink(dbEpisode, anime, source = sourceManager.getOrStub(anime.source)).awaitSingle()
                } catch (e: Exception) {
                    return@launchIO makeErrorToast(activity, e)
                }
                val downloadManager: AnimeDownloadManager = Injekt.get()
                val isDownloaded = downloadManager.isEpisodeDownloaded(dbEpisode, anime, true)
                if (video != null) {
                    AnimeController.EXT_EPISODE = dbEpisode
                    AnimeController.EXT_ANIME = anime

                    val source = sourceManager.getOrStub(anime.source)
                    val extIntent = ExternalIntents(anime, source).getExternalIntent(dbEpisode, video, isDownloaded, activity)
                    if (extIntent != null) try {
                        startActivityForResult(extIntent, AnimeController.REQUEST_EXTERNAL)
                    } catch (e: Exception) {
                        makeErrorToast(activity, e)
                    }
                } else {
                    makeErrorToast(activity, Exception("Couldn't find any video links."))
                }
            } else {
                startActivity(intent)
            }
        } else {
            activity.toast(R.string.no_next_episode)
        }
    }

    private fun makeErrorToast(context: Context, e: Exception?) {
        launchUI { context.toast(e?.message ?: "Cannot open episode") }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AnimeController.REQUEST_EXTERNAL && resultCode == Activity.RESULT_OK) {
            val anime = AnimeController.EXT_ANIME ?: return
            val currentExtEpisode = AnimeController.EXT_EPISODE ?: return
            val currentPosition: Long
            val duration: Long
            val cause = data!!.getStringExtra("end_by") ?: ""
            if (cause.isNotEmpty()) {
                val positionExtra = data.extras?.get("position")
                currentPosition = if (positionExtra is Int) {
                    positionExtra.toLong()
                } else {
                    positionExtra as? Long ?: 0L
                }
                val durationExtra = data.extras?.get("duration")
                duration = if (durationExtra is Int) {
                    durationExtra.toLong()
                } else {
                    durationExtra as? Long ?: 0L
                }
            } else {
                if (data.extras?.get("extra_position") != null) {
                    currentPosition = data.getLongExtra("extra_position", 0L)
                    duration = data.getLongExtra("extra_duration", 0L)
                } else {
                    currentPosition = data.getIntExtra("position", 0).toLong()
                    duration = data.getIntExtra("duration", 0).toLong()
                }
            }
            if (cause == "playback_completion") {
                AnimeController.setEpisodeProgress(currentExtEpisode, anime, currentExtEpisode.total_seconds, currentExtEpisode.total_seconds)
            } else {
                AnimeController.setEpisodeProgress(currentExtEpisode, anime, currentPosition, duration)
            }
            launchIO {
                AnimeController.saveEpisodeHistory(EpisodeItem(currentExtEpisode, anime))
            }
        }
    }

    fun resumeLastEpisodeSeen() {
        presenter.resumeLastEpisodeSeen()
    }
}
