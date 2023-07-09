package eu.kanade.tachiyomi.ui.player.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.anime.Episode
import eu.kanade.tachiyomi.ui.entries.anime.episodeDecimalFormat
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.presentation.core.components.LazyColumn
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.padding

@Composable
fun EpisodeListDialog(
    displayMode: Long,
    episodeList: List<Episode>,
    // currentEpisodeIndex: Int,
    onEpisodeClicked: (Episode) -> Unit,
    onBookmarkClicked: (Long?, Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val episodeListState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(fraction = 0.8F).fillMaxHeight(fraction = 0.8F),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.episodes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Spacer(modifier = Modifier.height(2.dp))

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
                            val title = if (displayMode == Anime.EPISODE_DISPLAY_NUMBER) {
                                stringResource(
                                    R.string.display_mode_episode,
                                    episodeDecimalFormat.format(episode.episode_number.toDouble()),
                                )
                            } else {
                                episode.name
                            }

                            var isBookmarked by remember { mutableStateOf(episode.bookmark) }

                            val clickBookmark: (Boolean) -> Unit = { bookmarked ->
                                onBookmarkClicked(episode.id, bookmarked)
                                isBookmarked = bookmarked
                            }

                            EpisodeListItem(
                                episode = episode,
                                title = title,
                                clickBookmark = clickBookmark,
                                onEpisodeClicked = onEpisodeClicked,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: Episode,
    title: String,
    clickBookmark: (Boolean) -> Unit,
    onEpisodeClicked: (Episode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onEpisodeClicked(episode) })
            .padding(vertical = MaterialTheme.padding.small),
    ) {
        if (episode.bookmark) {
            IconButton(onClick = { clickBookmark(false) }) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            IconButton(onClick = { clickBookmark(true) }) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    Spacer(modifier = Modifier.width(2.dp))

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = episode.date_fetch.toString(),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
