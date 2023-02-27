package eu.kanade.domain.items.manga.interactor

import eu.kanade.domain.entries.chapter.model.Chapter
import eu.kanade.domain.entries.chapter.repository.ChapterRepository
import eu.kanade.domain.items.manga.model.Manga
import eu.kanade.domain.items.manga.repository.MangaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMangaWithChapters(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    suspend fun subscribe(id: Long): Flow<Pair<Manga, List<Chapter>>> {
        return combine(
            mangaRepository.getMangaByIdAsFlow(id),
            chapterRepository.getChapterByMangaIdAsFlow(id),
        ) { manga, chapters ->
            Pair(manga, chapters)
        }
    }

    suspend fun awaitManga(id: Long): Manga {
        return mangaRepository.getMangaById(id)
    }

    suspend fun awaitChapters(id: Long): List<Chapter> {
        return chapterRepository.getChapterByMangaId(id)
    }
}
