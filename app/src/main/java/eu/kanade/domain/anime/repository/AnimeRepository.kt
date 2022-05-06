package eu.kanade.domain.anime.repository

import eu.kanade.domain.anime.model.Anime
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>>
}
