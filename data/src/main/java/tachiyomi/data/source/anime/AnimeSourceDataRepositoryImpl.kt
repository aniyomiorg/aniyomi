package tachiyomi.data.source.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.source.anime.model.AnimeSourceData
import tachiyomi.domain.source.anime.repository.AnimeSourceDataRepository

class AnimeSourceDataRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeSourceDataRepository {

    override fun subscribeAllAnime(): Flow<List<AnimeSourceData>> {
        return handler.subscribeToList { animesourcesQueries.findAll(animeSourceDataMapper) }
    }

    override suspend fun getAnimeSourceData(id: Long): AnimeSourceData? {
        return handler.awaitOneOrNull {
            animesourcesQueries.findOne(
                id,
                animeSourceDataMapper,
            )
        }
    }

    override suspend fun upsertAnimeSourceData(id: Long, lang: String, name: String) {
        handler.await { animesourcesQueries.upsert(id, lang, name) }
    }
}
