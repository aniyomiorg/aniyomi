package eu.kanade.tachiyomi.ui.watcher

import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.Episode
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.util.lang.awaitSingle
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EpisodeLoader {
    companion object {
        fun getLinks(episode: Episode, anime: Anime, source: AnimeSource): List<Video> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> {
                    val link = downloaded(episode, anime, source, downloadManager)
                    if (link != null) {
                        return mutableListOf(link)
                    } else {
                        return emptyList()
                    }
                }
                source is AnimeHttpSource -> notDownloaded(episode, anime, source)
                source is LocalAnimeSource -> {
                    val link = local(episode, source)
                    if (link != null) {
                        return mutableListOf(link)
                    } else {
                        return emptyList()
                    }
                }
                else -> error("source not supported")
            }
        }

        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode, anime, true)
        }

        fun getLink(episode: Episode, anime: Anime, source: AnimeSource): Video? {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> downloaded(episode, anime, source, downloadManager)
                source is AnimeHttpSource -> notDownloaded(episode, anime, source).first()
                source is LocalAnimeSource -> Video("path", "local")
                else -> error("no worky")
            }
        }

        fun notDownloaded(episode: Episode, anime: Anime, source: AnimeSource): List<Video> {
            try {
                val links = runBlocking {
                    return@runBlocking suspendCoroutine<List<Video>> { continuation ->
                        var links: List<Video>
                        launchIO {
                            try {
                                links = source.fetchVideoList(episode).awaitSingle()
                                continuation.resume(links)
                            } catch (e: Throwable) {
                                withUIContext { throw e }
                            }
                        }
                    }
                }
                return links
            } catch (error: Exception) {
                return emptyList()
            }
        }

        fun downloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager
        ): Video? {
            try {
                val path = runBlocking {
                    return@runBlocking suspendCoroutine<Uri> { continuation ->
                        launchIO {
                            val link =
                                downloadManager.buildVideo(source, anime, episode).awaitSingle().uri!!
                            continuation.resume(link)
                        }
                    }
                }
                return Video(path.toString(), "download")
            } catch (error: Exception) {
                return null
            }
        }

        fun local(
            episode: Episode,
            source: LocalAnimeSource
        ): Video? {
            try {
                val link = runBlocking {
                    return@runBlocking suspendCoroutine<Video> { continuation ->
                        launchIO {
                            val link =
                                source.fetchVideoList(episode).awaitSingle().first()
                            continuation.resume(link)
                        }
                    }
                }
                return link
            } catch (error: Exception) {
                return null
            }
        }
    }
}
