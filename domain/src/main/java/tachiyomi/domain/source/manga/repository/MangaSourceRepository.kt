package tachiyomi.domain.source.manga.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.manga.model.MangaSourceWithCount
import tachiyomi.domain.source.manga.model.Source

typealias SourcePagingSourceType = PagingSource<Long, SManga>

interface MangaSourceRepository {

    fun getMangaSources(): Flow<List<Source>>

    fun getOnlineMangaSources(): Flow<List<Source>>

    fun getMangaSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getMangaSourcesWithNonLibraryManga(): Flow<List<MangaSourceWithCount>>

    fun searchManga(sourceId: Long, query: String, filterList: FilterList): SourcePagingSourceType

    fun getPopularManga(sourceId: Long): SourcePagingSourceType

    fun getLatestManga(sourceId: Long): SourcePagingSourceType
}
