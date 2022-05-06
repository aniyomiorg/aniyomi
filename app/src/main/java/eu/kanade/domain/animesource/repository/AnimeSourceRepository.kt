package eu.kanade.domain.animesource.repository

import eu.kanade.domain.animesource.model.AnimeSource
import kotlinx.coroutines.flow.Flow

interface AnimeSourceRepository {

    fun getSources(): Flow<List<AnimeSource>>

    fun getOnlineSources(): Flow<List<AnimeSource>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<AnimeSource, Long>>>
}
