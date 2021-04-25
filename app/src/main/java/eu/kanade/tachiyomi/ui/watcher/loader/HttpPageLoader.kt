package eu.kanade.tachiyomi.ui.watcher.loader

import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import eu.kanade.tachiyomi.util.lang.plusAssign
import rx.Completable
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Loader used to load episodes from an online source.
 */
class HttpPageLoader(
    private val episode: WatcherEpisode,
    private val source: AnimeHttpSource,
    private val episodeCache: EpisodeCache = Injekt.get()
) : PageLoader() {

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue = PriorityBlockingQueue<PriorityPage>()

    /**
     * Current active subscriptions.
     */
    private val subscriptions = CompositeSubscription()

    private val preloadSize = 4

    init {
        subscriptions += Observable.defer { Observable.just(queue.take().page) }
            .filter { it.status == Page.QUEUE }
            .concatMap { source.fetchImageFromCacheThenNet(it) }
            .repeat()
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                },
                { error ->
                    if (error !is InterruptedException) {
                        Timber.e(error)
                    }
                }
            )
    }

    /**
     * Recycles this loader and the active subscriptions and queue.
     */
    override fun recycle() {
        super.recycle()
        subscriptions.unsubscribe()
        queue.clear()

        // Cache current page list progress for online episodes to allow a faster reopen
        val pages = episode.seconds
        if (pages != null) {
            Completable
                .fromAction {
                    // Convert to pages without watcher information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    episodeCache.putPageListToCache(episode.episode, pagesToSave)
                }
                .onErrorComplete()
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    /**
     * Returns an observable with the page list for a episode. It tries to return the page list from
     * the local cache, otherwise fallbacks to network.
     */
    override fun getPages(): Observable<List<WatcherPage>> {
        return episodeCache
            .getPageListFromCache(episode.episode)
            .onErrorResumeNext { source.fetchPageList(episode.episode) }
            .map { pages ->
                pages.mapIndexed { index, page ->
                    // Don't trust sources and use our own indexing
                    WatcherPage(index, page.url, page.imageUrl)
                }
            }
    }

    /**
     * Returns an observable that loads a page through the queue and listens to its result to
     * emit new states. It handles re-enqueueing pages if they were evicted from the cache.
     */
    override fun getPage(page: WatcherPage): Observable<Int> {
        return Observable.defer {
            val imageUrl = page.imageUrl

            // Check if the image has been deleted
            if (page.status == Page.READY && imageUrl != null && !episodeCache.isImageInCache(imageUrl)) {
                page.status = Page.QUEUE
            }

            // Automatically retry failed pages when subscribed to this page
            if (page.status == Page.ERROR) {
                page.status = Page.QUEUE
            }

            val statusSubject = SerializedSubject(PublishSubject.create<Int>())
            page.setStatusSubject(statusSubject)

            val queuedPages = mutableListOf<PriorityPage>()
            if (page.status == Page.QUEUE) {
                queuedPages += PriorityPage(page, 1).also { queue.offer(it) }
            }
            queuedPages += preloadNextPages(page, preloadSize)

            statusSubject.startWith(page.status)
                .doOnUnsubscribe {
                    queuedPages.forEach {
                        if (it.page.status == Page.QUEUE) {
                            queue.remove(it)
                        }
                    }
                }
        }
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: WatcherPage, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.episode.seconds ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == Page.QUEUE) {
                    PriorityPage(it, 0).apply { queue.offer(this) }
                } else null
            }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: WatcherPage) {
        if (page.status == Page.ERROR) {
            page.status = Page.QUEUE
        }
        queue.offer(PriorityPage(page, 2))
    }

    /**
     * Data class used to keep ordering of pages in order to maintain priority.
     */
    private class PriorityPage(
        val page: WatcherPage,
        val priority: Int
    ) : Comparable<PriorityPage> {
        companion object {
            private val idGenerator = AtomicInteger()
        }

        private val identifier = idGenerator.incrementAndGet()

        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)
            return if (p != 0) p else identifier.compareTo(other.identifier)
        }
    }

    /**
     * Returns an observable of the page with the downloaded image.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private fun AnimeHttpSource.fetchImageFromCacheThenNet(page: WatcherPage): Observable<WatcherPage> {
        return if (page.imageUrl.isNullOrEmpty()) {
            getImageUrl(page).flatMap { getCachedImage(it) }
        } else {
            getCachedImage(page)
        }
    }

    private fun AnimeHttpSource.getImageUrl(page: WatcherPage): Observable<WatcherPage> {
        page.status = Page.LOAD_PAGE
        return fetchImageUrl(page)
            .doOnError { page.status = Page.ERROR }
            .onErrorReturn { null }
            .doOnNext { page.imageUrl = it }
            .map { page }
    }

    /**
     * Returns an observable of the page that gets the image from the episode or fallbacks to
     * network and copies it to the cache calling [cacheImage].
     *
     * @param page the page.
     */
    private fun AnimeHttpSource.getCachedImage(page: WatcherPage): Observable<WatcherPage> {
        val imageUrl = page.imageUrl ?: return Observable.just(page)

        return Observable.just(page)
            .flatMap {
                if (!episodeCache.isImageInCache(imageUrl)) {
                    cacheImage(page)
                } else {
                    Observable.just(page)
                }
            }
            .doOnNext {
                page.stream = { episodeCache.getImageFile(imageUrl).inputStream() }
                page.status = Page.READY
            }
            .doOnError { page.status = Page.ERROR }
            .onErrorReturn { page }
    }

    /**
     * Returns an observable of the page that downloads the image to [EpisodeCache].
     *
     * @param page the page.
     */
    private fun AnimeHttpSource.cacheImage(page: WatcherPage): Observable<WatcherPage> {
        page.status = Page.DOWNLOAD_IMAGE
        return fetchImage(page)
            .doOnNext { episodeCache.putImageToCache(page.imageUrl!!, it) }
            .map { page }
    }
}
