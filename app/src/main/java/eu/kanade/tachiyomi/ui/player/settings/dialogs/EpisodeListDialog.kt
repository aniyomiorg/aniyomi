package eu.kanade.tachiyomi.ui.player.settings.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.DotSeparatorText
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.ui.entries.anime.episodeDecimalFormat
import eu.kanade.tachiyomi.util.lang.toRelativeString
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.ReadItemAlpha
import tachiyomi.presentation.core.components.material.padding
import java.text.DateFormat
import java.util.Date

@Composable
fun EpisodeListDialog(
    displayMode: Long,
    currentEpisodeIndex: Int,
    episodeList: List<Episode>,
    relativeTime: Int,
    dateFormat: DateFormat,
    onBookmarkClicked: (Long?, Boolean) -> Unit,
    onEpisodeClicked: (Long?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val itemScrollIndex = (episodeList.size - currentEpisodeIndex) - 1
    val episodeListState = rememberLazyListState(initialFirstVisibleItemIndex = itemScrollIndex)

    PlayerDialog(
        titleRes = R.string.episodes,
        modifier = Modifier.fillMaxHeight(fraction = 0.8F).fillMaxWidth(fraction = 0.8F),
        onDismissRequest = onDismissRequest,
    ) {
        VerticalFastScroller(
            listState = episodeListState,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                state = episodeListState,
            ) {
                items(
                    items = episodeList.reversed(),
                    key = { "episode-${it.id}" },
                    contentType = { "episode" },
                ) { episode ->

                    val isCurrentEpisode = episode.id == episodeList[currentEpisodeIndex].id

                    val title = if (displayMode == Anime.EPISODE_DISPLAY_NUMBER) {
                        stringResource(
                            R.string.display_mode_episode,
                            episodeDecimalFormat.format(episode.episode_number.toDouble()),
                        )
                    } else {
                        episode.name
                    }

                    val date = episode.date_upload
                        .takeIf { it > 0L }
                        ?.let {
                            Date(it).toRelativeString(
                                context,
                                relativeTime,
                                dateFormat,
                            )
                        } ?: ""

                    EpisodeListItem(
                        episode = episode,
                        isCurrentEpisode = isCurrentEpisode,
                        title = title,
                        date = date,
                        onBookmarkClicked = onBookmarkClicked,
                        onEpisodeClicked = onEpisodeClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: Episode,
    isCurrentEpisode: Boolean,
    title: String,
    date: String?,
    onBookmarkClicked: (Long?, Boolean) -> Unit,
    onEpisodeClicked: (Long?) -> Unit,
) {
    var isBookmarked by remember { mutableStateOf(episode.bookmark) }
    var textHeight by remember { mutableStateOf(0) }

    val bookmarkIcon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.Bookmark
    val bookmarkAlpha = if (isBookmarked) 1f else ReadItemAlpha
    val episodeColor = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val textAlpha = if (episode.seen) ReadItemAlpha else 1f
    val textWeight = if (isCurrentEpisode) FontWeight.Bold else FontWeight.Normal
    val textStyle = if (isCurrentEpisode) FontStyle.Italic else FontStyle.Normal

    val clickBookmark: (Boolean) -> Unit = { bookmarked ->
        episode.bookmark = bookmarked
        isBookmarked = bookmarked
        onBookmarkClicked(episode.id, bookmarked)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onEpisodeClicked(episode.id) })
            .padding(vertical = MaterialTheme.padding.small),
    ) {
        IconButton(onClick = { clickBookmark(!isBookmarked) }) {
            Icon(
                imageVector = bookmarkIcon,
                contentDescription = null,
                tint = episodeColor,
                modifier = Modifier
                    .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp })
                    .alpha(bookmarkAlpha),
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = episodeColor,
                modifier = Modifier.alpha(textAlpha),
                onTextLayout = { textHeight = it.size.height },
                fontWeight = textWeight,
                fontStyle = textStyle,
            )

            Row {
                if (date != null) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = episodeColor,
                        modifier = Modifier.alpha(textAlpha),
                        fontWeight = textWeight,
                        fontStyle = textStyle,
                    )
                    if (episode.scanlator != null) DotSeparatorText()
                }
                if (episode.scanlator != null) {
                    Text(
                        text = episode.scanlator!!,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = episodeColor,
                        modifier = Modifier.alpha(textAlpha),
                        fontWeight = textWeight,
                        fontStyle = textStyle,
                    )
                }
            }
        }
    }
}
