package eu.kanade.domain.animeupdates.interactor

import eu.kanade.domain.animeupdates.model.AnimeUpdatesWithRelations
import eu.kanade.domain.animeupdates.repository.AnimeUpdatesRepository
import eu.kanade.domain.library.service.LibraryPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.util.Calendar

class GetAnimeUpdates(
    private val repository: AnimeUpdatesRepository,
    private val preferences: LibraryPreferences,
) {

    fun subscribe(calendar: Calendar): Flow<List<AnimeUpdatesWithRelations>> = subscribe(calendar.time.time)

    fun subscribe(after: Long): Flow<List<AnimeUpdatesWithRelations>> {
        return repository.subscribeAll(after)
            .onEach { updates ->
                // Set unread chapter count for bottom bar badge
                preferences.unseenUpdatesCount().set(updates.count { !it.seen })
            }
    }
}
