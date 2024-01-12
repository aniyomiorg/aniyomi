package tachiyomi.domain.history.manga.model

import java.util.Date

data class MangaHistory(
    val id: Long,
    val chapterId: Long,
    val readAt: Date?,
    val readDuration: Long,
) {
    companion object {
        fun create() = MangaHistory(
            id = -1L,
            chapterId = -1L,
            readAt = null,
            readDuration = -1L,
        )
    }
}
