package eu.kanade.tachiyomi.ui.watcher.loader

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.source.AnimeSource
import eu.kanade.tachiyomi.source.LocalAnimeSource
import eu.kanade.tachiyomi.source.online.AnimeHttpSource
import eu.kanade.tachiyomi.ui.watcher.model.WatcherEpisode
import rx.Completable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber

/**
 * Loader used to retrieve the [PageLoader] for a given episode.
 */
class EpisodeLoader(
    private val context: Context,
    private val downloadManager: AnimeDownloadManager,
    private val anime: Anime,
    private val source: AnimeSource
) {

    /**
     * Returns a completable that assigns the page loader and loads the its pages. It just
     * completes if the episode is already loaded.
     */
    fun loadEpisode(episode: WatcherEpisode): Completable {
        if (episodeIsReady(episode)) {
            return Completable.complete()
        }

        return Observable.just(episode)
            .doOnNext { episode.state = WatcherEpisode.State.Loading }
            .observeOn(Schedulers.io())
            .flatMap { watcherEpisode ->
                Timber.d("Loading pages for ${episode.episode.name}")

                val loader = getPageLoader(watcherEpisode)
                episode.pageLoader = loader

                loader.getPages().take(1).doOnNext { pages ->
                    pages.forEach { it.episode = episode }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { pages ->
                if (pages.isEmpty()) {
                    throw Exception(context.getString(R.string.page_list_empty_error))
                }

                episode.state = WatcherEpisode.State.Loaded(pages)

                // If the episode is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
                if (!episode.episode.read) {
                    episode.requestedPage = episode.episode.last_page_read
                }
            }
            .toCompletable()
            .doOnError { episode.state = WatcherEpisode.State.Error(it) }
    }

    /**
     * Checks [episode] to be loaded based on present pages and loader in addition to state.
     */
    private fun episodeIsReady(episode: WatcherEpisode): Boolean {
        return episode.state is WatcherEpisode.State.Loaded && episode.pageLoader != null
    }

    /**
     * Returns the page loader to use for this [episode].
     */
    private fun getPageLoader(episode: WatcherEpisode): PageLoader {
        val isDownloaded = downloadManager.isEpisodeDownloaded(episode.episode, anime, true)
        return when {
            isDownloaded -> DownloadPageLoader(episode, anime, source, downloadManager)
            source is AnimeHttpSource -> HttpPageLoader(episode, source)
            source is LocalAnimeSource -> source.getFormat(episode.episode).let { format ->
                when (format) {
                    is LocalAnimeSource.Format.Directory -> DirectoryPageLoader(format.file)
                    is LocalAnimeSource.Format.Zip -> ZipPageLoader(format.file)
                    is LocalAnimeSource.Format.Rar -> RarPageLoader(format.file)
                    is LocalAnimeSource.Format.Anime -> EpubPageLoader(format.file)
                }
            }
            else -> error(context.getString(R.string.loader_not_implemented_error))
        }
    }
}
