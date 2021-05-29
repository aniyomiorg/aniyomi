package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import dev.chrisbanes.insetter.applyInsetter
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animelib.AnimelibUpdateService
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.databinding.AnimeUpdatesControllerBinding
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodesAdapter
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.watcher.WatcherActivity
import eu.kanade.tachiyomi.util.system.notificationManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.recyclerview.scrollStateChanges
import reactivecircus.flowbinding.swiperefreshlayout.refreshes
import timber.log.Timber
import uy.kohesive.injekt.api.get
import java.util.*

/**
 * Fragment that shows recent episodes.
 */
class AnimeUpdatesController :
    NucleusController<AnimeUpdatesControllerBinding, AnimeUpdatesPresenter>(),
    RootController,
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnUpdateListener,
    BaseEpisodesAdapter.OnEpisodeClickListener,
    ConfirmDeleteEpisodesDialog.Listener,
    AnimeUpdatesAdapter.OnCoverClickListener {

    /**
     * Action mode for multiple selection.
     */
    private var actionMode: ActionMode? = null

    /**
     * Adapter containing the recent episodes.
     */
    var adapter: AnimeUpdatesAdapter? = null
        private set

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_updates)
    }

    override fun createPresenter(): AnimeUpdatesPresenter {
        return AnimeUpdatesPresenter()
    }

    override fun createBinding(inflater: LayoutInflater) = AnimeUpdatesControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }
        binding.actionToolbar.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        view.context.notificationManager.cancel(Notifications.ID_NEW_EPISODES)

        // Init RecyclerView and adapter
        val layoutManager = LinearLayoutManager(view.context)
        binding.recycler.layoutManager = layoutManager
        binding.recycler.setHasFixedSize(true)
        adapter = AnimeUpdatesAdapter(this@AnimeUpdatesController, view.context)
        binding.recycler.adapter = adapter
        adapter?.fastScroller = binding.fastScroller

        binding.recycler.scrollStateChanges()
            .onEach {
                // Disable swipe refresh when view is not at the top
                val firstPos = layoutManager.findFirstCompletelyVisibleItemPosition()
                binding.swipeRefresh.isEnabled = firstPos <= 0
            }
            .launchIn(viewScope)

        binding.swipeRefresh.setDistanceToTriggerSync((2 * 64 * view.resources.displayMetrics.density).toInt())
        binding.swipeRefresh.refreshes()
            .onEach {
                updateLibrary()

                // It can be a very long operation, so we disable swipe refresh and show a toast.
                binding.swipeRefresh.isRefreshing = false
            }
            .launchIn(viewScope)

        (activity as? MainActivity)?.fixViewToBottom(binding.actionToolbar)
    }

    override fun onDestroyView(view: View) {
        destroyActionModeIfNeeded()
        (activity as? MainActivity)?.clearFixViewToBottom(binding.actionToolbar)
        binding.actionToolbar.destroy()
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
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            binding.actionToolbar.show(
                actionMode!!,
                R.menu.updates_episode_selection
            ) { onActionItemClicked(it!!) }
            (activity as? MainActivity)?.showBottomNav(visible = false, collapse = true)
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
     * Open episode in reader
     * @param episode selected episode
     */
    private fun openEpisode(item: AnimeUpdatesItem) {
        val activity = activity ?: return
        val episodeList = ArrayList<Episode>()
        val intent = WatcherActivity.newIntent(activity, item.anime, item.episode, episodeList)
        startActivity(intent)
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
     * Populate adapter with episodes
     * @param episodes list of [Any]
     */
    fun onNextRecentEpisodes(episodes: List<IFlexible<*>>) {
        destroyActionModeIfNeeded()
        adapter?.updateDataSet(episodes)
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
     * @param download [Download] object containing download progress.
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
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            deleteEpisodes(episodes)
        }
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
        router.pushController(AnimeController(episode.anime).withFadeTransaction())
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
        Timber.e(error)
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

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = adapter?.selectedItemCount ?: 0
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = count.toString()

            val episodes = getSelectedEpisodes()
            binding.actionToolbar.findItem(R.id.action_download)?.isVisible = episodes.any { !it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_delete)?.isVisible = episodes.any { it.isDownloaded }
            binding.actionToolbar.findItem(R.id.action_mark_as_seen)?.isVisible = episodes.any { !it.episode.seen }
            binding.actionToolbar.findItem(R.id.action_mark_as_unseen)?.isVisible = episodes.all { it.episode.seen }
        }

        return false
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
            R.id.action_mark_as_read -> markAsRead(getSelectedEpisodes())
            R.id.action_mark_as_unread -> markAsUnread(getSelectedEpisodes())
            else -> return false
        }
        return true
    }

    /**
     * Called when ActionMode destroyed
     * @param mode the ActionMode object
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        adapter?.mode = SelectableAdapter.Mode.IDLE
        adapter?.clearSelection()

        binding.actionToolbar.hide()
        (activity as? MainActivity)?.showBottomNav(visible = true, collapse = true)

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
