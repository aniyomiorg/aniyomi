package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.ui.player.PlayerViewModel.VideoTrack
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun AudioTracksSheet(
    tracks: ImmutableList<VideoTrack>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onAddAudioTrack: () -> Unit,
    onOpenDelayPanel: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        tracks,
        onDismissRequest = onDismissRequest,
        header = {
            AddTrackRow(
                stringResource(MR.strings.player_sheets_add_ext_audio),
                onAddAudioTrack,
                actions = {
                    IconButton(onClick = onOpenDelayPanel) {
                        Icon(Icons.Default.MoreTime, null)
                    }
                },
            )
        },
        track = {
            AudioTrackRow(
                title = getTrackTitle(it),
                isSelected = selectedId == it.id,
                onClick = { onSelect(it.id) },
            )
        },
        modifier = modifier,
    )
}

@Composable
fun AudioTrackRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = MaterialTheme.padding.small, end = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        RadioButton(
            isSelected,
            onClick,
        )
        Text(
            title,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            fontStyle = if (isSelected) FontStyle.Italic else FontStyle.Normal,
        )
    }
}
