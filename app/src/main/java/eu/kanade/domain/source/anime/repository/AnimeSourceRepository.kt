package eu.kanade.domain.source.anime.repository

import eu.kanade.domain.source.anime.model.AnimeSourcePagingSourceType
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.AnimeSourceWithCount

interface AnimeSourceRepository {

    fun getAnimeSources(): Flow<List<AnimeSource>>

    fun getOnlineAnimeSources(): Flow<List<AnimeSource>>

    fun getAnimeSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>>

    fun getSourcesWithNonLibraryAnime(): Flow<List<AnimeSourceWithCount>>

    fun searchAnime(sourceId: Long, query: String, filterList: AnimeFilterList): AnimeSourcePagingSourceType

    fun getPopularAnime(sourceId: Long): AnimeSourcePagingSourceType

    fun getLatestAnime(sourceId: Long): AnimeSourcePagingSourceType
}
