package eu.kanade.tachiyomi.ui.player

import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.fetchUrlFromVideo
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.database.models.toDomainAnime
import eu.kanade.tachiyomi.util.system.logcat
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.lang.Exception

class EpisodeLoader {
    companion object {
        var errorMessage = ""

        fun getLinks(episode: Episode, anime: Anime, source: AnimeSource): Observable<List<Video>> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
            return when {
                isDownloaded -> isDownloaded(episode, anime, source, downloadManager)
                source is AnimeHttpSource -> isHttp(episode, source)
                source is LocalAnimeSource -> isLocal(episode)
                else -> error("source not supported")
            }
        }

        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
        }

        fun getLink(episode: Episode, anime: Anime, source: AnimeSource): Observable<Video?> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
            return when {
                isDownloaded -> isDownloaded(episode, anime, source, downloadManager).map {
                    it.firstOrNull()
                }
                source is AnimeHttpSource -> isHttp(episode, source).map {
                    it.firstOrNull()
                }
                source is LocalAnimeSource -> isLocal(episode).map {
                    it.firstOrNull()
                }
                else -> error("source not supported")
            }
        }

        private fun isHttp(episode: Episode, source: AnimeHttpSource): Observable<List<Video>> {
            return source.fetchVideoList(episode)
                .flatMapIterable { it }
                .flatMap {
                    source.fetchUrlFromVideo(it)
                }.toList()
        }

        fun isDownloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager,
        ): Observable<List<Video>> {
            return downloadManager.buildVideo(source, anime.toDomainAnime()!!, episode)
                .onErrorReturn { null }
                .map {
                    if (it == null) {
                        emptyList()
                    } else {
                        listOf(it)
                    }
                }
        }

        fun isLocal(
            episode: Episode,
        ): Observable<List<Video>> {
            return try {
                logcat { episode.url }
                val video = Video(episode.url, "Local source: ${episode.url}", episode.url, Uri.parse(episode.url))
                Observable.just(listOf(video))
            } catch (e: Exception) {
                errorMessage = e.message ?: "error getting links"
                Observable.just(emptyList())
            }
        }
    }
}
