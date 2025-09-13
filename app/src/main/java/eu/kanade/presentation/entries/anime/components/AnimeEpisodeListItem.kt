package eu.kanade.presentation.entries.anime.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.LabelOff
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.entries.components.DotSeparatorText
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import tachiyomi.presentation.core.util.selectedBackground

@Composable
fun AnimeEpisodeListItem(
    title: String,
    date: String?,
    watchProgress: String?,
    scanlator: String?,
    summary: String?,
    previewUrl: String?,
    seen: Boolean,
    bookmark: Boolean,
    fillermark: Boolean,
    selected: Boolean,
    isAnyEpisodeSelected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((EpisodeDownloadAction) -> Unit)?,
    onEpisodeSwipe: (LibraryPreferences.EpisodeSwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = getSwipeAction(
        action = episodeSwipeStartAction,
        seen = seen,
        bookmark = bookmark,
        fillermark = fillermark,
        downloadState = downloadStateProvider(),
        background = MaterialTheme.colorScheme.primaryContainer,
        onSwipe = { onEpisodeSwipe(episodeSwipeStartAction) },
    )
    val end = getSwipeAction(
        action = episodeSwipeEndAction,
        seen = seen,
        bookmark = bookmark,
        fillermark = fillermark,
        downloadState = downloadStateProvider(),
        background = MaterialTheme.colorScheme.primaryContainer,
        onSwipe = { onEpisodeSwipe(episodeSwipeEndAction) },
    )

    SwipeableActionsBox(
        modifier = modifier.clipToBounds(),
        startActions = listOfNotNull(start),
        endActions = listOfNotNull(end),
        swipeThreshold = swipeActionThreshold,
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .selectedBackground(selected)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        ) {
            if (previewUrl.isNullOrBlank() && summary.isNullOrBlank()) {
                SimpleEpisodeListItemImpl(
                    title = title,
                    date = date,
                    watchProgress = watchProgress,
                    fillermark = fillermark,
                    scanlator = scanlator,
                    seen = seen,
                    bookmark = bookmark,
                    downloadIndicatorEnabled = downloadIndicatorEnabled,
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onDownloadClick = onDownloadClick,
                )
                return@Row
            }

            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EpisodeThumbnail(previewUrl = previewUrl)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val titleLines = if (previewUrl == null) 1 else 2
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 14.sp),
                                modifier = Modifier.weight(1f),
                                maxLines = titleLines,
                                minLines = titleLines,
                                overflow = TextOverflow.Ellipsis,
                                color = LocalContentColor.current.copy(alpha = if (seen) DISABLED_ALPHA else 1f),
                            )

                            if (previewUrl == null) {
                                BookmarkDownloadIcons(
                                    bookmark = bookmark,
                                    downloadIndicatorEnabled = downloadIndicatorEnabled,
                                    downloadStateProvider = downloadStateProvider,
                                    downloadProgressProvider = downloadProgressProvider,
                                    onDownloadClick = onDownloadClick,
                                )
                            }
                        }

                        EpisodeSummary(
                            seen = seen,
                            isAnyEpisodeSelected = isAnyEpisodeSelected,
                            summary = summary,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EpisodeInformation(
                        seen = seen,
                        date = date,
                        watchProgress = watchProgress,
                        fillermark = fillermark,
                        scanlator = scanlator,
                    )

                    if (previewUrl != null) {
                        BookmarkDownloadIcons(
                            bookmark = bookmark,
                            downloadIndicatorEnabled = downloadIndicatorEnabled,
                            downloadStateProvider = downloadStateProvider,
                            downloadProgressProvider = downloadProgressProvider,
                            onDownloadClick = onDownloadClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SimpleEpisodeListItemImpl(
    title: String,
    date: String?,
    watchProgress: String?,
    fillermark: Boolean,
    scanlator: String?,
    seen: Boolean,
    bookmark: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    onDownloadClick: ((EpisodeDownloadAction) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(if (fillermark) 0.dp else 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = LocalContentColor.current.copy(alpha = if (seen) DISABLED_ALPHA else 1f),
        )

        EpisodeInformation(
            seen = seen,
            date = date,
            watchProgress = watchProgress,
            fillermark = fillermark,
            scanlator = scanlator,
        )
    }

    BookmarkDownloadIcons(
        bookmark = bookmark,
        downloadIndicatorEnabled = downloadIndicatorEnabled,
        downloadStateProvider = downloadStateProvider,
        downloadProgressProvider = downloadProgressProvider,
        onDownloadClick = onDownloadClick,
    )
}

private fun getSwipeAction(
    action: LibraryPreferences.EpisodeSwipeAction,
    seen: Boolean,
    bookmark: Boolean,
    fillermark: Boolean,
    downloadState: AnimeDownload.State,
    background: Color,
    onSwipe: () -> Unit,
): me.saket.swipe.SwipeAction? {
    return when (action) {
        LibraryPreferences.EpisodeSwipeAction.ToggleSeen -> swipeAction(
            icon = if (!seen) Icons.Outlined.Done else Icons.Outlined.RemoveDone,
            background = background,
            isUndo = seen,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark -> swipeAction(
            icon = if (!bookmark) Icons.Outlined.BookmarkAdd else Icons.Outlined.BookmarkRemove,
            background = background,
            isUndo = bookmark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.ToggleFillermark -> swipeAction(
            icon = if (!fillermark) Icons.Outlined.NewLabel else Icons.AutoMirrored.Outlined.LabelOff,
            background = background,
            isUndo = fillermark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.Download -> swipeAction(
            icon = when (downloadState) {
                AnimeDownload.State.NOT_DOWNLOADED, AnimeDownload.State.ERROR -> Icons.Outlined.Download
                AnimeDownload.State.QUEUE, AnimeDownload.State.DOWNLOADING -> Icons.Outlined.FileDownloadOff
                AnimeDownload.State.DOWNLOADED -> Icons.Outlined.Delete
            },
            background = background,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.Disabled -> null
    }
}

@Composable
fun NextEpisodeAiringListItem(
    title: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(SECONDARY_ALPHA),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.alpha(SECONDARY_ALPHA)) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodySmall,
                ) {
                    Text(
                        text = date,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun swipeAction(
    onSwipe: () -> Unit,
    icon: ImageVector,
    background: Color,
    isUndo: Boolean = false,
): me.saket.swipe.SwipeAction {
    return me.saket.swipe.SwipeAction(
        icon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = icon,
                tint = contentColorFor(background),
                contentDescription = null,
            )
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
}

private val swipeActionThreshold = 56.dp

@Composable
private fun EpisodeThumbnail(
    previewUrl: String?,
) {
    val targetWidth = ((LocalConfiguration.current.screenWidthDp * 0.4f).coerceAtMost(250f))
    if (previewUrl != null) {
        ItemCover.Thumb(
            modifier = Modifier
                .width(targetWidth.dp)
                .padding(end = 8.dp),
            data = ImageRequest.Builder(LocalContext.current)
                .data(previewUrl)
                .crossfade(true)
                .build(),
        )
    }
}

@Composable
private fun EpisodeSummary(
    seen: Boolean,
    isAnyEpisodeSelected: Boolean,
    summary: String?,
) {
    var expandSummary by remember { mutableStateOf(false) }
    if (summary != null) {
        Text(
            text = summary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = if (expandSummary) Int.MAX_VALUE else 3,
            minLines = 3,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            overflow = TextOverflow.Ellipsis,
            color = LocalContentColor.current.copy(
                alpha = if (seen) DISABLED_ALPHA else SECONDARY_ALPHA,
            ),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
                .then(
                    if (isAnyEpisodeSelected) {
                        Modifier
                    } else {
                        Modifier.clickable { expandSummary = !expandSummary }
                    },
                ),
        )
    }
}

@Composable
private fun EpisodeInformation(
    seen: Boolean,
    date: String?,
    watchProgress: String?,
    fillermark: Boolean,
    scanlator: String?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val subtitleStyle = MaterialTheme.typography.bodySmall
            .merge(color = LocalContentColor.current.copy(alpha = if (seen) DISABLED_ALPHA else SECONDARY_ALPHA))
        ProvideTextStyle(value = subtitleStyle) {
            if (fillermark) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = stringResource(AYMR.strings.filler),
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = subtitleStyle.alpha),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = stringResource(AYMR.strings.filler),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = subtitleStyle.alpha),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            if (date != null) {
                Text(
                    text = date,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (watchProgress != null || scanlator != null) DotSeparatorText()
            }
            if (watchProgress != null) {
                Text(
                    text = watchProgress,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                )
                if (scanlator != null) DotSeparatorText()
            }
            if (scanlator != null) {
                Text(
                    text = scanlator,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BookmarkDownloadIcons(
    bookmark: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    onDownloadClick: ((EpisodeDownloadAction) -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (bookmark) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        EpisodeDownloadIndicator(
            enabled = downloadIndicatorEnabled,
            modifier = Modifier
                .padding(start = 4.dp),
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onClick = { onDownloadClick?.invoke(it) },
        )
    }
}

@Preview
@Composable
fun AnimeEpisodeListItemPreview() {
    AnimeEpisodeListItem(
        title = "Ep. 1 - To You, 2000 Years in the Future: The Fall of Zhiganshina (1)",
        date = "7/4/13",
        watchProgress = null,
        scanlator = null,
        summary = "As Titans continue to rampage, the townspeople gather at the inner gate. But a new Titan breaks " +
            "through and this one is unlike the others. Source: crunchyroll",
        previewUrl = null,
        seen = false,
        bookmark = false,
        fillermark = true,
        selected = false,
        isAnyEpisodeSelected = false,
        downloadIndicatorEnabled = true,
        downloadStateProvider = { AnimeDownload.State.NOT_DOWNLOADED },
        downloadProgressProvider = { 0 },
        episodeSwipeStartAction = LibraryPreferences.EpisodeSwipeAction.Disabled,
        episodeSwipeEndAction = LibraryPreferences.EpisodeSwipeAction.Disabled,
        onLongClick = {},
        onClick = {},
        onDownloadClick = {},
        onEpisodeSwipe = {},
    )
}
