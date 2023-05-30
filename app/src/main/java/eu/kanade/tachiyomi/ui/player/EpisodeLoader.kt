package eu.kanade.tachiyomi.ui.player

import android.net.Uri
import eu.kanade.domain.items.episode.model.toSEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.fetchUrlFromVideo
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import rx.Observable
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.lang.Exception

class EpisodeLoader {
    companion object {
        var errorMessage = ""

        fun getLinks(episode: Episode, anime: Anime, source: AnimeSource): Observable<List<Video>> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source, skipCache = true)
            return when {
                isDownloaded -> isDownloaded(episode, anime, source, downloadManager)
                source is AnimeHttpSource -> isHttp(episode, source)
                source is LocalAnimeSource -> isLocal(episode)
                else -> error("source not supported")
            }
        }

        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source, skipCache = true)
        }

        private fun isHttp(episode: Episode, source: AnimeHttpSource): Observable<List<Video>> {
            return source.fetchVideoList(episode.toSEpisode())
                .flatMapIterable { it }
                .flatMap {
                    source.fetchUrlFromVideo(it)
                }.toList()
        }

        private fun isDownloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager,
        ): Observable<List<Video>> {
            return downloadManager.buildVideo(source, anime, episode)
                .onErrorReturn { null }
                .map {
                    if (it == null) {
                        emptyList()
                    } else {
                        listOf(it)
                    }
                }
        }

        private fun isLocal(
            episode: Episode,
        ): Observable<List<Video>> {
            return try {
                logcat { episode.url }
                val video = Video(episode.url, "Local source: ${episode.url}", episode.url, Uri.parse(episode.url))
                Observable.just(listOf(video))
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error getting links"
                Observable.just(emptyList())
            }
        }
    }
}
