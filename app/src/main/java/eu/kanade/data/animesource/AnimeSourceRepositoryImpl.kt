package eu.kanade.data.animesource

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.animesource.model.AnimeSource
import eu.kanade.domain.animesource.repository.AnimeSourceRepository
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeSourceRepositoryImpl(
    private val sourceManager: AnimeSourceManager,
    private val handler: AnimeDatabaseHandler
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
                .map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId).run {
                        animesourceMapper(this)
                    }
                    source to count
                }
                .filterNot { it.first.id == LocalAnimeSource.ID }
        }
    }
}
