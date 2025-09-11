package tachiyomi.domain.source.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.entries.anime.repository.AnimeRepository
import tachiyomi.domain.source.anime.model.DeletableAnime

class GetAnimeSourcesWithNonLibraryAnime(
    private val repository: AnimeRepository,
) {

    fun subscribe(): Flow<List<DeletableAnime>> {
        return repository.getDeletableParentAnime()
    }

    suspend fun getDeletableChildren(parentId: Long): List<Anime> {
        return repository.getChildrenByParentId(parentId)
    }
}
