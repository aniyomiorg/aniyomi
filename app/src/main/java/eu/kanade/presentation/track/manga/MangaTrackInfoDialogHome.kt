package eu.kanade.presentation.track.manga

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.MangaTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.ui.entries.manga.track.MangaTrackItem
import eu.kanade.tachiyomi.util.lang.toLocalDate
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter

@Composable
fun MangaTrackInfoDialogHome(
    trackItems: List<MangaTrackItem>,
    dateFormat: DateTimeFormatter,
    onStatusClick: (MangaTrackItem) -> Unit,
    onChapterClick: (MangaTrackItem) -> Unit,
    onScoreClick: (MangaTrackItem) -> Unit,
    onStartDateEdit: (MangaTrackItem) -> Unit,
    onEndDateEdit: (MangaTrackItem) -> Unit,
    onNewSearch: (MangaTrackItem) -> Unit,
    onOpenInBrowser: (MangaTrackItem) -> Unit,
    onRemoved: (MangaTrackItem) -> Unit,
    onCopyLink: (MangaTrackItem) -> Unit,
    onTogglePrivate: (MangaTrackItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        trackItems.forEach { item ->
            if (item.track != null) {
                val supportsScoring = item.tracker.mangaService.getScoreList().isNotEmpty()
                val supportsReadingDates = item.tracker.supportsReadingDates
                val supportsPrivate = item.tracker.supportsPrivateTracking
                TrackInfoItem(
                    title = item.track.title,
                    tracker = item.tracker,
                    status = (item.tracker as? MangaTracker)?.getStatusForManga(item.track.status),
                    onStatusClick = { onStatusClick(item) },
                    chapters = "${item.track.lastChapterRead.toInt()}".let {
                        val totalChapters = item.track.totalChapters
                        if (totalChapters > 0) {
                            // Add known total chapter count
                            "$it / $totalChapters"
                        } else {
                            it
                        }
                    },
                    onChaptersClick = { onChapterClick(item) },
                    score = item.tracker.mangaService.displayScore(item.track)
                        .takeIf { supportsScoring && item.track.score != 0.0 },
                    onScoreClick = { onScoreClick(item) }
                        .takeIf { supportsScoring },
                    startDate = remember(item.track.startDate) {
                        dateFormat.format(
                            item.track.startDate.toLocalDate(),
                        )
                    }
                        .takeIf { supportsReadingDates && item.track.startDate != 0L },
                    onStartDateClick = { onStartDateEdit(item) } // TODO
                        .takeIf { supportsReadingDates },
                    endDate = dateFormat.format(item.track.finishDate.toLocalDate())
                        .takeIf { supportsReadingDates && item.track.finishDate != 0L },
                    onEndDateClick = { onEndDateEdit(item) }
                        .takeIf { supportsReadingDates },
                    onNewSearch = { onNewSearch(item) },
                    onOpenInBrowser = { onOpenInBrowser(item) },
                    onRemoved = { onRemoved(item) },
                    onCopyLink = { onCopyLink(item) },
                    private = item.track.private,
                    onTogglePrivate = { onTogglePrivate(item) }
                        .takeIf { supportsPrivate },
                )
            } else {
                TrackInfoItemEmpty(
                    tracker = item.tracker,
                    onNewSearch = { onNewSearch(item) },
                )
            }
        }
    }
}

@Composable
private fun TrackInfoItem(
    title: String,
    tracker: Tracker,
    status: StringResource?,
    onStatusClick: () -> Unit,
    chapters: String,
    onChaptersClick: () -> Unit,
    score: String?,
    onScoreClick: (() -> Unit)?,
    startDate: String?,
    onStartDateClick: (() -> Unit)?,
    endDate: String?,
    onEndDateClick: (() -> Unit)?,
    onNewSearch: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgedBox(
                badge = {
                    if (private) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.absoluteOffset(x = (-5).dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = stringResource(MR.strings.tracked_privately),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                },
            ) {
                TrackLogoIcon(
                    tracker = tracker,
                    onClick = onOpenInBrowser,
                    onLongClick = onCopyLink,
                )
            }
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .combinedClickable(
                        onClick = onNewSearch,
                        onLongClick = {
                            context.copyToClipboard(title, title)
                        },
                    )
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            VerticalDivider()
            TrackInfoItemMenu(
                onOpenInBrowser = onOpenInBrowser,
                onRemoved = onRemoved,
                onCopyLink = onCopyLink,
                private = private,
                onTogglePrivate = onTogglePrivate,
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            Column {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        text = status?.let { stringResource(it) } ?: "",
                        onClick = onStatusClick,
                    )
                    VerticalDivider()
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        text = chapters,
                        onClick = onChaptersClick,
                    )
                    if (onScoreClick != null) {
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier.weight(1f),
                            text = score,
                            placeholder = stringResource(MR.strings.score),
                            onClick = onScoreClick,
                        )
                    }
                }

                if (onStartDateClick != null && onEndDateClick != null) {
                    HorizontalDivider()
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        TrackDetailsItem(
                            modifier = Modifier.weight(1F),
                            text = startDate,
                            placeholder = stringResource(MR.strings.track_started_reading_date),
                            onClick = onStartDateClick,
                        )
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier.weight(1F),
                            text = endDate,
                            placeholder = stringResource(MR.strings.track_finished_reading_date),
                            onClick = onEndDateClick,
                        )
                    }
                }
            }
        }
    }
}

private const val UNSET_TEXT_ALPHA = 0.5F

@Composable
fun TrackDetailsItem(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxHeight()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text ?: placeholder,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (text == null) UNSET_TEXT_ALPHA else 1f),
        )
    }
}

@Composable
private fun TrackInfoItemEmpty(
    tracker: Tracker,
    onNewSearch: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackLogoIcon(tracker)
        TextButton(
            onClick = onNewSearch,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            Text(text = stringResource(MR.strings.add_tracking))
        }
    }
}

@Composable
fun TrackInfoItemMenu(
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
    onCopyLink: () -> Unit,
    private: Boolean,
    onTogglePrivate: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_open_in_browser)) },
                onClick = {
                    onOpenInBrowser()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_copy_link)) },
                onClick = {
                    onCopyLink()
                    expanded = false
                },
            )
            if (onTogglePrivate != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (private) {
                                    MR.strings.action_toggle_private_off
                                } else {
                                    MR.strings.action_toggle_private_on
                                },
                            ),
                        )
                    },
                    onClick = {
                        onTogglePrivate()
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_remove)) },
                onClick = {
                    onRemoved()
                    expanded = false
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TrackInfoDialogHomePreviews(
    @PreviewParameter(MangaTrackInfoDialogHomePreviewProvider::class)
    content: @Composable () -> Unit,
) {
    TachiyomiPreviewTheme { content() }
}
