package tachiyomi.data.source.anime

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.source.anime.model.AnimeSource
import tachiyomi.domain.source.anime.model.AnimeSourceWithCount
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourcePagingSourceType
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import tachiyomi.domain.source.anime.service.AnimeSourceManager

class AnimeSourceRepositoryImpl(
    private val sourceManager: AnimeSourceManager,
    private val handler: AnimeDatabaseHandler,
) : AnimeSourceRepository {

    override fun getAnimeSources(): Flow<List<AnimeSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map {
                animeSourceMapper(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineAnimeSources(): Flow<List<AnimeSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources
                .filterIsInstance<AnimeHttpSource>()
                .map(animeSourceMapper)
        }
    }

    override fun getAnimeSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>> {
        val sourceIdWithFavoriteCount = handler.subscribeToList { animesQueries.getAnimeSourceIdWithFavoriteCount() }
        return sourceIdWithFavoriteCount.map { sourceIdsWithCount ->
            sourceIdsWithCount
                .map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = animeSourceMapper(source).copy(
                        isStub = source is StubAnimeSource,
                    )
                    domainSource to count
                }
        }
    }

    override fun getSourcesWithNonLibraryAnime(): Flow<List<AnimeSourceWithCount>> {
        val sourceIdWithNonLibraryAnime = handler.subscribeToList { animesQueries.getSourceIdsWithNonLibraryAnime() }
        return sourceIdWithNonLibraryAnime.map { sourceId ->
            sourceId.map { (sourceId, count) ->
                val source = sourceManager.getOrStub(sourceId)
                val domainSource = animeSourceMapper(source).copy(
                    isStub = source is StubAnimeSource,
                )
                AnimeSourceWithCount(domainSource, count)
            }
        }
    }

    override fun searchAnime(
        sourceId: Long,
        query: String,
        filterList: AnimeFilterList,
    ): AnimeSourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return AnimeSourceSearchPagingSource(source, query, filterList)
    }

    override fun getPopularAnime(sourceId: Long): AnimeSourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return AnimeSourcePopularPagingSource(source)
    }

    override fun getLatestAnime(sourceId: Long): AnimeSourcePagingSourceType {
        val source = sourceManager.get(sourceId) as AnimeCatalogueSource
        return AnimeSourceLatestPagingSource(source)
    }
}
