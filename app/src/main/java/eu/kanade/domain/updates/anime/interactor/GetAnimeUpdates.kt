package eu.kanade.domain.updates.anime.interactor

import eu.kanade.domain.updates.anime.model.AnimeUpdatesWithRelations
import eu.kanade.domain.updates.anime.repository.AnimeUpdatesRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class GetAnimeUpdates(
    private val repository: AnimeUpdatesRepository,
) {

    fun subscribe(calendar: Calendar): Flow<List<AnimeUpdatesWithRelations>> = subscribe(calendar.time.time)

    fun subscribe(after: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return repository.subscribeAllAnimeUpdates(after)
    }
}
