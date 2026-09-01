package tachiyomi.domain.entries.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.anime.model.AnimeRelationGroup
import tachiyomi.domain.entries.anime.repository.AnimeRelationRepository

class GetRelatedAnime(
    private val relationRepository: AnimeRelationRepository,
) {
    fun subscribe(animeId: Long): Flow<List<AnimeRelationGroup>> {
        return relationRepository.subscribeRelatedAnime(animeId)
    }
}
