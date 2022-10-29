package eu.kanade.tachiyomi.ui.more

import android.os.Bundle
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MorePresenter(
    private val downloadManager: DownloadManager = Injekt.get(),
    private val animedownloadManager: AnimeDownloadManager = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
) : BasePresenter<MoreController>() {

    val downloadedOnly = preferences.downloadedOnly().asState()
    val incognitoMode = preferences.incognitoMode().asState()

    private var _state: MutableStateFlow<DownloadQueueState> = MutableStateFlow(DownloadQueueState.Stopped)
    val downloadQueueState: StateFlow<DownloadQueueState> = _state.asStateFlow()

    private var isDownloading: Boolean = false
    private var isDownloadingAnime: Boolean = false
    private var isDownloadingManga: Boolean = false
    private var downloadQueueSize: Int = 0
    private var downloadQueueSizeAnime: Int = 0
    private var downloadQueueSizeManga: Int = 0
    private var untilDestroySubscriptions = CompositeSubscription()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }

        initDownloadQueueSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        untilDestroySubscriptions.unsubscribe()
    }

    private fun initDownloadQueueSummary() {
        // Handle running/paused status change
        DownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { isRunning ->
                isDownloadingManga = isRunning
                isDownloading = isDownloadingManga || isDownloadingAnime
                updateDownloadQueueState()
            }

        AnimeDownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { isRunning ->
                isDownloadingAnime = isRunning
                isDownloading = isDownloadingManga || isDownloadingAnime
                updateDownloadQueueState()
            }

        // Handle queue progress updating
        downloadManager.queue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy {
                downloadQueueSizeManga = it.size
                downloadQueueSize = downloadQueueSizeManga + downloadQueueSizeAnime
                updateDownloadQueueState()
            }

        animedownloadManager.queue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy {
                downloadQueueSizeAnime = it.size
                downloadQueueSize = downloadQueueSizeManga + downloadQueueSizeAnime
                updateDownloadQueueState()
            }
    }

    private fun updateDownloadQueueState() {
        presenterScope.launchIO {
            combine(
                DownloadService.isRunning,
                downloadManager.queue.updatedFlow(),
            ) { isRunning, downloadQueue -> Pair(isRunning, downloadQueue.size) }
                .collectLatest { (isDownloading, downloadQueueSize) ->
                    val pendingDownloadExists = downloadQueueSize != 0
                    _state.value = when {
                        !pendingDownloadExists -> DownloadQueueState.Stopped
                        !isDownloading && !pendingDownloadExists -> DownloadQueueState.Paused(0)
                        !isDownloading && pendingDownloadExists -> DownloadQueueState.Paused(downloadQueueSize)
                        else -> DownloadQueueState.Downloading(downloadQueueSize)
                    }
                }
        }
    }
}

sealed class DownloadQueueState {
    object Stopped : DownloadQueueState()
    data class Paused(val pending: Int) : DownloadQueueState()
    data class Downloading(val pending: Int) : DownloadQueueState()
}
