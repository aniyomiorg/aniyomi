package eu.kanade.presentation.history.anime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.ItemCover
import eu.kanade.presentation.history.manga.MangaHistoryItem
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.util.ThemePreviews

private val HistoryItemHeight = 96.dp

@Composable
fun AnimeHistoryItem(
    modifier: Modifier = Modifier,
    history: AnimeHistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickResume)
            .height(HistoryItemHeight)
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier.fillMaxHeight(),
            data = history.coverData,
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = MaterialTheme.padding.medium, end = MaterialTheme.padding.small),
        ) {
            val textStyle = MaterialTheme.typography.bodyMedium
            Text(
                text = history.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = textStyle,
            )
            val seenAt = remember { history.seenAt?.toTimestampString() ?: "" }
            Text(
                text = if (history.episodeNumber > -1) {
                    stringResource(
                        R.string.recent_anime_time,
                        formatEpisodeNumber(history.episodeNumber),
                        seenAt,
                    )
                } else {
                    seenAt
                },
                modifier = Modifier.padding(top = 4.dp),
                style = textStyle,
            )
        }

        IconButton(onClick = onClickDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(AnimeHistoryWithRelationsProvider::class)
    historyWithRelations: AnimeHistoryWithRelations,
) {
    TachiyomiTheme {
        AnimeHistoryItem(
            history = historyWithRelations,
            onClickCover = {},
            onClickResume = {},
            onClickDelete = {},
        )
    }
}
