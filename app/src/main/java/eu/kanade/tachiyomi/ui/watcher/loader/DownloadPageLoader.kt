package eu.kanade.tachiyomi.ui.watcher.loader

import android.app.Application
import android.net.Uri
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import eu.kanade.tachiyomi.ui.watcher.model.WatcherPage
import rx.Observable
import uy.kohesive.injekt.injectLazy

/**
 * Loader used to load a episode from the downloaded episodes.
 */
class DownloadPageLoader(
    private val episode: WatcherEpisode,
    private val anime: Anime,
    private val source: Source,
    private val downloadManager: AnimeDownloadManager
) : PageLoader() {

    // Needed to open input streams
    private val context: Application by injectLazy()

    /**
     * Returns an observable containing the pages found on this downloaded episode.
     */
    override fun getPages(): Observable<List<WatcherPage>> {
        return downloadManager.buildPageList(source, anime, episode.episode)
            .map { pages ->
                pages.map { page ->
                    WatcherPage(page.index, page.url, page.imageUrl) {
                        context.contentResolver.openInputStream(page.uri ?: Uri.EMPTY)!!
                    }.apply {
                        status = Page.READY
                    }
                }
            }
    }

    override fun getPage(page: WatcherPage): Observable<Int> {
        return Observable.just(Page.READY) // TODO maybe check if file still exists?
    }
}
