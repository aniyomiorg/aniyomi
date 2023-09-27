package tachiyomi.data.source.manga

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.domain.source.manga.model.MangaSourceWithCount
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.domain.source.manga.model.StubMangaSource
import tachiyomi.domain.source.manga.repository.MangaSourceRepository
import tachiyomi.domain.source.manga.repository.SourcePagingSourceType
import tachiyomi.domain.source.manga.service.MangaSourceManager

class MangaSourceRepositoryImpl(
    private val sourceManager: MangaSourceManager,
    private val handler: MangaDatabaseHandler,
) : MangaSourceRepository {

    override fun getMangaSources(): Flow<List<Source>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map {
                mangaSourceMapper(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineMangaSources(): Flow<List<Source>> {
        return sourceManager.catalogueSources.map { sources ->
            sources
                .filterIsInstance<HttpSource>()
                .map(mangaSourceMapper)
        }
    }

    override fun getMangaSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>> {
        val sourceIdWithFavoriteCount = handler.subscribeToList { mangasQueries.getSourceIdWithFavoriteCount() }
        return sourceIdWithFavoriteCount.map { sourceIdsWithCount ->
            sourceIdsWithCount
                .map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mangaSourceMapper(source).copy(
                        isStub = source is StubMangaSource,
                    )
                    domainSource to count
                }
        }
    }

    override fun getMangaSourcesWithNonLibraryManga(): Flow<List<MangaSourceWithCount>> {
        val sourceIdWithNonLibraryManga = handler.subscribeToList { mangasQueries.getSourceIdsWithNonLibraryManga() }
        return sourceIdWithNonLibraryManga.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                val domainSource = mangaSourceMapper(source).copy(
                    isStub = source is StubMangaSource,
                )
                MangaSourceWithCount(domainSource, count)
            }
        }
    }

    override fun searchManga(
        sourceId: Long,
        query: String,
        filterList: FilterList,
    ): SourcePagingSourceType {
        val source = sourceManager.get(sourceId) as CatalogueSource
        return SourceSearchPagingSource(source, query, filterList)
    }

    override fun getPopularManga(sourceId: Long): SourcePagingSourceType {
        val source = sourceManager.get(sourceId) as CatalogueSource
        return SourcePopularPagingSource(source)
    }

    override fun getLatestManga(sourceId: Long): SourcePagingSourceType {
        val source = sourceManager.get(sourceId) as CatalogueSource
        return SourceLatestPagingSource(source)
    }
}
