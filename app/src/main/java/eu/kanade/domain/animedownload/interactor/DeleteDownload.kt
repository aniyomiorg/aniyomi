package eu.kanade.domain.animedownload.interactor

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class DeleteAnimeDownload(
    private val sourceManager: AnimeSourceManager,
    private val downloadManager: AnimeDownloadManager,
) {

    suspend fun awaitAll(anime: Anime, vararg values: Episode) = withContext(NonCancellable) {
        sourceManager.get(anime.source)?.let { source ->
            downloadManager.deleteEpisodes(values.toList(), anime, source)
        }
    }
}
