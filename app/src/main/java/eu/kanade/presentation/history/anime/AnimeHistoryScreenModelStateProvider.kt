package eu.kanade.presentation.history.anime

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.ui.history.anime.AnimeHistoryScreenModel
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.random.Random

class AnimeHistoryScreenModelStateProvider : PreviewParameterProvider<AnimeHistoryScreenModel.State> {

    private val multiPage = AnimeHistoryScreenModel.State(
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

    private val shortRecent = AnimeHistoryScreenModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerToday,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val shortFuture = AnimeHistoryScreenModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerTomorrow,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val empty = AnimeHistoryScreenModel.State(
        searchQuery = null,
        list = listOf(),
        dialog = null,
    )

    private val loadingWithSearchQuery = AnimeHistoryScreenModel.State(
        searchQuery = "Example Search Query",
    )

    private val loading = AnimeHistoryScreenModel.State(
        searchQuery = null,
        list = null,
        dialog = null,
    )

    override val values: Sequence<AnimeHistoryScreenModel.State> = sequenceOf(
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
            AnimeHistoryUiModel.Header(LocalDateTime.now().plusDays(1))

        fun header(instantBuilder: (Instant) -> Instant = { it }) =
            AnimeHistoryUiModel.Header(LocalDateTime.from(instantBuilder(Instant.now())))

        fun items() = sequence {
            var count = 1
            while (true) {
                yield(randItem { it.copy(title = "Example Title $count") })
                count += 1
            }
        }

        fun randItem(historyBuilder: (AnimeHistoryWithRelations) -> AnimeHistoryWithRelations = { it }) =
            AnimeHistoryUiModel.Item(
                historyBuilder(
                    AnimeHistoryWithRelations(
                        id = Random.nextLong(),
                        episodeId = Random.nextLong(),
                        animeId = Random.nextLong(),
                        title = "Test Title",
                        episodeNumber = Random.nextDouble(),
                        seenAt = Date.from(Instant.now()),
                        coverData = AnimeCover(
                            animeId = Random.nextLong(),
                            sourceId = Random.nextLong(),
                            isAnimeFavorite = Random.nextBoolean(),
                            url = "https://example.com/cover.png",
                            lastModified = Random.nextLong(),
                        ),
                    ),
                ),
            )
    }
}
