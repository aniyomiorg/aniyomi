package tachiyomi.domain.history.manga.model

import java.util.Date

data class MangaHistory(
    val id: Long,
    val chapterId: Long,
    val readAt: Date?,
    val readDuration: Long,
)
