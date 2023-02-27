package eu.kanade.domain.source.repository

import eu.kanade.domain.source.model.Source
import eu.kanade.domain.source.model.SourcePagingSourceType
import eu.kanade.domain.source.model.SourceWithCount
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.Flow

interface SourceRepository {

    fun getMangaSources(): Flow<List<Source>>

    fun getOnlineMangaSources(): Flow<List<Source>>

    fun getMangaSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getMangaSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>

    fun searchManga(sourceId: Long, query: String, filterList: FilterList): SourcePagingSourceType

    fun getPopularManga(sourceId: Long): SourcePagingSourceType

    fun getLatestManga(sourceId: Long): SourcePagingSourceType
}
