package eu.kanade.presentation.history.manga.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import java.util.Date

internal class MangaHistoryWithRelationsProvider : PreviewParameterProvider<MangaHistoryWithRelations> {

    private val simple = MangaHistoryWithRelations(
        id = 1L,
        chapterId = 2L,
        mangaId = 3L,
        title = "Test Title",
        chapterNumber = 10.2,
        readAt = Date(1697247357L),
        readDuration = 123L,
        coverData = tachiyomi.domain.entries.manga.model.MangaCover(
            mangaId = 3L,
            sourceId = 4L,
            isMangaFavorite = false,
            url = "https://example.com/cover.png",
            lastModified = 5L,
        ),
    )

    private val historyWithoutReadAt = MangaHistoryWithRelations(
        id = 1L,
        chapterId = 2L,
        mangaId = 3L,
        title = "Test Title",
        chapterNumber = 10.2,
        readAt = null,
        readDuration = 123L,
        coverData = tachiyomi.domain.entries.manga.model.MangaCover(
            mangaId = 3L,
            sourceId = 4L,
            isMangaFavorite = false,
            url = "https://example.com/cover.png",
            lastModified = 5L,
        ),
    )

    private val historyWithNegativeChapterNumber = MangaHistoryWithRelations(
        id = 1L,
        chapterId = 2L,
        mangaId = 3L,
        title = "Test Title",
        chapterNumber = -2.0,
        readAt = Date(1697247357L),
        readDuration = 123L,
        coverData = tachiyomi.domain.entries.manga.model.MangaCover(
            mangaId = 3L,
            sourceId = 4L,
            isMangaFavorite = false,
            url = "https://example.com/cover.png",
            lastModified = 5L,
        ),
    )

    override val values: Sequence<MangaHistoryWithRelations>
        get() = sequenceOf(simple, historyWithoutReadAt, historyWithNegativeChapterNumber)
}
