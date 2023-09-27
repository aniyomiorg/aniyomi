package eu.kanade.presentation.entries.anime.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.DotSeparatorText
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.components.material.ReadItemAlpha
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha
import tachiyomi.presentation.core.util.selectedBackground
import kotlin.math.min

@Composable
fun AnimeEpisodeListItem(
    modifier: Modifier = Modifier,
    title: String,
    date: String?,
    watchProgress: String?,
    scanlator: String?,
    seen: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((EpisodeDownloadAction) -> Unit)?,
    onEpisodeSwipe: (LibraryPreferences.EpisodeSwipeAction) -> Unit,
) {
    val textAlpha = remember(seen) { if (seen) ReadItemAlpha else 1f }
    val textSubtitleAlpha = remember(seen) { if (seen) ReadItemAlpha else SecondaryItemAlpha }

    val episodeSwipeStartEnabled = episodeSwipeStartAction != LibraryPreferences.EpisodeSwipeAction.Disabled
    val episodeSwipeEndEnabled = episodeSwipeEndAction != LibraryPreferences.EpisodeSwipeAction.Disabled

    val dismissState = rememberDismissState()
    val dismissDirections = remember { mutableSetOf<DismissDirection>() }
    var lastDismissDirection: DismissDirection? by remember { mutableStateOf(null) }
    if (lastDismissDirection == null) {
        if (episodeSwipeStartEnabled) {
            dismissDirections.add(DismissDirection.EndToStart)
        }
        if (episodeSwipeEndEnabled) {
            dismissDirections.add(DismissDirection.StartToEnd)
        }
    }
    val animateDismissContentAlpha by animateFloatAsState(
        label = "animateDismissContentAlpha",
        targetValue = if (lastDismissDirection != null) 1f else 0f,
        animationSpec = tween(durationMillis = if (lastDismissDirection != null) 500 else 0),
        finishedListener = {
            lastDismissDirection = null
        },
    )
    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            DismissValue.DismissedToEnd -> {
                lastDismissDirection = DismissDirection.StartToEnd
                val dismissDirectionsCopy = dismissDirections.toSet()
                dismissDirections.clear()
                onEpisodeSwipe(episodeSwipeEndAction)
                dismissState.snapTo(DismissValue.Default)
                dismissDirections.addAll(dismissDirectionsCopy)
            }

            DismissValue.DismissedToStart -> {
                lastDismissDirection = DismissDirection.EndToStart
                val dismissDirectionsCopy = dismissDirections.toSet()
                dismissDirections.clear()
                onEpisodeSwipe(episodeSwipeStartAction)
                dismissState.snapTo(DismissValue.Default)
                dismissDirections.addAll(dismissDirectionsCopy)
            }

            DismissValue.Default -> {}
        }
    }
    SwipeToDismiss(
        state = dismissState,
        directions = dismissDirections,
        background = {
            val backgroundColor = if (episodeSwipeEndEnabled && (dismissState.dismissDirection == DismissDirection.StartToEnd || lastDismissDirection == DismissDirection.StartToEnd)) {
                MaterialTheme.colorScheme.primary
            } else if (episodeSwipeStartEnabled && (dismissState.dismissDirection == DismissDirection.EndToStart || lastDismissDirection == DismissDirection.EndToStart)) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Unspecified
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                if (dismissState.dismissDirection in dismissDirections) {
                    val downloadState = downloadStateProvider()
                    SwipeBackgroundIcon(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .align(Alignment.CenterStart)
                            .alpha(
                                if (dismissState.dismissDirection == DismissDirection.StartToEnd) 1f else 0f,
                            ),
                        tint = contentColorFor(backgroundColor),
                        swipeAction = episodeSwipeEndAction,
                        seen = seen,
                        bookmark = bookmark,
                        downloadState = downloadState,
                    )
                    SwipeBackgroundIcon(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .align(Alignment.CenterEnd)
                            .alpha(
                                if (dismissState.dismissDirection == DismissDirection.EndToStart) 1f else 0f,
                            ),
                        tint = contentColorFor(backgroundColor),
                        swipeAction = episodeSwipeStartAction,
                        seen = seen,
                        bookmark = bookmark,
                        downloadState = downloadState,
                    )
                }
            }
        },
        dismissContent = {
            val animateCornerRatio = if (dismissState.offset.value != 0f) {
                min(
                    dismissState.progress.fraction / .075f,
                    1f,
                )
            } else {
                0f
            }
            val animateCornerShape = (8f * animateCornerRatio).dp
            val dismissContentAlpha =
                if (lastDismissDirection != null) animateDismissContentAlpha else 1f
            Card(
                modifier = modifier,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(animateCornerShape),
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.background.copy(dismissContentAlpha),
                        )
                        .selectedBackground(selected)
                        .alpha(dismissContentAlpha)
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            var textHeight by remember { mutableStateOf(0) }
                            if (!seen) {
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = stringResource(R.string.action_filter_unseen),
                                    modifier = Modifier
                                        .height(8.dp)
                                        .padding(end = 4.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (bookmark) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = stringResource(R.string.action_filter_bookmarked),
                                    modifier = Modifier
                                        .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalContentColor.current.copy(alpha = textAlpha),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textHeight = it.size.height },
                            )
                        }

                        Row {
                            ProvideTextStyle(
                                value = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    color = LocalContentColor.current.copy(alpha = textSubtitleAlpha),
                                ),
                            ) {
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
                                        modifier = Modifier.alpha(ReadItemAlpha),
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

                    if (onDownloadClick != null) {
                        EpisodeDownloadIndicator(
                            enabled = downloadIndicatorEnabled,
                            modifier = Modifier.padding(start = 4.dp),
                            downloadStateProvider = downloadStateProvider,
                            downloadProgressProvider = downloadProgressProvider,
                            onClick = onDownloadClick,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SwipeBackgroundIcon(
    modifier: Modifier = Modifier,
    tint: Color,
    swipeAction: LibraryPreferences.EpisodeSwipeAction,
    seen: Boolean,
    bookmark: Boolean,
    downloadState: AnimeDownload.State,
) {
    val imageVector = when (swipeAction) {
        LibraryPreferences.EpisodeSwipeAction.ToggleSeen -> {
            if (!seen) {
                Icons.Default.Visibility
            } else {
                Icons.Default.VisibilityOff
            }
        }
        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark -> {
            if (!bookmark) {
                Icons.Default.Bookmark
            } else {
                Icons.Default.BookmarkRemove
            }
        }
        LibraryPreferences.EpisodeSwipeAction.Download -> {
            when (downloadState) {
                AnimeDownload.State.NOT_DOWNLOADED,
                AnimeDownload.State.ERROR,
                -> { Icons.Default.Download }
                AnimeDownload.State.QUEUE,
                AnimeDownload.State.DOWNLOADING,
                -> { Icons.Default.FileDownloadOff }
                AnimeDownload.State.DOWNLOADED -> { Icons.Default.Delete }
            }
        }
        LibraryPreferences.EpisodeSwipeAction.Disabled -> {
            null
        }
    }
    imageVector?.let {
        Icon(
            modifier = modifier,
            imageVector = imageVector,
            tint = tint,
            contentDescription = null,
        )
    }
}

@Composable
fun NextEpisodeAiringListItem(
    modifier: Modifier = Modifier,
    title: String,
    date: String,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableStateOf(0) }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier.alpha(SecondaryItemAlpha),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.alpha(SecondaryItemAlpha)) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
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
