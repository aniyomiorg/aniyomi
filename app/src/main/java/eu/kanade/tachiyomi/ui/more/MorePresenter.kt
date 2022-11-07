package eu.kanade.tachiyomi.ui.more

import android.os.Bundle
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadService
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

    private var _stateAnime: MutableStateFlow<AnimeDownloadQueueState> = MutableStateFlow(AnimeDownloadQueueState.Stopped)
    val animeDownloadQueueState: StateFlow<AnimeDownloadQueueState> = _stateAnime.asStateFlow()

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Handle running/paused status change and queue progress updating
        presenterScope.launchIO {
            combine(
                AnimeDownloadService.isRunning,
                animedownloadManager.queue.updatedFlow(),
            ) { isRunning, downloadQueue -> Pair(isRunning, downloadQueue.size) }
                .collectLatest { (isDownloading, downloadQueueSize) ->
                    val pendingDownloadExists = downloadQueueSize != 0
                    _stateAnime.value = when {
                        !pendingDownloadExists -> AnimeDownloadQueueState.Stopped
                        !isDownloading && !pendingDownloadExists -> AnimeDownloadQueueState.Paused(0)
                        !isDownloading && pendingDownloadExists -> AnimeDownloadQueueState.Paused(
                            downloadQueueSize,
                        )
                        else -> AnimeDownloadQueueState.Downloading(downloadQueueSize)
                    }
                }
        }

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
                        !isDownloading && pendingDownloadExists -> DownloadQueueState.Paused(
                            downloadQueueSize,
                        )
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

sealed class AnimeDownloadQueueState {
    object Stopped : AnimeDownloadQueueState()
    data class Paused(val pending: Int) : AnimeDownloadQueueState()
    data class Downloading(val pending: Int) : AnimeDownloadQueueState()
}
