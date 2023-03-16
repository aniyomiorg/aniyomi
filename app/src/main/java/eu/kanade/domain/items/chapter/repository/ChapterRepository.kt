package eu.kanade.domain.items.chapter.repository

import eu.kanade.domain.items.chapter.model.Chapter
import eu.kanade.domain.items.chapter.model.ChapterUpdate
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {

    suspend fun addAllChapters(chapters: List<Chapter>): List<Chapter>

    suspend fun updateChapter(chapterUpdate: ChapterUpdate)

    suspend fun updateAllChapters(chapterUpdates: List<ChapterUpdate>)

    suspend fun removeChaptersWithIds(chapterIds: List<Long>)

    suspend fun getChapterByMangaId(mangaId: Long): List<Chapter>

    suspend fun getBookmarkedChaptersByMangaId(mangaId: Long): List<Chapter>

    suspend fun getChapterById(id: Long): Chapter?

    suspend fun getChapterByMangaIdAsFlow(mangaId: Long): Flow<List<Chapter>>

    suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter?
}
