package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.content.Intent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.UpdatesControllerBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.player.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.PlayerActivity
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.onAnimationsFinished
import eu.kanade.tachiyomi.widget.ActionModeWithToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.LogPriority
import reactivecircus.flowbinding.recyclerview.scrollStateChanges
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import uy.kohesive.injekt.injectLazy

/**
 * Fragment that shows recent episodes.
 */
class AnimeUpdatesController :
    NucleusController<UpdatesControllerBinding, AnimeUpdatesPresenter>(),
    RootController,
    ActionModeWithToolbar.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnUpdateListener,
    BaseEpisodesAdapter.OnEpisodeClickListener,
    ConfirmDeleteEpisodesDialog.Listener,
    AnimeUpdatesAdapter.OnCoverClickListener {

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionModeWithToolbar? = null

    /**
     * Adapter containing the recent episodes.
     */
    var adapter: AnimeUpdatesAdapter? = null
        private set

    private val preferences: PreferencesHelper by injectLazy()
    private val sourceManager: AnimeSourceManager by injectLazy()

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_updates)
    }

    override fun createPresenter(): AnimeUpdatesPresenter {
        return AnimeUpdatesPresenter()
    }

    override fun createBinding(inflater: LayoutInflater) = UpdatesControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        view.context.notificationManager.cancel(Notifications.ID_NEW_EPISODES)

        // Init RecyclerView and adapter
        val layoutManager = LinearLayoutManager(view.context)
        binding.recycler.layoutManager = layoutManager
        binding.recycler.setHasFixedSize(true)

        binding.recycler.scrollStateChanges()
            .onEach {
                // Disable swipe refresh when view is not at the top
                val firstPos = layoutManager.findFirstCompletelyVisibleItemPosition()
                binding.swipeRefresh.isEnabled = firstPos <= 0
            }
            .launchIn(viewScope)

        binding.swipeRefresh.isRefreshing = true
        binding.swipeRefresh.setDistanceToTriggerSync((2 * 64 * view.resources.displayMetrics.density).toInt())
        binding.swipeRefresh.refreshes()
            .onEach {
                updateLibrary()

                // It can be a very long operation, so we disable swipe refresh and show a toast.
                binding.swipeRefresh.isRefreshing = false
            }
            .launchIn(viewScope)

        viewScope.launch {
            presenter.updates.collectLatest { updatesItems ->
                destroyActionModeIfNeeded()
                if (adapter == null) {
                    adapter = AnimeUpdatesAdapter(this@AnimeUpdatesController, binding.recycler.context, updatesItems)
                    binding.recycler.adapter = adapter
                    adapter!!.fastScroller = binding.fastScroller
                } else {
                    adapter?.updateDataSet(updatesItems)
                }
                binding.swipeRefresh.isRefreshing = false
                binding.fastScroller.isVisible = true
                binding.recycler.onAnimationsFinished {
                    (activity as? MainActivity)?.ready = true
                }
            }
        }
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        adapter = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.updates, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_update_library -> updateLibrary()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateLibrary() {
        activity?.let {
            if (AnimelibUpdateService.start(it)) {
                it.toast(R.string.updating_library)
            }
        }
    }

    /**
     * Returns selected episodes
     * @return list of selected episodes
     */
    private fun getSelectedEpisodes(): List<AnimeUpdatesItem> {
        val adapter = adapter ?: return emptyList()
        return adapter.selectedPositions.mapNotNull { adapter.getItem(it) as? AnimeUpdatesItem }
    }

    /**
     * Called when item in list is clicked
     * @param position position of clicked item
     */
    override fun onItemClick(view: View, position: Int): Boolean {
        val adapter = adapter ?: return false

        // Get item from position
        val item = adapter.getItem(position) as? AnimeUpdatesItem ?: return false
        return if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleSelection(position)
            true
        } else {
            openEpisode(item)
            false
        }
    }

    /**
     * Called when item in list is long clicked
     * @param position position of clicked item
     */
    override fun onItemLongClick(position: Int) {
        val activity = activity
        if (actionMode == null && activity is MainActivity) {
            actionMode = activity.startActionModeAndToolbar(this)
            activity.showBottomNav(false)
        }
        toggleSelection(position)
    }

    /**
     * Called to toggle selection
     * @param position position of selected item
     */
    private fun toggleSelection(position: Int) {
        val adapter = adapter ?: return
        adapter.toggleSelection(position)
        actionMode?.invalidate()
    }

    /**
     * Open episode in player
     * @param item selected episode
     */
    private fun openEpisode(item: AnimeUpdatesItem, hasAnimation: Boolean = false, playerChangeRequested: Boolean = false) {
        val activity = activity ?: return
        val useExternal = preferences.alwaysUseExternalPlayer() != playerChangeRequested
        if (useExternal) {
            openEpisodeExternal(item)
        } else {
            val intent = PlayerActivity.newIntent(activity, item.anime.id, item.episode.id)
            if (hasAnimation) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
        }
    }

    private fun openEpisodeExternal(item: AnimeUpdatesItem) {
        val context = activity ?: return
        val anime = item.anime
        val domainAnime = item.anime
        val episode = item.episode
        val source = sourceManager.get(item.anime.source) ?: return
        launchIO {
            val video = try {
                EpisodeLoader.getLink(episode.toDbEpisode(), anime.toDbAnime(), source).awaitSingle()
            } catch (e: Exception) {
                launchUI { context.toast(e.message) }
                return@launchIO
            }
            if (video != null) {
                AnimeController.EXT_EPISODE = episode
                AnimeController.EXT_ANIME = domainAnime

                val extIntent = ExternalIntents(domainAnime, source).getExternalIntent(
                    episode,
                    video,
                    context,
                )
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

    /**
     * Download selected items
     * @param episodes list of selected [AnimeUpdatesItem]s
     */
    private fun downloadEpisodes(episodes: List<AnimeUpdatesItem>) {
        presenter.downloadEpisodes(episodes)
        destroyActionModeIfNeeded()
    }

    /**
     * Download selected items
     * @param episodes list of selected [AnimeUpdatesItem]s
     */
    private fun downloadEpisodesExternally(episodes: List<AnimeUpdatesItem>) {
        presenter.downloadEpisodesExternally(episodes)
        destroyActionModeIfNeeded()
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(R.string.information_no_recent)
        }
    }

    /**
     * Update download status of episode
     * @param download [AnimeDownload] object containing download progress.
     */
    fun onEpisodeDownloadUpdate(download: AnimeDownload) {
        adapter?.currentItems
            ?.filterIsInstance<AnimeUpdatesItem>()
            ?.find { it.episode.id == download.episode.id }?.let {
                adapter?.updateItem(it, it.status)
            }
    }

    /**
     * Mark episode as read
     * @param episodes list of episodes
     */
    private fun markAsRead(episodes: List<AnimeUpdatesItem>) {
        presenter.markEpisodeRead(episodes, true)
        destroyActionModeIfNeeded()
    }

    /**
     * Mark episode as unread
     * @param episodes list of selected [AnimeUpdatesItem]
     */
    private fun markAsUnread(episodes: List<AnimeUpdatesItem>) {
        presenter.markEpisodeRead(episodes, false)
        destroyActionModeIfNeeded()
    }

    override fun deleteEpisodes(episodesToDelete: List<AnimeUpdatesItem>) {
        presenter.deleteEpisodes(episodesToDelete)
        destroyActionModeIfNeeded()
    }

    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCoverClick(position: Int) {
        destroyActionModeIfNeeded()

        val episodeClicked = adapter?.getItem(position) as? AnimeUpdatesItem ?: return
        openAnime(episodeClicked)
    }

    private fun openAnime(episode: AnimeUpdatesItem) {
        parentController!!.router.pushController(AnimeController(episode.anime.id))
    }

    /**
     * Called when episodes are deleted
     */
    fun onEpisodesDeleted() {
        adapter?.notifyDataSetChanged()
    }

    /**
     * Called when error while deleting
     * @param error error message
     */
    fun onEpisodesDeletedError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
    }

    override fun downloadEpisode(position: Int) {
        val item = adapter?.getItem(position) as? AnimeUpdatesItem ?: return
        if (item.status == AnimeDownload.State.ERROR) {
            AnimeDownloadService.start(activity!!)
        } else {
            downloadEpisodes(listOf(item))
        }
        adapter?.updateItem(item)
    }

    override fun downloadEpisodeExternally(position: Int) {
        val item = adapter?.getItem(position) as? AnimeUpdatesItem ?: return
        if (item.status == AnimeDownload.State.ERROR) {
            AnimeDownloadService.start(activity!!)
        } else {
            downloadEpisodesExternally(listOf(item))
        }
        adapter?.updateItem(item)
    }

    override fun startDownloadNow(position: Int) {
        val item = adapter?.getItem(position) as? AnimeUpdatesItem ?: return
        presenter.startDownloadingNow(item.episode)
    }

    private fun bookmarkEpisodes(episodes: List<AnimeUpdatesItem>, bookmarked: Boolean) {
        presenter.bookmarkEpisodes(episodes, bookmarked)
        destroyActionModeIfNeeded()
    }

    override fun deleteEpisode(position: Int) {
        val item = adapter?.getItem(position) as? AnimeUpdatesItem ?: return
        deleteEpisodes(listOf(item))
        adapter?.updateItem(item)
    }

    /**
     * Called when ActionMode created.
     * @param mode the ActionMode object
     * @param menu menu object of ActionMode
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.generic_selection, menu)
        adapter?.mode = SelectableAdapter.Mode.MULTI
        return true
    }

    override fun onCreateActionToolbar(menuInflater: MenuInflater, menu: Menu) {
        menuInflater.inflate(R.menu.updates_episode_selection, menu)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()
        }
        return true
    }

    override fun onPrepareActionToolbar(toolbar: ActionModeWithToolbar, menu: Menu) {
        val episodes = getSelectedEpisodes()
        if (episodes.isEmpty()) return
        toolbar.findToolbarItem(R.id.action_download)?.isVisible = episodes.any { !it.isDownloaded }
        toolbar.findToolbarItem(R.id.action_delete)?.isVisible = episodes.any { it.isDownloaded }
        toolbar.findToolbarItem(R.id.action_bookmark)?.isVisible = episodes.any { !it.episode.bookmark }
        toolbar.findToolbarItem(R.id.action_remove_bookmark)?.isVisible = episodes.all { it.episode.bookmark }
        toolbar.findToolbarItem(R.id.action_mark_as_seen)?.isVisible = episodes.any { !it.episode.seen }
        toolbar.findToolbarItem(R.id.action_mark_as_unseen)?.isVisible = episodes.all { it.episode.seen }
        toolbar.findToolbarItem(R.id.action_play_externally)?.isVisible = !preferences.alwaysUseExternalPlayer() && episodes.size == 1
        toolbar.findToolbarItem(R.id.action_play_internally)?.isVisible = preferences.alwaysUseExternalPlayer() && episodes.size == 1
    }

    /**
     * Called when ActionMode item clicked
     * @param mode the ActionMode object
     * @param item item from ActionMode.
     */
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return onActionItemClicked(item)
    }

    private fun onActionItemClicked(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select_all -> selectAll()
            R.id.action_select_inverse -> selectInverse()
            R.id.action_download -> downloadEpisodes(getSelectedEpisodes())
            R.id.action_delete ->
                ConfirmDeleteEpisodesDialog(this, getSelectedEpisodes())
                    .showDialog(router)
            R.id.action_bookmark -> bookmarkEpisodes(getSelectedEpisodes(), true)
            R.id.action_remove_bookmark -> bookmarkEpisodes(getSelectedEpisodes(), false)
            R.id.action_mark_as_seen -> markAsRead(getSelectedEpisodes())
            R.id.action_mark_as_unseen -> markAsUnread(getSelectedEpisodes())
            R.id.action_play_internally -> openEpisode(getSelectedEpisodes().last(), playerChangeRequested = true)
            R.id.action_play_externally -> openEpisode(getSelectedEpisodes().last(), playerChangeRequested = true)
            else -> return false
        }
        return true
    }

    /**
     * Called when ActionMode destroyed
     * @param mode the ActionMode object
     */
    override fun onDestroyActionMode(mode: ActionMode) {
        adapter?.mode = SelectableAdapter.Mode.IDLE
        adapter?.clearSelection()

        (activity as? MainActivity)?.showBottomNav(true)

        actionMode = null
    }

    private fun selectAll() {
        val adapter = adapter ?: return
        adapter.selectAll()
        actionMode?.invalidate()
    }

    private fun selectInverse() {
        val adapter = adapter ?: return
        for (i in 0..adapter.itemCount) {
            adapter.toggleSelection(i)
        }
        actionMode?.invalidate()
        adapter.notifyDataSetChanged()
    }
}
