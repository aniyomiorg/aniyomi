package eu.kanade.domain.anime.repository

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.anime.model.AnimeUpdate
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {

    suspend fun getAnimeById(id: Long): Anime

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>>

    suspend fun getDuplicateLibraryAnime(title: String, sourceId: Long): Anime?

    suspend fun resetViewerFlags(): Boolean

    suspend fun update(update: AnimeUpdate): Boolean
}
