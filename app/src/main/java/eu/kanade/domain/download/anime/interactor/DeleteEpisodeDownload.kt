package eu.kanade.domain.download.anime.interactor

import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import tachiyomi.core.util.lang.withNonCancellableContext
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

class DeleteAnimeDownload(
    private val sourceManager: AnimeSourceManager,
    private val downloadManager: AnimeDownloadManager,
) {

    suspend fun awaitAll(anime: Anime, vararg episodes: Episode) = withNonCancellableContext {
        sourceManager.get(anime.source)?.let { source ->
            downloadManager.deleteEpisodes(episodes.toList(), anime, source)
        }
    }
}
