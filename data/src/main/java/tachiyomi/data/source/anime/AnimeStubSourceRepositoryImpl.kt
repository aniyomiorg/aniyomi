package tachiyomi.data.source.anime

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.domain.source.anime.model.StubAnimeSource
import tachiyomi.domain.source.anime.repository.AnimeStubSourceRepository

class AnimeStubSourceRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeStubSourceRepository {

    override fun subscribeAllAnime(): Flow<List<StubAnimeSource>> {
        return handler.subscribeToList { animesourcesQueries.findAll(animeSourceDataMapper) }
    }

    override suspend fun getStubAnimeSource(id: Long): StubAnimeSource? {
        return handler.awaitOneOrNull {
            animesourcesQueries.findOne(
                id,
                animeSourceDataMapper,
            )
        }
    }

    override suspend fun upsertStubAnimeSource(id: Long, lang: String, name: String) {
        handler.await { animesourcesQueries.upsert(id, lang, name) }
    }
}
