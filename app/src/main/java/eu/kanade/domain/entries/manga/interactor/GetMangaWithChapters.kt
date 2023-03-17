package eu.kanade.domain.entries.manga.interactor

import eu.kanade.domain.entries.manga.model.Manga
import eu.kanade.domain.entries.manga.repository.MangaRepository
import eu.kanade.domain.items.chapter.model.Chapter
import eu.kanade.domain.items.chapter.repository.ChapterRepository
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
