package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.animesource.model.Track
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.MPVKtSpacing
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SubtitlesSheet(
    tracks: ImmutableList<Track>,
    selectedTracks: ImmutableList<String>,
    onSelect: (String) -> Unit,
    onAddSubtitle: () -> Unit,
    onOpenSubtitleSettings: () -> Unit,
    onOpenSubtitleDelay: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GenericTracksSheet(
        tracks,
        onDismissRequest = onDismissRequest,
        header = {
            AddTrackRow(
                stringResource(MR.strings.player_sheets_add_ext_sub),
                onAddSubtitle,
                actions = {
                    IconButton(onClick = onOpenSubtitleSettings) {
                        Icon(Icons.Default.Palette, null)
                    }
                    IconButton(onClick = onOpenSubtitleDelay) {
                        Icon(Icons.Default.MoreTime, null)
                    }
                },
            )
        },
        track = { track ->
            SubtitleTrackRow(
                title = getTrackTitle(track),
                selected = selectedTracks.indexOf(track.url),
                onClick = { onSelect(track.url) },
            )
        },
        footer = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.MPVKtSpacing.medium),
                horizontalAlignment = Alignment.Start,
            ) {
                Icon(Icons.Outlined.Info, null)
                Text(stringResource(MR.strings.player_sheets_subtitles_footer_secondary_sid_no_styles))
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SubtitleTrackRow(
    title: String,
    selected: Int, // -1 unselected, otherwise return 0 and 1 for the selected indices
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = MaterialTheme.MPVKtSpacing.smaller, end = MaterialTheme.MPVKtSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            selected > -1,
            onCheckedChange = { _ -> onClick() },
        )
        Text(
            title,
            fontStyle = if (selected > -1) FontStyle.Italic else FontStyle.Normal,
            fontWeight = if (selected > -1) FontWeight.ExtraBold else FontWeight.Normal,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (selected != -1) {
            Text(
                "#${selected + 1}",
                fontStyle = if (selected > -1) FontStyle.Italic else FontStyle.Normal,
                fontWeight = if (selected > -1) FontWeight.ExtraBold else FontWeight.Normal,
            )
        }
    }
}
