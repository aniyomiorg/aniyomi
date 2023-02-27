package eu.kanade.domain.source.manga.repository

import eu.kanade.domain.source.manga.model.MangaSourceWithCount
import eu.kanade.domain.source.manga.model.Source
import eu.kanade.domain.source.manga.model.SourcePagingSourceType
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.Flow

interface MangaSourceRepository {

    fun getMangaSources(): Flow<List<Source>>

    fun getOnlineMangaSources(): Flow<List<Source>>

    fun getMangaSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getMangaSourcesWithNonLibraryManga(): Flow<List<MangaSourceWithCount>>

    fun searchManga(sourceId: Long, query: String, filterList: FilterList): SourcePagingSourceType

    fun getPopularManga(sourceId: Long): SourcePagingSourceType

    fun getLatestManga(sourceId: Long): SourcePagingSourceType
}
