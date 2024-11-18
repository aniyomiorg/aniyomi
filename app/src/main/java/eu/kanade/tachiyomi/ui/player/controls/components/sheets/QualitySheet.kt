package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.presentation.core.components.material.MPVKtSpacing

@Composable
fun QualitySheet(
    videoList: ImmutableList<Video>,
    currentVideo: Video?,
    onClick: (Video) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        videoList,
        track = {
            VideoTrack(
                it,
                selected = currentVideo == it,
                onClick = { onClick(it) },
            )
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .padding(vertical = MaterialTheme.MPVKtSpacing.medium),
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
                vertical = MaterialTheme.MPVKtSpacing.smaller,
                horizontal = MaterialTheme.MPVKtSpacing.medium,
            ),
    )
}
