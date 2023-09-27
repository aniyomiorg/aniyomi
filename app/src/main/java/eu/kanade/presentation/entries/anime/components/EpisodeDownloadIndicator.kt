package eu.kanade.presentation.entries.anime.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import eu.kanade.presentation.components.ArrowModifier
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.IndicatorModifier
import eu.kanade.presentation.components.IndicatorSize
import eu.kanade.presentation.components.IndicatorStrokeWidth
import eu.kanade.presentation.components.commonClickable
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import tachiyomi.presentation.core.components.material.IconButtonTokens
import tachiyomi.presentation.core.util.secondaryItemAlpha

enum class EpisodeDownloadAction {
    START,
    START_NOW,
    CANCEL,
    DELETE,
    SHOW_QUALITIES,
}

@Composable
fun EpisodeDownloadIndicator(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    onClick: (EpisodeDownloadAction) -> Unit,
) {
    when (val downloadState = downloadStateProvider()) {
        AnimeDownload.State.NOT_DOWNLOADED -> NotDownloadedIndicator(
            enabled = enabled,
            modifier = modifier,
            onClick = onClick,
        )
        AnimeDownload.State.QUEUE, AnimeDownload.State.DOWNLOADING -> DownloadingIndicator(
            enabled = enabled,
            modifier = modifier,
            downloadState = downloadState,
            downloadProgressProvider = downloadProgressProvider,
            onClick = onClick,
        )
        AnimeDownload.State.DOWNLOADED -> DownloadedIndicator(
            enabled = enabled,
            modifier = modifier,
            onClick = onClick,
        )
        AnimeDownload.State.ERROR -> ErrorIndicator(
            enabled = enabled,
            modifier = modifier,
            onClick = onClick,
        )
    }
}

@Composable
private fun NotDownloadedIndicator(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (EpisodeDownloadAction) -> Unit,
) {
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                onLongClick = { onClick(EpisodeDownloadAction.SHOW_QUALITIES) },
                onClick = { onClick(EpisodeDownloadAction.START) },
            )
            .secondaryItemAlpha(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_download_item_24dp),
            contentDescription = stringResource(R.string.manga_download),
            modifier = Modifier.size(IndicatorSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DownloadingIndicator(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    downloadState: AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    onClick: (EpisodeDownloadAction) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                onLongClick = { onClick(EpisodeDownloadAction.CANCEL) },
                onClick = { isMenuExpanded = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val arrowColor: Color
        val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant
        val downloadProgress = downloadProgressProvider()
        val indeterminate = downloadState == AnimeDownload.State.QUEUE ||
            (downloadState == AnimeDownload.State.DOWNLOADING && downloadProgress == 0)
        if (indeterminate) {
            arrowColor = strokeColor
            CircularProgressIndicator(
                modifier = IndicatorModifier,
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
                modifier = IndicatorModifier,
                color = strokeColor,
                strokeWidth = IndicatorSize / 2,
            )
        }
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
        Icon(
            imageVector = Icons.Outlined.ArrowDownward,
            contentDescription = null,
            modifier = ArrowModifier,
            tint = arrowColor,
        )
    }
}

@Composable
private fun DownloadedIndicator(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (EpisodeDownloadAction) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                onLongClick = { isMenuExpanded = true },
                onClick = { isMenuExpanded = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(IndicatorSize),
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
    }
}

@Composable
private fun ErrorIndicator(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (EpisodeDownloadAction) -> Unit,
) {
    Box(
        modifier = modifier
            .size(IconButtonTokens.StateLayerSize)
            .commonClickable(
                enabled = enabled,
                onLongClick = { onClick(EpisodeDownloadAction.START) },
                onClick = { onClick(EpisodeDownloadAction.START) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = stringResource(R.string.download_error),
            modifier = Modifier.size(IndicatorSize),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}
