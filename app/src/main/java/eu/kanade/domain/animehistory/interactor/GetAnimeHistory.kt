package eu.kanade.domain.animehistory.interactor

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import eu.kanade.domain.animehistory.model.AnimeHistoryWithRelations
import eu.kanade.domain.animehistory.repository.AnimeHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetAnimeHistory(
    private val repository: AnimeHistoryRepository,
) {

    fun subscribe(query: String): Flow<PagingData<AnimeHistoryWithRelations>> {
        return Pager(
            PagingConfig(pageSize = 25),
        ) {
            repository.getHistory(query)
        }.flow
    }
}
