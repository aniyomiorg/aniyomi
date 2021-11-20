package eu.kanade.tachiyomi.ui.player

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.fetchUrlFromVideo
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EpisodeLoader {
    companion object {
        var errorMessage = ""

        fun getLinks(episode: Episode, anime: Anime, source: AnimeSource): Observable<List<Video>> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> isDownloaded(episode, anime, source, downloadManager).map { it ?: emptyList() }
                source is AnimeHttpSource -> isHttp(episode, source)
                source is LocalAnimeSource -> source.fetchVideoList(episode)
                else -> error("source not supported")
            }
        }

        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode, anime, true)
        }

        fun getLink(episode: Episode, anime: Anime, source: AnimeSource): Observable<Video?> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> isDownloaded(episode, anime, source, downloadManager).map {
                    it?.first()
                }
                source is AnimeHttpSource -> isHttp(episode, source).map {
                    if (it.isEmpty()) error(errorMessage)
                    else it.first()
                }
                source is LocalAnimeSource -> isLocal(episode, source).map {
                    if (it.isEmpty()) error(errorMessage)
                    else it.first()
                }
                else -> error("source not supported")
            }
        }

        private fun isHttp(episode: Episode, source: AnimeHttpSource): Observable<List<Video>> {
            return source.fetchVideoList(episode)
                .onErrorReturn {
                    errorMessage = it.message ?: "error getting links"
                    emptyList()
                }
                .flatMapIterable { it }
                .flatMap {
                    source.fetchUrlFromVideo(it)
                }.toList()
        }

        fun isDownloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager
        ): Observable<List<Video>?> {
            return downloadManager.buildVideo(source, anime, episode)
                .onErrorReturn { null }
                .toList()
        }

        fun isLocal(
            episode: Episode,
            source: LocalAnimeSource
        ): Observable<List<Video>> {
            return source.fetchVideoList(episode)
                .onErrorReturn { emptyList() }
        }
    }
}
