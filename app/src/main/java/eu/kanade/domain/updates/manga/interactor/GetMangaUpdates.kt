package eu.kanade.domain.updates.manga.interactor

import eu.kanade.domain.updates.manga.repository.MangaUpdatesRepository
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import java.util.Calendar

class GetMangaUpdates(
    private val repository: MangaUpdatesRepository,
) {

    fun subscribe(calendar: Calendar): Flow<List<MangaUpdatesWithRelations>> = subscribe(calendar.time.time)

    fun subscribe(after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return repository.subscribeAllMangaUpdates(after)
    }
}
