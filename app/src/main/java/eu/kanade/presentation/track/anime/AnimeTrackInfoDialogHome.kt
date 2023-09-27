package eu.kanade.presentation.track.anime

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.domain.track.anime.model.toDbTrack
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.presentation.track.manga.TrackDetailsItem
import eu.kanade.presentation.track.manga.TrackInfoItemMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.entries.anime.track.AnimeTrackItem
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.presentation.core.components.material.Divider
import tachiyomi.presentation.core.components.material.VerticalDivider
import java.text.DateFormat

private const val UnsetStatusTextAlpha = 0.5F

@Composable
fun AnimeTrackInfoDialogHome(
    trackItems: List<AnimeTrackItem>,
    dateFormat: DateFormat,
    onStatusClick: (AnimeTrackItem) -> Unit,
    onEpisodeClick: (AnimeTrackItem) -> Unit,
    onScoreClick: (AnimeTrackItem) -> Unit,
    onStartDateEdit: (AnimeTrackItem) -> Unit,
    onEndDateEdit: (AnimeTrackItem) -> Unit,
    onNewSearch: (AnimeTrackItem) -> Unit,
    onOpenInBrowser: (AnimeTrackItem) -> Unit,
    onRemoved: (AnimeTrackItem) -> Unit,
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
                val supportsScoring = item.service.animeService.getScoreList().isNotEmpty()
                val supportsReadingDates = item.service.supportsReadingDates
                TrackInfoItem(
                    title = item.track.title,
                    service = item.service,
                    status = item.service.getStatus(item.track.status.toInt()),
                    onStatusClick = { onStatusClick(item) },
                    episodes = "${item.track.lastEpisodeSeen.toInt()}".let {
                        val totalEpisodes = item.track.totalEpisodes
                        if (totalEpisodes > 0) {
                            // Add known total episode count
                            "$it / $totalEpisodes"
                        } else {
                            it
                        }
                    },
                    onEpisodesClick = { onEpisodeClick(item) },
                    score = item.service.animeService.displayScore(item.track.toDbTrack())
                        .takeIf { supportsScoring && item.track.score != 0F },
                    onScoreClick = { onScoreClick(item) }
                        .takeIf { supportsScoring },
                    startDate = remember(item.track.startDate) { dateFormat.format(item.track.startDate) }
                        .takeIf { supportsReadingDates && item.track.startDate != 0L },
                    onStartDateClick = { onStartDateEdit(item) } // TODO
                        .takeIf { supportsReadingDates },
                    endDate = dateFormat.format(item.track.finishDate)
                        .takeIf { supportsReadingDates && item.track.finishDate != 0L },
                    onEndDateClick = { onEndDateEdit(item) }
                        .takeIf { supportsReadingDates },
                    onNewSearch = { onNewSearch(item) },
                    onOpenInBrowser = { onOpenInBrowser(item) },
                    onRemoved = { onRemoved(item) },
                )
            } else {
                TrackInfoItemEmpty(
                    service = item.service,
                    onNewSearch = { onNewSearch(item) },
                )
            }
        }
    }
}

@Composable
private fun TrackInfoItem(
    title: String,
    service: TrackService,
    @StringRes status: Int?,
    onStatusClick: () -> Unit,
    episodes: String,
    onEpisodesClick: () -> Unit,
    score: String?,
    onScoreClick: (() -> Unit)?,
    startDate: String?,
    onStartDateClick: (() -> Unit)?,
    endDate: String?,
    onEndDateClick: (() -> Unit)?,
    onNewSearch: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onRemoved: () -> Unit,
) {
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackLogoIcon(
                service = service,
                onClick = onOpenInBrowser,
            )
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
                )
            }
            VerticalDivider()
            TrackInfoItemMenu(
                onOpenInBrowser = onOpenInBrowser,
                onRemoved = onRemoved,
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
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
                        text = episodes,
                        onClick = onEpisodesClick,
                    )
                    if (onScoreClick != null) {
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier
                                .weight(1f)
                                .alpha(if (score == null) UnsetStatusTextAlpha else 1f),
                            text = score ?: stringResource(R.string.score),
                            onClick = onScoreClick,
                        )
                    }
                }

                if (onStartDateClick != null && onEndDateClick != null) {
                    Divider()
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        TrackDetailsItem(
                            modifier = Modifier.weight(1F),
                            text = startDate,
                            placeholder = stringResource(R.string.track_started_reading_date),
                            onClick = onStartDateClick,
                        )
                        VerticalDivider()
                        TrackDetailsItem(
                            modifier = Modifier.weight(1F),
                            text = endDate,
                            placeholder = stringResource(R.string.track_finished_reading_date),
                            onClick = onEndDateClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackInfoItemEmpty(
    service: TrackService,
    onNewSearch: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackLogoIcon(service)
        TextButton(
            onClick = onNewSearch,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            Text(text = stringResource(R.string.add_tracking))
        }
    }
}
