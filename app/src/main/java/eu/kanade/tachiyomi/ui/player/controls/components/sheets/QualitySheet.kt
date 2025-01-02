package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.presentation.core.components.material.padding

@Composable
fun QualitySheet(
    videoList: ImmutableList<Video>,
    currentVideo: Video?,
    onClick: (Video) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldDismissSheet by remember { mutableStateOf(false) }

    GenericTracksSheet(
        videoList,
        track = {
            VideoTrack(
                it,
                selected = currentVideo == it,
                onClick = {
                    shouldDismissSheet = true
                    onClick(it)
                },
            )
        },
        onDismissRequest = {
            shouldDismissSheet = false
            onDismissRequest()
        },
        dismissEvent = shouldDismissSheet,
        modifier = modifier
            .padding(vertical = MaterialTheme.padding.medium),
    )
}

@Composable
fun VideoTrack(
    video: Video,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = video.quality,
        fontStyle = if (selected) FontStyle.Italic else FontStyle.Normal,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                vertical = MaterialTheme.padding.small,
                horizontal = MaterialTheme.padding.medium,
            ),
    )
}
