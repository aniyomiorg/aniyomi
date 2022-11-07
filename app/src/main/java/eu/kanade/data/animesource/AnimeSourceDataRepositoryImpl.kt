package eu.kanade.data.animesource

import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.domain.animesource.model.AnimeSourceData
import eu.kanade.domain.animesource.repository.AnimeSourceDataRepository
import kotlinx.coroutines.flow.Flow

class AnimeSourceDataRepositoryImpl(
    private val handler: AnimeDatabaseHandler,
) : AnimeSourceDataRepository {

    override fun subscribeAll(): Flow<List<AnimeSourceData>> {
        return handler.subscribeToList { animesourcesQueries.findAll(animesourceDataMapper) }
    }

    override suspend fun getSourceData(id: Long): AnimeSourceData? {
        return handler.awaitOneOrNull { animesourcesQueries.findOne(id, animesourceDataMapper) }
    }

    override suspend fun upsertSourceData(id: Long, lang: String, name: String) {
        handler.await { animesourcesQueries.upsert(id, lang, name) }
    }
}
