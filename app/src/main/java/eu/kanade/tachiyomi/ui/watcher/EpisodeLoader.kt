package eu.kanade.tachiyomi.ui.watcher

import android.net.Uri
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
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
        fun getUri(episode: Episode, anime: Anime, source: AnimeSource): String {
            val downloadManager: AnimeDownloadManager = Injekt.get()
            val isDownloaded = downloadManager.isEpisodeDownloaded(episode, anime, true)
            return when {
                isDownloaded -> downloaded(episode, anime, source, downloadManager).toString()
                source is AnimeHttpSource -> notDownloaded(episode, anime, source)
                source is LocalAnimeSource -> "path"
                else -> error("no worky")
            }
        }

        fun notDownloaded(episode: Episode, anime: Anime, source: AnimeSource): String {
            val link = runBlocking {
                return@runBlocking suspendCoroutine<String> { continuation ->
                    var link: String
                    launchIO {
                        try {
                            link = source.getEpisodeLink(episode.toEpisodeInfo())
                            continuation.resume(link)
                        } catch (e: Throwable) {
                            withUIContext { throw e }
                        }
                    }
                }
            }
            return link
        }

        fun downloaded(
            episode: Episode,
            anime: Anime,
            source: AnimeSource,
            downloadManager: AnimeDownloadManager
        ): Uri {
            val path = runBlocking {
                return@runBlocking suspendCoroutine<Uri> { continuation ->
                    launchIO {
                        val link =
                            downloadManager.buildVideo(source, anime, episode).awaitSingle().uri!!
                        continuation.resume(link)
                    }
                }
            }
            return path
        }
    }
}
