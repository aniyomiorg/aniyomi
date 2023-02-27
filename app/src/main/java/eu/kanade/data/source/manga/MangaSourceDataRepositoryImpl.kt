package eu.kanade.data.source.manga

import eu.kanade.data.handlers.manga.MangaDatabaseHandler
import eu.kanade.domain.source.manga.model.MangaSourceData
import eu.kanade.domain.source.manga.repository.MangaSourceDataRepository
import kotlinx.coroutines.flow.Flow

class MangaSourceDataRepositoryImpl(
    private val handler: MangaDatabaseHandler,
) : MangaSourceDataRepository {

    override fun subscribeAllManga(): Flow<List<MangaSourceData>> {
        return handler.subscribeToList { sourcesQueries.findAll(mangaSourceDataMapper) }
    }

    override suspend fun getMangaSourceData(id: Long): MangaSourceData? {
        return handler.awaitOneOrNull { sourcesQueries.findOne(id, mangaSourceDataMapper) }
    }

    override suspend fun upsertMangaSourceData(id: Long, lang: String, name: String) {
        handler.await { sourcesQueries.upsert(id, lang, name) }
    }
}
