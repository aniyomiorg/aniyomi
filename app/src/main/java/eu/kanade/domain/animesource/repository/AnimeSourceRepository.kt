package eu.kanade.domain.animesource.repository

import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.AnimeSourcePagingSourceType
import eu.kanade.domain.animesource.model.AnimeSourceWithCount
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import kotlinx.coroutines.flow.Flow

interface AnimeSourceRepository {

    fun getSources(): Flow<List<AnimeSource>>

    fun getOnlineSources(): Flow<List<AnimeSource>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>>

    fun getSourcesWithNonLibraryAnime(): Flow<List<AnimeSourceWithCount>>

    fun search(sourceId: Long, query: String, filterList: AnimeFilterList): AnimeSourcePagingSourceType

    fun getPopular(sourceId: Long): AnimeSourcePagingSourceType

    fun getLatest(sourceId: Long): AnimeSourcePagingSourceType
}
