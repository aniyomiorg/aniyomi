package tachiyomi.domain.history.manga.model

import tachiyomi.domain.entries.manga.model.MangaCover
import java.util.Date

data class MangaHistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    val title: String,
    val chapterNumber: Float,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: MangaCover,
)
