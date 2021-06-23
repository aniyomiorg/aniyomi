package eu.kanade.tachiyomi.ui.watcher

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import rx.Observable
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EpisodeLoader {
    companion object {
        fun getLinks(episode: Episode, anime: Anime, source: AnimeSource): Observable<List<Video>> {
            Timber.i("hello2")
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> {
                    return downloaded(episode, anime, source, downloadManager)
                        ?.map { listOf(it) } ?: Observable.just(emptyList())
                }
                source is AnimeHttpSource -> notDownloaded(episode, anime, source)
                    ?: Observable.just(emptyList())
                source is LocalAnimeSource -> {
                    return local(episode, source) ?: Observable.just(emptyList())
                }
                else -> error("source not supported")
            }
        }

        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode, anime, true)
        }

        fun getLink(episode: Episode, anime: Anime, source: AnimeSource): Observable<Video>? {
            Timber.i("hello2")
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> downloaded(episode, anime, source, downloadManager)
                source is AnimeHttpSource -> notDownloaded(episode, anime, source)?.map { it.first() }
                source is LocalAnimeSource -> local(episode, source)?.map { it.first() }
                else -> error("no worky")
            }
        }

        fun notDownloaded(episode: Episode, anime: Anime, source: AnimeSource): Observable<List<Video>>? {
            Timber.i("hello3")
            return try {
                source.fetchVideoList(episode)
            } catch (error: Exception) {
                null
            }
        }

        fun downloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager
        ): Observable<Video>? {
            return try {
                downloadManager.buildVideo(source, anime, episode)
            } catch (error: Exception) {
                null
            }
        }

        fun local(
            episode: Episode,
            source: LocalAnimeSource
        ): Observable<List<Video>>? {
            return try {
                source.fetchVideoList(episode)
            } catch (error: Exception) {
                null
            }
        }
    }
}
