package eu.kanade.data.animesource

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.model.AnimeSourcePagingSourceType
import eu.kanade.domain.animesource.model.AnimeSourceWithCount
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeSourceRepositoryImpl(
    private val sourceManager: AnimeSourceManager,
    private val handler: AnimeDatabaseHandler,
) : AnimeSourceRepository {

    override fun getSources(): Flow<List<AnimeSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map(catalogueSourceMapper)
        }
    }

    override fun getOnlineSources(): Flow<List<AnimeSource>> {
        return sourceManager.onlineSources.map { sources ->
            sources.map(animesourceMapper)
        }
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>> {
        val sourceIdWithFavoriteCount = handler.subscribeToList { animesQueries.getAnimeSourceIdWithFavoriteCount() }
        return sourceIdWithFavoriteCount.map { sourceIdsWithCount ->
            sourceIdsWithCount
                .filterNot { it.source == LocalAnimeSource.ID }
                .map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId).run {
                        animesourceMapper(this)
                    }
                    source to count
                }
        }
    }

    override fun getSourcesWithNonLibraryAnime(): Flow<List<AnimeSourceWithCount>> {
        val sourceIdWithNonLibraryAnime = handler.subscribeToList { animesQueries.getSourceIdsWithNonLibraryAnime() }
        return sourceIdWithNonLibraryAnime.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                AnimeSourceWithCount(animesourceMapper(source), count)
            }
        }
    }

    override fun search(
        sourceId: Long,
        query: String,
        filterList: AnimeFilterList,
    ): AnimeSourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return AnimeSourceSearchPagingSource(source, query, filterList)
    }

    override fun getPopular(sourceId: Long): AnimeSourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return AnimeSourcePopularPagingSource(source)
    }

    override fun getLatest(sourceId: Long): AnimeSourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return AnimeSourceLatestPagingSource(source)
    }
}
