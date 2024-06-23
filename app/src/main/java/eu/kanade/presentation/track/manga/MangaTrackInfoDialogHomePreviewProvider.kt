package eu.kanade.presentation.track.manga

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.ui.entries.manga.track.MangaTrackItem
import eu.kanade.test.DummyTracker
import tachiyomi.domain.track.manga.model.MangaTrack
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class MangaTrackInfoDialogHomePreviewProvider :
    PreviewParameterProvider<@Composable () -> Unit> {

    private val aTrack = MangaTrack(
        id = 1L,
        mangaId = 2L,
        trackerId = 3L,
        remoteId = 4L,
        libraryId = null,
        title = "Manage Name On Tracker Site",
        lastChapterRead = 2.0,
        totalChapters = 12L,
        status = 1L,
        score = 2.0,
        remoteUrl = "https://example.com",
        startDate = 0L,
        finishDate = 0L,
    )
    private val trackItemWithoutTrack = MangaTrackItem(
        track = null,
        tracker = DummyTracker(
            id = 1L,
            name = "Example Tracker",
        ),
    )
    private val trackItemWithTrack = MangaTrackItem(
        track = aTrack,
        tracker = DummyTracker(
            id = 2L,
            name = "Example Tracker 2",
        ),
    )

    private val trackersWithAndWithoutTrack = @Composable {
        MangaTrackInfoDialogHome(
            trackItems = listOf(
                trackItemWithoutTrack,
                trackItemWithTrack,
            ),
            dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
            onStatusClick = {},
            onChapterClick = {},
            onScoreClick = {},
            onStartDateEdit = {},
            onEndDateEdit = {},
            onNewSearch = {},
            onOpenInBrowser = {},
            onRemoved = {},
        )
    }

    private val noTrackers = @Composable {
        MangaTrackInfoDialogHome(
            trackItems = listOf(),
            dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
            onStatusClick = {},
            onChapterClick = {},
            onScoreClick = {},
            onStartDateEdit = {},
            onEndDateEdit = {},
            onNewSearch = {},
            onOpenInBrowser = {},
            onRemoved = {},
        )
    }

    override val values: Sequence<@Composable () -> Unit>
        get() = sequenceOf(
            trackersWithAndWithoutTrack,
            noTrackers,
        )
}
