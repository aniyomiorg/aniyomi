package eu.kanade.tachiyomi.ui.download.anime

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.databinding.DownloadControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.FabController
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.util.view.shrinkOnScroll
import rx.android.schedulers.AndroidSchedulers

/**
 * Controller that shows the currently active downloads.
 * Uses R.layout.fragment_download_queue.
 */
class DownloadController :
    NucleusController<DownloadControllerBinding, DownloadPresenter>(),
    FabController,
    DownloadAdapter.DownloadItemListener {

    /**
     * Adapter containing the active downloads.
     */
    private var adapter: DownloadAdapter? = null
    private var actionFab: ExtendedFloatingActionButton? = null
    private var actionFabScrollListener: RecyclerView.OnScrollListener? = null
    private var isTabSelected: Boolean = true

    /**
     * Whether the download queue is running or not.
     */
    private var isRunning: Boolean = false

    init {
        setHasOptionsMenu(true)
    }

    override fun createBinding(inflater: LayoutInflater) = DownloadControllerBinding.inflate(inflater)

    override fun createPresenter(): DownloadPresenter {
        return DownloadPresenter()
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_download_queue)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.recycler.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        // Check if download queue is empty and update information accordingly.
        setInformationView()

        // Initialize adapter.
        adapter = DownloadAdapter(this@DownloadController)
        binding.recycler.adapter = adapter
        adapter?.isHandleDragEnabled = true
        adapter?.fastScroller = binding.fastScroller

        // Set the layout manager for the recycler and fixed size.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)

        actionFabScrollListener = actionFab?.shrinkOnScroll(binding.recycler)

        // Subscribe to changes
        AnimeDownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onQueueStatusChange(it) }

        presenter.getDownloadStatusObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onStatusChange(it) }

        presenter.getDownloadProgressObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onUpdateDownloadedPages(it) }

        presenter.getDownloadPreciseProgressObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { onUpdateProgress(it) }

        presenter.downloadQueue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy {
                updateTitle(it.size)
            }
    }

    override fun configureFab(fab: ExtendedFloatingActionButton) {
        actionFab = fab
        fab.setOnClickListener {
            val context = applicationContext ?: return@setOnClickListener

            if (isRunning) {
                AnimeDownloadService.stop(context)
                presenter.pauseDownloads()
            } else {
                AnimeDownloadService.start(context)
            }

            setInformationView()
        }
    }

    override fun cleanupFab(fab: ExtendedFloatingActionButton) {
        fab.setOnClickListener(null)
        actionFabScrollListener?.let { binding.recycler.removeOnScrollListener(it) }
        actionFab = null
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_queue, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.clear_queue).isVisible = !presenter.downloadQueue.isEmpty()
        menu.findItem(R.id.reorder).isVisible = !presenter.downloadQueue.isEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context = applicationContext ?: return false
        when (item.itemId) {
            R.id.clear_queue -> {
                AnimeDownloadService.stop(context)
                presenter.clearQueue()
            }
            R.id.newest, R.id.oldest -> {
                reorderQueue({ it.download.episode.dateUpload }, item.itemId == R.id.newest)
            }
            R.id.asc, R.id.desc -> {
                reorderQueue({ it.download.episode.episodeNumber }, item.itemId == R.id.desc)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun <R : Comparable<R>> reorderQueue(selector: (DownloadItem) -> R, reverse: Boolean = false) {
        val adapter = adapter ?: return
        val newDownloads = mutableListOf<AnimeDownload>()
        adapter.headerItems.forEach { headerItem ->
            headerItem as DownloadHeaderItem
            headerItem.subItems = headerItem.subItems.sortedBy(selector).toMutableList().apply {
                if (reverse) {
                    reverse()
                }
            }
            newDownloads.addAll(headerItem.subItems.map { it.download })
        }
        presenter.reorder(newDownloads)
    }

    /**
     * Called when the status of a download changes.
     *
     * @param download the download whose status has changed.
     */
    private fun onStatusChange(download: AnimeDownload) {
        when (download.status) {
            AnimeDownload.State.DOWNLOADING -> {
                // Initial update of the downloaded pages
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            AnimeDownload.State.DOWNLOADED -> {
                onUpdateProgress(download)
                onUpdateDownloadedPages(download)
            }
            else -> { /* unused */ }
        }
    }

    /**
     * Called when the queue's status has changed. Updates the visibility of the buttons.
     *
     * @param running whether the queue is now running or not.
     */
    private fun onQueueStatusChange(running: Boolean) {
        isRunning = running
        activity?.invalidateOptionsMenu()

        // Check if download queue is empty and update information accordingly.
        setInformationView()
    }

    /**
     * Called from the presenter to assign the downloads for the adapter.
     *
     * @param downloads the downloads from the queue.
     */
    fun onNextDownloads(downloads: List<DownloadHeaderItem>) {
        activity?.invalidateOptionsMenu()
        setInformationView()
        adapter?.updateDataSet(downloads)
    }

    /**
     * Called when the progress of a download changes.
     *
     * @param download the download whose progress has changed.
     */
    private fun onUpdateProgress(download: AnimeDownload) {
        getHolder(download)?.notifyProgress()
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    private fun onUpdateDownloadedPages(download: AnimeDownload) {
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Returns the holder for the given download.
     *
     * @param download the download to find.
     * @return the holder of the download or null if it's not bound.
     */
    private fun getHolder(download: AnimeDownload): DownloadHolder? {
        return binding.recycler.findViewHolderForItemId(download.episode.id!!) as? DownloadHolder
    }

    /**
     * Set information view when queue is empty
     */
    fun setInformationView() {
        if (presenter.downloadQueue.isEmpty()) {
            binding.emptyView.show(R.string.information_no_downloads)
            actionFab?.isVisible = false
            updateTitle()
        } else {
            if (view == null) return
            binding.emptyView.hide()
            actionFab?.apply {
                isVisible = true

                setText(
                    if (isRunning) {
                        R.string.action_pause
                    } else {
                        R.string.action_resume
                    },
                )

                setIconResource(
                    if (isRunning) {
                        R.drawable.ic_pause_24dp
                    } else {
                        R.drawable.ic_play_arrow_24dp
                    },
                )
            }
        }
        updateTitle(presenter.downloadQueue.size)
    }

    /**
     * Called when an item is released from a drag.
     *
     * @param position The position of the released item.
     */
    override fun onItemReleased(position: Int) {
        val adapter = adapter ?: return
        val downloads = adapter.headerItems.flatMap { header ->
            adapter.getSectionItems(header).map { item ->
                (item as DownloadItem).download
            }
        }
        presenter.reorder(downloads)
    }

    /**
     * Called when the menu item of a download is pressed
     *
     * @param position The position of the item
     * @param menuItem The menu Item pressed
     */
    override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
        val item = adapter?.getItem(position) ?: return
        if (item is DownloadItem) {
            when (menuItem.itemId) {
                R.id.move_to_top, R.id.move_to_bottom -> {
                    val headerItems = adapter?.headerItems ?: return
                    val newDownloads = mutableListOf<AnimeDownload>()
                    headerItems.forEach { headerItem ->
                        headerItem as DownloadHeaderItem
                        if (headerItem == item.header) {
                            headerItem.removeSubItem(item)
                            if (menuItem.itemId == R.id.move_to_top) {
                                headerItem.addSubItem(0, item)
                            } else {
                                headerItem.addSubItem(item)
                            }
                        }
                        newDownloads.addAll(headerItem.subItems.map { it.download })
                    }
                    presenter.reorder(newDownloads)
                }
                R.id.cancel_download -> {
                    presenter.cancelDownload(item.download)
                }
                R.id.cancel_series -> {
                    val allDownloadsForSeries = adapter?.currentItems
                        ?.filterIsInstance<DownloadItem>()
                        ?.filter { item.download.anime.id == it.download.anime.id }
                        ?.map(DownloadItem::download)
                    if (!allDownloadsForSeries.isNullOrEmpty()) {
                        presenter.cancelDownloads(allDownloadsForSeries)
                    }
                }
            }
        }
    }

    private fun updateTitle(queueSize: Int = 0) {
        if (!isTabSelected) return
        val defaultTitle = getTitle()

        val controller = parentController as? BaseController<*> ?: this

        if (queueSize == 0) {
            controller.setTitle(defaultTitle)
        } else {
            controller.setTitle("$defaultTitle ($queueSize)")
        }
    }

    fun selectTab() {
        isTabSelected = true
    }

    fun unselectTab() {
        isTabSelected = false
    }
}
