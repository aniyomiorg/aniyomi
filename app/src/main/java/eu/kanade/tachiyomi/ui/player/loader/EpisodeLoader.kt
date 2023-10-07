package eu.kanade.tachiyomi.ui.player.loader

import android.net.Uri
import eu.kanade.domain.items.episode.model.toSEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.fetchUrlFromVideo
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import rx.Observable
import tachiyomi.core.util.system.logcat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.source.local.entries.anime.LocalAnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.lang.Exception

/**
 * Loader used to retrieve the video links for a given episode.
 */
class EpisodeLoader {

    companion object {

        private var errorMessage = ""

        /**
         * Returns an observable list of videos of an [episode] based on the type of [source] used.
         *
         * @param episode the episode being parsed.
         * @param anime the anime of the episode.
         * @param source the source of the anime.
         */
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

        /**
         * Returns true if the given [episode] is downloaded.
         *
         * @param episode the episode being parsed.
         * @param anime the anime of the episode.
         */
        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source, skipCache = true)
        }

        /**
         * Returns an observable list of videos when the [episode] is online.
         *
         * @param episode the episode being parsed.
         * @param source the online source of the episode.
         */
        private fun isHttp(episode: Episode, source: AnimeHttpSource): Observable<List<Video>> {
            return source.fetchVideoList(episode.toSEpisode())
                .flatMapIterable { it }
                .flatMap {
                    source.fetchUrlFromVideo(it)
                }.toList()
        }

        /**
         * Returns an observable list of videos when the [episode] is downloaded.
         *
         * @param episode the episode being parsed.
         * @param anime the anime of the episode.
         * @param source the source of the anime.
         * @param downloadManager the AnimeDownloadManager instance to use.
         */
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

        /**
         * Returns an observable list of videos when the [episode] is from local source.
         *
         * @param episode the episode being parsed.
         */
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
