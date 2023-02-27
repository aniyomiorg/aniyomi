package eu.kanade.data.source.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.source.model.SourceData
import eu.kanade.domain.source.repository.SourceDataRepository
import kotlinx.coroutines.flow.Flow

class MangaSourceDataRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : SourceDataRepository {

    override fun subscribeAllManga(): Flow<List<SourceData>> {
        return handler.subscribeToList { sourcesQueries.findAll(mangaSourceDataMapper) }
    }

    override suspend fun getMangaSourceData(id: Long): SourceData? {
        return handler.awaitOneOrNull { sourcesQueries.findOne(id, mangaSourceDataMapper) }
    }

    override suspend fun upsertMangaSourceData(id: Long, lang: String, name: String) {
        handler.await { sourcesQueries.upsert(id, lang, name) }
    }
}
