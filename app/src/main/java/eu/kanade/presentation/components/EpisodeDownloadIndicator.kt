package eu.kanade.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.EpisodeDownloadAction
import eu.kanade.presentation.util.secondaryItemAlpha
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.AnimeDownload

@Composable
fun EpisodeDownloadIndicator(
    modifier: Modifier = Modifier,
    downloadState: AnimeDownload.State,
    downloadProgress: Int,
    onClick: (EpisodeDownloadAction) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
            val isDownloaded = downloadState == AnimeDownload.State.DOWNLOADED
            val isDownloading = downloadState != AnimeDownload.State.NOT_DOWNLOADED
            var isMenuExpanded by remember(downloadState) { mutableStateOf(false) }
            IconButton(
                onClick = {
                    if (isDownloaded || isDownloading) {
                        isMenuExpanded = true
                    } else {
                        onClick(EpisodeDownloadAction.START)
                    }
                },
                onLongClick = {
                    val episodeDownloadAction = when {
                        isDownloaded -> EpisodeDownloadAction.DELETE
                        isDownloading -> EpisodeDownloadAction.CANCEL
                        else -> EpisodeDownloadAction.START_NOW
                    }
                    onClick(episodeDownloadAction)
                },
            ) {
                val indicatorModifier = Modifier
                    .size(IndicatorSize)
                    .padding(IndicatorPadding)
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = indicatorModifier,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_delete)) },
                            onClick = {
                                onClick(EpisodeDownloadAction.DELETE)
                                isMenuExpanded = false
                            },
                        )
                    }
                } else {
                    val inactiveAlphaModifier = if (!isDownloading) Modifier.secondaryItemAlpha() else Modifier
                    val arrowModifier = Modifier
                        .size(IndicatorSize - 7.dp)
                        .then(inactiveAlphaModifier)
                    val arrowColor: Color
                    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant
                    if (isDownloading) {
                        val indeterminate = downloadState == AnimeDownload.State.QUEUE ||
                            (downloadState == AnimeDownload.State.DOWNLOADING && downloadProgress == 0)
                        if (indeterminate) {
                            arrowColor = strokeColor
                            CircularProgressIndicator(
                                modifier = indicatorModifier,
                                color = strokeColor,
                                strokeWidth = IndicatorStrokeWidth,
                            )
                        } else {
                            val animatedProgress by animateFloatAsState(
                                targetValue = downloadProgress / 100f,
                                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                            )
                            arrowColor = if (animatedProgress < 0.5f) {
                                strokeColor
                            } else {
                                MaterialTheme.colorScheme.background
                            }
                            CircularProgressIndicator(
                                progress = animatedProgress,
                                modifier = indicatorModifier,
                                color = strokeColor,
                                strokeWidth = IndicatorSize / 2,
                            )
                        }
                    } else {
                        arrowColor = strokeColor
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = indicatorModifier.then(inactiveAlphaModifier),
                            color = strokeColor,
                            strokeWidth = IndicatorStrokeWidth,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = arrowModifier,
                        tint = arrowColor,
                    )
                    DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_start_downloading_now)) },
                            onClick = {
                                onClick(EpisodeDownloadAction.START_NOW)
                                isMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_cancel)) },
                            onClick = {
                                onClick(EpisodeDownloadAction.CANCEL)
                                isMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private val IndicatorSize = 26.dp
private val IndicatorPadding = 2.dp

// To match composable parameter name when used later
private val IndicatorStrokeWidth = IndicatorPadding
