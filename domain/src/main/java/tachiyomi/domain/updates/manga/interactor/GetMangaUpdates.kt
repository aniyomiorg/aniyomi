package tachiyomi.domain.updates.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.domain.updates.manga.repository.MangaUpdatesRepository
import java.util.Calendar

class GetMangaUpdates(
    private val repository: MangaUpdatesRepository,
) {

    fun subscribe(calendar: Calendar): Flow<List<MangaUpdatesWithRelations>> = subscribe(calendar.time.time)

    fun subscribe(after: Long): Flow<List<MangaUpdatesWithRelations>> {
        return repository.subscribeAllMangaUpdates(after)
    }
}
