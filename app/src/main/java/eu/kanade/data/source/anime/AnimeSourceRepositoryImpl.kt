package eu.kanade.data.source.anime

import eu.kanade.data.handlers.anime.AnimeDatabaseHandler
import eu.kanade.domain.source.anime.model.AnimeSource
import eu.kanade.domain.source.anime.model.AnimeSourcePagingSourceType
import eu.kanade.domain.source.anime.model.AnimeSourceWithCount
import eu.kanade.domain.source.anime.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.source.anime.AnimeSourceManager
import eu.kanade.tachiyomi.source.anime.LocalAnimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeSourceRepositoryImpl(
    private val sourceManager: AnimeSourceManager,
    private val handler: AnimeDatabaseHandler,
) : AnimeSourceRepository {

    override fun getAnimeSources(): Flow<List<AnimeSource>> {
        return sourceManager.catalogueSources.map { sources ->
            sources.map(catalogueAnimeSourceMapper)
        }
    }

    override fun getOnlineAnimeSources(): Flow<List<AnimeSource>> {
        return sourceManager.onlineSources.map { sources ->
            sources.map(animeSourceMapper)
        }
    }

    override fun getAnimeSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>> {
        val sourceIdWithFavoriteCount = handler.subscribeToList { animesQueries.getAnimeSourceIdWithFavoriteCount() }
        return sourceIdWithFavoriteCount.map { sourceIdsWithCount ->
            sourceIdsWithCount
                .filterNot { it.source == LocalAnimeSource.ID }
                .map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId).run {
                        animeSourceMapper(this)
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
                AnimeSourceWithCount(animeSourceMapper(source), count)
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
