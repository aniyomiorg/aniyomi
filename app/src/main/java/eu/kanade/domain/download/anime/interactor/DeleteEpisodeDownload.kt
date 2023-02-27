package eu.kanade.domain.download.anime.interactor

import eu.kanade.domain.entries.episode.model.Episode
import eu.kanade.domain.items.anime.model.Anime
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.animedownload.AnimeDownloadManager
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext

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
