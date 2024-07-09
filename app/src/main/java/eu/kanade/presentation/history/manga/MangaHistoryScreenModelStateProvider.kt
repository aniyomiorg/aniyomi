package eu.kanade.presentation.history.manga

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.ui.history.manga.MangaHistoryScreenModel
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.random.Random

class MangaHistoryScreenModelStateProvider : PreviewParameterProvider<MangaHistoryScreenModel.State> {

    private val multiPage = MangaHistoryScreenModel.State(
        searchQuery = null,
        list =
        listOf(HistoryUiModelExamples.headerToday)
            .asSequence()
            .plus(HistoryUiModelExamples.items().take(3))
            .plus(HistoryUiModelExamples.header { it.minus(1, ChronoUnit.DAYS) })
            .plus(HistoryUiModelExamples.items().take(1))
            .plus(HistoryUiModelExamples.header { it.minus(2, ChronoUnit.DAYS) })
            .plus(HistoryUiModelExamples.items().take(7))
            .toList(),
        dialog = null,
    )

    private val shortRecent = MangaHistoryScreenModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerToday,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val shortFuture = MangaHistoryScreenModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerTomorrow,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val empty = MangaHistoryScreenModel.State(
        searchQuery = null,
        list = listOf(),
        dialog = null,
    )

    private val loadingWithSearchQuery = MangaHistoryScreenModel.State(
        searchQuery = "Example Search Query",
    )

    private val loading = MangaHistoryScreenModel.State(
        searchQuery = null,
        list = null,
        dialog = null,
    )

    override val values: Sequence<MangaHistoryScreenModel.State> = sequenceOf(
        multiPage,
        shortRecent,
        shortFuture,
        empty,
        loadingWithSearchQuery,
        loading,
    )

    private object HistoryUiModelExamples {
        val headerToday = header()
        val headerTomorrow =
            MangaHistoryUiModel.Header(LocalDate.now().plusDays(1))

        fun header(instantBuilder: (Instant) -> Instant = { it }) =
            MangaHistoryUiModel.Header(LocalDate.from(instantBuilder(Instant.now())))

        fun items() = sequence {
            var count = 1
            while (true) {
                yield(randItem { it.copy(title = "Example Title $count") })
                count += 1
            }
        }

        fun randItem(historyBuilder: (MangaHistoryWithRelations) -> MangaHistoryWithRelations = { it }) =
            MangaHistoryUiModel.Item(
                historyBuilder(
                    MangaHistoryWithRelations(
                        id = Random.nextLong(),
                        chapterId = Random.nextLong(),
                        mangaId = Random.nextLong(),
                        title = "Test Title",
                        chapterNumber = Random.nextDouble(),
                        readAt = Date.from(Instant.now()),
                        readDuration = Random.nextLong(),
                        coverData = MangaCover(
                            mangaId = Random.nextLong(),
                            sourceId = Random.nextLong(),
                            isMangaFavorite = Random.nextBoolean(),
                            url = "https://example.com/cover.png",
                            lastModified = Random.nextLong(),
                        ),
                    ),
                ),
            )
    }
}
