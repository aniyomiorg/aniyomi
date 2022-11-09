package eu.kanade.presentation.anime.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.EpisodeDownloadAction
import eu.kanade.presentation.components.EpisodeDownloadIndicator
import eu.kanade.presentation.manga.components.DotSeparatorText
import eu.kanade.presentation.util.ReadItemAlpha
import eu.kanade.presentation.util.SecondaryItemAlpha
import eu.kanade.presentation.util.selectedBackground
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.animedownload.model.AnimeDownload

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
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((EpisodeDownloadAction) -> Unit)?,
) {
    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val textColor = if (bookmark && !seen) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val textAlpha = remember(seen) { if (seen) ReadItemAlpha else 1f }
            val textSubtitleAlpha = remember(seen) { if (seen) ReadItemAlpha else SecondaryItemAlpha }

            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableStateOf(0) }
                if (bookmark) {
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
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier.alpha(textAlpha),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.alpha(textSubtitleAlpha)) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium
                        .copy(color = textColor, fontSize = 12.sp),
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

        // Download view
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
