package eu.kanade.presentation.updates.anime

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.DotSeparatorText
import eu.kanade.presentation.entries.ItemCover
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadIndicator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesItem
import tachiyomi.domain.updates.anime.model.AnimeUpdatesWithRelations
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.ReadItemAlpha
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.selectedBackground
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.animeUpdatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "animeUpdates-lastUpdated") {
        val time = remember(lastUpdated) {
            val now = Date().time
            if (now - lastUpdated < 1.minutes.inWholeMilliseconds) {
                null
            } else {
                DateUtils.getRelativeTimeSpanString(lastUpdated, now, DateUtils.MINUTE_IN_MILLIS)
            }
        }

        Box(
            modifier = Modifier
                .animateItemPlacement()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        ) {
            Text(
                text = if (time.isNullOrEmpty()) {
                    stringResource(R.string.updates_last_update_info, stringResource(R.string.updates_last_update_info_just_now))
                } else {
                    stringResource(R.string.updates_last_update_info, time)
                },
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.animeUpdatesUiItems(
    uiModels: List<AnimeUpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (AnimeUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (AnimeUpdatesItem) -> Unit,
    onClickUpdate: (AnimeUpdatesItem, altPlayer: Boolean) -> Unit,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
) {
    items(
        items = uiModels,
        contentType = {
            when (it) {
                is AnimeUpdatesUiModel.Header -> "header"
                is AnimeUpdatesUiModel.Item -> "item"
            }
        },
        key = {
            when (it) {
                is AnimeUpdatesUiModel.Header -> "animeUpdatesHeader-${it.hashCode()}"
                is AnimeUpdatesUiModel.Item -> "animeUpdates-${it.item.update.animeId}-${it.item.update.episodeId}"
            }
        },
    ) { item ->
        when (item) {
            is AnimeUpdatesUiModel.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemPlacement(),
                    text = item.date,
                )
            }
            is AnimeUpdatesUiModel.Item -> {
                val updatesItem = item.item
                AnimeUpdatesUiItem(
                    modifier = Modifier.animateItemPlacement(),
                    update = updatesItem.update,
                    selected = updatesItem.selected,
                    watchProgress = updatesItem.update.lastSecondSeen
                        .takeIf { !updatesItem.update.seen && it > 0L }
                        ?.let {
                            stringResource(
                                R.string.episode_progress,
                                formatProgress(it),
                                formatProgress(updatesItem.update.totalSeconds),
                            )
                        },
                    onLongClick = {
                        onUpdateSelected(updatesItem, !updatesItem.selected, true, true)
                    },
                    onClick = {
                        when {
                            selectionMode -> onUpdateSelected(updatesItem, !updatesItem.selected, true, false)
                            else -> onClickUpdate(updatesItem, false)
                        }
                    },
                    onClickCover = { onClickCover(updatesItem) }.takeIf { !selectionMode },
                    onDownloadEpisode = { action: EpisodeDownloadAction ->
                        onDownloadEpisode(listOf(updatesItem), action)
                    }.takeIf { !selectionMode },
                    downloadStateProvider = updatesItem.downloadStateProvider,
                    downloadProgressProvider = updatesItem.downloadProgressProvider,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeUpdatesUiItem(
    modifier: Modifier,
    update: AnimeUpdatesWithRelations,
    selected: Boolean,
    watchProgress: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickCover: (() -> Unit)?,
    onDownloadEpisode: ((EpisodeDownloadAction) -> Unit)?,
    // Download Indicator
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
) {
    val haptic = LocalHapticFeedback.current
    val textAlpha = if (update.seen) ReadItemAlpha else 1f

    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Square(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxHeight(),
            data = update.coverData,
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
        ) {
            Text(
                text = update.animeTitle,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = textAlpha),
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableStateOf(0) }
                if (!update.seen) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = stringResource(R.string.unread),
                        modifier = Modifier
                            .height(8.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (update.bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.action_filter_bookmarked),
                        modifier = Modifier
                            .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = update.episodeName,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = textAlpha),
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false),
                )
                if (watchProgress != null) {
                    DotSeparatorText()
                    Text(
                        text = watchProgress,
                        maxLines = 1,
                        color = LocalContentColor.current.copy(alpha = ReadItemAlpha),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        EpisodeDownloadIndicator(
            enabled = onDownloadEpisode != null,
            modifier = Modifier.padding(start = 4.dp),
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onClick = { onDownloadEpisode?.invoke(it) },
        )
    }
}

private fun formatProgress(milliseconds: Long): String {
    return if (milliseconds > 3600000L) {
        String.format(
            "%d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(milliseconds),
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    } else {
        String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)),
        )
    }
}
