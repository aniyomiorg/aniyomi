package tachiyomi.domain.history.manga.model

import java.util.Date

data class MangaHistoryUpdate(
    val chapterId: Long,
    val readAt: Date,
    val sessionReadDuration: Long,
)
