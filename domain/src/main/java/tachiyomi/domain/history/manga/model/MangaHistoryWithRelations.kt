package tachiyomi.domain.history.manga.model

import java.util.Date
import tachiyomi.domain.entries.manga.model.MangaCover

data class MangaHistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    val title: String,
    val chapterNumber: Double,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: MangaCover,
)
