package eu.kanade.tachiyomi.ui.watcher

import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.Link
import eu.kanade.tachiyomi.animesource.model.toEpisodeInfo
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
        fun getLinks(episode: Episode, anime: Anime, source: AnimeSource): List<Link> {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> mutableListOf(downloaded(episode, anime, source, downloadManager))
                source is AnimeHttpSource -> notDownloaded(episode, anime, source)
                source is LocalAnimeSource -> mutableListOf(Link("path", "local"))
                else -> error("no worky")
            }
        }

        fun isDownloaded(episode: Episode, anime: Anime): Boolean {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            return downloadManager.isEpisodeDownloaded(episode, anime, true)
        }

        fun getLink(episode: Episode, anime: Anime, source: AnimeSource): Link {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> downloaded(episode, anime, source, downloadManager)
                source is AnimeHttpSource -> notDownloaded(episode, anime, source).first()
                source is LocalAnimeSource -> Link("path", "local")
                else -> error("no worky")
            }
        }

        fun notDownloaded(episode: Episode, anime: Anime, source: AnimeSource): List<Link> {
            val links = runBlocking {
                return@runBlocking suspendCoroutine<List<Link>> { continuation ->
                    var links: List<Link>
                    launchIO {
                        try {
                            links = source.getVideoList(episode.toEpisodeInfo())
                            continuation.resume(links)
                        } catch (e: Throwable) {
                            withUIContext { throw e }
                        }
                    }
                }
            }
            return links
        }

        fun downloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager
        ): Link {
            val path = runBlocking {
                return@runBlocking suspendCoroutine<Uri> { continuation ->
                    launchIO {
                        val link =
                            downloadManager.buildVideo(source, anime, episode).awaitSingle().uri!!
                        continuation.resume(link)
                    }
                }
            }
            return Link(path.toString(), "download")
        }
    }
}
