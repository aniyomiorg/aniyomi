package tachiyomi.data.source.anime

import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeSourcePagingSourceType
import tachiyomi.domain.source.anime.repository.AnimeSourceRepository
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.anime.model.AnimeSource as DomainSource

class AnimeSourceRepositoryImpl(
    private val sourceManager: AnimeSourceManager,
    private val handler: AnimeDatabaseHandler,
) : AnimeSourceRepository {

    override fun getAnimeSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources.map {
                mapSourceToDomainSource(it).copy(
                    supportsLatest = it.supportsLatest,
                )
            }
        }
    }

    override fun getOnlineAnimeSources(): Flow<List<DomainSource>> {
        return sourceManager.sources.map { sources ->
            sources
                .filterIsInstance<AnimeHttpSource>()
                .map(::mapSourceToDomainSource)
        }
    }

    override fun getAnimeSourcesWithFavoriteCount(): Flow<List<Pair<DomainSource, Long>>> {
        return combine(
            handler.subscribeToList { animesQueries.getAnimeSourceIdWithFavoriteCount() },
            sourceManager.sources,
        ) { sourceIdWithFavoriteCount, _ -> sourceIdWithFavoriteCount }
            .map {
                it.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    val domainSource = mapSourceToDomainSource(source).copy(
                        isStub = source is StubAnimeSource,
                    )
                    domainSource to count
                }
            }
    }

    override fun searchAnime(
        sourceId: Long,
        query: String,
        filterList: AnimeFilterList,
    ): AnimeSourcePagingSourceType {
        return AnimeSourceSearchPagingSource(sourceManager.getOrStub(sourceId), query, filterList)
    }

    override fun getPopularAnime(sourceId: Long): AnimeSourcePagingSourceType {
        return AnimeSourcePopularPagingSource(sourceManager.getOrStub(sourceId))
    }

    override fun getLatestAnime(sourceId: Long): AnimeSourcePagingSourceType {
        return AnimeSourceLatestPagingSource(sourceManager.getOrStub(sourceId))
    }
}

fun mapSourceToDomainSource(source: AnimeSource): DomainSource = DomainSource(
    id = source.id,
    lang = source.lang,
    name = source.name,
    supportsLatest = false,
    isStub = false,
)
