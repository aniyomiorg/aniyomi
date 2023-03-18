package eu.kanade.data.source.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.source.manga.model.MangaSourceWithCount
import eu.kanade.domain.source.manga.model.Source
import eu.kanade.domain.source.manga.model.SourcePagingSourceType
import eu.kanade.domain.source.manga.repository.MangaSourceRepository
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.manga.LocalMangaSource
import eu.kanade.tachiyomi.source.manga.MangaSourceManager
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MangaSourceRepositoryImpl(
    private val sourceManager: MangaSourceManager,
    private val handler: MangaDatabaseHandler,
) : MangaSourceRepository {

    override fun getMangaSources(): Flow<List<Source>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map(catalogueMangaSourceMapper)
        }
    }

    override fun getOnlineMangaSources(): Flow<List<Source>> {
        return sourceManager.onlineSources.map { sources ->
            sources.map(mangaSourceMapper)
        }
    }

    override fun getMangaSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>> {
        val sourceIdWithFavoriteCount = handler.subscribeToList { mangasQueries.getSourceIdWithFavoriteCount() }
        return sourceIdWithFavoriteCount.map { sourceIdsWithCount ->
            sourceIdsWithCount
                .filterNot { it.source == LocalMangaSource.ID }
                .map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId).run {
                        mangaSourceMapper(this)
                    }
                    source to count
                }
        }
    }

    override fun getMangaSourcesWithNonLibraryManga(): Flow<List<MangaSourceWithCount>> {
        val sourceIdWithNonLibraryManga = handler.subscribeToList { mangasQueries.getSourceIdsWithNonLibraryManga() }
        return sourceIdWithNonLibraryManga.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                MangaSourceWithCount(mangaSourceMapper(source), count)
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
